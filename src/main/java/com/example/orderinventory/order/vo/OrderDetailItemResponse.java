package com.example.orderinventory.order.vo;

import com.example.orderinventory.order.mapper.model.OrderDetailItemQueryResult;

import java.util.Objects;

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
 * @date 2026/7/29 21:47
 * @description 类的详细说明
 */
public record OrderDetailItemResponse(
         Long productId,

       String skuCode,

       String productName,

       Long salePrice,

       Integer quantity,

       Long itemAmount


) {

    public static OrderDetailItemResponse from(OrderDetailItemQueryResult orderItem){

        Objects.requireNonNull(
                orderItem,
                "orderItem must not be null"
        );
        return new OrderDetailItemResponse(
                orderItem.getProductId(),
                orderItem.getSkuCode(),
                orderItem.getProductName(),
                orderItem.getSalePrice(),
                orderItem.getQuantity(),
                orderItem.getItemAmount()
        );
    }

}
