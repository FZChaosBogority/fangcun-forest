package com.fangcun.forest.storefront.aspect;

import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ServiceTimingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

    @Around("execution(public * com.fangcun.forest.storefront.service..*(..))")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        long started = System.nanoTime();
        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            // 不记录参数、返回数据或密码；异常继续向上传递，不改变事务结果。
            log.info("service={} success={} elapsedMs={}",
                    joinPoint.getSignature().toShortString(), success, elapsed);
        }
    }
}
