package com.example.orderinventory.common.exception;

import com.example.orderinventory.common.result.ApiResult;
import com.example.orderinventory.common.result.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * REST 接口统一异常处理器，将请求处理过程中传播到 MVC 的异常转换为统一响应。
 * 包括参数校验异常、业务异常及未被专门处理的系统异常。
 * 不负责捕获后台线程、定时任务等 MVC 请求处理链之外的异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务规则不满足时主动抛出的异常，例如商品不存在、库存不足、订单状态非法。
     * HTTP 状态码和业务错误码取自异常携带的 ErrorCode；优先返回异常中的具体说明。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        String message = defaultIfBlank(exception.getMessage(), errorCode.getMessage());
        return buildResponse(errorCode, message);
    }

    /**
     * 处理请求对象绑定或 Bean Validation 校验失败产生的参数异常。
     * 例如带 @Valid 的请求 DTO 中，必填字段为空、数量不满足 @Positive、文本超过 @Size。
     * 汇总字段级和对象级错误，返回 HTTP 400 和 PARAM_ERROR。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        /**
         * 这里使用getAllErrors ＋   Collectors.joining("; ") 是列出所有的参数错误
         */
        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(this::formatObjectError)
                .collect(Collectors.joining("; "));

        return buildResponse(ErrorCode.PARAM_ERROR, defaultIfBlank(message, ErrorCode.PARAM_ERROR.getMessage()));
    }

    /**
     * 处理缺少必填 @RequestParam 参数的异常，例如接口要求传入 pageNo，但请求没有提供。
     * 也包括参数转换后变成 null、且框架将其判定为缺失的情况。
     * 此处理器不处理 JSON 请求体中的缺失字段；返回 HTTP 400 和 PARAM_ERROR。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        String message = exception.getMessage();
        return buildResponse(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理路径参数、查询参数类型转换失败。
     * 例如 productId 要求 Long，但客户端传入 abc。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {

        String parameterName = exception.getName();
        Class<?> requiredType = exception.getRequiredType();

        String message;
        if (requiredType == Long.class || requiredType == long.class
                || requiredType == Integer.class || requiredType == int.class) {
            message = "参数 " + parameterName + " 格式错误，必须是有效范围内的整数";
        } else if (requiredType == Boolean.class || requiredType == boolean.class) {
            message = "参数 " + parameterName + " 格式错误，请使用 true 或 false";
        } else {
            message = "参数 " + parameterName + " 格式错误，请检查参数格式";
        }

        return buildResponse(ErrorCode.PARAM_ERROR, message);
    }


    /**
     * 处理以 ConstraintViolationException 形式抛出的约束校验异常。
     * 典型场景是 @Validated 方法校验发现路径参数上的 @Positive、@NotBlank 等约束不满足。
     * 汇总约束路径和错误信息，返回 HTTP 400 和 PARAM_ERROR。
     * Spring MVC 原生方法校验可能抛出其他异常类型，不由本方法直接处理。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));

        return buildResponse(ErrorCode.PARAM_ERROR, defaultIfBlank(message, ErrorCode.PARAM_ERROR.getMessage()));
    }

    /**
     * 处理 HTTP 请求体无法读取或反序列化的异常。
     * 例如必填请求体为空、JSON 语法错误、字段值无法转换为目标 Java 类型。
     * 小数转整数只有在 Jackson 配置为拒绝该转换时才会触发此类异常。
     * 返回 HTTP 400 和 PARAM_ERROR，不向客户端暴露底层反序列化细节。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return buildResponse(ErrorCode.PARAM_ERROR, "请求体为空、JSON 格式错误或字段类型不匹配，"
                + "请检查请求参数");
    }

    /**
     * 兜底处理没有匹配到更具体异常处理方法的 Exception，例如空指针、未转换的数据库异常。
     * 记录完整异常日志，向客户端返回 HTTP 500 和 SYSTEM_ERROR。
     * 当前未单独处理的请求异常也可能落入此处，因此到达此方法不一定代表服务端故障。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception exception) {
        log.error("Unhandled system exception", exception);
        return buildResponse(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMessage());
    }

    /**
     * 响应构建辅助方法，不直接捕获异常。
     * 根据 ErrorCode 设置 HTTP 状态码，并将错误码和消息包装成统一失败响应。
     */
    private ResponseEntity<ApiResult<Void>> buildResponse(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResult.fail(errorCode.getCode(), message));
    }

    /**
     * 格式化请求对象的绑定或校验错误，不直接捕获异常。
     * 字段错误输出“字段名: 错误说明”，对象级错误输出“对象名: 错误说明”。
     */
    private String formatObjectError(ObjectError error) {
        String message = defaultIfBlank(error.getDefaultMessage(), ErrorCode.PARAM_ERROR.getMessage());
        if (error instanceof FieldError fieldError) {
            return fieldError.getField() + ": " + message;
        }
        return error.getObjectName() + ": " + message;
    }

    /**
     * 格式化单条约束违反信息，不直接捕获异常。
     * 输出“约束属性路径: 错误说明”，便于定位方法参数或对象属性上的校验失败。
     */
    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": "
                + defaultIfBlank(violation.getMessage(), ErrorCode.PARAM_ERROR.getMessage());
    }

    /**
     * 错误消息兜底辅助方法，不直接捕获异常。
     * 原始消息为 null、空字符串或纯空白时返回默认消息，否则保留原始消息。
     */
    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
