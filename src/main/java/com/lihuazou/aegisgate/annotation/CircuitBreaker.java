package com.lihuazou.aegisgate.annotation;

import java.lang.annotation.*;

/**
 * 分布式熔断注解
 * 用于方法级熔断保护
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CircuitBreaker {

    /**
     * 熔断唯一标识
     * 默认使用方法签名
     */
    String key() default "";

    /**
     * 失败率阈值（0.0 ~ 1.0）
     */
    double failureThreshold() default 0.5;

    /**
     * 最少请求数，低于该数不进行熔断
     */
    int minimumRequest() default 10;

    /**
     * 统计窗口时间（秒）
     */
    long window() default 60;

    /**
     * 熔断开启时间（秒），触发熔断后经过此时间尝试半开状态
     */
    long resetTimeout() default 30;

    /**
     * 回退方法名称
     */
    String fallbackMethod() default "";

    /**
     * 熔断触发提示
     */
    String message() default "请求被熔断，请稍后重试";
}
