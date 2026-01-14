package com.csa.official.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default ""; // 限流 Key 的前缀

    int time() default 300; // 时间窗口 (秒)

    int count() default 10; // 允许请求的次数
}