package com.example.orderinventory.common.enums;

import java.util.Arrays;

/**
 * Order status.
 * @author Administrator
 */
public enum OrderStatus {

    CREATED(10, "已创建"),
    CANCELLED(20, "已取消"),
    COMPLETED(30, "已完成");

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

    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(null);
    }

    public static boolean isValidCode(Integer code) {
        return fromCode(code) != null;
    }
}
