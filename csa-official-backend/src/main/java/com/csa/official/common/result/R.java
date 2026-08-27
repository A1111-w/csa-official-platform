package com.csa.official.common.result;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.observability.TraceContext;
import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private Integer code; // 200:成功, 500:错误
    private String message;
    private T data;
    private String errorCode;
    private String traceId;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "Success";
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(500, ApiErrorCode.INTERNAL_ERROR.name(), msg);
    }

    public static <T> R<T> fail(Integer code, String msg) {
        return fail(code, ApiErrorCode.fromHttpStatus(code).name(), msg);
    }

    public static <T> R<T> fail(Integer code, String errorCode, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.message = msg;
        r.errorCode = errorCode;
        r.traceId = TraceContext.currentTraceId();
        return r;
    }
}
