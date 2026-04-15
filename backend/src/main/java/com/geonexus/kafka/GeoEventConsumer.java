package com.geonexus.kafka;

import com.geonexus.api.config.RealtimeWebSocketHandler;
import com.geonexus.event.GeoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * 地理事件消费者
 *
 * 消费 geonexus.geo-events 主题的事件数据，并推送到 WebSocket
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoEventConsumer {

    private final RealtimeWebSocketHandler realtimeHandler;

    /**
     * 消费地理事件
     */
    @KafkaListener(
            topics = "geonexus.geo-events",
            groupId = "${geonexus.kafka.consumer.group-id:geonexus-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeGeoEvent(
            @Payload GeoEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("[Kafka] 收到地理事件: eventId={}, type={}, source={}, partition={}, offset={}",
                event.getEventId(), event.getEventType(), event.getSourceId(), partition, offset);

        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("[Kafka] 处理事件失败: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 处理单个事件
     */
    protected void processEvent(GeoEvent event) {
        String channel = "trajectory." + event.getSourceId();

        switch (event.getEventType()) {
            case "trajectory_update" -> handleTrajectoryUpdate(event, channel);
            case "alert" -> handleAlert(event, channel);
            default -> {
                log.debug("[Kafka] 未处理的事件类型: {}", event.getEventType());
                realtimeHandler.pushGeoEvent(channel, event);
            }
        }
    }

    private void handleTrajectoryUpdate(GeoEvent event, String channel) {
        log.debug("[Kafka] 处理轨迹更新: sourceId={}, ({}, {})",
                event.getSourceId(), event.getLongitude(), event.getLatitude());

        // 构建 Cesium 实体更新数据
        Map<String, Object> entityData = Map.of(
                "id", event.getSourceId(),
                "position", event.getLongitude() != null && event.getLatitude() != null
                        ? java.util.List.of(event.getLongitude(), event.getLatitude())
                        : java.util.List.of(0, 0),
                "timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : java.time.Instant.now().toString(),
                "heading", event.getProperties() != null ? event.getProperties().get("heading") : 0,
                "speed", event.getProperties() != null ? event.getProperties().get("speed") : 0
        );

        realtimeHandler.pushEntityUpdate(channel, entityData);
    }

    private void handleAlert(GeoEvent event, String channel) {
        log.warn("[Kafka] 收到告警: sourceId={}, properties={}",
                event.getSourceId(), event.getProperties());
        realtimeHandler.pushAlert(channel, event);
    }
}
