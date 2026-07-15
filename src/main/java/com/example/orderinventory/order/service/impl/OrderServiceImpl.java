package com.example.orderinventory.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.orderinventory.common.enums.OperatorType;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.order.common.constant.OrderLimits;
import com.example.orderinventory.order.common.util.OrderNoGenerator;
import com.example.orderinventory.order.dto.OrderCreateRequest;
import com.example.orderinventory.order.entity.OrderInfo;
import com.example.orderinventory.order.entity.OrderItem;
import com.example.orderinventory.order.enums.OrderStatus;
import com.example.orderinventory.order.mapper.OrderInfoMapper;
import com.example.orderinventory.order.mapper.OrderItemMapper;
import com.example.orderinventory.order.service.OrderService;
import com.example.orderinventory.order.vo.OrderCreateVO;
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
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Administrator
 * @description 针对表【order_info(订单主表：保存订单整体信息)】的数据库操作Service实现
 * @createDate 2026-07-01 14:59:51
 */
@Service
public class OrderServiceImpl implements OrderService {

    public static final int INSERT_BATCH_SIZE = 100;

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

    /**
     * TODO 后续将这个方法拆成不同的bean
     * @param orderCreateRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrder(OrderCreateRequest orderCreateRequest) {
        /**
         * 【学习】
         * 必须保证所有订单采用一致的加锁顺序
         * 不一致的顺序：
         * 线程1：先锁 商品A，再锁 商品B
         * 线程2：先锁 商品B，再锁 商品A
         * 可能出现：
         * 线程1 已持有 商品A，等待 商品B
         * 线程2 已持有 商品B，等待 商品A
         */
        LocalDateTime now = LocalDateTime.now();
        List<OrderCreateRequest.ItemsDTO> sortedItems
                = orderCreateRequest.getItems().stream()
                .sorted(Comparator.comparing(
                        OrderCreateRequest.ItemsDTO::getProductId))
                .toList();

