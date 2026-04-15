package com.geonexus.api.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API日志切面 - 结构化日志记录
 */
@Slf4j
@Component
@Aspect
public class LoggingAspect {
    
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    
    // 敏感参数名称（不记录值）
    private static final String[] SENSITIVE_PARAMS = {
            "password", "token", "secret", "apiKey", "authorization", "credential"
    };
    
    @Around("@annotation(org.springframework.web.bind.annotation.RestController)")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes != null 
                ? ((ServletRequestAttributes) requestAttributes).getRequest() 
                : null;
        
        // 生成请求ID
        String requestId = request != null 
                ? request.getHeader(REQUEST_ID_HEADER) != null 
                        ? request.getHeader(REQUEST_ID_HEADER) 
                        : UUID.randomUUID().toString()
                : UUID.randomUUID().toString();
        
        // 构建日志上下文
        Map<String, Object> logContext = new HashMap<>();
        logContext.put("requestId", requestId);
        logContext.put("method", request != null ? request.getMethod() : "INTERNAL");
        logContext.put("path", request != null ? request.getRequestURI() : "internal");
        logContext.put("class", joinPoint.getTarget().getClass().getSimpleName());
        logContext.put("action", joinPoint.getSignature().getName());
        
        // 记录入参
        logContext.put("args", sanitizeArgs(joinPoint.getArgs()));
        log.info("[API] Request started: {}", formatLog(logContext));
        
        Object result = null;
        Throwable error = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            logContext.put("duration", duration);
            logContext.put("success", error == null);
            
            if (error != null) {
                logContext.put("error", error.getClass().getSimpleName());
                logContext.put("errorMessage", error.getMessage());
            }
            
            log.info("[API] Request completed: {}", formatLog(logContext));
        }
    }
    
    @Around("@annotation(org.springframework.stereotype.Service)")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> logContext = new HashMap<>();
        logContext.put("class", joinPoint.getTarget().getClass().getSimpleName());
        logContext.put("method", joinPoint.getSignature().getName());
        logContext.put("args", sanitizeArgs(joinPoint.getArgs()));
        
        try {
            Object result = joinPoint.proceed();
            log.debug("[SVC] {} took {}ms", 
                    joinPoint.getSignature().getName(), 
                    System.currentTimeMillis() - startTime);
            return result;
        } catch (Throwable e) {
            log.warn("[SVC] {} failed after {}ms: {}", 
                    joinPoint.getSignature().getName(),
                    System.currentTimeMillis() - startTime,
                    e.getMessage());
            throw e;
        }
    }
    
    /**
     * 异步任务日志
     */
    @Before("@annotation(org.springframework.scheduling.annotation.Async)")
    public void logAsyncStart(JoinPoint joinPoint) {
        log.info("[ASYNC] Starting async task: {}.{}()",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName());
    }
    
    /**
     * 异常日志（专门处理）
     */
    @AfterThrowing(pointcut = "@annotation(org.springframework.web.bind.annotation.RestController)", 
                   throwing = "exception")
    public void logApiException(JoinPoint joinPoint, Throwable exception) {
        Map<String, Object> logContext = new HashMap<>();
        logContext.put("class", joinPoint.getTarget().getClass().getSimpleName());
        logContext.put("method", joinPoint.getSignature().getName());
        logContext.put("exception", exception.getClass().getName());
        logContext.put("message", exception.getMessage());
        
        log.error("[API-ERROR] {}: {}", 
                exception.getClass().getSimpleName(),
                formatLog(logContext));
    }
    
    /**
     * 清理敏感参数
     */
    private Object sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        Object[] sanitized = new Object[args.length];
        MethodSignature signature = null;
        
        try {
            signature = (MethodSignature) args[0];
        } catch (Exception ignored) {}
        
        String[] paramNames = signature != null ? signature.getParameterNames() : null;
        
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            
            if (arg == null) {
                sanitized[i] = null;
            } else if (paramNames != null && isSensitive(paramNames[i])) {
                sanitized[i] = "***REDACTED***";
            } else if (arg instanceof String && ((String) arg).length() > 1000) {
                sanitized[i] = ((String) arg).substring(0, 1000) + "...[truncated]";
            } else if (arg.getClass().isArray()) {
                sanitized[i] = "Array[" + ((Object[]) arg).length + "]";
            } else {
                sanitized[i] = arg;
            }
        }
        
        return Arrays.asList(sanitized);
    }
    
    private boolean isSensitive(String paramName) {
        String lower = paramName.toLowerCase();
        for (String sensitive : SENSITIVE_PARAMS) {
            if (lower.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }
    
    private String formatLog(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        context.forEach((key, value) -> {
            sb.append(key).append("=").append(value).append(" ");
        });
        return sb.toString().trim();
    }
}
