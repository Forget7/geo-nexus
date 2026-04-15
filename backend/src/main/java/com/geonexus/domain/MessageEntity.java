package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_session", columnList = "session_id"),
        @Index(name = "idx_message_timestamp", columnList = "timestamp")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSessionEntity session;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
    
    /**
     * 消息时间戳（与父类 createdAt 不同，这是消息业务时间）
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }
    
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
