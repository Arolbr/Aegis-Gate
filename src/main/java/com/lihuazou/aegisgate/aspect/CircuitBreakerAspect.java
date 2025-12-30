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

        String key = cb.key().isEmpty()
                ? "circuit_breaker:" + method.getDeclaringClass().getName() + "#" + method.getName()
                : "circuit_breaker:" + cb.key();

        try {
            Object result = joinPoint.proceed();
            // 成功调用
            executor.tryExecute(key, cb.failureThreshold(), cb.minimumRequest(),
                    cb.window(), cb.resetTimeout(), false);
            return result;
        } catch (Exception e) {
            // 失败调用
            boolean allowed = executor.tryExecute(key, cb.failureThreshold(), cb.minimumRequest(),
                    cb.window(), cb.resetTimeout(), true);
            if (!allowed) {
                log.warn("触发熔断，key={}", key);
                // 调用回退方法
                if (!cb.fallbackMethod().isEmpty()) {
                    Method fallback = method.getDeclaringClass().getMethod(cb.fallbackMethod(), method.getParameterTypes());
                    return fallback.invoke(joinPoint.getTarget(), joinPoint.getArgs());
                }
                throw new CircuitBreakerException(cb.message());
            }
            throw e;
        }
    }
}
