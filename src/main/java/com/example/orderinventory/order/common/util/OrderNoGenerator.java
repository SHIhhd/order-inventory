package com.example.orderinventory.order.common.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
 * @date 2026/7/9 21:16
 * @description 类的详细说明
 */
public final class OrderNoGenerator {

    private static final String ORDER_PREFIX = "ORD";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    private OrderNoGenerator() {
    }

    /**
     * TODO 后续的订单号再做优化
     */
    public static String generateOrderNo() {
        /**
         * 【学习】
         * 雪花算法
         */
        String snowflakeId = IdWorker.getIdStr();
        return ORDER_PREFIX
                + LocalDateTime.now().format(FORMATTER)
                + snowflakeId;
    }
}
