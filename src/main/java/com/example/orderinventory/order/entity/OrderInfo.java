package com.example.orderinventory.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.orderinventory.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单主表：保存订单整体信息
 * @TableName order_info
 */
@TableName(value ="order_info")
@Data
public class OrderInfo extends BaseEntity implements Serializable {
    /**
     * 订单编号，业务唯一标识
     */
    private String orderNo;

    /**
     * 下单用户ID，第一版不建立用户表，仅保留扩展字段
     */
    private Long buyerId;

    /**
     * 订单状态：10-已创建，20-已取消，30-已完成
     */
    private Integer orderStatus;

    /**
     * 订单总金额，单位：分
     */
    private Long totalAmount;

    /**
     * 订单商品总数量
     */
    private Integer totalQuantity;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 备注
     */
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}