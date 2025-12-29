package com.lihuazou.aegisgate.core;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class RedisRateLimitExecutor {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    public RedisRateLimitExecutor(StringRedisTemplate redisTemplate) throws IOException {
        this.redisTemplate = redisTemplate;

        ClassPathResource resource = new ClassPathResource("lua/rate_limit.lua");
        String lua = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptText(lua);
        this.redisScript.setResultType(Long.class);
    }

    public boolean tryAcquire(String key, long maxRequests, long windowSeconds) {
        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(maxRequests),
                String.valueOf(windowSeconds * 1000)
        );
        return result == 1;
    }
}
