package com.example.orderinventory.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.orderinventory.common.enums.OperatorType;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.order.common.constant.orderCreationLimits;
import com.example.orderinventory.order.common.util.OrderNoGenerator;
import com.example.orderinventory.order.dto.OrderCreateItemRequest;
import com.example.orderinventory.order.dto.OrderCreateRequest;
import com.example.orderinventory.order.entity.OrderInfo;
import com.example.orderinventory.order.entity.OrderItem;
import com.example.orderinventory.order.enums.OrderStatus;
import com.example.orderinventory.order.mapper.OrderInfoMapper;
import com.example.orderinventory.order.mapper.OrderItemMapper;
import com.example.orderinventory.order.service.OrderService;
import com.example.orderinventory.order.vo.OrderCreateResponse;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    //TODO 保留
    private void lockStock(OrderCreationContext context , CalculatedOrder calculateOrder,List<Long> productIds ) {
        //锁库存
        productStockMapper.lockStocks(productIds);
        //校验库存快速失败



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




