package com.csa.official.common.result;

import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private Integer code; // 200:成功, 500:错误
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "Success";
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.code = 500;
        r.message = msg;
        return r;
    }

    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.message = msg;
        return r;
    }
}