package com.lihuazou.aegisgate.core;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Redis + Lua 分布式熔断执行器
 */
@Component
public class RedisCircuitBreakerExecutor {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> luaScript;

    public RedisCircuitBreakerExecutor(StringRedisTemplate redisTemplate) throws IOException {
        this.redisTemplate = redisTemplate;

        ClassPathResource resource = new ClassPathResource("lua/circuit_breaker.lua");
        String lua = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        this.luaScript = new DefaultRedisScript<>();
        this.luaScript.setScriptText(lua);
        this.luaScript.setResultType(Long.class);
    }

    /**
     * @param key 熔断标识
     * @param failureThreshold 失败率阈值
     * @param minimumRequest 最少请求数
     * @param window 统计时间窗口（秒）
     * @param resetTimeout 熔断开启时间（秒）
     * @param failure 当前请求是否失败
     * @return 是否允许执行
     */
    public boolean tryExecute(String key, double failureThreshold, int minimumRequest,
                              long window, long resetTimeout, boolean failure) {

        Long result = redisTemplate.execute(
                luaScript,
                Arrays.asList(key),
                String.valueOf(failureThreshold),
                String.valueOf(minimumRequest),
                String.valueOf(window * 1000),
                String.valueOf(resetTimeout * 1000),
                failure ? "1" : "0"
        );

        return result == 1;
    }
}
