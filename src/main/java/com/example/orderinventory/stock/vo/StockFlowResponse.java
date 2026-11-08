package com.example.orderinventory.stock.vo;

import com.example.orderinventory.stock.entity.StockFlow;

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
 * @date 2026/8/2 17:36
 * @description 类的详细说明
 */
public record StockFlowResponse(
         Long id,
         Long productId,
         String skuCode,
         String bizNo,
         Integer bizType,
         String bizTypeName,
         Integer changeQuantity,
         Integer beforeQuantity,
         Integer afterQuantity,
         Long operatorId,
         Integer operatorType,
         String operatorTypeName,
         String remark,
         LocalDateTime createTime
        
) {
    /**
     * 【学习】
     * 这个方法只是转换类型，如果不传入
     * bizTypeName 和   operatorTypeName，而是在该方法
     * 内部从 stockFlow 中获取bizType ， operatorType
     * 在转换成bizTypeName 和  operatorTypeName
     * 那么耦合度就很高
     */
    public static StockFlowResponse from(
            StockFlow stockFlow,
            String bizTypeName,
            String operatorTypeName
            ){
        return new StockFlowResponse (
                stockFlow.getId(),
                stockFlow.getProductId(),
                stockFlow.getSkuCode(),
                stockFlow.getBizNo(),
                stockFlow.getBizType(),
                bizTypeName,
                stockFlow.getChangeQuantity(),
                stockFlow.getBeforeQuantity(),
                stockFlow.getAfterQuantity(),
                stockFlow.getOperatorId(),
                stockFlow.getOperatorType(),
                operatorTypeName,
                stockFlow.getRemark(),
                stockFlow.getCreateTime()
        );
    }
    
}
