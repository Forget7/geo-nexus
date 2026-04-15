package com.geonexus.api.v1;

import com.geonexus.service.StreamingChatService;
import com.geonexus.service.StreamingChatService.ChatMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式聊天控制器 - SSE实时推送
 */
@Slf4j
@Tag(name = "StreamingChat", description = "流式聊天服务 - 支持SSE实时推送")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class StreamingChatController {
    
    private final StreamingChatService chatService;
    
    // SSE连接超时时间：10分钟
    private static final long SSE_TIMEOUT = 10 * 60 * 1000L;
    
    // 本地存储SSE发射器，用于主动关闭
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    
    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    @Operation(summary = "创建会话", description = "创建新的聊天会话，返回sessionId")
    public ResponseEntity<Map<String, String>> createSession() {
        String sessionId = UUID.randomUUID().toString();
        log.info("创建新会话: {}", sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "created"
        ));
    }
    
    /**
     * 建立SSE连接 - 订阅会话消息流
     */
    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流订阅", description = "建立SSE连接，订阅指定会话的消息流")
    public SseEmitter subscribeSession(@PathVariable String sessionId) {
        log.info("SSE连接建立: sessionId={}", sessionId);
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        activeEmitters.put(sessionId, emitter);
        
        // 发送连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("{\"type\":\"connected\",\"content\":\"连接已建立\",\"sessionId\":\"" + sessionId + "\"}"));
        } catch (Exception e) {
            log.error("发送连接事件失败", e);
        }
        
        // 订阅消息流
        Flux<ChatMessage> messageFlux = chatService.subscribe(sessionId);
        
        messageFlux.subscribe(
                message -> {
                    try {
                        String data = String.format(
                                "{\"type\":\"%s\",\"content\":\"%s\",\"sessionId\":\"%s\",\"mapUrl\":\"%s\",\"timestamp\":\"%s\"}",
                                escapeJson(message.type()),
                                escapeJson(message.content() != null ? message.content() : ""),
                                escapeJson(message.sessionId() != null ? message.sessionId() : ""),
                                escapeJson(message.mapUrl() != null ? message.mapUrl() : ""),
                                message.timestamp().toString()
                        );
                        emitter.send(SseEmitter.event().name("message").data(data));
                    } catch (Exception e) {
                        log.error("发送消息失败: {}", e.getMessage());
                    }
                },
                error -> {
                    log.error("流式消息错误: {}", error.getMessage());
                    try {
                        emitter.send(SseEmitter.event().name("error")
                                .data("{\"type\":\"error\",\"content\":\"连接异常\"}"));
                    } catch (Exception e) {
                        log.error("发送错误事件失败", e);
                    }
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("流式消息完成: sessionId={}", sessionId);
                    emitter.complete();
                }
        );
        
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: sessionId={}", sessionId);
            activeEmitters.remove(sessionId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE连接超时: sessionId={}", sessionId);
            activeEmitters.remove(sessionId);
        });
        
        emitter.onError(e -> {
            log.error("SSE错误: sessionId={}, error={}", sessionId, e.getMessage());
            activeEmitters.remove(sessionId);
        });
        
        return emitter;
    }
    
    /**
     * 发送消息并处理（同步模式，返回完整响应）
     */
    @PostMapping("/sessions/{sessionId}/message")
    @Operation(summary = "发送消息", description = "发送消息并处理，返回完整响应（非流式）")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        String model = request.getOrDefault("model", "default");
        
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "消息内容不能为空"
            ));
        }
        
        log.info("收到消息: sessionId={}, message={}", sessionId, message);
        
        StringBuilder fullResponse = new StringBuilder();
        String mapUrl = null;
        
        chatService.processStreamChat(sessionId, message, model)
                .doOnNext(msg -> {
                    if (msg.content() != null) {
                        if (msg.type().equals("chunk")) {
                            fullResponse.append(msg.content());
                        } else if (msg.type().equals("done") && msg.mapUrl() != null) {
                            mapUrl = msg.mapUrl();
                        }
                    }
                })
                .blockLast(Duration.ofSeconds(60));
        
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "response", fullResponse.toString(),
                "mapUrl", mapUrl != null ? mapUrl : ""
        ));
    }
    
    /**
     * 发送消息并返回流式响应（SSE推送）
     */
    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送消息", description = "发送消息并通过SSE流式返回响应")
    public Flux<ChatMessage> sendMessageStream(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        String model = request.getOrDefault("model", "default");
        
        log.info("流式消息: sessionId={}, message={}", sessionId, message);
        
        return chatService.processStreamChat(sessionId, message, model);
    }
    
    /**
     * 关闭会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "关闭会话", description = "关闭指定的聊天会话")
    public ResponseEntity<Map<String, String>> closeSession(@PathVariable String sessionId) {
        log.info("关闭会话: sessionId={}", sessionId);
        
        // 关闭SSE连接
        SseEmitter emitter = activeEmitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("close").data("{\"type\":\"close\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("关闭SSE连接失败: {}", e.getMessage());
            }
        }
        
        // 关闭服务会话
        chatService.closeSession(sessionId);
        
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "closed"
        ));
    }
    
    /**
     * 获取活跃会话数
     */
    @GetMapping("/sessions/count")
    @Operation(summary = "获取活跃会话数")
    public ResponseEntity<Map<String, Object>> getActiveSessionCount() {
        return ResponseEntity.ok(Map.of(
                "count", chatService.getActiveSessionCount(),
                "activeEmitters", activeEmitters.size()
        ));
    }
    
    /**
     * JSON字符串转义
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
