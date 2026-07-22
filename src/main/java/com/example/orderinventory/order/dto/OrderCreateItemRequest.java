package com.example.orderinventory.order.dto;

import com.example.orderinventory.order.common.constant.orderCreationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


public record OrderCreateItemRequest (
    @NotNull
    @Positive(message = "商品ID不合法")
     Long productId,

    @NotNull(message = "商品数量不能为空")
    @Positive(message = "购买数量必须大于0")
    @Max(
            value = orderCreationLimits.MAX_ITEM_QUANTITY ,
            message = "单个商品购买数量不能超过{value}"
    )
     Integer quantity){
}
