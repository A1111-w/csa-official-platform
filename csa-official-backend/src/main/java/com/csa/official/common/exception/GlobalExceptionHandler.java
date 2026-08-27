package com.csa.official.common.exception;

import com.csa.official.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindingException(BindException e) {
        String msg = fieldErrorMessage(e.getBindingResult());
        log.warn("参数校验失败: {}", msg);
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.MISSING_PARAMETER,
                "缺少必填参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getName());
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.TYPE_MISMATCH,
                "参数类型不正确: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_REQUEST,
                "请求体格式错误或缺少必要字段");
    }

    @ExceptionHandler(CsaException.class)
    public ResponseEntity<R<Void>> handleCsaException(CsaException e) {
        HttpStatus status = resolveHttpStatus(e.getCode());
        if (status.is5xxServerError()) {
            log.error("业务处理失败 [errorCode={}, status={}]", e.getErrorCode(), status.value(), e);
        } else {
            log.warn("业务异常 [errorCode={}, status={}]: {}", e.getErrorCode(), status.value(), e.getMessage());
        }
        return ResponseEntity.status(status)
                .body(R.fail(status.value(), e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.warn("文件上传超限: {}", e.getMessage());
        return error(HttpStatus.PAYLOAD_TOO_LARGE, ApiErrorCode.PAYLOAD_TOO_LARGE,
                "文件过大，请上传小于 50MB 的文件");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据库唯一键冲突: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT,
                "数据已存在（用户名或关键信息重复）");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<R<Void>> handleDataAccessException(DataAccessException e) {
        log.error("数据库访问失败", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.DATABASE_ERROR,
                "系统繁忙，请稍后再试");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("授权失败: {}", e.getMessage());
        return error(HttpStatus.FORBIDDEN, ApiErrorCode.ACCESS_DENIED, "无权访问");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return error(HttpStatus.METHOD_NOT_ALLOWED, ApiErrorCode.METHOD_NOT_ALLOWED,
                "不支持的请求方法: " + e.getMethod());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<R<Void>> handleRuntimeException(RuntimeException e) {
        log.error("未处理的运行时异常", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR,
                "系统运行异常，请联系管理员");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("系统严重错误", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR,
                "系统繁忙，请稍后再试");
    }

    private String fieldErrorMessage(BindingResult bindingResult) {
        return buildFieldErrorMessage(bindingResult.getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; ")));
    }

    private String buildFieldErrorMessage(String details) {
        if (details == null || details.isBlank()) {
            return "参数错误";
        }
        return "参数错误: " + details;
    }

    private HttpStatus resolveHttpStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status != null && status.isError() ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ResponseEntity<R<Void>> error(HttpStatus status, ApiErrorCode errorCode, String message) {
        return ResponseEntity.status(status)
                .body(R.fail(status.value(), errorCode.name(), message));
    }
}
