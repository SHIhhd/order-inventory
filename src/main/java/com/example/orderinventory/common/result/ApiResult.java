package com.example.orderinventory.common.result;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Unified API response structure.
 *
 * <p>字段类型选择说明：
 * success 是必填字段，因此这里使用基本类型 boolean，而不是包装类型 Boolean。
 * Boolean 可以表示 true、false、null 三种状态；如果调用方忘记赋值，接口可能返回
 * "success": null，这会破坏“是否成功”这个必填语义。boolean 只有 true/false，
 * 默认值是 false，更适合必填布尔字段。</p>
 *
 * <p>基本类型和包装类型的区别：
 * 基本类型（int、long、boolean）不能为 null，有默认值，适合必填字段和简单计算；
 * 包装类型（Integer、Long、Boolean）可以为 null，适合表达“未传入、未知、不更新”
 * 这类三态语义，也常用于请求参数、数据库实体和需要泛型的场景。统一返回结构中的
 * 必填状态字段应优先使用基本类型，并通过 success()/fail() 工厂方法统一创建。</p>
 *
 * @author Administrator
 * @param <T> response data type
 */
@Getter
public final class ApiResult<T> {

    private final String code;

    private final String message;

    /**
     * Object 能用，但太宽泛；T 更好。
     */
    private final T data;

    /**
     * 这里的success属于必填项，使用基本类型，因为基本类型不能为null
     */
    private final boolean success;

    private final LocalDateTime timestamp;

    /**
     * Objects.requireNonNull 这个API 的缺点：
     * ①只能判断是否为NULL
     * ②抛出的异常是NullPointerException，不一定能被全局异常捕获
     * @param code
     * @param message
     * @param data
     * @param success
     */
    private ApiResult(String code, String message, T data, boolean success) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.data = data;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResult<T> success() {
        return success(null);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data,
                true
        );
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode, null);
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, T data) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return new ApiResult<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                data,
                false
        );
    }

    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(code, message, null, false);
    }
}
