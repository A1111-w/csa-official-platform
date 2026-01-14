package com.csa.official.common.exception;

import com.csa.official.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 参数校验异常 (返回具体字段错误，方便前端提示)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<String> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg); // 记录一下警告即可
        return R.fail("参数错误: " + msg);
    }

    // 2. 业务异常 (这是我们自己抛的，直接返回给前端)
    @ExceptionHandler(CsaException.class)
    public R<String> handleCsaException(CsaException e) {
        log.warn("业务异常 [Code: {}]: {}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    // 3. 文件大小超限
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<String> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.warn("文件上传超限: {}", e.getMessage());
        return R.fail("文件过大！请上传小于 500MB 的文件");
    }

    // 4. 数据库唯一索引冲突
    @ExceptionHandler(DuplicateKeyException.class)
    public R<String> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据库唯一键冲突: {}", e.getMessage());
        return R.fail("数据已存在 (用户名或关键信息重复)");
    }

    // 5. 运行时异常 (潜在的 Bug，不能暴露细节)
    @ExceptionHandler(RuntimeException.class)
    public R<String> handleRuntimeException(RuntimeException e) {
        log.error("未处理的运行时异常", e);
        return R.fail("系统运行异常，请联系管理员");
    }

    // 6. 兜底未知异常
    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("系统严重错误", e);
        return R.fail("系统繁忙，请稍后再试");
    }
}