package com.example.orderinventory.common.enums;

import java.util.Arrays;

/**
 * Operation source type.
 * @author Administrator
 */
public enum OperatorType {

    SYSTEM(0, "系统"),
    USER(1, "用户"),
    ADMIN(2, "管理员");

    private final int code;

    private final String message;

    OperatorType(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static OperatorType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.code == code)
                .findFirst()
                .orElse(null);
    }

    public static boolean isValidCode(Integer code) {
        return fromCode(code) != null;
    }
}
