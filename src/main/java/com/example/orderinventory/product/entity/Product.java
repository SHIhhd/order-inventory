package com.example.orderinventory.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.orderinventory.common.domain.BaseEntity;
import lombok.Data;

/**
 * 商品表：保存商品基础信息
 * @TableName product
 */
@TableName(value ="product")
@Data
public class Product extends BaseEntity {
    /**
     * 商品SKU编码，业务唯一标识
     */
    private String skuCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品状态：0-下架，1-上架
     */
    private Integer productStatus;

    /**
     * 销售单价，单位：分
     */
    private Long salePrice;

    /**
     * 备注
     */
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}