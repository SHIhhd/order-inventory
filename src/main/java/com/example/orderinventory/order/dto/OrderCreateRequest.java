package com.example.orderinventory.order.dto;

import com.example.orderinventory.order.common.constant.orderCreationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
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
 * @date 2026/7/7 21:31
 * @description 类的详细说明
 */
public record OrderCreateRequest(

        @NotNull(message = "客户端请求号不能为空")
        Long requestId,

        @NotNull(message = "用户ID不能为空")
        @Positive(message = "用户ID不合法")
        Long buyerId,

        @NotEmpty(message = "商品明细不能为空")
        @Size(
                max = orderCreationLimits.MAX_ITEM_TYPES_COUNT,
                message = "订单商品种类不能超过{max}"
        )
        List<
                @NotNull(message = "商品明细不能为空")
                @Valid OrderCreateItemRequest
                > items,

        @Size(max = 512, message = "备注最大长度为{max}")
        String remark
) {

    public OrderCreateRequest {
        items = items == null ? null : List.copyOf(items);
    }

    @AssertTrue(message = "同一订单内不允许出现重复 productId")
    private  boolean isProductIdUnique(){
        /**
         * 【学习】
         * 默认情况下 Bean Validation 会尽量执行所有适用的约束，
         * 不会因为某个校验“不通过”就自动停止后续校验。
         * 空集合由 @NotEmpty 负责，避免重复错误信息
          */
        HashSet<Long> set = new HashSet<>();
        for (OrderCreateItemRequest item : items) {
            /**
             * null 元素由 List 元素上的 @NotNull 负责
             * productId 为空由 ItemsDTO.productId 的 @NotNull 负责
             */
            if(Objects.isNull(item) || Objects.isNull(item.productId())){
                continue;
            }
            if(set.contains(item.productId())){
                return false;
            }
            set.add(item.productId());
        }
        return true;

    }
}
