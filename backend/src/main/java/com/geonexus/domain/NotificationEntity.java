package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user", columnList = "userId"),
        @Index(name = "idx_notif_user_read", columnList = "userId, read"),
        @Index(name = "idx_notif_created", columnList = "createdAt")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String title;
    private String body;
    private String type;
    private String url;
    private boolean read;

    private String senderId;
    private String senderName;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
