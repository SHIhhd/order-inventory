package com.example.orderinventory.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.product.dto.ProductCreateRequest;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.mapper.ProductMapper;
import com.example.orderinventory.product.service.ProductService;
import com.example.orderinventory.product.vo.ProductVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * @date 2026/7/1 14:22
 * @description 类的详细说明
 */
@Service
public class ProductServiceImpl  extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ProductVO createProduct(ProductCreateRequest request) {
        //如果其他类调用需要业务边界校验
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品创建参数不能为空");
        }
        Product product = buildProduct(request);
        try {
             baseMapper.insert(product);
        }catch (DuplicateKeyException e){
            throw new BusinessException(ErrorCode.PRODUCT_SKU_DUPLICATE);
        }
        ProductVO productVO =  ProductVO.from(product);
        return productVO;
    }

    private Product buildProduct(ProductCreateRequest request) {
        Product product = new Product();
        product.setSkuCode(request.getSkuCode());
        product.setProductName(request.getProductName());
        product.setProductStatus(request.getProductStatus());
        product.setSalePrice(request.getSalePrice());
        product.setRemark(request.getRemark());
        return product;
    }
}
