package com.lihuazou.aegisgate.core;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class RedisCircuitBreakerExecutor {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    public RedisCircuitBreakerExecutor(StringRedisTemplate redisTemplate) throws IOException {
        this.redisTemplate = redisTemplate;

        ClassPathResource resource = new ClassPathResource("lua/circuit_breaker.lua");
        String lua = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptText(lua);
        this.redisScript.setResultType(Long.class);
    }

    public boolean tryExecute(String key, double failureThreshold, long windowSeconds, long openTimeSeconds, boolean isFailure) {
        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(failureThreshold),
                String.valueOf(windowSeconds * 1000),
                String.valueOf(openTimeSeconds * 1000),
                isFailure ? "1" : "0"
        );
        return result == 1;
    }
}
