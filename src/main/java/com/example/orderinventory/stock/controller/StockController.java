package com.example.orderinventory.stock.controller;

import com.example.orderinventory.common.result.ApiResult;
import com.example.orderinventory.stock.dto.StockInitRequest;
import com.example.orderinventory.stock.vo.ProductStockVO;
import com.example.orderinventory.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请遵循六道之力
 * 【①】这个字段会不会是 null？
 * 【②】这个类需不需要 setter？
 * 【③】有没有统一创建方法？
 * 【④】有没有泛型？
 * 【⑤】有没有依赖具体实现类？
 * 【⑥】传错参数会不会悄悄生成错误结果？
 *
 * @author 我他妈莱纳
 * @date 2026/7/1 14:54
 * @description 类的详细说明
 */
@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }


    @PostMapping("/init")
    public ApiResult<ProductStockVO> initStock(@Valid @RequestBody StockInitRequest stockInitRequest){
        ProductStockVO productStockVO = stockService.initStock(stockInitRequest);
        return ApiResult.success(productStockVO);
    }
}
