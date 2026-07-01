package com.example.orderinventory.common.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stock flow business type.
 */
public enum StockFlowBizType {

    ORDER_DEDUCT(1, "下单扣减库存"),
    ORDER_CANCEL_ROLLBACK(2, "取消订单回滚库存"),
    MANUAL_INIT(3, "人工初始化库存");

    private static final Map<Integer, StockFlowBizType> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(StockFlowBizType::getCode, Function.identity()));

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
        return CODE_MAP.get(code);
    }

    public static boolean isValidCode(Integer code) {
        return fromCode(code) != null;
    }
}
