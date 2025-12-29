package com.lihuazou.aegisgate.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CircuitBreaker {

    /**
     * 失败率阈值（0.0 ~ 1.0）
     */
    double failureThreshold() default 0.5;

    /**
     * 统计窗口时间（秒）
     */
    long window() default 60;

    /**
     * 熔断开启时间（秒）
     */
    long openTime() default 30;

    /**
     * 熔断触发提示
     */
    String message() default "请求被熔断，请稍后重试";
}
