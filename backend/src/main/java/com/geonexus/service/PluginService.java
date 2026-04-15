package com.geonexus.service;

import com.geonexus.domain.PluginInstanceEntity;
import com.geonexus.domain.PluginManifestEntity;
import com.geonexus.repository.PluginInstanceRepository;
import com.geonexus.repository.PluginManifestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 插件服务 - 管理插件市场与用户插件实例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginManifestRepository manifestRepo;
    private final PluginInstanceRepository instanceRepo;

    // ===== Manifest 管理 =====

    @Transactional
    public PluginManifestEntity registerPlugin(PluginManifestEntity manifest) {
        if (manifestRepo.existsByPluginId(manifest.getPluginId())) {
            throw new RuntimeException("Plugin ID already taken: " + manifest.getPluginId());
        }
        manifest.setId(UUID.randomUUID().toString());
        manifest.setStatus("PENDING");
        manifest.setInstallCount(0);
        manifest.setRating(0.0);
        manifest.setCreatedAt(LocalDateTime.now());
        manifest.setUpdatedAt(LocalDateTime.now());
        return manifestRepo.save(manifest);
    }

    @Transactional
    public PluginManifestEntity approvePlugin(String id) {
        PluginManifestEntity p = manifestRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Plugin not found: " + id));
        p.setStatus("APPROVED");
        p.setApprovedAt(LocalDateTime.now());
        return manifestRepo.save(p);
    }

    public List<PluginManifestEntity> searchPlugins(String query, String category, String tag) {
        if (query != null && !query.isBlank()) {
            return manifestRepo.findByStatusOrderByInstallCountDesc("APPROVED");
        }
        if (category != null) {
            return manifestRepo.findByCategoryAndStatusOrderByInstallCountDesc(category, "APPROVED");
        }
        if (tag != null) {
            return manifestRepo.findByTagsContainingAndStatusOrderByInstallCountDesc(tag, "APPROVED");
        }
        return manifestRepo.findByStatusOrderByInstallCountDesc("APPROVED");
    }

    public PluginManifestEntity getByPluginId(String pluginId) {
        return manifestRepo.findByPluginId(pluginId).orElse(null);
    }

    // ===== 用户安装管理 =====

    @Transactional
    public PluginInstanceEntity installPlugin(String pluginId, String userId, Map<String, Object> config) {
        if (instanceRepo.existsByPluginIdAndUserId(pluginId, userId)) {
            throw new RuntimeException("Plugin already installed: " + pluginId);
        }
        PluginInstanceEntity instance = PluginInstanceEntity.builder()
            .id(UUID.randomUUID().toString())
            .pluginId(pluginId)
            .userId(userId)
            .status("ACTIVE")
            .config(config != null ? toJson(config) : "{}")
            .installedAt(LocalDateTime.now())
            .lastEnabledAt(LocalDateTime.now())
            .build();
        // increase install count
        manifestRepo.findByPluginId(pluginId).ifPresent(m -> {
            m.setInstallCount(m.getInstallCount() + 1);
            manifestRepo.save(m);
        });
        return instanceRepo.save(instance);
    }

    @Transactional
    public void uninstallPlugin(String pluginId, String userId) {
        instanceRepo.deleteByPluginIdAndUserId(pluginId, userId);
    }

    @Transactional
    public void setPluginEnabled(String pluginId, String userId, boolean enabled) {
        PluginInstanceEntity inst = instanceRepo.findByPluginIdAndUserId(pluginId, userId)
            .orElseThrow(() -> new RuntimeException("Plugin not installed"));
        inst.setStatus(enabled ? "ACTIVE" : "DISABLED");
        if (enabled) inst.setLastEnabledAt(LocalDateTime.now());
        instanceRepo.save(inst);
    }

    public List<PluginInstanceEntity> getUserPlugins(String userId) {
        return instanceRepo.findByUserIdAndStatus(userId, "ACTIVE");
    }

    // ===== 资源访问 =====

    public String getPluginResourceUrl(String pluginId, String path) {
        return "/api/v1/plugins/" + pluginId + "/resource?path=" + path;
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(k).append("\":");
            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else {
                sb.append(v);
            }
        });
        sb.append("}");
        return sb.toString();
    }
}
