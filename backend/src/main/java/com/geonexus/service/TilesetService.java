package com.geonexus.service;

import com.geonexus.model.dto.TilesetDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 3D Tileset 管理服务
 *
 * 提供 tileset 注册、查询、管理功能
 */
@Slf4j
@Service
public class TilesetService {

    private final CacheService cacheService;

    // tileset 存储
    private final Map<String, TilesetDTO> tilesets = new ConcurrentHashMap<>();

    public TilesetService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 注册新的 3D Tileset
     */
    public TilesetDTO register(TilesetDTO request) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();

        TilesetDTO tileset = TilesetDTO.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .url(request.getUrl())
                .type(request.getType())
                .ionAssetId(request.getIonAssetId())
                .accessToken(request.getAccessToken())
                .longitude(request.getLongitude())
                .latitude(request.getLatitude())
                .height(request.getHeight() != null ? request.getHeight() : 0.0)
                .heading(request.getHeading() != null ? request.getHeading() : 0.0)
                .pitch(request.getPitch() != null ? request.getPitch() : 0.0)
                .roll(request.getRoll() != null ? request.getRoll() : 0.0)
                .maxScreenSpaceError(request.getMaxScreenSpaceError() != null ? request.getMaxScreenSpaceError() : 16)
                .status("ready")
                .createdAt(now)
                .createdBy(request.getCreatedBy() != null ? request.getCreatedBy() : "system")
                .build();

        tilesets.put(id, tileset);
        log.info("[Tileset] 注册 tileset: id={}, name={}, type={}", id, tileset.getName(), tileset.getType());

        return tileset;
    }

    /**
     * 获取 tileset
     */
    public Optional<TilesetDTO> getTileset(String id) {
        return Optional.ofNullable(tilesets.get(id));
    }

    /**
     * 列出所有 tileset
     */
    public List<TilesetDTO> listTilesets() {
        return tilesets.values().stream().toList();
    }

    /**
     * 列出指定类型的 tileset
     */
    public List<TilesetDTO> listTilesetsByType(TilesetDTO.TilesetType type) {
        return tilesets.values().stream()
                .filter(t -> t.getType() == type)
                .toList();
    }

    /**
     * 删除 tileset
     */
    public boolean deleteTileset(String id) {
        TilesetDTO removed = tilesets.remove(id);
        if (removed != null) {
            log.info("[Tileset] 删除 tileset: id={}, name={}", id, removed.getName());
            return true;
        }
        return false;
    }

    /**
     * 更新 tileset 状态
     */
    public void updateStatus(String id, String status) {
        TilesetDTO tileset = tilesets.get(id);
        if (tileset != null) {
            tileset.setStatus(status);
            log.debug("[Tileset] 更新状态: id={}, status={}", id, status);
        }
    }

    /**
     * 获取可用的加载配置（用于前端）
     */
    public Map<String, Object> getLoadConfig(String id) {
        return tilesets.get(id);
    }
}
