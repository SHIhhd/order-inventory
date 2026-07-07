package com.example.orderinventory.stock.service.impl;

import com.example.orderinventory.common.enums.OperatorType;
import com.example.orderinventory.common.exception.BusinessAssert;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.mapper.ProductMapper;
import com.example.orderinventory.stock.vo.ProductStockVO;
import com.example.orderinventory.stock.dto.StockInitRequest;
import com.example.orderinventory.stock.entity.ProductStock;
import com.example.orderinventory.stock.entity.StockFlow;
import com.example.orderinventory.stock.enums.StockFlowBizType;
import com.example.orderinventory.stock.mapper.ProductStockMapper;
import com.example.orderinventory.stock.mapper.StockFlowMapper;
import com.example.orderinventory.stock.service.StockService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author Administrator
* @description 针对表【product_stock(商品库存表：保存商品当前库存信息)】的数据库操作Service实现
* @createDate 2026-07-01 14:49:46
*/
@Service
public class StockServiceImpl implements StockService {

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
        //TODO 后续添加登录功能后再修改，先默认 0-系统
        stockFlow.setOperatorType(OperatorType.SYSTEM.getCode());
        return stockFlow;
    }
    private String buildStockInitBizNo(Long productId) {
        return "STOCK_INIT_" + productId;
    }
}




