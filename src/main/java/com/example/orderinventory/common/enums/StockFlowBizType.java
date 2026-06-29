package com.example.orderinventory.common.enums;

import java.util.Arrays;

/**
 * Stock flow business type.
 */
public enum StockFlowBizType {

    ORDER_DEDUCT(1, "下单扣减库存"),
    ORDER_CANCEL_ROLLBACK(2, "取消订单回滚库存"),
    MANUAL_INIT(3, "人工初始化库存");

    private final int code;

    private final String message;

    StockFlowBizType(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static StockFlowBizType fromCode(Integer code) {
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
