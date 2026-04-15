package com.geonexus.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.stereotype.Component;

import com.geonexus.service.StreamingChatService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket配置 - 支持流式AI响应
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/v1/chat")
                .setAllowedOrigins("*");
        registry.addHandler(realtimeWebSocketHandler, "/ws/v1/realtime")
                .setAllowedOrigins("*");
    }
}

/**
 * 聊天WebSocket处理器 - 支持流式LLM输出
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final StreamingChatService streamingChatService;
    
    // 在线会话
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket连接建立: {}", session.getId());
        
        // 发送欢迎消息
        session.sendMessage(new org.springframework.web.socket.TextMessage(
                "{\"type\":\"connected\",\"sessionId\":\"" + session.getId() + "\"}"
        ));
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到消息: {} from {}", payload, session.getId());
        
        // 解析消息
        // 格式: {"action": "chat", "message": "...", "sessionId": "..."}
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(payload, Map.class);
            
            String action = (String) data.get("action");
            
            switch (action) {
                case "chat" -> handleChat(session, data);
                case "ping" -> session.sendMessage(new org.springframework.web.socket.TextMessage(
                        "{\"type\":\"pong\",\"timestamp\":" + System.currentTimeMillis() + "}"));
                default -> session.sendMessage(new org.springframework.web.socket.TextMessage(
                        "{\"type\":\"error\",\"message\":\"Unknown action: " + action + "\"}"));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            session.sendMessage(new org.springframework.web.socket.TextMessage(
                    "{\"type\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}"
            ));
        }
    }
    
    private void handleChat(WebSocketSession session, Map<String, Object> data) {
        String textMessage = (String) data.get("message");
        String sessionId = (String) data.getOrDefault("sessionId", session.getId());
        
        if (textMessage == null || textMessage.isBlank()) {
            try {
                session.sendMessage(new org.springframework.web.socket.TextMessage(
                        "{\"type\":\"error\",\"message\":\"Message cannot be empty\"}"
                ));
            } catch (Exception e) {
                log.error("发送错误消息失败", e);
            }
            return;
        }
        
        log.info("处理聊天消息: sessionId={}, message={}", sessionId, textMessage);
        
        // 发送处理中状态
        try {
            session.sendMessage(new org.springframework.web.socket.TextMessage(
                    "{\"type\":\"processing\",\"message\":\"正在思考...\"}"
            ));
        } catch (Exception e) {
            log.error("发送处理中状态失败", e);
        }
        
        // 调用流式聊天服务并逐块发送响应
        try {
            streamingChatService.processStreamChat(sessionId, textMessage, null)
                    .subscribe(
                        // onNext: 发送每个块
                        chatMessage -> {
                            try {
                                if (session.isOpen()) {
                                    String json = String.format(
                                            "{\"type\":\"%s\",\"content\":\"%s\",\"sessionId\":\"%s\"%s}",
                                            chatMessage.type(),
                                            escapeJson(chatMessage.content()),
                                            chatMessage.sessionId() != null ? chatMessage.sessionId() : sessionId,
                                            chatMessage.mapUrl() != null ? ",\"mapUrl\":\"" + escapeJson(chatMessage.mapUrl()) + "\"" : ""
                                    );
                                    session.sendMessage(new org.springframework.web.socket.TextMessage(json));
                                }
                            } catch (Exception e) {
                                log.error("发送流式消息失败: {}", e.getMessage());
                            }
                        },
                        // onError
                        error -> {
                            log.error("流式聊天出错", error);
                            try {
                                if (session.isOpen()) {
                                    session.sendMessage(new org.springframework.web.socket.TextMessage(
                                            "{\"type\":\"error\",\"message\":\"" + escapeJson(error.getMessage()) + "\"}"
                                    ));
                                }
                            } catch (Exception e) {
                                log.error("发送错误消息失败", e);
                            }
                        },
                        // onComplete
                        () -> {
                            log.debug("流式聊天完成: {}", sessionId);
                        }
                    );
            
        } catch (Exception e) {
            log.error("启动流式聊天失败", e);
            try {
                session.sendMessage(new org.springframework.web.socket.TextMessage(
                        "{\"type\":\"error\",\"message\":\"处理失败: " + escapeJson(e.getMessage()) + "\"}"
                ));
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }
    
    /**
     * JSON字符串转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket连接关闭: {}, status: {}", session.getId(), status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", session.getId(), exception);
        sessions.remove(session.getId());
    }
    
    /**
     * 广播消息到所有连接
     */
    public void broadcast(String message) {
        sessions.values().forEach(s -> {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new org.springframework.web.socket.TextMessage(message));
                }
            } catch (Exception e) {
                log.error("广播消息失败", e);
            }
        });
    }
    
    /**
     * 发送消息到指定会话
     */
    public void sendToSession(String targetSessionId, String message) {
        WebSocketSession s = sessions.get(targetSessionId);
        if (s != null && s.isOpen()) {
            try {
                s.sendMessage(new org.springframework.web.socket.TextMessage(message));
            } catch (Exception e) {
                log.error("发送消息到会话失败: {}", targetSessionId, e);
            }
        }
    }
    
    /**
     * 获取在线会话数
     */
    public int getOnlineCount() {
        return sessions.size();
    }
}
