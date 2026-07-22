package com.example.orderinventory.stock.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderinventory.stock.entity.ProductStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Administrator
* @description 针对表【product_stock(商品库存表：保存商品当前库存信息)】的数据库操作Mapper
* @createDate 2026-07-01 14:49:46
* @Entity com.example.orderinventory.stock.entity.ProductStock
*/
public interface ProductStockMapper extends BaseMapper<ProductStock> {
    /**
     * 【】
     * 不应依赖订单层的 ItemsDTO
     * int updateQuantity(OrderCreateRequest.ItemsDTO itemsDTO);
     * 建议改成 deductStock(Long productId, Integer quantity)，降低模块耦合
     * @param
     * @return
     */
    int deductStock(@Param("productId") Long productId,
                    @Param("quantity") Integer quantity);

    List<Integer> lockStocks(@Param("productIds")List<Long> productIds);





}




