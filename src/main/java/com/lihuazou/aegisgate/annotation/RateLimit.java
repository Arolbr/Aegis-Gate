package com.lihuazou.aegisgate.annotation;

import java.lang.annotation.*;

/**
 * 分布式限流注解
 * 用于方法级或类级限流控制
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 key
     * 支持 SpEL（如：#userId、#request.ip）
     * 为空则使用：类名 + 方法名
     */
    String key() default "";

    /**
     * 时间窗口大小（秒）
     */
    long window() default 1;

    /**
     * 窗口内最大请求数
     */
    long maxRequests();

    /**
     * 限流维度
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 被限流时的提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
