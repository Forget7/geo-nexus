package com.geonexus.kafka;

import com.geonexus.event.GeoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 地理事件生产者
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${geonexus.kafka.topics.geo-events:geonexus.geo-events}")
    private String geoEventsTopic;

    @Value("${geonexus.kafka.topics.trajectory:geonexus.trajectory}")
    private String trajectoryTopic;

    @Value("${geonexus.kafka.topics.alerts:geonexus.alerts}")
    private String alertsTopic;

    @Value("${geonexus.kafka.topics.analytics:geonexus.analytics}")
    private String analyticsTopic;

    /**
     * 发送地理事件
     */
    public CompletableFuture<SendResult<String, Object>> sendGeoEvent(GeoEvent event) {
        log.debug("[Kafka] 发送地理事件: eventId={}, type={}", event.getEventId(), event.getEventType());
        return kafkaTemplate.send(geoEventsTopic, event.getSourceId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] 发送事件失败: eventId={}, error={}",
                                event.getEventId(), ex.getMessage());
                    } else {
                        log.debug("[Kafka] 事件已发送: eventId={}, partition={}, offset={}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * 发送轨迹事件
     */
    public CompletableFuture<SendResult<String, Object>> sendTrajectoryEvent(GeoEvent event) {
        return kafkaTemplate.send(trajectoryTopic, event.getSourceId(), event);
    }

    /**
     * 发送告警事件
     */
    public CompletableFuture<SendResult<String, Object>> sendAlert(GeoEvent event) {
        return kafkaTemplate.send(alertsTopic, event.getSourceId(), event);
    }

    /**
     * 发送分析任务事件
     */
    public CompletableFuture<SendResult<String, Object>> sendAnalyticsTask(GeoEvent event) {
        return kafkaTemplate.send(analyticsTopic, event.getSourceId(), event);
    }
}
