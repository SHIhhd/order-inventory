package com.example.orderinventory.order.vo;

import com.example.orderinventory.order.enums.OrderStatus;
import com.example.orderinventory.order.mapper.model.OrderDetailQueryResult;

import java.time.LocalDateTime;
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
 * @date 2026/7/29 21:39
 * @description 类的详细说明
 */
public record OrderDetailResponse(

        String orderNo,

        Long buyerId,

        Integer orderStatus,

        String orderStatusName,

        Long totalAmount,

        Integer totalQuantity,

        String remark,

        LocalDateTime createTime,

        LocalDateTime updateTime,

        List<OrderDetailItemResponse> items
) {
    /**
     * 自定义构造器：将 实体(Entity) 转换为 DTO
     * 必须通过 this(...) 调用 Record 自动生成的全参构造器
     */
    public OrderDetailResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static OrderDetailResponse from(OrderDetailQueryResult orderDetail,
                                           OrderStatus orderStatus){
        //【学习】
        // 这是编程契约检查，不是业务判空
        Objects.requireNonNull(
                orderDetail,
                "orderDetail must not be null"
        );
        Objects.requireNonNull(
                orderStatus,
                "orderStatus must not be null"
        );

        List<OrderDetailItemResponse> itemResponses = orderDetail.getItems()
                .stream()
                .map(OrderDetailItemResponse::from)
                .toList();

        return new OrderDetailResponse(
                orderDetail.getOrderNo(),
                orderDetail.getBuyerId(),
                orderStatus.getCode(),
                orderStatus.getMessage(),
                orderDetail.getTotalAmount(),
                orderDetail.getTotalQuantity(),
                orderDetail.getRemark(),
                orderDetail.getCreateTime(),
                orderDetail.getUpdateTime(),
                itemResponses
        );
    }
}
