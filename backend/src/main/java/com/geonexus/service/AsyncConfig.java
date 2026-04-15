package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

/**
 * 异步任务配置 - 用于长时GIS计算
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * GIS计算线程池
     */
    @Bean(name = "gisTaskExecutor")
    public Executor gisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("gis-");
        executor.setRejectedExecutionHandler((r, e) -> 
            log.warn("GIS task rejected, queue is full"));
        executor.initialize();
        return executor;
    }
    
    /**
     * LLM调用线程池
     */
    @Bean(name = "llmTaskExecutor")
    public Executor llmTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("llm-");
        executor.initialize();
        return executor;
    }
}
