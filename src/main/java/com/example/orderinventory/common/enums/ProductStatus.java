package com.example.orderinventory.common.enums;

import java.util.Arrays;

/**
 * Product status.
 */
public enum ProductStatus {

    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    private final int code;

    private final String message;

    ProductStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ProductStatus fromCode(Integer code) {
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
