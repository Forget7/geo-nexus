package com.geonexus.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API限流拦截器
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    // 简单实现 - 生产环境应使用Redis
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_SIZE_MS = 60_000;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = getClientId(request);
        long now = System.currentTimeMillis();
        
        synchronized (requestCounts) {
            Long lastWindow = windowStart.get(clientId);
            
            // 新窗口或窗口过期
            if (lastWindow == null || now - lastWindow > WINDOW_SIZE_MS) {
                windowStart.put(clientId, now);
                requestCounts.put(clientId, new AtomicInteger(1));
                return true;
            }
            
            AtomicInteger count = requestCounts.get(clientId);
            if (count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for client: {}", clientId);
                response.setStatus(429);
                response.setContentType("application/json");
                response.setHeader("Retry-After", "60");
                response.getWriter().write("{\"error\":\"Too many requests\",\"retryAfterSeconds\":60}");
                return false;
            }
        }
        
        return true;
    }
    
    private String getClientId(HttpServletRequest request) {
        // 优先使用X-Forwarded-For头（负载均衡环境）
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        // 其次使用X-Real-IP
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        // 最后使用RemoteAddr
        return request.getRemoteAddr();
    }
}
