package com.example.orderinventory.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.orderinventory.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单明细表：保存订单中的商品快照信息
 * @TableName order_item
 */
@TableName(value ="order_item")
@Data
public class OrderItem extends BaseEntity implements Serializable {
    /**
     * 订单ID，对应order_info.id
     */
    private Long orderId;

    /**
     * 订单编号，对应order_info.order_no
     */
    private String orderNo;

    /**
     * 商品ID，对应product.id
     */
    private Long productId;

    /**
     * 商品SKU编码，下单时快照
     */
    private String skuCode;

    /**
     * 商品名称，下单时快照
     */
    private String productName;

    /**
     * 销售单价，下单时快照，单位：分
     */
    private Long salePrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 明细金额，单位：分，计算规则：sale_price * quantity
     */
    private Long itemAmount;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}