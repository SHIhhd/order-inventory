package com.example.orderinventory.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.mapper.ProductMapper;
import com.example.orderinventory.product.service.ProductService;
import org.springframework.stereotype.Service;

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
}
