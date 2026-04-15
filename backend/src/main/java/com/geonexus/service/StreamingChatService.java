package com.geonexus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式聊天服务 - 支持WebSocket流式输出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingChatService {
    
    private final LLMService llmService;
    
    // 订阅者管理
    private final Map<String, Sinks.Many<ChatMessage>> sessionSinks = new ConcurrentHashMap<>();
    
    /**
     * 聊天消息
     */
    public record ChatMessage(
            String type,        // connected, message, error, done
            String content,
            String sessionId,
            String mapUrl,
            LocalDateTime timestamp
    ) {
        public static ChatMessage connected(String sessionId) {
            return new ChatMessage("connected", "连接已建立", sessionId, null, LocalDateTime.now());
        }
        
        public static ChatMessage processing(String content) {
            return new ChatMessage("processing", content, null, null, LocalDateTime.now());
        }
        
        public static ChatMessage chunk(String content) {
            return new ChatMessage("chunk", content, null, null, LocalDateTime.now());
        }
        
        public static ChatMessage done(String content, String mapUrl) {
            return new ChatMessage("done", content, null, mapUrl, LocalDateTime.now());
        }
        
        public static ChatMessage error(String error) {
            return new ChatMessage("error", error, null, null, LocalDateTime.now());
        }
    }
    
    /**
     * 订阅会话的流
     */
    public Flux<ChatMessage> subscribe(String sessionId) {
        Sinks.Many<ChatMessage> sink = sessionSinks.computeIfAbsent(sessionId, 
                id -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux();
    }
    
    /**
     * 发布消息到会话
     */
    private void emit(String sessionId, ChatMessage message) {
        Sinks.Many<ChatMessage> sink = sessionSinks.get(sessionId);
        if (sink != null) {
            sink.tryEmitNext(message);
        }
    }
    
    /**
     * 处理流式聊天
     */
    public Flux<ChatMessage> processStreamChat(String sessionId, String message, String model) {
        // 发送处理中状态
        emit(sessionId, ChatMessage.processing("正在理解您的问题..."));
        
        // 构建消息列表
        var messages = java.util.List.of(
                Map.of("role", "system", "content", getSystemPrompt()),
                Map.of("role", "user", "content", message)
        );
        
        // 如果LLM未配置，返回模拟流
        if (!llmService.isConfigured()) {
            return generateMockStream(sessionId, message);
        }
        
        try {
            // 调用LLM（这里应该使用流式API，但简化处理）
            String response = llmService.chat(model, messages).block();
            
            if (response != null) {
                // 模拟逐字输出
                return Flux.range(0, response.length())
                        .map(i -> {
                            String chunk = response.substring(0, i + 1);
                            return ChatMessage.chunk(chunk);
                        })
                        .concatWith(Flux.just(
                                response.contains("地图") 
                                        ? ChatMessage.done(response, "/api/v1/map/demo_" + sessionId.substring(0, 8) + "/html")
                                        : ChatMessage.done(response, null)
                        ));
            }
            
            return Flux.just(ChatMessage.error("Empty response from LLM"));
            
        } catch (Exception e) {
            log.error("流式聊天处理失败", e);
            return Flux.just(ChatMessage.error("处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 生成模拟流式响应
     */
    private Flux<ChatMessage> generateMockStream(String sessionId, String message) {
        String response;
        String mapUrl = null;
        
        if (message.contains("距离") || message.contains("distance")) {
            response = "我可以帮您计算距离。请提供两个坐标点，例如：\n\n" +
                      "• 北京 (39.9042, 116.4074)\n" +
                      "• 上海 (31.2304, 121.4737)\n\n" +
                      "请问您要计算哪两个地点之间的距离？";
        } else if (message.contains("缓冲") || message.contains("buffer")) {
            response = "缓冲区分析功能已就绪。请提供：\n\n" +
                      "1. 几何对象（点、线、面）\n" +
                      "2. 缓冲距离（公里）\n\n" +
                      "系统将计算指定距离内的缓冲区域。";
        } else if (message.contains("地图") || message.contains("map")) {
            response = "地图生成功能已就绪！\n\n" +
                      "我可以为您生成：\n" +
                      "🗺️ 2D地图 (Leaflet)\n" +
                      "🌍 3D地图 (Cesium)\n\n" +
                      "请告诉我您想在地图上展示什么内容？";
            mapUrl = "/api/v1/map/demo_" + sessionId.substring(0, 8) + "/html";
        } else {
            response = "收到您的消息：\"" + message + "\"\n\n" +
                      "我是GeoNexus数字GIS专家系统，我可以帮您：\n\n" +
                      "🗺️ **空间分析**\n" +
                      "   • 距离计算\n" +
                      "   • 缓冲区分析\n" +
                      "   • 叠加分析\n\n" +
                      "📊 **地图可视化**\n" +
                      "   • 2D/3D交互地图\n" +
                      "   • 热力图\n" +
                      "   • 分级设色图\n\n" +
                      "🔄 **数据处理**\n" +
                      "   • 格式转换\n" +
                      "   • 坐标投影\n\n" +
                      "请告诉我您需要什么帮助？";
        }
        
        // 模拟逐字输出
        final String finalResponse = response;
        final String finalMapUrl = mapUrl;
        
        return Flux.range(0, finalResponse.length())
                .map(i -> ChatMessage.chunk(finalResponse.substring(0, i + 1)))
                .concatWith(Flux.just(
                        ChatMessage.done(finalResponse, finalMapUrl)
                ));
    }
    
    /**
     * 获取系统提示
     */
    private String getSystemPrompt() {
        return """
                你是一个专业的GIS（地理信息系统）助手。你的能力包括：
                
                1. **空间分析**：缓冲区分析、距离计算、叠加分析、空间连接
                2. **地图可视化**：生成2D/3D交互式地图
                3. **数据处理**：格式转换、坐标投影转换
                4. **地理编码**：地址与坐标的相互转换
                
                当用户询问GIS相关问题时，请提供专业、准确的回答。
                如果需要生成地图，请明确说明将使用什么底图和图层。
                
                始终用简洁、易懂的语言解释专业概念。
                
                回复格式建议：
                - 使用 Markdown 格式化
                - 适当使用 Emoji 增加可读性
                - 重要的数据或结论用粗体标注
                """;
    }
    
    /**
     * 关闭会话
     */
    public void closeSession(String sessionId) {
        Sinks.Many<ChatMessage> sink = sessionSinks.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
    
    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return sessionSinks.size();
    }
}
