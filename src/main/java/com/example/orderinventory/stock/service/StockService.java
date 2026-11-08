package com.example.orderinventory.stock.service;

import com.example.orderinventory.common.result.PageResult;
import com.example.orderinventory.stock.dto.StockFlowPageRequest;
import com.example.orderinventory.stock.dto.StockInitRequest;
import com.example.orderinventory.stock.vo.ProductStockVO;
import com.example.orderinventory.stock.vo.StockFlowResponse;

/**
* @author Administrator
* @description 针对表【product_stock(商品库存表：保存商品当前库存信息)】的数据库操作Service
* @createDate 2026-07-01 14:49:46
*/
public interface StockService {

    ProductStockVO initStock(StockInitRequest stockInitRequest);

    PageResult<StockFlowResponse> pageStockFlows(StockFlowPageRequest query);
}
