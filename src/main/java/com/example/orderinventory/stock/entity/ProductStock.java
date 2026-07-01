package com.example.orderinventory.stock.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.orderinventory.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品库存表：保存商品当前库存信息
 * @TableName product_stock
 */
@TableName(value ="product_stock")
@Data
public class ProductStock extends BaseEntity implements Serializable {
    /**
     * 商品ID，对应product.id
     */
    private Long productId;

    /**
     * 可用库存数量
     */
    private Integer availableQuantity;

    /**
     * 锁定库存数量，第一版可不启用，预留给后续库存预占
     */
    private Integer lockedQuantity;

    /**
     * 累计入库数量
     */
    private Integer totalInQuantity;

    /**
     * 累计出库数量
     */
    private Integer totalOutQuantity;

    /**
     * 备注
     */
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}