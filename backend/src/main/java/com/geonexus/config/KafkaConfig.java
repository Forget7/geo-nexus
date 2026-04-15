package com.geonexus.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 全局配置
 *
 * Topic 设计：
 * - geonexus.geo-events    (6 partitions) - 原始地理事件
 * - geonexus.trajectory    (6 partitions) - 轨迹处理
 * - geonexus.czml          (3 partitions) - CZML 生成
 * - geonexus.alerts        (3 partitions) - 告警事件
 * - geonexus.analytics    (6 partitions) - 分析任务
 */
@Configuration
public class KafkaConfig {

    @Value("${geonexus.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${geonexus.kafka.consumer.group-id:geonexus-group}")
    private String groupId;

    // ==================== Topic 自动创建 ====================

    @Bean
    public NewTopic geoEventsTopic() {
        return TopicBuilder.name("geonexus.geo-events")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic trajectoryTopic() {
        return TopicBuilder.name("geonexus.trajectory")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic czmlTopic() {
        return TopicBuilder.name("geonexus.czml")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic alertsTopic() {
        return TopicBuilder.name("geonexus.alerts")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic analyticsTopic() {
        return TopicBuilder.name("geonexus.analytics")
                .partitions(6)
                .replicas(1)
                .build();
    }

    // ==================== Producer 配置 ====================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ==================== Consumer 配置 ====================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.geonexus.event,*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // 并发消费者数量
        factory.setConcurrency(3);
        return factory;
    }
}
