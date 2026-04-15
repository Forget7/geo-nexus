package com.geonexus.aspect;

import com.geonexus.annotation.Audited;
import com.geonexus.config.ChatContextHolder;
import com.geonexus.service.EnterpriseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计切面 - 自动记录带有 @Audited 注解的方法调用
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final EnterpriseService enterpriseService;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        String userId = ChatContextHolder.getCurrentUser();
        String action = audited.action();
        String resourceType = audited.resourceType();
        String methodName = pjp.getSignature().getName();
        Map<String, Object> details = new HashMap<>();

        // 记录方法签名
        details.put("method", methodName);
        details.put("class", pjp.getTarget().getClass().getSimpleName());

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            // 记录参数
            if (audited.logParams()) {
                details.put("params", getParams(pjp));
            }

            // 执行方法
            result = pjp.proceed();

            // 记录结果
            if (audited.logResult() && result != null) {
                details.put("result", result.toString());
            }

            details.put("success", true);
            return result;
        } catch (Throwable t) {
            error = t;
            details.put("success", false);
            details.put("error", t.getMessage());
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            details.put("durationMs", duration);

            // 记录审计日志
            try {
                enterpriseService.logAudit(
                        com.geonexus.service.EnterpriseService.AuditLog.builder()
                                .id(java.util.UUID.randomUUID().toString())
                                .userId(userId != null ? userId : "system")
                                .action(action)
                                .resource(resourceType)
                                .resourceId(extractResourceId(result))
                                .details(details)
                                .timestamp(System.currentTimeMillis())
                                .build()
                );
            } catch (Exception e) {
                log.error("审计日志记录失败: method={}, error={}", methodName, e.getMessage());
            }

            log.debug("[Audit] {} {} {}.{} ({}ms) - {}",
                    userId, action, resourceType, methodName, duration, error != null ? "FAILED" : "OK");
        }
    }

    private Map<String, Object> getParams(ProceedingJoinPoint pjp) {
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = pjp.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                // 避免记录敏感参数
                if (!isSensitive(paramNames[i])) {
                    params.put(paramNames[i], sanitize(paramValues[i]));
                }
            }
        }
        return params;
    }

    private boolean isSensitive(String paramName) {
        return paramName.toLowerCase().contains("password")
                || paramName.toLowerCase().contains("secret")
                || paramName.toLowerCase().contains("token")
                || paramName.toLowerCase().contains("key");
    }

    private Object sanitize(Object value) {
        if (value == null) return null;
        String str = value.toString();
        // 截断过长的值
        if (str.length() > 500) {
            return str.substring(0, 500) + "...[truncated]";
        }
        return value;
    }

    private String extractResourceId(Object result) {
        if (result == null) return null;
        // 尝试从返回对象中提取 ID
        try {
            if (result instanceof java.util.Map) {
                return String.valueOf(((java.util.Map<?, ?>) result).get("id"));
            }
            Method getId = result.getClass().getMethod("getId");
            return String.valueOf(getId.invoke(result));
        } catch (Exception e) {
            return null;
        }
    }
}
