package com.example.orderinventory.common.result;

import org.springframework.http.HttpStatus;

/**
 * 因为在全局遗产类中HttpStatus httpStatus =
 *              HttpStatus.valueOf(errorCode.getHttpStatus());
 * 会获取ErrorCode的httpStatus，然后传入HttpStatus.valueOf()中
 * 如果有一个httpStatus 不在 HttpStatus的枚举类中就会报错，比如SYSTEM_ERROR(700, "系统异常")
 * 如果ErrorCode中的httpStatus 直接拿  HttpStatus的枚举类中code就行
 */
public enum ErrorCode {

    SUCCESS(HttpStatus.OK, "操作成功"),
    PARAM_ERROR(HttpStatus.BAD_REQUEST, "参数校验失败"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "商品不存在"),
    PRODUCT_STATUS_INVALID(HttpStatus.CONFLICT, "商品状态非法"),
    PRODUCT_SKU_DUPLICATE(HttpStatus.CONFLICT, "商品 SKU 编码重复"),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "库存记录不存在"),
    STOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "库存记录已存在"),
    STOCK_FLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "库存流水已存在"),
    STOCK_NOT_ENOUGH(HttpStatus.CONFLICT, "库存不足"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "订单不存在"),
    ORDER_STATUS_INVALID(HttpStatus.CONFLICT, "订单状态非法"),
    ORDER_ITEM_EMPTY(HttpStatus.BAD_REQUEST, "订单明细不能为空"),
    ORDER_ITEM_DUPLICATE(HttpStatus.BAD_REQUEST, "订单中存在重复商品"),
    CONCURRENT_UPDATE_FAILED(HttpStatus.CONFLICT, "并发更新失败"),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系统异常");

    private final HttpStatus httpStatus;

    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return name();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
