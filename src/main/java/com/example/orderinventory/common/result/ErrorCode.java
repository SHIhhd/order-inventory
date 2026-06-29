package com.example.orderinventory.common.result;

/**
 * Common API error codes.
 * @author Administrator
 */
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数校验失败"),
    PRODUCT_NOT_FOUND(404, "商品不存在"),
    PRODUCT_STATUS_INVALID(409, "商品状态非法"),
    PRODUCT_SKU_DUPLICATE(409, "商品 SKU 编码重复"),
    STOCK_NOT_FOUND(404, "库存记录不存在"),
    STOCK_ALREADY_EXISTS(409, "库存记录已存在"),
    STOCK_NOT_ENOUGH(409, "库存不足"),
    ORDER_NOT_FOUND(404, "订单不存在"),
    ORDER_STATUS_INVALID(409, "订单状态非法"),
    ORDER_ITEM_EMPTY(400, "订单明细不能为空"),
    ORDER_ITEM_DUPLICATE(400, "订单中存在重复商品"),
    CONCURRENT_UPDATE_FAILED(409, "并发更新失败"),
    SYSTEM_ERROR(500, "系统异常");

    private final int httpStatus;

    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return name();
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
