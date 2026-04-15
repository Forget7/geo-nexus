package com.geonexus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多模型提供商服务 - 支持OpenAI、Anthropic、Google、Ollama
 */
@Slf4j
@Service
public class LLMProviderService {
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;
    
    @Value("${google.api.key:}")
    private String googleApiKey;
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // 模型配置
    private static final Map<String, ModelConfig> MODEL_CONFIGS = Map.of(
            "gpt-4o", new ModelConfig("openai", "gpt-4o", 128000, 3.0, 15.0),
            "gpt-4-turbo", new ModelConfig("openai", "gpt-4-turbo", 128000, 10.0, 30.0),
            "gpt-3.5-turbo", new ModelConfig("openai", "gpt-3.5-turbo", 16385, 0.5, 1.5),
            "claude-3-5-sonnet", new ModelConfig("anthropic", "claude-3-5-sonnet-20241022", 200000, 3.0, 15.0),
            "claude-3-opus", new ModelConfig("anthropic", "claude-3-opus-20240229", 200000, 15.0, 75.0),
            "gemini-1.5-pro", new ModelConfig("google", "gemini-1.5-pro", 1000000, 0.0, 0.0),
            "gemini-1.5-flash", new ModelConfig("google", "gemini-1.5-flash", 1000000, 0.0, 0.0),
            "llama3", new ModelConfig("ollama", "llama3", 8192, 0.0, 0.0),
            "mistral", new ModelConfig("ollama", "mistral", 8192, 0.0, 0.0)
    );
    
    // 使用统计
    private final Map<String, UsageStats> usageStats = new ConcurrentHashMap<>();
    
