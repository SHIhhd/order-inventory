package com.example.orderinventory.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.orderinventory.common.result.PageResult;
import com.example.orderinventory.product.dto.ProductCreateRequest;
import com.example.orderinventory.product.dto.ProductStatusUpdateRequest;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.vo.ProductStatusUpdateVO;
import com.example.orderinventory.product.vo.ProductVO;

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
 * @date 2026/7/1 14:18
 * @description 类的详细说明
 */
public interface ProductService extends IService<Product> {
    ProductVO createProduct(ProductCreateRequest productCreateRequest);

    PageResult<ProductVO> getProductPage(Integer pageNo, Integer pageSize, String keyword, Integer productStatus);

    ProductStatusUpdateVO updateProductStatus(Long productId , ProductStatusUpdateRequest productStatusUpdateRequest);
}
