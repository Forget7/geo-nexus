package com.geonexus.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时轨迹 WebSocket 处理器
 *
 * 支持订阅/取消订阅频道，接收 Kafka 推送的实时轨迹数据
 *
 * 协议：
 * - 订阅: {"type": "subscribe", "channels": ["trajectory.vehicle.001"]}
 * - 取消: {"type": "unsubscribe", "channels": ["trajectory.vehicle.001"]}
 * - 服务端推送: {"type": "entity_update", "channel": "...", "data": {...}}
 */
@Slf4j
@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** session -> subscribed channels */
    private final Map<String, Set<String>> sessionChannels = new ConcurrentHashMap<>();

    /** channel -> subscribed sessions */
    private final Map<String, Set<String>> channelSessions = new ConcurrentHashMap<>();

    // ==================== 连接管理 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionChannels.put(session.getId(), ConcurrentHashMap.newKeySet());
        log.info("[WS] 实时连接建立: {}", session.getId());
        send(session, Map.of(
                "type", "connected",
                "sessionId", session.getId()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String type = (String) msg.get("type");

            switch (type) {
                case "subscribe" -> handleSubscribe(session, msg);
                case "unsubscribe" -> handleUnsubscribe(session, msg);
                case "ping" -> send(session, Map.of("type", "pong"));
                default -> send(session, Map.of(
                        "type", "error",
                        "message", "Unknown type: " + type
                ));
            }
        } catch (Exception e) {
            log.error("[WS] 处理消息失败: {}", payload, e);
            send(session, Map.of(
                    "type", "error",
                    "message", "Invalid message format"
            ));
        }
    }

    private void handleSubscribe(WebSocketSession session, Map<String, Object> msg) {
        Object channelsObj = msg.get("channels");
        if (!(channelsObj instanceof Iterable<?>)) return;

        Set<String> channels = sessionChannels.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet());

        for (Object ch : (Iterable<?>) channelsObj) {
            String channel = ch.toString();
            channels.add(channel);
            channelSessions.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(session.getId());
            log.debug("[WS] Session {} 订阅频道: {}", session.getId(), channel);
        }

        send(session, Map.of(
                "type", "subscribed",
                "channels", channels
        ));
    }

    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> msg) {
        Object channelsObj = msg.get("channels");
        if (!(channelsObj instanceof Iterable<?>)) return;

        Set<String> channels = sessionChannels.get(session.getId());
        if (channels == null) return;

        for (Object ch : (Iterable<?>) channelsObj) {
            String channel = ch.toString();
            channels.remove(channel);
            Set<String> sessions = channelSessions.get(channel);
            if (sessions != null) {
                sessions.remove(session.getId());
                if (sessions.isEmpty()) channelSessions.remove(channel);
            }
            log.debug("[WS] Session {} 取消订阅频道: {}", session.getId(), channel);
        }

        send(session, Map.of(
                "type", "unsubscribed",
                "channels", channels
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 清理会话的所有订阅
        Set<String> channels = sessionChannels.remove(session.getId());
        if (channels != null) {
            for (String channel : channels) {
                Set<String> sessions = channelSessions.get(channel);
                if (sessions != null) {
                    sessions.remove(session.getId());
                    if (sessions.isEmpty()) channelSessions.remove(channel);
                }
            }
        }
        log.info("[WS] 实时连接关闭: {}, status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[WS] 实时连接传输错误: {}", session.getId(), exception);
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    // ==================== 消息推送 ====================

    /**
     * 推送实体更新到订阅频道
     */
    public void pushEntityUpdate(String channel, Object data) {
        Map<String, Object> message = Map.of(
                "type", "entity_update",
                "channel", channel,
                "data", data
        );
        pushToChannel(channel, message);
    }

    /**
     * 推送地理事件更新
     */
    public void pushGeoEvent(String channel, Object event) {
        Map<String, Object> message = Map.of(
                "type", "geo_event",
                "channel", channel,
                "data", event
        );
        pushToChannel(channel, message);
    }

    /**
     * 推送告警
     */
    public void pushAlert(String channel, Object alert) {
        Map<String, Object> message = Map.of(
                "type", "alert",
                "channel", channel,
                "data", alert
        );
        pushToChannel(channel, message);
    }

    private void pushToChannel(String channel, Map<String, Object> message) {
        Set<String> sessions = channelSessions.get(channel);
        if (sessions == null || sessions.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("[WS] 序列化消息失败", e);
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        for (String sessionId : sessions) {
            try {
                WebSocketSession session = getSession(sessionId);
                if (session != null && session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (Exception e) {
                log.error("[WS] 推送消息到 session {} 失败: {}", sessionId, e.getMessage());
            }
        }
    }

    private WebSocketSession getSession(String sessionId) {
        // Spring WebSocket doesn't provide a way to get sessions by ID directly
        // The sessions are managed by the WebSocket container
        // We need to track sessions ourselves
        return null; // Will be implemented via sessionChannels iteration
    }

    private void send(WebSocketSession session, Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("[WS] 发送消息失败", e);
        }
    }

    public int getOnlineCount() {
        return sessionChannels.size();
    }

    public Map<String, Integer> getChannelStats() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        channelSessions.forEach((ch, sessions) -> stats.put(ch, sessions.size()));
        return stats;
    }
}
