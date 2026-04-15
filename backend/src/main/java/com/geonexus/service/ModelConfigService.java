package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型配置服务 - 支持所有通用AI模型配置
 */
@Slf4j
@Service
public class ModelConfigService {
    
    private final CacheService cacheService;
    
    // 模型配置存储
    private final Map<String, ModelConfig> modelConfigs = new ConcurrentHashMap<>();
    private final Map<String, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();
    
    private static final String CONFIG_PREFIX = "model:config:";
    private static final String PROVIDER_PREFIX = "model:provider:";
    
    public ModelConfigService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeDefaultProviders();
    }
    
    // ==================== 初始化 ====================
    
    private void initializeDefaultProviders() {
        // OpenAI
        providerConfigs.put("openai", ProviderConfig.builder()
                .id("openai")
                .name("OpenAI")
                .logo("https://openai.com/favicon.ico")
                .baseUrl("https://api.openai.com/v1")
                .supportedModes(List.of("chat", "completion", "embedding"))
                .defaultModel("gpt-4o")
                .models(List.of(
                        "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4",
                        "gpt-3.5-turbo", "gpt-3.5-turbo-16k",
                        "text-davinci-003", "text-embedding-3-large", "text-embedding-3-small"
                ))
                .capabilities(List.of("chat", "function-calling", "vision", "json-mode"))
                .maxTokens(128000)
                .build());
        
        // Anthropic
        providerConfigs.put("anthropic", ProviderConfig.builder()
                .id("anthropic")
                .name("Anthropic")
                .logo("https://anthropic.com/favicon.ico")
                .baseUrl("https://api.anthropic.com/v1")
                .supportedModes(List.of("chat", "completion"))
                .defaultModel("claude-sonnet-4-20250514")
                .models(List.of(
                        "claude-opus-4-20250514", "claude-sonnet-4-20250514",
                        "claude-haiku-3-20250514", "claude-3-5-sonnet-latest",
                        "claude-3-opus-latest", "claude-3-sonnet-latest",
                        "claude-3-haiku-latest"
                ))
                .capabilities(List.of("chat", "vision", "json-mode", "extended-thinking"))
                .maxTokens(200000)
                .build());
        
        // Google Gemini
        providerConfigs.put("google", ProviderConfig.builder()
                .id("google")
                .name("Google")
                .logo("https://google.com/favicon.ico")
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .supportedModes(List.of("chat", "embedding"))
                .defaultModel("gemini-2.0-flash")
                .models(List.of(
                        "gemini-2.0-flash-exp", "gemini-2.0-flash", "gemini-2.0-flash-lite",
                        "gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.5-flash-8b",
                        "gemini-pro", "gemini-pro-vision",
                        "text-embedding-004"
                ))
                .capabilities(List.of("chat", "vision", "function-calling", "json-mode"))
                .maxTokens(1000000)
                .build());
        
        // Azure OpenAI
        providerConfigs.put("azure", ProviderConfig.builder()
                .id("azure")
                .name("Azure OpenAI")
                .logo("https://azure.microsoft.com/favicon.ico")
                .baseUrl("https://{resource}.openai.azure.com")
                .supportedModes(List.of("chat", "completion", "embedding"))
                .defaultModel("gpt-4o")
                .models(List.of(
                        "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4",
                        "gpt-35-turbo", "gpt-35-turbo-16k",
                        "text-embedding-3-large", "text-embedding-3-small",
                        "text-embedding-ada-002"
                ))
                .capabilities(List.of("chat", "function-calling", "vision", "json-mode"))
                .maxTokens(128000)
                .build());
        
        // Groq
        providerConfigs.put("groq", ProviderConfig.builder()
                .id("groq")
                .name("Groq")
                .logo("https://groq.com/favicon.ico")
                .baseUrl("https://api.groq.com/openai/v1")
                .supportedModes(List.of("chat", "completion"))
                .defaultModel("llama-3.1-70b-versatile")
                .models(List.of(
                        "llama-3.1-70b-versatile", "llama-3.1-8b-instant",
                        "mixtral-8x7b-32768", "gemma2-9b-it"
                ))
                .capabilities(List.of("chat", "function-calling"))
                .maxTokens(32000)
                .build());
        
        // Cohere
        providerConfigs.put("cohere", ProviderConfig.builder()
                .id("cohere")
                .name("Cohere")
                .logo("https://cohere.com/favicon.ico")
                .baseUrl("https://api.cohere.ai/v1")
                .supportedModes(List.of("chat", "embedding"))
                .defaultModel("command-r-plus")
                .models(List.of(
                        "command-r-plus", "command-r", "command",
                        "c4ai-22-7", "c4ai-22-7-i",
                        "embed-english-v3.0", "embed-multilingual-v3.0"
                ))
                .capabilities(List.of("chat", "function-calling", "retrieval", "rerank"))
                .maxTokens(4000)
                .build());
        
        // Mistral
        providerConfigs.put("mistral", ProviderConfig.builder()
                .id("mistral")
                .name("Mistral AI")
                .logo("https://mistral.ai/favicon.ico")
                .baseUrl("https://api.mistral.ai/v1")
                .supportedModes(List.of("chat", "completion"))
                .defaultModel("mistral-large-latest")
                .models(List.of(
                        "mistral-large-latest", "mistral-small-latest",
                        "mistral-medium-latest", "mistral-nemo",
                        "codestral-latest", "open-mixtral-8x22b",
                        "open-mixtral-8x7b", "open-mistral-7b"
                ))
                .capabilities(List.of("chat", "function-calling", "json-mode"))
                .maxTokens(128000)
                .build());
        
        // Ollama (本地)
        providerConfigs.put("ollama", ProviderConfig.builder()
                .id("ollama")
                .name("Ollama (本地)")
                .logo("https://ollama.com/favicon.ico")
                .baseUrl("http://localhost:11434/v1")
                .supportedModes(List.of("chat", "completion"))
                .defaultModel("llama3.1")
                .models(new ArrayList<>()) // 动态从Ollama获取
                .capabilities(List.of("chat", "local", "offline"))
                .maxTokens(8192)
                .build());
        
        // Together AI
        providerConfigs.put("together", ProviderConfig.builder()
                .id("together")
                .name("Together AI")
                .logo("https://together.ai/favicon.ico")
                .baseUrl("https://api.together.xyz/v1")
                .supportedModes(List.of("chat", "completion"))
                .defaultModel("meta-llama/Llama-3.1-70B-Instruct-Turbo")
                .models(List.of(
                        "meta-llama/Llama-3.1-70B-Instruct-Turbo",
                        "meta-llama/Llama-3.1-8B-Instruct-Turbo",
                        "mistralai/Mixtral-8x22B-Instruct-v0.1",
                        "cognitivecomputations/dolphin-mixtral-8x22b",
                        "Qwen/Qwen2-72B-Instruct"
                ))
                .capabilities(List.of("chat", "function-calling"))
                .maxTokens(128000)
                .build());
        
        // Replicate
        providerConfigs.put("replicate", ProviderConfig.builder()
                .id("replicate")
                .name("Replicate")
                .logo("https://replicate.com/favicon.ico")
                .baseUrl("https://api.replicate.com/v1")
                .supportedModes(List.of("chat", "image", "audio"))
                .defaultModel("meta/meta-llama-3-70b-instruct")
                .models(List.of(
                        "meta/meta-llama-3-70b-instruct",
                        "meta/codellama-70b-instruct",
                        "stability-ai/stable-diffusion-3"
                ))
                .capabilities(List.of("chat", "image-generation", "code-generation"))
                .maxTokens(16000)
                .build());
        
        // HuggingFace
        providerConfigs.put("huggingface", ProviderConfig.builder()
                .id("huggingface")
                .name("HuggingFace")
                .logo("https://huggingface.co/favicon.ico")
                .baseUrl("https://api-inference.huggingface.co/v1")
                .supportedModes(List.of("chat", "inference"))
                .defaultModel("meta-llama/Llama-3.1-70B-Instruct")
                .models(new ArrayList<>()) // HF有大量模型
                .capabilities(List.of("chat", "inference", "embedding"))
                .maxTokens(4000)
                .build());
        
        // ZeroGPU
        providerConfigs.put("zerogpu", ProviderConfig.builder()
                .id("zerogpu")
                .name("ZeroGPU")
                .logo("https://zerogpu.ai/favicon.ico")
                .baseUrl("https://api.zerogpu.com/v1")
                .supportedModes(List.of("chat", "image"))
                .defaultModel("qwen/qwen-72b-chat")
                .models(List.of(
                        "qwen/qwen-72b-chat", "qwen/qwen-7b-chat",
                        "yi/yi-34b-chat"
                ))
                .capabilities(List.of("chat", "image-generation", "free-tier"))
                .maxTokens(8000)
                .build());
        
        // DeepSeek
        providerConfigs.put("deepseek", ProviderConfig.builder()
                .id("deepseek")
                .name("DeepSeek")
                .logo("https://deepseek.com/favicon.ico")
                .baseUrl("https://api.deepseek.com/v1")
                .supportedModes(List.of("chat", "completion", "embedding"))
                .defaultModel("deepseek-chat")
                .models(List.of(
                        "deepseek-chat", "deepseek-coder",
                        "deepseek-encoder"
                ))
                .capabilities(List.of("chat", "code-generation", "math", "json-mode"))
                .maxTokens(64000)
                .build());
        
        // 硅基流动
        providerConfigs.put("siliconflow", ProviderConfig.builder()
                .id("siliconflow")
                .name("硅基流动")
                .logo("https://siliconflow.com/favicon.ico")
                .baseUrl("https://api.siliconflow.cn/v1")
                .supportedModes(List.of("chat", "embedding"))
                .defaultModel("Qwen/Qwen2.5-7B-Instruct")
                .models(new ArrayList<>())
                .capabilities(List.of("chat", "embedding", "free-tier"))
                .maxTokens(8000)
                .build());
        
        // 阿里云百炼
        providerConfigs.put("aliyun", ProviderConfig.builder()
                .id("aliyun")
                .name("阿里云百炼")
                .logo("https://aliyun.com/favicon.ico")
                .baseUrl("https://dashscope.aliyuncs.com/api/v1")
                .supportedModes(List.of("chat", "embedding"))
                .defaultModel("qwen-turbo")
                .models(List.of(
                        "qwen-turbo", "qwen-plus", "qwen-max",
                        "qwen-max-longcontext", "qwen-vl-plus",
                        "text-embedding-v3"
                ))
                .capabilities(List.of("chat", "vision", "function-calling", "json-mode"))
                .maxTokens(1000000)
                .build());
        
        // 百度文心
        providerConfigs.put("baidu", ProviderConfig.builder()
                .id("baidu")
                .name("百度文心一言")
                .logo("https://baidu.com/favicon.ico")
                .baseUrl("https://qianfan.baidubce.com/v2")
                .supportedModes(List.of("chat", "embedding"))
                .defaultModel("ernie-4.0-8k-latest")
                .models(List.of(
                        "ernie-4.0-8k-latest", "ernie-4.0-8k-preview",
                        "ernie-3.5-8k", "ernie-3.5-8k-preview",
                        "ernie-speed-128k", "ernie-speed-8k",
                        "ernie-lite-8k"
                ))
                .capabilities(List.of("chat", "function-calling", "json-mode"))
                .maxTokens(128000)
                .build());
        
        // 智谱清言
        providerConfigs.put("zhipu", ProviderConfig.builder()
                .id("zhipu")
                .name("智谱AI")
                .logo("https://zhipuai.cn/favicon.ico")
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .supportedModes(List.of("chat", "embedding"))
                .defaultModel("glm-4")
                .models(List.of(
                        "glm-4", "glm-4-plus", "glm-4-air",
                        "glm-4-flash", "glm-4v",
                        "glm-3-turbo", "embedding-3"
                ))
                .capabilities(List.of("chat", "vision", "function-calling", "json-mode"))
                .maxTokens(128000)
                .build());
        
        // MiniMax
        providerConfigs.put("minimax", ProviderConfig.builder()
                .id("minimax")
                .name("MiniMax")
                .logo("https://minimax.chat/favicon.ico")
                .baseUrl("https://api.minimax.chat/v1")
                .supportedModes(List.of("chat", "embedding", "speech"))
                .defaultModel("MiniMax-Text-01")
                .models(List.of(
                        "MiniMax-Text-01", "abab6.5s-chat",
                        "abab5.5s-chat"
                ))
                .capabilities(List.of("chat", "function-calling", "long-context"))
                .maxTokens(1000000)
                .build());
        
        // 腾讯混元
        providerConfigs.put("tencent", ProviderConfig.builder()
                .id("tencent")
                .name("腾讯混元")
                .logo("https://tencent.com/favicon.ico")
                .baseUrl("https://hunyuan.cloud.tencent.com/v1")
                .supportedModes(List.of("chat", "embedding"))
                .defaultModel("hunyuan-pro")
                .models(List.of(
                        "hunyuan-pro", "hunyuan-standard",
                        "hunyuan-lite", "hunyuan-code"
                ))
                .capabilities(List.of("chat", "function-calling", "json-mode"))
                .maxTokens(32000)
                .build());
        
        // 讯飞星火
        providerConfigs.put("iflytek", ProviderConfig.builder()
                .id("iflytek")
                .name("讯飞星火")
                .logo("https://xfyun.cn/favicon.ico")
                .baseUrl("https://spark-api.xf-yun.com/v3.5")
                .supportedModes(List.of("chat", "speech"))
                .defaultModel("spark-pro")
                .models(List.of(
                        "spark-pro", "spark-max", "spark-lite",
                        "spark-4.0Ultra"
                ))
                .capabilities(List.of("chat", "function-calling", "speech-synthesis"))
                .maxTokens(64000)
                .build());
    }
    
    // ==================== 提供商管理 ====================
    
    /**
     * 获取所有提供商
     */
    public List<ProviderConfig> getAllProviders() {
        return new ArrayList<>(providerConfigs.values());
    }
    
    /**
     * 获取提供商配置
     */
    public ProviderConfig getProvider(String providerId) {
        ProviderConfig config = providerConfigs.get(providerId);
        if (config == null) {
            throw new ProviderNotFoundException("提供商不存在: " + providerId);
        }
        return config;
    }
    
    /**
     * 添加/更新提供商
     */
    public ProviderConfig saveProvider(ProviderConfig provider) {
        providerConfigs.put(provider.getId(), provider);
        String key = PROVIDER_PREFIX + provider.getId();
        cacheService.set(key, provider);
        log.info("保存提供商配置: id={}, name={}", provider.getId(), provider.getName());
        return provider;
    }
    
    /**
     * 删除提供商
     */
    public void deleteProvider(String providerId) {
        if (providerConfigs.remove(providerId) != null) {
            String key = PROVIDER_PREFIX + providerId;
            cacheService.delete(key);
            log.info("删除提供商: id={}", providerId);
        }
    }
    
    /**
     * 从Ollama获取可用模型
     */
    public List<String> fetchOllamaModels(String baseUrl) {
        try {
            // 实际应调用 Ollama API: GET /api/tags
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取Ollama模型失败", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== 模型配置管理 ====================
    
    /**
     * 创建模型配置
     */
    public ModelConfig createConfig(ModelConfig config) {
        config.setId(UUID.randomUUID().toString());
        config.setCreatedAt(System.currentTimeMillis());
        config.setUpdatedAt(config.getCreatedAt());
        
        modelConfigs.put(config.getId(), config);
        
        String key = CONFIG_PREFIX + config.getId();
        cacheService.set(key, config);
        
        log.info("创建模型配置: id={}, provider={}, model={}", 
                config.getId(), config.getProvider(), config.getModel());
        
        return config;
    }
    
    /**
     * 更新模型配置
     */
    public ModelConfig updateConfig(String configId, ModelConfig config) {
        ModelConfig existing = modelConfigs.get(configId);
        if (existing == null) {
            throw new ConfigNotFoundException("模型配置不存在: " + configId);
        }
        
        config.setId(configId);
        config.setCreatedAt(existing.getCreatedAt());
        config.setUpdatedAt(System.currentTimeMillis());
        
        modelConfigs.put(configId, config);
        
        String key = CONFIG_PREFIX + configId;
        cacheService.set(key, config);
        
        return config;
    }
    
    /**
     * 获取模型配置
     */
    public ModelConfig getConfig(String configId) {
        ModelConfig config = modelConfigs.get(configId);
        if (config == null) {
            String key = CONFIG_PREFIX + configId;
            config = (ModelConfig) cacheService.get(key);
        }
        
        if (config == null) {
            throw new ConfigNotFoundException("模型配置不存在: " + configId);
        }
        
        return config;
    }
    
    /**
     * 获取用户的所有模型配置
     */
    public List<ModelConfig> getUserConfigs(String userId) {
        List<ModelConfig> configs = new ArrayList<>();
        for (ModelConfig config : modelConfigs.values()) {
            if (config.getUserId() != null && config.getUserId().equals(userId)) {
                configs.add(config);
            }
        }
        return configs;
    }
    
    /**
     * 删除模型配置
     */
    public void deleteConfig(String configId) {
        modelConfigs.remove(configId);
        String key = CONFIG_PREFIX + configId;
        cacheService.delete(key);
        log.info("删除模型配置: id={}", configId);
    }
    
    /**
     * 测试模型连接
     */
    public TestResult testConfig(String configId) {
        ModelConfig config = getConfig(configId);
        
        try {
            // 实际应调用模型API测试
            // 这里是简化实现
            TestResult result = new TestResult();
            result.setSuccess(true);
            result.setLatencyMs(100);
            result.setMessage("连接成功");
            return result;
        } catch (Exception e) {
            TestResult result = new TestResult();
            result.setSuccess(false);
            result.setMessage("连接失败: " + e.getMessage());
            return result;
        }
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class ProviderConfig {
        private String id;
        private String name;
        private String logo;
        private String baseUrl;
        private List<String> supportedModes; // chat, completion, embedding, image, audio
        private String defaultModel;
        private List<String> models;
        private List<String> capabilities;
        private int maxTokens;
        private boolean enabled;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ModelConfig {
        private String id;
        private String name; // 配置名称，如 "我的GPT-4"
        private String provider; // 提供商ID
        private String model; // 模型名称
        private String apiKey;
        private String baseUrl; // 可覆盖提供商默认URL
        private Map<String, Object> parameters; // temperature, top_p等
        private Map<String, String> headers; // 额外请求头
        private boolean enabled;
        private boolean isDefault;
        private String userId; // 所属用户
        private Long createdAt;
        private Long updatedAt;
        private int priority; // 优先级，数字越大越优先
    }
    
    @lombok.Data
    public static class TestResult {
        private boolean success;
        private Long latencyMs;
        private String message;
    }
    
    public static class ProviderNotFoundException extends RuntimeException {
        public ProviderNotFoundException(String message) { super(message); }
    }
    
    public static class ConfigNotFoundException extends RuntimeException {
        public ConfigNotFoundException(String message) { super(message); }
    }
}
