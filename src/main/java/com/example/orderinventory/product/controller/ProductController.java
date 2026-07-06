package com.example.orderinventory.product.controller;

import com.example.orderinventory.common.result.ApiResult;
import com.example.orderinventory.common.result.PageResult;
import com.example.orderinventory.product.dto.ProductCreateRequest;
import com.example.orderinventory.product.dto.ProductPageRequest;
import com.example.orderinventory.product.service.ProductService;
import com.example.orderinventory.product.vo.ProductVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
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
 * @date 2026/7/1 14:23
 * @description 类的详细说明
 */
@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 新增商品基础信息
     * 新增成功后，系统只创建 `product` 记录，不自动创建库存记录。
     * 库存需要调用“初始化库存”接口单独处理。
     * @return
     */
    @PostMapping("/products")
    public ApiResult<ProductVO> createProduct(@Valid @RequestBody ProductCreateRequest
                                                        productCreateRequest){
        ProductVO productVO = productService.createProduct(productCreateRequest);
        return ApiResult.success(productVO);
    }

    @GetMapping("/products")
    public ApiResult<PageResult<ProductVO>> getProductPage(@Valid ProductPageRequest productPageRequest){
        PageResult<ProductVO> pageResult = productService.getProductPage(productPageRequest.getPageNo(),
                productPageRequest.getPageSize(),
                productPageRequest.getKeyword(),
                productPageRequest.getProductStatus());

        return ApiResult.success(pageResult);
    }

}
