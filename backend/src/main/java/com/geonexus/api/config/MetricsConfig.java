package com.geonexus.api.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指标配置 - Prometheus/Micrometer 指标收集
 */
@Configuration
public class MetricsConfig {
    
    // 请求计数器
    private final Counter chatRequestCounter;
    private final Counter mapRequestCounter;
    private final Counter dataRequestCounter;
    private final Counter toolRequestCounter;
    
    // 错误计数器
    private final Counter errorCounter;
    
    // 活跃请求数
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    // 计时器
    private final Timer chatResponseTimer;
    
    public MetricsConfig(MeterRegistry registry) {
        // 请求计数
        this.chatRequestCounter = Counter.builder("geonexus_requests_total")
                .tag("endpoint", "chat")
                .description("Total chat requests")
                .register(registry);
        
        this.mapRequestCounter = Counter.builder("geonexus_requests_total")
                .tag("endpoint", "map")
                .description("Total map requests")
                .register(registry);
        
        this.dataRequestCounter = Counter.builder("geonexus_requests_total")
                .tag("endpoint", "data")
                .description("Total data requests")
                .register(registry);
        
        this.toolRequestCounter = Counter.builder("geonexus_requests_total")
                .tag("endpoint", "tools")
                .description("Total tool requests")
                .register(registry);
        
        // 错误计数
        this.errorCounter = Counter.builder("geonexus_errors_total")
                .description("Total errors")
                .register(registry);
        
        // 活跃请求
        Gauge.builder("geonexus_active_requests", activeRequests, AtomicInteger::get)
                .description("Active requests")
                .register(registry);
        
        // 响应时间
        this.chatResponseTimer = Timer.builder("geonexus_chat_response_time")
                .description("Chat response time")
                .register(registry);
    }
    
    public Counter getChatRequestCounter() { return chatRequestCounter; }
    public Counter getMapRequestCounter() { return mapRequestCounter; }
    public Counter getDataRequestCounter() { return dataRequestCounter; }
    public Counter getToolRequestCounter() { return toolRequestCounter; }
    public Counter getErrorCounter() { return errorCounter; }
    public Timer getChatResponseTimer() { return chatResponseTimer; }
    
    public int incrementActiveRequests() {
        return activeRequests.incrementAndGet();
    }
    
    public int decrementActiveRequests() {
        return activeRequests.decrementAndGet();
    }
}
