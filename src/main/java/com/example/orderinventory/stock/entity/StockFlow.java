package com.example.orderinventory.stock.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.orderinventory.common.domain.BaseEntity;
import lombok.Data;

/**
 * 库存流水表：记录每一次库存变化
 * @TableName stock_flow
 */
@TableName(value ="stock_flow")
@Data

public class StockFlow extends BaseEntity {
    /**
     * 商品ID，对应product.id
     */
    private Long productId;

    /**
     * 商品SKU编码
     */
    private String skuCode;

    /**
     * 业务单号，例如订单编号
     */
    private String bizNo;

    /**
     * 业务类型：1-下单扣减库存，2-取消订单回滚库存，3-人工调整库存
     */
    private Integer bizType;

    /**
     * 库存变化数量：正数表示增加，负数表示扣减
     */
    private Integer changeQuantity;

    /**
     * 变更前可用库存数量
     */
    private Integer beforeQuantity;

    /**
     * 变更后可用库存数量
     */
    private Integer afterQuantity;

    /**
     * 操作人ID，系统操作时可为空
     */
    private Long operatorId;

    /**
     * 操作来源：0-系统，1-用户，2-管理员
     */
    private Integer operatorType;

    /**
     * 备注
     */
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}