package com.example.orderinventory.product.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Product status.
 */
public enum ProductStatus {

    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    private static final Map<Integer, ProductStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ProductStatus::getCode, Function.identity()));

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
        return CODE_MAP.get(code);
    }

    public static boolean isValidCode(Integer code) {
        return fromCode(code) != null;
    }
}
