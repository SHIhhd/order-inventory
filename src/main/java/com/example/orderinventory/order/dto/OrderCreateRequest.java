package com.example.orderinventory.order.dto;

import com.example.orderinventory.order.common.constant.OrderLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

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
@Getter
@Setter
@Validated
public class OrderCreateRequest{

    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID不合法")
    private Long buyerId;

    @Valid
    @NotEmpty(message = "商品明细不能为空")
    @Size(
            min = 1 ,
            max = OrderLimits.MAX_ITEM_TYPES,
            message = "订单商品种类必须在1到100之间"
    )
    private List<@NotNull(message = "商品明细不能为空") @Valid ItemsDTO> items;

    @Size(max = 512 ,message = "备注最大长度为512")
    private String remark;

    @Getter
    @Setter
    public static class ItemsDTO {
        @NotNull
        @Positive(message = "商品ID不合法")
        private Long productId;

        @NotNull(message = "商品数量不能为空")
        @Positive(message = "购买数量必须大于0")
        @Max(
                value = OrderLimits.MAX_ITEM_QUANTITY ,
                message = "单个商品购买数量不能超过10000"
        )
        private Integer quantity;
    }

    @AssertTrue(message = "同一订单内不允许出现重复 productId")
    private  boolean isProductIdUnique(){
        /**
         * 【学习】
         * 默认情况下 Bean Validation 会尽量执行所有适用的约束，
         * 不会因为某个校验“不通过”就自动停止后续校验。
         * 空集合由 @NotEmpty 负责，避免重复错误信息
          */
        if (CollectionUtils.isEmpty(items)) {
            return true;
        }
        // null 元素由 List 元素上的 @NotNull 负责
        if (items.stream().anyMatch(Objects::isNull)) {
            return true;
        }
        // productId 为空由 ItemsDTO.productId 的 @NotNull 负责

        if(items.stream().map(ItemsDTO::getProductId)
                .anyMatch(Objects::isNull)){
            return true;
        }

        // 【学习】优化：利用 Stream 的 distinct() 去重后比对数量，代码更具有声明式的美感
        long uniqueCount = items.stream()
                .map(ItemsDTO::getProductId)
                // 容错处理，防止前面报NotNull之前这里先NPE
                .filter(Objects::nonNull)
                .distinct()
                .count();
        return uniqueCount == items.size();
    }
}
