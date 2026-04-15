package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天会话实体
 */
@Entity
@Table(name = "chat_sessions", indexes = {
        @Index(name = "idx_session_user", columnList = "userId"),
        @Index(name = "idx_session_created", columnList = "createdAt"),
        @Index(name = "idx_session_updated", columnList = "updatedAt")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionEntity extends BaseEntity {
    
    @Column(nullable = false)
    private String userId;
    
    @Column(length = 500)
    private String title;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    @Builder.Default
    private List<MessageEntity> messages = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String contextJson;
    
    public enum SessionStatus {
        ACTIVE, ARCHIVED, DELETED
    }
    
    public void addMessage(MessageEntity message) {
        messages.add(message);
        message.setSession(this);
    }
}
