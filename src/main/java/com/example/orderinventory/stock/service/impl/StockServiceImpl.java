package com.example.orderinventory.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.orderinventory.common.enums.OperatorType;
import com.example.orderinventory.common.exception.BusinessAssert;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.common.result.PageResult;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.mapper.ProductMapper;
import com.example.orderinventory.stock.dto.StockFlowPageRequest;
import com.example.orderinventory.stock.dto.StockInitRequest;
import com.example.orderinventory.stock.entity.ProductStock;
import com.example.orderinventory.stock.entity.StockFlow;
import com.example.orderinventory.stock.enums.StockFlowBizType;
import com.example.orderinventory.stock.mapper.ProductStockMapper;
import com.example.orderinventory.stock.mapper.StockFlowMapper;
import com.example.orderinventory.stock.service.StockService;
import com.example.orderinventory.stock.vo.ProductStockVO;
import com.example.orderinventory.stock.vo.StockFlowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
* @author Administrator
* @description 针对表【product_stock(商品库存表：保存商品当前库存信息)】的数据库操作Service实现
* @createDate 2026-07-01 14:49:46
*/
@Service
public class StockServiceImpl implements StockService {
    private static final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);

    private static final int INITIAL_ZERO_QUANTITY = 0;

    private final ProductMapper productMapper;

    private final ProductStockMapper productStockMapper;

    private final StockFlowMapper stockFlowMapper;

    public StockServiceImpl(ProductMapper productMapper, ProductStockMapper productStockMapper, StockFlowMapper stockFlowMapper) {
        this.productMapper = productMapper;
        this.productStockMapper = productStockMapper;
        this.stockFlowMapper = stockFlowMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductStockVO initStock(StockInitRequest stockInitRequest) {
        Long productId = stockInitRequest.getProductId();
        Product product = productMapper.selectById(productId);
        BusinessAssert.notNull(product, ErrorCode.PRODUCT_NOT_FOUND,
                "当前商品不存在");
        ProductStock productStock = buildProductStock(stockInitRequest);
        try {
            productStockMapper.insert(productStock);
        }catch (DuplicateKeyException e){
            throw new BusinessException(ErrorCode.STOCK_ALREADY_EXISTS);
        }
        StockFlow stockFlow = buildStockFlow(stockInitRequest, product);
        try {
            stockFlowMapper.insert(stockFlow);
        }catch (DuplicateKeyException e){
            throw new BusinessException(ErrorCode.STOCK_FLOW_ALREADY_EXISTS,
                    "库存已初始化！");
        }

        return ProductStockVO.from(productStock);
    }

    /**
     *
     * @param query
     * @return
     */
    @Override
    @Transactional( readOnly = true)
    public PageResult<StockFlowResponse> pageStockFlows(StockFlowPageRequest query) {
        Integer pageNo = query.pageNo();

        Integer pageSize = query.pageSize();
        LambdaQueryWrapper<StockFlow> queryWrapper = new LambdaQueryWrapper<>();
        /**
         * todo 改成游标分页
         * 由于create_time不是唯一，
         * 可能出现：
         *第一次请求第1页。
         * 同时插入新流水。
         * 第二次请求第2页时，OFFSET 整体后移。
         * 某条数据重复出现，或者被直接跳过。
         */
        /**
         * todo 这里的查询没有合适的索引，后续要给数据库添加索引
         */

        queryWrapper.eq(
                        !Objects.isNull(query.productId()),
                        StockFlow::getProductId,query.productId()
                )
                .eq(
                        !Objects.isNull(query.skuCode()),
                        StockFlow::getSkuCode,query.skuCode()
                ).eq(
                        !Objects.isNull(query.bizNo()),
                        StockFlow::getBizNo,query.bizNo()
                ).eq(
                        !Objects.isNull(query.bizType()),
                        StockFlow::getBizType,query.bizType()
                ).between(
                        !Objects.isNull(query.startTime())
                                && !Objects.isNull(query.endTime())
                        ,StockFlow::getCreateTime
                        ,query.startTime()
                        ,query.endTime()
                )
                .orderByDesc(StockFlow::getCreateTime);
        Page<StockFlow> stockFlowPage = new Page<>(pageNo, pageSize);
        stockFlowPage = stockFlowMapper
                .selectPage(stockFlowPage, queryWrapper);
        List<StockFlowResponse> records = stockFlowPage
                .getRecords().stream()
                .map(this::buildStockFlowResponse)
                .toList();

        return PageResult.of(records,pageNo,pageSize,stockFlowPage.getTotal());
    }

    private StockFlowResponse buildStockFlowResponse(StockFlow stockFlow) {
        Integer bizTypeCode = stockFlow.getBizType();
        Integer operatorTypeCode = stockFlow.getOperatorType();
        /**
         * 【学习】
         *如果这里的bizType和operatorType 数据库脏枚举会导致整页接口失败
         * 推荐采用：
         * 查询端：容错返回原始数据，枚举名称降级为“未知类型(code)”。
         * 写入端 (insert方法)：严格阻止非法枚举进入数据库。
         * todo 监控端：记录日志和指标，及时修复脏数据。
         * 不要在 Java 中把脏记录过滤掉，否则分页数量和游标会不稳定。
         */
        String bizTypeName =  StockFlowBizType.findByCode(bizTypeCode)
                .map(StockFlowBizType::getMessage)
                .orElseGet(() -> {
                    reportInvalidBizType(stockFlow);
                    return buildUnknownName("未知业务类型", bizTypeCode);
                });
        String operatorTypeName = OperatorType.findByCode(operatorTypeCode)
                .map(OperatorType::getMessage)
                .orElseGet(() -> {
                    reportInvalidOperatorType(stockFlow);
                    return buildUnknownName("未知操作来源", operatorTypeCode);
                });
        return StockFlowResponse
                .from(stockFlow,bizTypeName,operatorTypeName);
    }
    private void reportInvalidBizType(StockFlow stockFlow) {
        log.warn(
                "Invalid stock flow bizType, id={}, bizNo={}, bizType={}",
                stockFlow.getId(),
                stockFlow.getBizNo(),
                stockFlow.getBizType()
        );
    }

    private void reportInvalidOperatorType(StockFlow stockFlow) {
        log.warn(
                "Invalid stock flow operatorType, id={}, bizNo={}, bizType={}",
                stockFlow.getId(),
                stockFlow.getBizNo(),
                stockFlow.getOperatorType()
        );
    }
    private String buildUnknownName(String name, Integer code) {
        return name + "(" + (code == null ? "null" : code) + ")";
    }


    private ProductStock buildProductStock(StockInitRequest stockInitRequest){
        ProductStock productStock = new ProductStock();
        productStock.setProductId(stockInitRequest.getProductId());
        productStock.setAvailableQuantity(stockInitRequest.getAvailableQuantity());
        productStock.setLockedQuantity(INITIAL_ZERO_QUANTITY);
        productStock.setTotalInQuantity(stockInitRequest.getAvailableQuantity());
        productStock.setTotalOutQuantity(INITIAL_ZERO_QUANTITY);
        productStock.setRemark(stockInitRequest.getRemark());
        return productStock;

    }
    private StockFlow buildStockFlow(StockInitRequest stockInitRequest,Product product){
        StockFlow stockFlow = new StockFlow();
        stockFlow.setProductId(stockInitRequest.getProductId());
        stockFlow.setSkuCode(product.getSkuCode());
        //TODO 第一版的stock_flow.bizNo = "STOCK_INIT_" + productId，后续再调整
        stockFlow.setBizNo(buildStockInitBizNo(stockInitRequest.getProductId()));
        stockFlow.setBizType(StockFlowBizType.MANUAL_INIT.getCode());
        stockFlow.setChangeQuantity(stockInitRequest.getAvailableQuantity());
        stockFlow.setBeforeQuantity(INITIAL_ZERO_QUANTITY);
        stockFlow.setOperatorId(stockInitRequest.getOperatorId());
        stockFlow.setAfterQuantity(stockInitRequest.getAvailableQuantity());
        stockFlow.setRemark(stockInitRequest.getRemark());
        //TODO 后续添加登录功能后再修改，先默认 0-系统
        stockFlow.setOperatorType(OperatorType.SYSTEM.getCode());
        return stockFlow;
    }
    private String buildStockInitBizNo(Long productId) {
        return "STOCK_INIT_" + productId;
    }
}




