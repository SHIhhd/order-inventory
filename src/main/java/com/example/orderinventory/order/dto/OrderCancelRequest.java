package com.example.orderinventory.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
 * @date 2026/7/22 14:16
 * @description 类的详细说明
 */
public record OrderCancelRequest(
        @NotBlank(message = "取消原因不能为空")
        @Size(max = 255 , message = "取消原因最大长度不能超过{max}")
        String cancelReason
) {
}
