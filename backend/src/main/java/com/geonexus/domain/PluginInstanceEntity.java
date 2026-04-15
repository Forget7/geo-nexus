package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 插件实例实体 - 用户安装的插件实例
 */
@Entity
@Table(name = "plugin_instances")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginInstanceEntity {

    @Id
    private String id;

    private String pluginId;      // references PluginManifestEntity.pluginId
    private String userId;        // who installed it

    private String status;         // ACTIVE / DISABLED

    @Column(columnDefinition = "TEXT")
    private String config;         // JSON: user-provided config

    private LocalDateTime installedAt;
    private LocalDateTime lastEnabledAt;
}
