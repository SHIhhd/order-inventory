package com.example.orderinventory.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
 * @date 2026/7/7 10:52
 * @description 类的详细说明
 */
@Data
public class StockInitRequest {


    @NotNull(message = "商品ID需大于0")
    @Positive(message = "商品ID需大于0")
    private Long productId;

    @NotNull(message = "库存量需大于等于0")
    @PositiveOrZero(message = "库存量需大于等于0")
    private Integer availableQuantity;

    @Positive(message = "操作人⚪ID需大于0")
    private Long operatorId;

    @Size(max = 512 ,message = "备注字段长度小于等于512")
    private String remark;
}