    public LLMProviderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * 聊天完成
     */
    public LLMResponse chat(ChatRequest request) {
        String model = request.model != null ? request.model : "gpt-4o";
        ModelConfig config = MODEL_CONFIGS.getOrDefault(model, MODEL_CONFIGS.get("gpt-4o"));
        
        try {
            LLMResponse response = switch (config.provider) {
                case "openai" -> chatOpenAI(request, config);
                case "anthropic" -> chatAnthropic(request, config);
                case "google" -> chatGoogle(request, config);
                case "ollama" -> chatOllama(request, config);
                default -> throw new IllegalArgumentException("Unknown provider: " + config.provider);
            };
            
            // 记录使用统计
            recordUsage(model, response);
            
            return response;
            
        } catch (Exception e) {
            log.error("LLM chat failed for model {}: {}", model, e.getMessage());
            throw new RuntimeException("LLM request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式聊天
     */
    public void streamChat(ChatRequest request, StreamCallback callback) {
        String model = request.model != null ? request.model : "gpt-4o";
        ModelConfig config = MODEL_CONFIGS.getOrDefault(model, MODEL_CONFIGS.get("gpt-4o"));
        
        try {
            switch (config.provider) {
                case "openai" -> streamOpenAI(request, config, callback);
                case "anthropic" -> streamAnthropic(request, config, callback);
                case "google" -> streamGoogle(request, config, callback);
                case "ollama" -> streamOllama(request, config, callback);
                default -> callback.onError(new IllegalArgumentException("Unknown provider: " + config.provider));
            }
        } catch (Exception e) {
            log.error("LLM stream failed for model {}: {}", model, e.getMessage());
            callback.onError(e);
        }
    }
    
    // ==================== OpenAI ====================
    
    private LLMResponse chatOpenAI(ChatRequest request, ModelConfig config) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.modelName);
        body.put("messages", request.messages);
        body.put("temperature", request.temperature != null ? request.temperature : 0.7);
        
        if (request.maxTokens != null) {
            body.put("max_tokens", request.maxTokens);
        }
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.body());
        }
        
        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No response from OpenAI");
        }
        
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        
        return LLMResponse.builder()
                .content((String) message.get("content"))
                .model(modelName(config))
                .usage(parseUsage(responseBody))
                .finishReason((String) choices.get(0).get("finish_reason"))
                .build();
    }
    
    private void streamOpenAI(ChatRequest request, ModelConfig config, StreamCallback callback) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.modelName);
        body.put("messages", request.messages);
        body.put("temperature", request.temperature != null ? request.temperature : 0.7);
        body.put("stream", true);
        
        if (request.maxTokens != null) {
            body.put("max_tokens", request.maxTokens);
        }
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            callback.onError(new RuntimeException("OpenAI API error: " + response.body()));
            return;
        }
        
        // 处理SSE流
        String[] lines = response.body().split("\n");
        StringBuilder content = new StringBuilder();
        
        for (String line : lines) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if (data.equals("[DONE]")) break;
                
                try {
                    Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                        if (delta != null && delta.get("content") != null) {
                            String token = (String) delta.get("content");
                            content.append(token);
                            callback.onToken(token);
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
        
        callback.onComplete(content.toString());
    }
    
    // ==================== Anthropic ====================
    
    private LLMResponse chatAnthropic(ChatRequest request, ModelConfig config) throws Exception {
        String url = "https://api.anthropic.com/v1/messages";
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.modelName);
        body.put("messages", request.messages);
        body.put("temperature", request.temperature != null ? request.temperature : 0.7);
        
        if (request.maxTokens != null) {
            body.put("max_tokens", Math.min(request.maxTokens, 4096));
        } else {
            body.put("max_tokens", 1024);
        }
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic API error: " + response.body());
        }
        
        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
        
        return LLMResponse.builder()
                .content((String) responseBody.get("content"))
                .model(modelName(config))
                .usage(Usage.builder()
                        .inputTokens((Integer) responseBody.get("usage.input_tokens"))
                        .outputTokens((Integer) responseBody.get("usage.output_tokens"))
                        .build())
                .finishReason((String) responseBody.get("stop_reason"))
                .build();
    }
    
    private void streamAnthropic(ChatRequest request, ModelConfig config, StreamCallback callback) throws Exception {
        // Anthropic流式处理类似OpenAI
        chatAnthropic(request, config); // 简化为非流式
    }
    
    // ==================== Google ====================
    
    private LLMResponse chatGoogle(ChatRequest request, ModelConfig config) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" 
                + config.modelName + ":generateContent?key=" + googleApiKey;
        
        Map<String, Object> body = new HashMap<>();
        
        // 转换消息格式
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> msg : request.messages) {
            contents.add(Map.of(
                    "role", msg.get("role").equals("user") ? "user" : "model",
                    "parts", List.of(Map.of("text", msg.get("content")))
            ));
        }
        body.put("contents", contents);
        
        if (request.temperature != null) {
            body.put("temperature", request.temperature);
        }
        
        if (request.maxTokens != null) {
            body.put("maxOutputTokens", request.maxTokens);
        }
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Google API error: " + response.body());
        }
        
        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
        
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No response from Google");
        }
        
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> part : parts) {
            if (part.get("text") != null) {
                text.append(part.get("text"));
            }
        }
        
        return LLMResponse.builder()
                .content(text.toString())
                .model(modelName(config))
                .build();
    }
    
    private void streamGoogle(ChatRequest request, ModelConfig config, StreamCallback callback) throws Exception {
        chatGoogle(request, config);
    }
    
    // ==================== Ollama ====================
    
    private LLMResponse chatOllama(ChatRequest request, ModelConfig config) throws Exception {
        String url = ollamaBaseUrl + "/api/chat";
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.modelName);
        
        // 转换消息格式
        List<Map<String, String>> messages = new ArrayList<>();
        for (Map<String, String> msg : request.messages) {
            messages.add(Map.of(
                    "role", msg.get("role"),
                    "content", msg.get("content")
            ));
        }
        body.put("messages", messages);
        
        if (request.temperature != null) {
            body.put("temperature", request.temperature);
        }
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API error: " + response.body());
        }
        
        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
        Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
        
        return LLMResponse.builder()
                .content((String) message.get("content"))
                .model(config.modelName)
                .build();
    }
    
    private void streamOllama(ChatRequest request, ModelConfig config, StreamCallback callback) throws Exception {
        String url = ollamaBaseUrl + "/api/chat";
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.modelName);
        
        List<Map<String, String>> messages = new ArrayList<>();
        for (Map<String, String> msg : request.messages) {
            messages.add(Map.of(
                    "role", msg.get("role"),
                    "content", msg.get("content")
            ));
        }
        body.put("messages", messages);
        body.put("stream", true);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            callback.onError(new RuntimeException("Ollama API error: " + response.body()));
            return;
        }
        
        String[] lines = response.body().split("\n");
        StringBuilder content = new StringBuilder();
        
        for (String line : lines) {
            if (!line.isEmpty()) {
                try {
                    Map<String, Object> chunk = objectMapper.readValue(line, Map.class);
                    Map<String, Object> message = (Map<String, Object>) chunk.get("message");
                    if (message != null && message.get("content") != null) {
                        String token = (String) message.get("content");
                        content.append(token);
                        callback.onToken(token);
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
        }
        
        callback.onComplete(content.toString());
    }
    
    // ==================== 工具方法 ====================
    
    private Usage parseUsage(Map<String, Object> responseBody) {
        if (responseBody.containsKey("usage")) {
            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            return Usage.builder()
                    .inputTokens((Integer) usage.get("prompt_tokens"))
                    .outputTokens((Integer) usage.get("completion_tokens"))
                    .totalTokens((Integer) usage.get("total_tokens"))
                    .build();
        }
        return null;
    }
    
    private String modelName(ModelConfig config) {
        return config.provider + ":" + config.modelName;
    }
    
    private void recordUsage(String model, LLMResponse response) {
        UsageStats stats = usageStats.computeIfAbsent(model, k -> new UsageStats());
        stats.requestCount.incrementAndGet();
        if (response.usage != null) {
            if (response.usage.inputTokens != null) {
                stats.totalInputTokens.addAndGet(response.usage.inputTokens);
            }
            if (response.usage.outputTokens != null) {
                stats.totalOutputTokens.addAndGet(response.usage.outputTokens);
            }
        }
    }
    
    public Map<String, UsageStats> getUsageStats() {
        return new HashMap<>(usageStats);
    }
    
    public List<String> getAvailableModels() {
        return new ArrayList<>(MODEL_CONFIGS.keySet());
    }
    
    // ==================== 内部类 ====================
    
    public static class ChatRequest {
        public String model;
        public List<Map<String, String>> messages;
        public Double temperature;
        public Integer maxTokens;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class LLMResponse {
        private String content;
        private String model;
        private Usage usage;
        private String finishReason;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class Usage {
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
    }
    
    @lombok.Data
    private static class ModelConfig {
        String provider;
        String modelName;
        int contextWindow;
        double inputCostPer1M;
        double outputCostPer1M;
    }
    
    @lombok.Data
    public static class UsageStats {
        public final AtomicInteger requestCount = new AtomicInteger(0);
        public final AtomicInteger totalInputTokens = new AtomicInteger(0);
        public final AtomicInteger totalOutputTokens = new AtomicInteger(0);
    }
    
    public interface StreamCallback {
        void onToken(String token);
        void onComplete(String fullContent);
        void onError(Exception e);
    }
}
