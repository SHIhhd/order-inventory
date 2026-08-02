package com.example.orderinventory.order.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Order status.
 * @author Administrator
 */
public enum OrderStatus {

    CREATED(10, "已创建"),
    CANCELLED(20, "已取消"),
    COMPLETED(30, "已完成");

    private static final Map<Integer, OrderStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(OrderStatus::getCode, Function.identity()));

    private final int code;

    private final String message;

    OrderStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean canCancel() {
        return this == CREATED;
    }

    /**
     * 尝试转换，未知状态返回 null。
     */
    public static OrderStatus fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    public static Optional<OrderStatus> findByCode(Integer code) {
        return Optional.ofNullable(code)
                .map(CODE_MAP::get);
    }

}
