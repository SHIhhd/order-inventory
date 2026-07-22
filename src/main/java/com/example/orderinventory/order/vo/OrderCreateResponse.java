package com.example.orderinventory.order.vo;

import com.example.orderinventory.order.entity.OrderInfo;
import com.example.orderinventory.order.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
 * @date 2026/7/7 22:03
 * @description 类的详细说明
 */

@Getter
@Setter
public class OrderCreateVO {

    private OrderCreateVO(OrderInfo orderInfo) {
        this.orderNo=orderInfo.getOrderNo();
        this.buyerId = orderInfo.getBuyerId();
        this.orderStatus = orderInfo.getOrderStatus();
        this.orderStatusName= OrderStatus
                .fromCode(orderInfo.getOrderStatus())
                .getMessage();
        this.totalAmount = orderInfo.getTotalAmount();
        this.totalQuantity = orderInfo.getTotalQuantity();
        this.createTime = orderInfo.getCreateTime();
    }

    private String orderNo;
    private Long buyerId;
    private Integer orderStatus;
    private String orderStatusName;
    private Long totalAmount;
    private Integer totalQuantity;
    private LocalDateTime createTime;

    public static OrderCreateVO from(OrderInfo orderInfo){
        return new OrderCreateVO(orderInfo);
    }
}
