package com.lihuazou.aegisgate.annotation;

/**
 * 限流维度
 */
public enum LimitType {

    /**
     * 默认（方法级）
     */
    DEFAULT,

    /**
     * 按 IP 限流
     */
    IP,

    /**
     * 按用户限流
     */
    USER,

    /**
     * 自定义 key（完全交给使用者）
     */
    CUSTOM
}
