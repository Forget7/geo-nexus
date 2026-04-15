package com.geonexus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM Provider Service - 统一管理多个LLM提供商
 * 提供模型列表和使用统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMProviderService {

    private final LLMService llmService;

    /**
     * 获取所有可用的模型列表
     */
    public List<String> getAvailableModels() {
        return llmService.getSupportedModels();
    }

    /**
     * 使用统计
     */
    public static class UsageStats {
        public long totalRequests;
        public long totalTokens;
        public long successfulRequests;
        public long failedRequests;
        public double avgLatencyMs;
    }

    // 内存中的使用统计（生产环境应使用Redis或Metrics）
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> tokenCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> successCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failureCounts = new ConcurrentHashMap<>();

    /**
     * 获取各模型的使用统计
     */
    public Map<String, UsageStats> getUsageStats() {
        List<String> models = getAvailableModels();
        Map<String, UsageStats> statsMap = new ConcurrentHashMap<>();

        for (String model : models) {
            UsageStats stats = new UsageStats();
            stats.totalRequests = requestCounts.getOrDefault(model, new AtomicLong(0)).get();
            stats.totalTokens = tokenCounts.getOrDefault(model, new AtomicLong(0)).get();
            stats.successfulRequests = successCounts.getOrDefault(model, new AtomicLong(0)).get();
            stats.failedRequests = failureCounts.getOrDefault(model, new AtomicLong(0)).get();
            stats.avgLatencyMs = 0; // TODO: 集成Micrometer Timer
            statsMap.put(model, stats);
        }

        return statsMap;
    }

    /**
     * 记录请求（由拦截器调用）
     */
    public void recordRequest(String model, int tokens, boolean success) {
        requestCounts.computeIfAbsent(model, k -> new AtomicLong(0)).incrementAndGet();
        tokenCounts.computeIfAbsent(model, k -> new AtomicLong(0)).addAndGet(tokens);
        if (success) {
            successCounts.computeIfAbsent(model, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            failureCounts.computeIfAbsent(model, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
}
