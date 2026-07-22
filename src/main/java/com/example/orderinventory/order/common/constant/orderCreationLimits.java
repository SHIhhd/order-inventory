package com.example.orderinventory.order.common.constant;

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
 * @date 2026/7/11 20:30
 * @description 类的详细说明
 */
public final class orderCreationLimits {
    public static final int MAX_ITEM_TYPES_COUNT = 100;

    public static final long MAX_ITEM_QUANTITY = 10_000L;

    public static final long MAX_ORDER_AMOUNT = 10_000_000_000L;

    /**
     * 声明一个私有构造器，阻止Java自动生成公共构造器
     * 这个属于一种规范，防止其他人 初始化该对象
     * 如果 改成配置对象
     * @ConfigurationProperties(prefix = "order.limits")
     * public class OrderLimitProperties {
     *
     *     private int maxItemTypes;
     *     private int maxItemQuantity;
     * }
     * 这种类需要被Spring实例化，因此不能使用私有构造器。
     */

    private orderCreationLimits() {
    }
}
