package com.geonexus.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 叙事地图实体
 */
@Entity
@Table(name = "story_maps", indexes = {
        @Index(name = "idx_story_author", columnList = "authorId"),
        @Index(name = "idx_story_status", columnList = "status"),
        @Index(name = "idx_story_share_token", columnList = "shareToken", unique = true)
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryMapEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String coverImageUrl;

    @Column(columnDefinition = "TEXT")
    private String content; // 富文本 HTML

    @Column(columnDefinition = "TEXT")
    private String chaptersJson; // 章节列表 JSON

    @Column(nullable = false, length = 50)
    private String authorId;

    private String authorName;

    @Column(nullable = false, length = 20)
    private String status; // DRAFT / PUBLISHED

    @Column(unique = true, length = 20)
    private String shareToken; // 公开分享 token

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    // ==================== 审计字段 ====================
    @CreatedBy
    @Column(nullable = false, updatable = false, length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(nullable = false, length = 50)
    private String updatedBy;

    // ==================== 软删除 ====================
    @Column
    private LocalDateTime deletedAt;

    @Column(length = 50)
    private String deletedBy;

    // ==================== 嵌套实体 ====================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryChapter implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String title;
        @Column(columnDefinition = "TEXT")
        private String content; // HTML
        private Double centerLat;
        private Double centerLng;
        private Double zoom;
        private String tileType;
        @Builder.Default
        private List<String> visibleLayerIds = new ArrayList<>();
        private String mediaType; // none/image/video
        private String mediaUrl;
    }

    // ==================== 辅助方法 ====================
    @JsonIgnore
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted(String deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}
