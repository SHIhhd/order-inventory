package com.example.orderinventory.order.mapper.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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
 * @date 2026/7/31 21:06
 * @description 类的详细说明
 */
@Getter
@Setter
public class OrderDetailQueryResult {


    private String orderNo;

    private Long buyerId;

    private Integer orderStatus;

    private String orderStatusName;

    private Long totalAmount;

    private Integer totalQuantity;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<OrderDetailItemQueryResult> items;
}