        /**
         * 【学习】
         * 查询并封装所有的商品，这里是批量查询
         * 减少数据库IO
         */
        //TODO 商品信息在获得库存锁之前批量读取,若下单过程中商品状态或价格变更会导致不一致，后续再修改
        Map<Long, Product> idToProductMap = getIdToProductMap(sortedItems);
        Map<Long, ProductStock> productIdToStockMap = getProductIdToStockMap(sortedItems);
        //校验参数
        validateOrderItems(sortedItems,idToProductMap,productIdToStockMap);
        //创建订单主表
        OrderInfo orderInfo = buildAndInsertOrderInfo(orderCreateRequest, sortedItems,idToProductMap);
        /**
         * 【学习】
         * 不要一边遍历一边插入数据库，会导致网络 I/O 爆炸和数据库事务/日志频繁刷盘
         * 这里选择批量插入
         */
        buildAndInsertOrderItems(sortedItems,orderInfo,idToProductMap,now);
        deductStock(sortedItems);
        //读取最新的库存信息
        buildAndInsertStockFlowList(sortedItems,
                orderInfo,
                idToProductMap,
                now);
        return OrderCreateVO.from(orderInfo);
    }

    private void deductStock(List<OrderCreateRequest.ItemsDTO> sortedItems) {
        for (OrderCreateRequest.ItemsDTO itemDTO : sortedItems) {
            Integer quantity = itemDTO.getQuantity();
            Long productId = itemDTO.getProductId();
            /**
             * 变更库存【学习】
             * 首先执行库存扣减，给该行记录加上  X锁，必须等事务提交后才会释放🔒
             * 完成库存扣减后，直接对流水进行插入，插入的流水数据一定是准确的
             * 不存在当前事务还没有提交，对应商品的库存数量发生变动，导致流程数据不准确
             *
             */
            int updatedFlowRows = productStockMapper.deductStock(productId,quantity);
            if(updatedFlowRows == 0){
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH,
                        "库存不足或并发失败！");
            }


        }
    }

    private Long calculateTotalAmount(List<OrderCreateRequest.ItemsDTO> itemsDTO,
                                      Map<Long, Product> idToProductMap) {
        Long totalAmount= 0L;
        for (OrderCreateRequest.ItemsDTO itemDTO : itemsDTO) {
            Long productId = itemDTO.getProductId();
            Integer quantity = itemDTO.getQuantity();
            try {
                /**
                 * 以下的计算使用Math的api，可以防止溢出
                 * salePrice * quantity
                 * 发生溢出时会静默截断，可能变成负数；而：
                 * Math.multiplyExact(salePrice, quantity)
                 * 发生溢出会抛出 ArithmeticException，事务随之回滚。
                 * 同理Math.addExact(totalQuantity,itemDTO.getQuantity())
                 * 发生溢出会抛出 ArithmeticException
                 *
                 */
                ;
                //计算订单商品总金额
                totalAmount = Math.addExact(
                        totalAmount,
                        Math.multiplyExact(idToProductMap
                                        .get(productId)
                                        .getSalePrice(), quantity)
                );
            } catch (ArithmeticException exception) {
                throw new BusinessException(
                        ErrorCode.PARAM_ERROR,
                        "订单总金额超出系统允许范围",
                        exception
                );
            }
        }
        if (totalAmount > OrderLimits.MAX_ORDER_AMOUNT) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "订单总金额超出系统允许范围"
            );
        }
        return totalAmount;

    }


    private void checkInsert(List batch, int insertedRows) {
        if(batch.size() != insertedRows){
            throw  new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED);
        }
    }
    private void checkInsert(Object obj, int insertedRows) {
        if(1 != insertedRows){
            throw  new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED);
        }
    }

    private OrderInfo buildAndInsertOrderInfo(OrderCreateRequest orderCreateRequest,
                                              List<OrderCreateRequest.ItemsDTO> itemsDTO,
                                              Map<Long, Product> idToProductMap){
        //计算订单商品总数和总结
        Integer totalQuantity;
        try {

            totalQuantity = itemsDTO.stream()
                    .map(OrderCreateRequest.ItemsDTO::getQuantity)
                    .reduce(0, Math::addExact);
        }catch (ArithmeticException exception){
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "订单商品总数超出系统允许范围",
                    exception
            );
        }
        Long totalAmount = calculateTotalAmount(itemsDTO,idToProductMap);
        OrderInfo orderInfo = new OrderInfo();
        Long buyerId = orderCreateRequest.getBuyerId();
        String orderNo = OrderNoGenerator.generate();
        orderInfo.setOrderNo(orderNo);
        orderInfo.setBuyerId(buyerId);
        orderInfo.setTotalQuantity(totalQuantity);
        orderInfo.setTotalAmount(totalAmount);
        orderInfo.setOrderStatus(OrderStatus.CREATED.getCode());
        orderInfo.setRemark(orderCreateRequest.getRemark());
        //插入主表，生成主键
        int insertedOrderRows = orderInfoMapper.insert(orderInfo);
        checkInsert(orderInfo,insertedOrderRows);
        return orderInfo;
    }

    private void buildAndInsertStockFlowList(List<OrderCreateRequest.ItemsDTO> sortedItems,
                                               OrderInfo orderInfo,
                                               Map<Long,Product> latestProducts,
                                             LocalDateTime now) {
        ArrayList<StockFlow> stockFlowList = new ArrayList<>();
        Map<Long,ProductStock>  productIdToStockMap = getProductIdToStockMap(sortedItems);
        for (OrderCreateRequest.ItemsDTO itemDTO : sortedItems) {
            Long productId = itemDTO.getProductId();
            Integer quantity = itemDTO.getQuantity();
            // 由于这里的库存信息是扣减后的，所以afterQuantity等于当前库存
            ProductStock productStock = productIdToStockMap.get(productId);
            Integer afterQuantity = productStock.getAvailableQuantity();
            Integer beforeQuantity = afterQuantity + quantity;
            Product latestProduct = latestProducts.get(productId);
            StockFlow stockFlow = new StockFlow();
            stockFlow.setId(IdWorker.getId());
            stockFlow.setProductId(productId);
            stockFlow.setSkuCode(latestProduct.getSkuCode());
            stockFlow.setBizNo(orderInfo.getOrderNo());
            stockFlow.setBizType(StockFlowBizType.ORDER_DEDUCT.getCode());
            stockFlow.setChangeQuantity(itemDTO.getQuantity() * (-1));
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
        checkInsert(stockFlowList,insertedFlowRows);


    }

    private void buildAndInsertOrderItems(List<OrderCreateRequest.ItemsDTO> itemsDTO,
                                     OrderInfo orderInfo,
                                     Map<Long , Product> latestProducts,
                                          LocalDateTime now){
        ArrayList<OrderItem> orderItemBatch = new ArrayList<>();
        for (OrderCreateRequest.ItemsDTO itemDTO : itemsDTO) {
            Long productId = itemDTO.getProductId();
            Integer quantity = itemDTO.getQuantity();
            Product latestProduct = latestProducts.get(productId);
            Long salePrice = latestProduct.getSalePrice();
            OrderItem orderItem = new OrderItem();
            //因为使用xml进行插入，这里使用雪花算法生成id
            orderItem.setId(IdWorker.getId());
            orderItem.setOrderId(orderInfo.getId());
            orderItem.setOrderNo(orderInfo.getOrderNo());
            orderItem.setProductId(productId);
            orderItem.setSkuCode(latestProduct.getSkuCode());
            orderItem.setProductName(latestProduct.getProductName());
            orderItem.setSalePrice(salePrice);
            orderItem.setQuantity(quantity);
            Long itemAmount = calculateItemAmount(quantity, salePrice);
            orderItem.setItemAmount(itemAmount);
            orderItem.setCreateTime(now);
            orderItem.setUpdateTime(now);
            orderItemBatch.add(orderItem);
        }
        int insertedItemRows = orderItemMapper.batchInsert(orderItemBatch);
        checkInsert(orderItemBatch,insertedItemRows);
    }

    private Long calculateItemAmount(Integer quantity, Long salePrice) {
        try {
            return Math.multiplyExact(salePrice, quantity);
        }catch (ArithmeticException exception){
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "商品总金额超出范围",
                    exception);
        }
    }

    private void validateOrderItems(List<OrderCreateRequest.ItemsDTO> itemsDTO,
                                    Map<Long, Product> idToProductMap,
                                    Map<Long, ProductStock> productIdToStockMap){
        for (OrderCreateRequest.ItemsDTO itemDTO : itemsDTO) {
            Long productId = itemDTO.getProductId();
            Product latestProduct = idToProductMap.get(productId);
            if (latestProduct == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "不存在ID为：" + productId + "的商品！");
            }
            if (!Objects.equals(latestProduct.getProductStatus(),
                    ProductStatus.ON_SHELF.getCode())) {
                throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID,
                        "商品ID为：" + productId + "的商品，状态非法！");
            }
            ProductStock latestProductStock = productIdToStockMap.get(productId);
            if (latestProductStock == null) {
                throw new BusinessException(ErrorCode.STOCK_NOT_FOUND,
                        "商品ID为：" + productId + "的"
                                + ErrorCode.STOCK_NOT_FOUND.getMessage());
            }
        }
    }

    private  Map<Long, Product> getIdToProductMap(
            List<OrderCreateRequest.ItemsDTO> itemsDTO){
        List<Long> productIds = new ArrayList<>();
        for (OrderCreateRequest.ItemsDTO dto : itemsDTO) {
            productIds.add(dto.getProductId());
        }
        List<Product> productsList = productMapper.selectByIds(productIds);
        Map<Long, Product> latestProducts = new HashMap<>();
        for (Product product : productsList) {
            latestProducts.put(product.getId(),product);
        }
        return latestProducts;

    }

    private Map<Long , ProductStock> getProductIdToStockMap(
            List<OrderCreateRequest.ItemsDTO> itemsDTO){
        List<Long> productIdLIst = itemsDTO.stream()
                .map(OrderCreateRequest.ItemsDTO::getProductId)
                .toList();
        LambdaQueryWrapper<ProductStock> productStockLambdaQueryWrapper
                = new LambdaQueryWrapper<>();
        productStockLambdaQueryWrapper
                .in(ProductStock::getProductId,productIdLIst);
        List<ProductStock> productStocks =
                productStockMapper.selectList(productStockLambdaQueryWrapper);
        HashMap<Long, ProductStock> productIdToStockMap = new HashMap<>();
        for (ProductStock productStock : productStocks) {
            productIdToStockMap.put(productStock.getProductId(),productStock);
        }
        return productIdToStockMap;

    }

    /**
     *     TODO 第一版先用@Size限制传入的数量，后续再 开发分批插入
     */
    private void batchInsertOrderItem(List<OrderItem> orderItemBatch,
                                      int batchSize){
        //疑问：这里还需要校验吗？
        checkParam(orderItemBatch,batchSize);
        int itemCount = orderItemBatch.size();
        for (int i = 0; i < itemCount; i=+batchSize) {
            int fromIndex = i;
            int toIndex = Math.min(i+batchSize, itemCount);
            List<OrderItem> currentBatch = orderItemBatch.subList(fromIndex, toIndex);
        }

    }

    private void checkParam(List<OrderItem> orderItemBatch,
                            int batchSize) {
        if(CollectionUtils.isEmpty(orderItemBatch) || batchSize < 1){
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "参数校验失败");
        }
    }

}




