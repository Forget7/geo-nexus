package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.TileCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 瓦片缓存控制器 - REST API 暴露 TileCacheService
 */
@RestController
@RequestMapping("/api/v1/tiles")
@Tag(name = "瓦片缓存", description = "地图瓦片缓存管理与预热")
@RequiredArgsConstructor
public class TileCacheController {

    private final TileCacheService tileCacheService;

    @PostMapping("/caches")
    @Operation(summary = "创建瓦片缓存")
    public ResponseEntity<ApiResponse<String>> createCache(
            @RequestBody TileCacheService.CreateCacheRequest request) {
        String cacheId = tileCacheService.createCache(request);
        return ResponseEntity.ok(ApiResponse.success(cacheId));
    }

    @GetMapping("/caches/{cacheId}")
    @Operation(summary = "获取缓存信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheInfo(@PathVariable String cacheId) {
        TileCacheService.TileCache cache = tileCacheService.getCache(cacheId);
        if (cache == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Cache not found"));
        }
        TileCacheService.TileStats stats = cache.getStats();
        Map<String, Object> info = Map.of(
                "id", cache.getId(),
                "name", cache.getName() != null ? cache.getName() : "",
                "description", cache.getDescription() != null ? cache.getDescription() : "",
                "tileSource", cache.getTileSource() != null ? cache.getTileSource() : "",
                "minZoom", cache.getMinZoom(),
                "maxZoom", cache.getMaxZoom(),
                "bounds", cache.getBounds() != null ? cache.getBounds() : new double[0],
                "format", cache.getFormat() != null ? cache.getFormat() : "",
                "tileSize", cache.getTileSize(),
                "status", cache.getStatus() != null ? cache.getStatus() : "",
                "createdAt", cache.getCreatedAt(),
                "tileCount", stats != null ? stats.getCachedTiles() : 0,
                "hitRate", stats != null ? stats.getHitRate() : 0.0
        );
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @DeleteMapping("/caches/{cacheId}")
    @Operation(summary = "删除缓存")
    public ResponseEntity<ApiResponse<Void>> deleteCache(@PathVariable String cacheId) {
        tileCacheService.deleteCache(cacheId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/caches")
    @Operation(summary = "列出所有缓存")
    public ResponseEntity<ApiResponse<List<TileCacheService.TileCache>>> listCaches() {
        return ResponseEntity.ok(ApiResponse.success(tileCacheService.listCaches()));
    }

    @GetMapping("/{cacheId}/{z}/{x}/{y}")
    @Operation(summary = "获取瓦片")
    public ResponseEntity<byte[]> getTile(
            @PathVariable String cacheId,
            @PathVariable int x, @PathVariable int y, @PathVariable int z) {
        try {
            byte[] tile = tileCacheService.getTile(cacheId, x, y, z);
            if (tile == null) return ResponseEntity.notFound().build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(tile, headers, 200);
        } catch (TileCacheService.CacheNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/url/{cacheId}/{z}/{x}/{y}")
    @Operation(summary = "获取瓦片URL")
    public ResponseEntity<ApiResponse<String>> getTileUrl(
            @PathVariable String cacheId,
            @PathVariable int x, @PathVariable int y, @PathVariable int z) {
        return ResponseEntity.ok(ApiResponse.success(
                tileCacheService.getTileUrl(cacheId, x, y, z)));
    }

    @PostMapping("/caches/{cacheId}/precache")
    @Operation(summary = "预缓存瓦片")
    public ResponseEntity<ApiResponse<String>> preCache(
            @PathVariable String cacheId,
            @RequestBody TileCacheService.PreCacheRequest request) {
        String jobId = tileCacheService.preCache(cacheId, request);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(jobId));
    }

    @GetMapping("/caches/{cacheId}/status")
    @Operation(summary = "缓存状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStatus(@PathVariable String cacheId) {
        TileCacheService.TileCache cache = tileCacheService.getCache(cacheId);
        if (cache == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Cache not found"));
        }
        TileCacheService.TileStats stats = cache.getStats();
        Map<String, Object> status = Map.of(
                "cacheId", cacheId,
                "status", cache.getStatus() != null ? cache.getStatus() : "unknown",
                "totalTiles", stats != null ? stats.getTotalTiles() : 0,
                "cachedTiles", stats != null ? stats.getCachedTiles() : 0,
                "hitCount", stats != null ? stats.getHitCount() : 0L,
                "missCount", stats != null ? stats.getMissCount() : 0L,
                "hitRate", stats != null ? stats.getHitRate() : 0.0
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
