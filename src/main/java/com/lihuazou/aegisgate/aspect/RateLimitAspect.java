package com.lihuazou.aegisgate.aspect;

import com.lihuazou.aegisgate.annotation.RateLimit;
import com.lihuazou.aegisgate.core.RedisRateLimitExecutor;
import com.lihuazou.aegisgate.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final RedisRateLimitExecutor redisExecutor;

    public RateLimitAspect(RedisRateLimitExecutor redisExecutor) {
        this.redisExecutor = redisExecutor;
    }

    @Around("@annotation(com.lihuazou.aegisgate.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String key = buildKey(method, rateLimit);

        boolean allowed = redisExecutor.tryAcquire(key, rateLimit.maxRequests(), rateLimit.window());

        if (!allowed) {
            log.warn("触发限流，key={}", key);
            throw new RateLimitException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String buildKey(Method method, RateLimit rateLimit) {
        if (!rateLimit.key().isEmpty()) {
            return "rate_limit:" + rateLimit.key();
        }
        return "rate_limit:" +
                method.getDeclaringClass().getName() +
                "#" +
                method.getName();
    }
}
