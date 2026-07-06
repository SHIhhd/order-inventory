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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.stream.Collectors;

/**
 * 【学习】
 * @RestControllerAdvice 意思是
 * Controller 参数校验异常
 * Service 抛出的业务异常
 * Controller 方法内部运行时异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        String message = defaultIfBlank(exception.getMessage(), errorCode.getMessage());
        return buildResponse(errorCode, message);
    }

    /**
     * 【学习】
     * Controller 参数校验异常
     * @param exception
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(this::formatObjectError)
                .collect(Collectors.joining("; "));

        return buildResponse(ErrorCode.PARAM_ERROR, defaultIfBlank(message, ErrorCode.PARAM_ERROR.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        String message = exception.getMessage();
        return buildResponse(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));

        return buildResponse(ErrorCode.PARAM_ERROR, defaultIfBlank(message, ErrorCode.PARAM_ERROR.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return buildResponse(ErrorCode.PARAM_ERROR, "请求体不能为空或 JSON 格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception exception) {
        log.error("Unhandled system exception", exception);
        return buildResponse(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMessage());
    }

    private ResponseEntity<ApiResult<Void>> buildResponse(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResult.fail(errorCode.getCode(), message));
    }

    private String formatObjectError(ObjectError error) {
        String message = defaultIfBlank(error.getDefaultMessage(), ErrorCode.PARAM_ERROR.getMessage());
        if (error instanceof FieldError fieldError) {
            return fieldError.getField() + ": " + message;
        }
        return error.getObjectName() + ": " + message;
    }

    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": "
                + defaultIfBlank(violation.getMessage(), ErrorCode.PARAM_ERROR.getMessage());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
