package com.example.orderinventory.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
 * @date 2026/7/6 10:43
 * @description 类的详细说明
 */
@Data
public class ProductStatusUpdateRequest {


    @NotNull(message = "商品状态必填！")
    @Min(value = 0 , message = "商品状态只能是0或1")
    @Max(value = 1 , message = "商品状态只能是0或1")
    private Integer productStatus;
}
