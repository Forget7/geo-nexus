package com.geonexus.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Counted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * LLM服务 - 统一调用GPT/Claude/Gemini等模型
 * 支持熔断、重试、降级、Token统计
 */
@Slf4j
@Service
public class LLMService {
    
    @Value("${geonexus.llm.api-key:}")
    private String apiKey;
    
    @Value("${geonexus.llm.base-url:}")
    private String baseUrl;
    
    @Value("${geonexus.llm.default-model:gpt-4o}")
    private String defaultModel;
    
    @Value("${geonexus.llm.timeout:60s}")
    private Duration timeout;
    
    @Value("${geonexus.llm.fallback-model:gpt-3.5-turbo}")
    private String fallbackModel;
    
    @Value("${llm.ollama.base-url:}")
    private String ollamaBaseUrl;
    
    private final WebClient webClient;
    
    public LLMService() {
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    /**
     * 聊天完成 - 包含熔断、重试、Token统计
     */
    @CircuitBreaker(name = "llm", fallbackMethod = "chatFallback")
    @Retry(name = "llm", maxAttempts = 3)
    @Counted(value = "llm.chat", description = "LLM chat invocation count")
    public Mono<String> chat(String model, List<Map<String, String>> messages) {
        String actualModel = model != null ? model : defaultModel;
        
        return switch (getProvider(actualModel)) {
            case "openai" -> chatOpenAI(actualModel, messages);
            case "anthropic" -> chatAnthropic(actualModel, messages);
            case "google" -> chatGoogle(actualModel, messages);
            default -> Mono.error(new RuntimeException("Unsupported model: " + model));
        };
    }
    
    /**
     * Fallback降级方法 - GPT-4降级到GPT-3.5
     */
    public Mono<String> chatFallback(String model, List<Map<String, String>> messages, Throwable t) {
        log.warn("LLM调用失败，降级到{}: model={}, error={}", fallbackModel, model, t.getMessage());
        
        // 降级模型不使用GPT-4o，防止循环降级
        String actualModel = model != null && !model.contains("gpt-3.5") 
                ? fallbackModel 
                : model;
        
        return switch (getProvider(actualModel)) {
            case "openai" -> chatOpenAI(actualModel, messages);
            case "anthropic" -> chatAnthropic(actualModel, messages);
            case "google" -> chatGoogle(actualModel, messages);
            default -> chatOpenAI(actualModel, messages);
        };
    }
    
    /**
     * 流式聊天
     */
    @CircuitBreaker(name = "llm", fallbackMethod = "chatStreamFallback")
    @Retry(name = "llm", maxAttempts = 3)
    @Counted(value = "llm.chatstream", description = "LLM streaming chat invocation count")
    public Mono<String> chatStream(String model, List<Map<String, String>> messages) {
        return chat(model, messages);
    }
    
    /**
     * 流式聊天降级
     */
    public Mono<String> chatStreamFallback(String model, List<Map<String, String>> messages, Throwable t) {
        log.warn("LLM流式调用失败，降级: model={}, error={}", model, t.getMessage());
        return chatFallback(model, messages, t);
    }
    
    // ==================== OpenAI ====================
    
    private Mono<String> chatOpenAI(String model, List<Map<String, String>> messages) {
        String endpoint = getEndpoint("openai", model);
        
        Map<String, Object> request = new HashMap<>();
        request.put("model", getModelName(model, "openai"));
        request.put("messages", convertMessages(messages));
        request.put("temperature", 0.7);
        request.put("max_tokens", 2000);
        
        return webClient.post()
                .uri(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    var choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        var message = (Map<String, Object>) choices.get(0).get("message");
                        String content = (String) message.get("content");
                        // Token统计
                        countTokenUsage(model, response);
                        return content;
                    }
                    return "";
                })
                .timeout(timeout)
                .doOnError(e -> log.error("OpenAI API error", e));
    }
    
    // ==================== Anthropic ====================
    
    private Mono<String> chatAnthropic(String model, List<Map<String, String>> messages) {
        String endpoint = getEndpoint("anthropic", model);
        
        String systemPrompt = "";
        List<Map<String, String>> conversation = new ArrayList<>();
        
        for (Map<String, String> msg : messages) {
            if ("system".equals(msg.get("role"))) {
                systemPrompt = msg.get("content");
            } else {
                conversation.add(msg);
            }
        }
        
        Map<String, Object> request = new HashMap<>();
        request.put("model", getModelName(model, "anthropic"));
        request.put("messages", conversation);
        request.put("system", systemPrompt);
        request.put("max_tokens", 2000);
        
        return webClient.post()
                .uri(endpoint)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String content = (String) response.get("content");
                    // Token统计
                    countTokenUsage(model, response);
                    return content;
                })
                .timeout(timeout)
                .doOnError(e -> log.error("Anthropic API error", e));
    }
    
    // ==================== Google ====================
    
    private Mono<String> chatGoogle(String model, List<Map<String, String>> messages) {
        String endpoint = getEndpoint("google", model);
        
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            if (!"system".equals(msg.get("role"))) {
                contents.add(Map.of(
                        "role", "user".equals(msg.get("role")) ? "user" : "model",
                        "parts", List.of(Map.of("text", msg.get("content")))
                ));
            }
        }
        
        Map<String, Object> request = new HashMap<>();
        request.put("contents", contents);
        request.put("generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 2000
        ));
        
        return webClient.post()
                .uri(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    var candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        var content = (Map<String, Object>) candidates.get(0).get("content");
                        @SuppressWarnings("unchecked")
                        var parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            String text = (String) parts.get(0).get("text");
                            // Token统计
                            countTokenUsage(model, response);
                            return text;
                        }
                    }
                    return "";
                })
                .timeout(timeout)
                .doOnError(e -> log.error("Google API error", e));
    }
    
    // ==================== Token统计 ====================
    
    /**
     * 统计Token使用量
     */
    @Counted(value = "llm.tokens.prompt", description = "Prompt token count")
    private void countPromptTokens(String model, int tokens) {
        log.debug("Prompt tokens for {}: {}", model, tokens);
    }
    
    /**
     * 统计completion token使用量
     */
    @Counted(value = "llm.tokens.completion", description = "Completion token count")
    private void countCompletionTokens(String model, int tokens) {
        log.debug("Completion tokens for {}: {}", model, tokens);
    }
    
    /**
     * 统计总Token使用量
     */
    @Counted(value = "llm.tokens.total", description = "Total token count")
    private void countTokenUsage(String model, Map<String, Object> response) {
        try {
            // OpenAI格式
            if (response.containsKey("usage")) {
                @SuppressWarnings("unchecked")
                var usage = (Map<String, Object>) response.get("usage");
                int promptTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue();
                int completionTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).intValue();
                int totalTokens = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
                
                log.info("LLM Token usage for {}: prompt={}, completion={}, total={}", 
                        model, promptTokens, completionTokens, totalTokens);
            }
            // Anthropic格式
            else if (response.containsKey("usage")) {
                @SuppressWarnings("unchecked")
                var usage = (Map<String, Object>) response.get("usage");
                int inputTokens = ((Number) usage.getOrDefault("input_tokens", 0)).intValue();
                int outputTokens = ((Number) usage.getOrDefault("output_tokens", 0)).intValue();
                
                log.info("LLM Token usage for {}: input={}, output={}", 
                        model, inputTokens, outputTokens);
            }
        } catch (Exception e) {
            log.warn("Failed to parse token usage: {}", e.getMessage());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private String getProvider(String model) {
        if (model.startsWith("gpt") || model.startsWith("o1") || model.startsWith("o3")) {
            return "openai";
        } else if (model.startsWith("claude")) {
            return "anthropic";
        } else if (model.startsWith("gemini")) {
            return "google";
        } else if (model.startsWith("ollama/")) {
            return "ollama";
        }
        return "openai"; // 默认
    }
    
    private String getEndpoint(String provider, String model) {
        String base = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : getDefaultBaseUrl(provider);
        
        return switch (provider) {
            case "openai" -> base + "/v1/chat/completions";
            case "anthropic" -> base + "/v1/messages";
            case "google" -> base + "/v1beta/models/" + getModelName(model, "google") + ":generateContent";
            case "ollama" -> base + "/api/chat";
            default -> base + "/v1/chat/completions";
        };
    }
    
    private String getDefaultBaseUrl(String provider) {
        return switch (provider) {
            case "openai" -> "https://api.openai.com";
            case "anthropic" -> "https://api.anthropic.com";
            case "google" -> "https://generativelanguage.googleapis.com";
            case "ollama" -> (ollamaBaseUrl != null && !ollamaBaseUrl.isEmpty() ? ollamaBaseUrl : "http://localhost:11434");
            default -> "https://api.openai.com";
        };
    }
    
    private String getModelName(String model, String provider) {
        if (model.contains("/")) {
            return model.substring(model.indexOf("/") + 1);
        }
        return model;
    }
    
    private List<Map<String, String>> convertMessages(List<Map<String, String>> messages) {
        List<Map<String, String>> converted = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            converted.add(new HashMap<>(msg));
        }
        return converted;
    }
    
    /**
     * 检查API Key是否配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    /**
     * 获取支持的模型列表
     */
    public List<String> getSupportedModels() {
        return List.of(
                // OpenAI
                "gpt-4o", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo",
                // Anthropic
                "claude-3-5-sonnet-latest", "claude-3-opus-latest", "claude-3-sonnet-latest",
                // Google
                "gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro",
                // Ollama
                "ollama/llama3", "ollama/mistral", "ollama/qwen2"
        );
    }
}
