package com.example.orderinventory.product.controller;

import com.example.orderinventory.common.result.ApiResult;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
 * @date 2026/7/1 14:23
 * @description 类的详细说明
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @GetMapping("test")
    public ApiResult<List<Product>> test(){
        List<Product> productList = productService.list();
        return ApiResult.success(productList);
    }
}
