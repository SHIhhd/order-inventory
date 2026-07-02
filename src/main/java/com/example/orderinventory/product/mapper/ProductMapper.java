package com.example.orderinventory.product.mapper;

import com.example.orderinventory.product.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【product(商品表：保存商品基础信息)】的数据库操作Mapper
* @createDate 2026-07-01 14:14:38
* @Entity com.example.orderinventory.product.entity.Product
*/
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

}




