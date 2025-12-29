package com.lihuazou.aegisgate.aspect;

import com.lihuazou.aegisgate.annotation.CircuitBreaker;
import com.lihuazou.aegisgate.core.RedisCircuitBreakerExecutor;
import com.lihuazou.aegisgate.exception.CircuitBreakerException;
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
public class CircuitBreakerAspect {

    private final RedisCircuitBreakerExecutor executor;

    public CircuitBreakerAspect(RedisCircuitBreakerExecutor executor) {
        this.executor = executor;
    }

    @Around("@annotation(com.lihuazou.aegisgate.annotation.CircuitBreaker)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        CircuitBreaker cb = method.getAnnotation(CircuitBreaker.class);

        String key = "circuit_breaker:" + method.getDeclaringClass().getName() + "#" + method.getName();

        boolean allowed;
        try {
            Object result = joinPoint.proceed();
            // 成功调用
            allowed = executor.tryExecute(key, cb.failureThreshold(), cb.window(), cb.openTime(), false);
            return result;
        } catch (Exception e) {
            // 失败调用
            allowed = executor.tryExecute(key, cb.failureThreshold(), cb.window(), cb.openTime(), true);
            if (!allowed) {
                log.warn("触发熔断，key={}", key);
                throw new CircuitBreakerException(cb.message());
            }
            throw e;
        }
    }
}
