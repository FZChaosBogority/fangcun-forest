package com.fangcun.forest.admin.aspect;

import com.fangcun.forest.admin.security.AdminDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 5)
public class AdminAuditAspect {
    private static final Logger log = LoggerFactory.getLogger("ADMIN_AUDIT");
    @Around("@annotation(action)")
    public Object audit(ProceedingJoinPoint point, AdminAction action) throws Throwable {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null && auth.getPrincipal() instanceof AdminDetails admin ? admin.getUserId().toString() : "system";
        Object[] arguments = point.getArgs();
        String target = arguments.length > 0 && arguments[0] instanceof Long id ? id.toString() : "new";
        boolean success = false;
        try {
            Object result = point.proceed();
            if ("new".equals(target) && result instanceof Long id) target = id.toString();
            success = true; return result;
        }
        finally { log.info("adminId={} action={} target={} success={}", actor, action.value(), target, success); }
    }
}
