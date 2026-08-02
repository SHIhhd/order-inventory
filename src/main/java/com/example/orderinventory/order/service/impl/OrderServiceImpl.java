package com.example.orderinventory.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.orderinventory.common.enums.OperatorType;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.order.common.constant.orderCreationLimits;
import com.example.orderinventory.order.common.util.OrderNoGenerator;
import com.example.orderinventory.order.dto.OrderCancelRequest;
import com.example.orderinventory.order.dto.OrderCreateItemRequest;
import com.example.orderinventory.order.dto.OrderCreateRequest;
import com.example.orderinventory.order.entity.OrderInfo;
import com.example.orderinventory.order.entity.OrderItem;
import com.example.orderinventory.order.enums.OrderStatus;
import com.example.orderinventory.order.mapper.OrderInfoMapper;
import com.example.orderinventory.order.mapper.OrderItemMapper;
import com.example.orderinventory.order.mapper.model.OrderDetailQueryResult;
import com.example.orderinventory.order.service.OrderService;
import com.example.orderinventory.order.vo.OrderCancelResponse;
import com.example.orderinventory.order.vo.OrderCreateResponse;
import com.example.orderinventory.order.vo.OrderDetailResponse;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.enums.ProductStatus;
import com.example.orderinventory.product.mapper.ProductMapper;
import com.example.orderinventory.stock.entity.ProductStock;
import com.example.orderinventory.stock.entity.StockFlow;
import com.example.orderinventory.stock.enums.StockFlowBizType;
import com.example.orderinventory.stock.mapper.ProductStockMapper;
import com.example.orderinventory.stock.mapper.StockFlowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @description 针对表【order_info(订单主表：保存订单整体信息)】的数据库操作Service实现
 * @createDate 2026-07-01 14:59:51
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final ProductMapper productMapper;

    private final StockFlowMapper stockFlowMapper;

    private final ProductStockMapper productStockMapper;

    private final OrderInfoMapper orderInfoMapper;

    private final OrderItemMapper orderItemMapper;

    public OrderServiceImpl(ProductMapper productMapper,
                            StockFlowMapper stockFlowMapper,
                            ProductStockMapper productStockMapper,
                            OrderInfoMapper orderInfoMapper,
                            OrderItemMapper orderItemMapper) {
        this.productMapper = productMapper;
        this.stockFlowMapper = stockFlowMapper;
        this.productStockMapper = productStockMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
    }
    //TODO 目前仅在 orderService 中使用，后续如果多处调用，再移到context包中
    private record OrderCreationContext(
            List<OrderCreateItemRequest> sortedItems,
            Map<Long, Product> productsById,
            Map<Long, ProductStock> stockSnapshotsByProductId
    ) {
    }

    private record OrderCancelContext(
            OrderCancelRequest orderCancelRequest,
            String orderNo,
            LocalDateTime now

    ){}

    private record CalculatedOrder(
            long totalAmount,
            int totalQuantity,
            List<CalculatedOrderLine> lines
    ){}
    private record CalculatedOrderLine(
            long productId,
            String productName,
            String skuCode,
            int quantity,
            long salePrice,
            long itemAmount
    ){
    }



    /**
     * TODO 后续将这个方法拆成不同的bean
     * @param orderCreateRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest) {
        LocalDateTime now = LocalDateTime.now();
        //预先排序
        List<OrderCreateItemRequest> sortedItems = normalizeItems(
                orderCreateRequest.items());
        List<Long> productIds = buildProductIds(sortedItems);
        //加载上下文
        OrderCreationContext context = loadCreationContext(sortedItems,productIds);
        //校验参数
        validateOrderItems(context);
        CalculatedOrder calculateOrder = calculateOrder(
                sortedItems,
                context.productsById()
        );
        //TODO 后续调整减库存逻辑-扣减
        //lockStock(context,calculateOrder,productIds);
        //创建订单主表
        OrderInfo orderInfo = createAndPersistOrder(
                orderCreateRequest,
                calculateOrder,
                now);
        //创建订单明细
        createAndPersistOrderItems(orderInfo,calculateOrder,now);
        //扣减库存
        deductStock(sortedItems);
        //创建流水
        createAndPersistStockFlows(calculateOrder,orderInfo,now);
        return OrderCreateResponse.from(orderInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCancelResponse cancelOrder(String orderNo,
                                           OrderCancelRequest orderCancelRequest) {
        /**
         * 根据订单查询商品信息
         * 修改订单状态为 20
         * 商品信息按id排序
         * select   from update 锁库存
         * 库存回滚
         * 插入流水
         */

        LocalDateTime now = LocalDateTime.now();
        OrderCancelContext orderCancelContext =
                new OrderCancelContext(orderCancelRequest,orderNo,now);
        OrderInfo canceledOrder = markOrderCancelled(orderCancelContext);
        Map<Long, OrderItem> orderItemByProductId=
                loadAndValidateOrderItems(canceledOrder);
        List<ProductStock> rollBackProductStocks =
                loadAndLockProductStock(orderItemByProductId);
        rollbackStocks(rollBackProductStocks,now);
        createAndPersistStockFlows(
                canceledOrder.getBuyerId(),
                rollBackProductStocks,
                orderItemByProductId,
                orderCancelContext);
        return new OrderCancelResponse(orderNo,
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.CANCELLED.getMessage(),
                orderCancelRequest.cancelReason(),
                now);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(String orderNo)  {
        OrderDetailQueryResult orderDetail
                = orderInfoMapper.selectOrderDetailByOrderNo(orderNo);
        if(Objects.isNull(orderDetail)){
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        Integer statusCode = orderDetail.getOrderStatus();
        OrderStatus orderStatus = OrderStatus.findByCode(statusCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SYSTEM_ERROR,
                        "订单状态数据非法，orderStatus=" + statusCode
                ));
        return OrderDetailResponse.from(orderDetail,orderStatus);
    }

    private void validateLockedStocks(
            Map<Long, OrderItem> orderItemByProductId,
            List<ProductStock> lockedStocks) {

        Set<Long> expectedIds = orderItemByProductId.keySet();

        Set<Long> actualIds = lockedStocks.stream()
                .map(ProductStock::getProductId)
                .collect(Collectors.toSet());

        if (actualIds.size() != lockedStocks.size()) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "库存查询结果存在重复商品"
            );
        }

        Set<Long> missingIds = new HashSet<>(expectedIds);
        missingIds.removeAll(actualIds);

        if (!missingIds.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.STOCK_NOT_FOUND,
                    "库存记录不存在，productIds=" + missingIds
            );
        }

        Set<Long> unexpectedIds = new HashSet<>(actualIds);
        unexpectedIds.removeAll(expectedIds);

        if (!unexpectedIds.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "库存查询包含非订单商品，productIds=" + unexpectedIds
            );
        }
    }

    private void rollbackStocks(List<ProductStock> productStocks, LocalDateTime now) {
        int batchRollbackStocks = productStockMapper
                .batchRollbackStocks(productStocks,now);
        assertAffectedRows(productStocks,batchRollbackStocks);
    }


    private Map<Long, OrderItem>  loadAndValidateOrderItems(OrderInfo orderInfo) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderNo, orderInfo.getOrderNo())
                        .orderByAsc(OrderItem::getProductId)
        );

        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_EMPTY);
        }

        validateOrderItemFields(orderInfo, orderItems);
        validateOrderItemSummary(orderInfo, orderItems);

        return orderItems.stream().collect(
                Collectors.toMap(OrderItem::getProductId, Function.identity())
        );
    }


    private void validateOrderItemFields(
            OrderInfo orderInfo,
            List<OrderItem> orderItems) {

        Set<Long> productIds = new HashSet<>(orderItems.size());

        for (OrderItem item : orderItems) {
            if (!Objects.equals(item.getOrderId(), orderInfo.getId())
                    || !Objects.equals(item.getOrderNo(), orderInfo.getOrderNo())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "订单明细归属关系错误");
            }

            if (item.getProductId() == null || item.getProductId() <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "订单明细商品ID非法");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "订单明细数量非法，productId=" + item.getProductId());
            }

            if (!StringUtils.hasText(item.getSkuCode())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "订单明细SKU快照为空，productId=" + item.getProductId());
            }

            if (!productIds.add(item.getProductId())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "订单存在重复商品明细，productId=" + item.getProductId());
            }
        }
    }
    private void validateOrderItemSummary(
            OrderInfo orderInfo,
            List<OrderItem> orderItems) {

        int calculatedTotalQuantity = 0;
        long calculatedTotalAmount = 0L;

        try {
            for (OrderItem item : orderItems) {
                long expectedItemAmount = Math.multiplyExact(
                        item.getSalePrice(),
                        item.getQuantity().longValue()
                );

                if (!Objects.equals(item.getItemAmount(), expectedItemAmount)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                            "订单明细金额不一致，productId=" + item.getProductId());
                }

                calculatedTotalQuantity = Math.addExact(
                        calculatedTotalQuantity,
                        item.getQuantity()
                );

                calculatedTotalAmount = Math.addExact(
                        calculatedTotalAmount,
                        item.getItemAmount()
                );
            }
        } catch (ArithmeticException exception) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "订单明细汇总计算溢出",
                    exception
            );
        }

        if (!Objects.equals(
                orderInfo.getTotalQuantity(),
                calculatedTotalQuantity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "订单明细总数量与订单主表不一致");
        }

        if (!Objects.equals(
                orderInfo.getTotalAmount(),
                calculatedTotalAmount)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "订单明细总金额与订单主表不一致");
        }
    }


    /**
     * 插入流水
     */
    private void createAndPersistStockFlows(Long buyerId,
                                            List<ProductStock> productStocks,
                                            Map<Long, OrderItem>  orderItemByProductId,
                                            OrderCancelContext orderCancelContext) {
        List<StockFlow>  stockFlows = new ArrayList<>(productStocks.size());
        String orderNo = orderCancelContext.orderNo();
        LocalDateTime now = orderCancelContext.now();
        for (ProductStock productStock : productStocks) {
            Long productId = productStock.getProductId();
            OrderItem orderItem = orderItemByProductId.get(productId);
            Integer quantity = orderItem.getQuantity();
            StockFlow stockFlow = new StockFlow();
            stockFlow.setId(IdWorker.getId());
            stockFlow.setProductId(productId);
            stockFlow.setSkuCode(orderItem.getSkuCode());
            stockFlow.setBizNo(orderNo);
            stockFlow.setBizType(StockFlowBizType.ORDER_CANCEL_ROLLBACK.getCode());
            stockFlow.setChangeQuantity(quantity);
            stockFlow.setBeforeQuantity(productStock.getAvailableQuantity() - quantity);
            stockFlow.setAfterQuantity(productStock.getAvailableQuantity());
            stockFlow.setOperatorType(OperatorType.USER.getCode());
            stockFlow.setOperatorId(buyerId);
            stockFlow.setRemark(orderCancelContext.orderCancelRequest().cancelReason());
            stockFlow.setCreateTime(now);
            stockFlow.setUpdateTime(now);
            stockFlows.add(stockFlow);
        }
        int insertedFlowRows = stockFlowMapper.batchInsert(stockFlows);
        assertAffectedRows(stockFlows,insertedFlowRows);
    }

    /**
     * 锁库存，内存获取回滚后的库存信息
     * @param
     * @return
     */
    private List<ProductStock> loadAndLockProductStock(
            Map<Long, OrderItem> orderItemByProductId){
        List<Long> productIds = orderItemByProductId.keySet()
                .stream()
                .sorted()
                .toList();
        if(productIds.size()==0){
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                    "商品不存在");
        }
        List<ProductStock> lockedStockSnapshots =
                productStockMapper.selectByProductIdsForUpdate(productIds);
        validateLockedStocks(orderItemByProductId, lockedStockSnapshots);
        List<ProductStock> productStocks = lockedStockSnapshots
                .stream().map(productStock -> {
            Long productId = productStock.getProductId();
            int changeQuantity = orderItemByProductId.get(productId).getQuantity();
            int beforeQuantity = productStock.getAvailableQuantity();
            Integer totalOutQuantity = productStock.getTotalOutQuantity();
            if (totalOutQuantity < changeQuantity) {
                throw new BusinessException(
                        ErrorCode.CONCURRENT_UPDATE_FAILED,
                        "累计出库量小于订单回补量"
                );
            }
            /**
             * 风险：
             * 历史库存异常时，total_out_quantity < quantity 会产生负数。
             * availableQuantity 接近 Integer.MAX_VALUE 时，加法会溢出为负数。
             * Java int 溢出不会抛异常，只会按补码截断。
             * 数据库没有非负 CHECK 约束，错误值可以落库。
             */
            int afterAvailable;
            int afterTotalOut;
            try {
                afterAvailable = Math.addExact(beforeQuantity, changeQuantity);
                afterTotalOut = Math.subtractExact(totalOutQuantity, changeQuantity);
            }catch (ArithmeticException exception){
                throw new BusinessException(
                        ErrorCode.PARAM_ERROR,
                        "回补后库存量超出范围或累计累计出库量小于回补量",
                        exception
                );
            }

            productStock.setTotalOutQuantity(afterTotalOut);
            productStock.setAvailableQuantity(afterAvailable);
            return productStock;
        }).toList();

        return productStocks;
    }




    private OrderInfo markOrderCancelled(OrderCancelContext orderCancelContext){
        /**
         * 这里其实直接使用mybatis自带的updateById,使用乐观锁也可以实现高并发修改
         * 这种依赖存在以下隐患：
         * 后续改成自定义 Mapper SQL 时忘记加 version。
         * 实体查询没有查出 version。
         * 乐观锁插件配置被移除或失效。
         * 其他服务、脚本直接更新订单，没有经过当前 Service。
         * 后续新增新的订单状态，Java 判断遗漏。
         * 阅读 SQL 时无法直接确认状态迁移是否合法。
         * 生产核心状态机应该使用业务条件进行第一层保护，乐观锁可以作为附加保护，而不应该替代状态条件。
         */
        int affectedOrderRows = orderInfoMapper.cancelCreatedOrder(
                orderCancelContext.orderNo(),
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.CREATED.getCode(),
                orderCancelContext.orderCancelRequest().cancelReason(),
                orderCancelContext.now()
                );
        //这里如果返回值为0 有两种可能， ①订单不存在  ②状态有问题
        OrderInfo currentOrder =  orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderCancelContext.orderNo())
        );
        if (affectedOrderRows != 1) {


            if (currentOrder == null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }

            OrderStatus status
                    = OrderStatus.findByCode(currentOrder.getOrderStatus()).get();
            if (status == null || !status.canCancel()) {
                throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
            }

            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED);
        }
        return currentOrder;
    }




    private OrderCreationContext loadCreationContext(List<OrderCreateItemRequest> sortedItems,
                                                     List<Long> productIds) {
        //TODO 商品信息在获得库存锁之前批量读取,若下单过程中商品状态或价格变更会导致不一致，后续再修改
        Map<Long, Product> productsById = loadProductsById(productIds);
        Map<Long, ProductStock> stockSnapshotsByProductId =
                loadStocksByProductId(productIds);
        return new OrderCreationContext(
                sortedItems,
                productsById,
                stockSnapshotsByProductId);
    }

    private  List<Long> buildProductIds(List<OrderCreateItemRequest> sortedItems){
        return  sortedItems.stream()
                .map(OrderCreateItemRequest::productId)
                .toList();
    }
    private List<OrderCreateItemRequest> normalizeItems(List<OrderCreateItemRequest> items) {
        /**
         * 【学习】
         * 所有订单按 productId 升序获取库存行锁，降低多商品订单死锁概率。
         * 不一致的顺序：
         * 线程1：先锁 商品A，再锁 商品B
         * 线程2：先锁 商品B，再锁 商品A
         * 可能出现：
         * 线程1 已持有 商品A，等待 商品B
         * 线程2 已持有 商品B，等待 商品A
         */
        return  items.stream()
                .sorted(Comparator.comparing(
                        OrderCreateItemRequest::productId))
                .toList();
    }

    private void deductStock(List<OrderCreateItemRequest> sortedItems) {
        for (OrderCreateItemRequest itemDTO : sortedItems) {
            Integer quantity = itemDTO.quantity();
            Long productId = itemDTO.productId();
            /**
             * 变更库存【学习】
             * 首先执行库存扣减，给该行记录加上  X锁，必须等事务提交后才会释放🔒
             * 完成库存扣减后，直接对流水进行插入，插入的流水数据一定是准确的
             * 不存在当前事务还没有提交，对应商品的库存数量发生变动，导致流程数据不准确
             *
             */
            int affectedStockRows = productStockMapper.deductStock(productId,quantity);
            if(affectedStockRows == 0){
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }


        }
    }


    private CalculatedOrder calculateOrder(List<OrderCreateItemRequest> items,
                                           Map<Long, Product> productsById) {
        long totalAmount= 0L;
        int totalQuantity = 0;
        List<CalculatedOrderLine> lines = new ArrayList<>(items.size());
        for (OrderCreateItemRequest item : items) {
            Long productId = item.productId();
            Product product = productsById.get(productId);
            Integer quantity = item.quantity();


            try {
                /**
                 * 以下的计算使用Math的api，可以防止溢出
                 * salePrice * quantity
                 * 发生溢出时会静默截断，可能变成负数；而：
                 * Math.multiplyExact(salePrice, quantity)
                 * 发生溢出会抛出 ArithmeticException，事务随之回滚。
                 * 同理Math.addExact(totalQuantity,item.getQuantity())
                 * 发生溢出会抛出 ArithmeticException
                 *
                 */
                Long itemAmount = Math.multiplyExact(
                        product.getSalePrice(),
                        quantity);
                totalAmount = Math.addExact(
                        totalAmount,
                        itemAmount
                );
                totalQuantity = Math.addExact(
                        totalQuantity,
                        item.quantity()
                );
                lines.add(new CalculatedOrderLine(
                        product.getId(),
                        product.getProductName(),
                        product.getSkuCode(),
                        quantity,
                        product.getSalePrice(),
                        itemAmount
                ));
            } catch (ArithmeticException exception) {
                throw new BusinessException(
                        ErrorCode.PARAM_ERROR,
                        "订单总金额超出系统允许范围",
                        exception
                );
            }
        }
        if (totalAmount > orderCreationLimits.MAX_ORDER_AMOUNT) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "订单总金额超出系统允许范围"
            );
        }
        return new CalculatedOrder(
                totalAmount,
                totalQuantity,
                lines
        );

    }


    private void assertAffectedRows(List<?> batch, int insertedRows) {
        if(batch.size() != insertedRows){
            throw  new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED);
        }
    }
    private void assertAffectedSingleRow(int insertedRows, String errorMessage) {
        if(1 != insertedRows){
            throw  new BusinessException(
                    ErrorCode.CONCURRENT_UPDATE_FAILED,
                    errorMessage);
        }
    }

    private OrderInfo createAndPersistOrder(OrderCreateRequest orderCreateRequest,
                                            CalculatedOrder calculation,
                                            LocalDateTime now){
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(OrderNoGenerator.generateOrderNo());
        orderInfo.setBuyerId(orderCreateRequest.buyerId());
        orderInfo.setRequestId(orderCreateRequest.requestId());
        orderInfo.setTotalQuantity(calculation.totalQuantity());
        orderInfo.setTotalAmount(calculation.totalAmount());
        orderInfo.setOrderStatus(OrderStatus.CREATED.getCode());
        orderInfo.setRemark(orderCreateRequest.remark());
        orderInfo.setUpdateTime(now);
        orderInfo.setCreateTime(now);
        int insertedOrderRows = orderInfoMapper.insert(orderInfo);
        assertAffectedSingleRow(insertedOrderRows,"创建订单主表失败");
        return orderInfo;
    }

    private void createAndPersistStockFlows(CalculatedOrder orderCalculation,
                                            OrderInfo orderInfo,
                                            LocalDateTime now) {
        List<CalculatedOrderLine> lines = orderCalculation.lines();
        ArrayList<StockFlow> stockFlowList = new ArrayList<>(lines.size());
        /**
         * 【学习】
         * 这里是查询库存所有的字段，但是其实只用到了available_quantity
         * 需要一个轻量查询
         */
        Map<Long, ProductStock> deductedStocksByProductId =
                loadStocksByProductId(
                        lines.stream()
                                .map(CalculatedOrderLine::productId)
                                .toList()
                );
        for (CalculatedOrderLine line : lines) {
            long productId = line.productId();
            Integer quantity = line.quantity();
            // 由于这里的库存信息是扣减后的，所以afterQuantity等于当前库存
            ProductStock productStock = deductedStocksByProductId.get(productId);
            Integer afterQuantity = productStock.getAvailableQuantity();
            Integer beforeQuantity = afterQuantity + quantity;
            StockFlow stockFlow = new StockFlow();
            stockFlow.setId(IdWorker.getId());
            stockFlow.setProductId(productId);
            stockFlow.setSkuCode(line.skuCode());
            stockFlow.setBizNo(orderInfo.getOrderNo());
            stockFlow.setBizType(StockFlowBizType.ORDER_DEDUCT.getCode());
            stockFlow.setChangeQuantity(quantity * (-1));
            stockFlow.setBeforeQuantity(beforeQuantity);
            stockFlow.setAfterQuantity(afterQuantity);
            //TODO 第一版操作人默认是用户，后续再调整
            stockFlow.setOperatorType(OperatorType.USER.getCode());
            stockFlow.setOperatorId(orderInfo.getBuyerId());
            stockFlow.setCreateTime(now);
            stockFlow.setUpdateTime(now);
            stockFlowList.add(stockFlow);
        }
        //插入流水表
        int insertedFlowRows = stockFlowMapper.batchInsert(stockFlowList);
        assertAffectedRows(stockFlowList,insertedFlowRows);


    }

    private void createAndPersistOrderItems(OrderInfo orderInfo,
                                            CalculatedOrder orderCalculation,
                                            LocalDateTime now){
        List<CalculatedOrderLine> lines = orderCalculation.lines();
        ArrayList<OrderItem> orderItemBatch = new ArrayList<>(lines.size());
        for (CalculatedOrderLine line : lines) {
            OrderItem orderItem = new OrderItem();
            //因为使用xml进行插入，这里使用雪花算法生成id
            orderItem.setId(IdWorker.getId());
            orderItem.setOrderId(orderInfo.getId());
            orderItem.setOrderNo(orderInfo.getOrderNo());
            orderItem.setProductId(line.productId());
            orderItem.setSkuCode(line.skuCode());
            orderItem.setProductName(line.productName());
            orderItem.setSalePrice(line.salePrice());
            orderItem.setQuantity(line.quantity());
            orderItem.setItemAmount(line.itemAmount());
            orderItem.setCreateTime(now);
            orderItem.setUpdateTime(now);
            orderItemBatch.add(orderItem);
        }
        int insertedItemRows = orderItemMapper.batchInsert(orderItemBatch);
        assertAffectedRows(orderItemBatch,insertedItemRows);
    }

    private void validateOrderItems(OrderCreationContext context){
        List<OrderCreateItemRequest> sortedItems = context.sortedItems();
        Map<Long, Product> productsById = context.productsById();
        Map<Long, ProductStock> stockSnapshotsByProductId =
                context.stockSnapshotsByProductId();
        for (OrderCreateItemRequest request : sortedItems) {
            Long productId = request.productId();
            Product productSnapshot = productsById.get(productId);
            if (productSnapshot == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "不存在ID为：" + productId + "的商品！");
            }
            if (!Objects.equals(productSnapshot.getProductStatus(),
                    ProductStatus.ON_SHELF.getCode())) {
                throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID,
                        "商品ID为：" + productId + "的商品，状态非法！");
            }
            ProductStock stockSnapshot = stockSnapshotsByProductId.get(productId);
            if (stockSnapshot == null) {
                throw new BusinessException(ErrorCode.STOCK_NOT_FOUND,
                        "商品ID为：" + productId + "的"
                                + ErrorCode.STOCK_NOT_FOUND.getMessage());
            }
        }
    }

    private  Map<Long, Product> loadProductsById(
            List<Long> productIds){
        List<Product> productsList = productMapper.selectByIds(productIds);
        return productsList.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

    }

    private Map<Long , ProductStock> loadStocksByProductId(
            List<Long> productIds){
        LambdaQueryWrapper<ProductStock> productStockLambdaQueryWrapper
                = new LambdaQueryWrapper<>();
        productStockLambdaQueryWrapper
                .in(ProductStock::getProductId,productIds);
        List<ProductStock> productStocks =
                productStockMapper.selectList(productStockLambdaQueryWrapper);
        return productStocks.stream()
                .collect(Collectors.toMap(ProductStock::getProductId,Function.identity()));
    }



}




