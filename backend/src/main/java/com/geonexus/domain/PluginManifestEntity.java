package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 插件清单实体 - 存储插件市场中的插件元数据
 */
@Entity
@Table(name = "plugin_manifests")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginManifestEntity implements BaseEntity {

    @Id
    private String id;

    private String pluginId;       // unique slug, e.g. "traffic-monitor"
    private String name;           // display name
    private String version;        // semver, e.g. "1.0.0"
    private String description;    // one-liner
    private String category;       // visualization / ai / data / utility
    private String iconUrl;

    private String authorName;
    private String authorUrl;

    private String homepage;       // plugin detail page
    private String license;        // MIT / Apache-2.0 / proprietary

    @Column(columnDefinition = "TEXT")
    private String permissions;    // JSON: { "camera": true, "layers": ["read"] }

    @Column(columnDefinition = "TEXT")
    private String tags;          // JSON array: ["traffic", "real-time", "china"]

    private String downloadUrl;   // ZIP file URL
    private long downloadSize;     // bytes
    private String checksum;       // SHA-256 of ZIP

    private String status;         // PENDING / APPROVED / REJECTED / DEPRECATED

    private int installCount;
    private double rating;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
}
