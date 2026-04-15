package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 瓦片缓存服务 - 预生成和缓存地图瓦片
 */
@Slf4j
@Service
public class TileCacheService {
    
    private final CacheService cacheService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 瓦片缓存
    private final Map<String, TileCache> caches = new ConcurrentHashMap<>();
    
    // 预生成任务
    private final Map<String, PreCacheJob> preCacheJobs = new ConcurrentHashMap<>();
    
    private static final String TILE_PREFIX = "tile:";
    
    public TileCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    // ==================== 瓦片缓存管理 ====================
    
    /**
     * 创建瓦片缓存
     */
    public String createCache(CreateCacheRequest request) {
        String cacheId = UUID.randomUUID().toString();
        
        TileCache cache = TileCache.builder()
                .id(cacheId)
                .name(request.getName())
                .description(request.getDescription())
                .tileSource(request.getTileSource())
                .minZoom(request.getMinZoom())
                .maxZoom(request.getMaxZoom())
                .bounds(request.getBounds())
                .tileFormat(request.getFormat())
                .tileSize(request.getTileSize())
                .status("created")
                .createdAt(System.currentTimeMillis())
                .stats(TileStats.builder()
                        .totalTiles(0)
                        .cachedTiles(0)
                        .hitCount(0)
                        .missCount(0)
                        .build())
                .build();
        
        caches.put(cacheId, cache);
        
        log.info("创建瓦片缓存: id={}, name={}", cacheId, request.getName());
        
        return cacheId;
    }
    
    /**
     * 获取瓦片
     */
    public byte[] getTile(String cacheId, int x, int y, int z) {
        String key = getTileKey(cacheId, x, y, z);
        
        TileCache cache = caches.get(cacheId);
        if (cache == null) {
            throw new CacheNotFoundException("缓存不存在: " + cacheId);
        }
        
        // 检查缓存
        byte[] tile = (byte[]) cacheService.get(key);
        
        if (tile != null) {
            cache.getStats().setHitCount(cache.getStats().getHitCount() + 1);
            log.debug("瓦片命中: cacheId={}, x={}, y={}, z={}", cacheId, x, y, z);
        } else {
            cache.getStats().setMissCount(cache.getStats().getMissCount() + 1);
            
            // 生成瓦片
            tile = generateTile(cache, x, y, z);
            
            // 存储
            if (tile != null) {
                cacheService.set(key, tile);
                cache.getStats().setCachedTiles(cache.getStats().getCachedTiles() + 1);
            }
            
            log.debug("瓦片生成: cacheId={}, x={}, y={}, z={}", cacheId, x, y, z);
        }
        
        return tile;
    }
    
    /**
     * 获取瓦片URL
     */
    public String getTileUrl(String cacheId, int x, int y, int z) {
        return String.format("/api/v1/tiles/%s/%d/%d/%d", cacheId, z, x, y);
    }
    
    /**
     * 预生成瓦片
     */
    public String preCache(String cacheId, PreCacheRequest request) {
        TileCache cache = caches.get(cacheId);
        if (cache == null) {
            throw new CacheNotFoundException("缓存不存在: " + cacheId);
        }
        
        String jobId = UUID.randomUUID().toString();
        
        PreCacheJob job = PreCacheJob.builder()
                .id(jobId)
                .cacheId(cacheId)
                .minZoom(request.getMinZoom())
                .maxZoom(request.getMaxZoom())
                .bounds(request.getBounds())
                .status("running")
                .progress(0)
                .createdAt(System.currentTimeMillis())
                .build();
        
        preCacheJobs.put(jobId, job);
        
        // 异步执行预生成
        scheduler.submit(() -> executePreCache(job, cache));
        
        log.info("启动预生成任务: jobId={}, cacheId={}", jobId, cacheId);
        
        return jobId;
    }
    
    private void executePreCache(PreCacheJob job, TileCache cache) {
        try {
            int totalTiles = 0;
            int completed = 0;
            
            // 计算总瓦片数
            for (int z = job.getMinZoom(); z <= job.getMaxZoom(); z++) {
                int tileCount = calculateTileCount(z, job.getBounds());
                totalTiles += tileCount;
            }
            
            job.setTotalTiles(totalTiles);
            
            // 生成瓦片
            for (int z = job.getMinZoom(); z <= job.getMaxZoom(); z++) {
                int minX = lonToTileX(job.getBounds()[0], z);
                int maxX = lonToTileX(job.getBounds()[2], z);
                int minY = latToTileY(job.getBounds()[3], z);
                int maxY = latToTileY(job.getBounds()[1], z);
                
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        String key = getTileKey(cache.getId(), x, y, z);
                        
                        if (cacheService.get(key) == null) {
                            byte[] tile = generateTile(cache, x, y, z);
                            if (tile != null) {
                                cacheService.set(key, tile);
                                cache.getStats().setCachedTiles(cache.getStats().getCachedTiles() + 1);
                            }
                        }
                        
                        completed++;
                        job.setProgress((double) completed / totalTiles * 100);
                        
                        // 每1000个瓦片输出一次日志
                        if (completed % 1000 == 0) {
                            log.info("预生成进度: jobId={}, progress={:.1f}%", 
                                    job.getId(), job.getProgress());
                        }
                    }
                }
            }
            
            job.setStatus("completed");
            job.setCompletedAt(System.currentTimeMillis());
            
            cache.getStats().setTotalTiles(totalTiles);
            
            log.info("预生成完成: jobId={}, tiles={}", job.getId(), totalTiles);
            
        } catch (Exception e) {
            job.setStatus("failed");
            job.setError(e.getMessage());
            log.error("预生成失败: jobId={}", job.getId(), e);
        }
    }
    
    /**
     * 获取预生成进度
     */
    public PreCacheJob getJobProgress(String jobId) {
        return preCacheJobs.get(jobId);
    }
    
    /**
     * 删除缓存
     */
    public void deleteCache(String cacheId) {
        TileCache cache = caches.remove(cacheId);
        if (cache != null) {
            // 清理相关预生成任务
            preCacheJobs.entrySet().removeIf(e -> e.getValue().getCacheId().equals(cacheId));
            log.info("删除瓦片缓存: cacheId={}", cacheId);
        }
    }
    
    /**
     * 获取缓存列表
     */
    public List<TileCache> listCaches() {
        return new ArrayList<>(caches.values());
    }
    
    /**
     * 获取缓存
     */
    public TileCache getCache(String cacheId) {
        return caches.get(cacheId);
    }

    /**
     * 获取缓存统计
     */
    public TileStats getCacheStats(String cacheId) {
        TileCache cache = caches.get(cacheId);
        if (cache == null) {
            throw new CacheNotFoundException("缓存不存在: " + cacheId);
        }
        return cache.getStats();
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanExpiredTiles() {
        for (TileCache cache : caches.values()) {
            cache.getStats().setCachedTiles(0);
            log.info("清理瓦片缓存: cacheId={}", cache.getId());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private byte[] generateTile(TileCache cache, int x, int y, int z) {
        // 简化实现：根据瓦片源生成占位图
        // 实际应调用WMTS/WMS服务获取真实瓦片
        
        String placeholder = String.format("Tile %d/%d/%d", z, x, y);
        return placeholder.getBytes();
    }
    
    private String getTileKey(String cacheId, int x, int y, int z) {
        return TILE_PREFIX + cacheId + ":" + z + "/" + x + "/" + y;
    }
    
    private int calculateTileCount(int zoom, double[] bounds) {
        int minX = lonToTileX(bounds[0], zoom);
        int maxX = lonToTileX(bounds[2], zoom);
        int minY = latToTileY(bounds[3], zoom);
        int maxY = latToTileY(bounds[1], zoom);
        
        return (maxX - minX + 1) * (maxY - minY + 1);
    }
    
    private int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * Math.pow(2, zoom));
    }
    
    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * Math.pow(2, zoom));
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class TileCache {
        private String id;
        private String name;
        private String description;
        private String tileSource; // wmts, wms, tms
        private int minZoom;
        private int maxZoom;
        private double[] bounds;
        private String format;
        private int tileSize;
        private String status;
        private long createdAt;
        private TileStats stats;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TileStats {
        private int totalTiles;
        private int cachedTiles;
        private long hitCount;
        private long missCount;
        
        public double getHitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total * 100 : 0;
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PreCacheJob {
        private String id;
        private String cacheId;
        private int minZoom;
        private int maxZoom;
        private double[] bounds;
        private String status;
        private double progress;
        private int totalTiles;
        private long createdAt;
        private Long completedAt;
        private String error;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class CreateCacheRequest {
        private String name;
        private String description;
        private String tileSource;
        private int minZoom;
        private int maxZoom;
        private double[] bounds;
        private String format;
        private int tileSize;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PreCacheRequest {
        private int minZoom;
        private int maxZoom;
        private double[] bounds;
    }
    
    public static class CacheNotFoundException extends RuntimeException {
        public CacheNotFoundException(String msg) { super(msg); }
    }

    // ==================== 离线包生成 ====================

    /**
     * 生成离线瓦片包
     *
     * @param request 离线包请求参数
     * @return 离线包下载 URL
     */
    public OfflinePackageResult generateOfflinePackage(OfflinePackageRequest request) {
        log.info("开始生成离线包: bounds={}, zoomLevels={}, source={}",
                request.getBounds(), request.getZoomLevels(), request.getTileSource());

        // 1. 计算瓦片数量
        int tileCount = calculateTileCount(request.getBounds(), request.getZoomLevels());
        log.info("瓦片数量: {}", tileCount);

        // 2. 估算包大小（每个瓦片约 15KB）
        long estimatedSize = tileCount * 15 * 1024L;
        String estimatedSizeStr = formatSize(estimatedSize);
        log.info("预估大小: {}", estimatedSizeStr);

        // 3. 异步生成包
        String jobId = UUID.randomUUID().toString();
        scheduler.submit(() -> {
            try {
                buildOfflinePackage(jobId, request);
            } catch (Exception e) {
                log.error("离线包生成失败: {}", e.getMessage(), e);
            }
        });

        return OfflinePackageResult.builder()
                .jobId(jobId)
                .tileCount(tileCount)
                .estimatedSize(estimatedSizeStr)
                .status("PROCESSING")
                .build();
    }

    private int calculateTileCount(double[] bounds, List<Integer> zoomLevels) {
        int count = 0;
        for (int z : zoomLevels) {
            int minX = lonToTileX(bounds[0], z);
            int maxX = lonToTileX(bounds[2], z);
            int minY = latToTileY(bounds[3], z);
            int maxY = latToTileY(bounds[1], z);
            count += (maxX - minX + 1) * (maxY - minY + 1);
        }
        return count;
    }

    private int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * Math.pow(2.0, zoom));
    }

    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * Math.pow(2.0, zoom));
    }

    private void buildOfflinePackage(String jobId, OfflinePackageRequest request) {
        // 生成 manifest.json
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("version", "1.0");
        manifest.put("name", request.getName());
        manifest.put("tileSource", request.getTileSource());
        manifest.put("bounds", request.getBounds());
        manifest.put("zoomLevels", request.getZoomLevels());
        manifest.put("created", java.time.Instant.now().toString());

        // 生成瓦片配置
        Map<String, Object> config = new HashMap<>();
        config.put("minZoom", request.getZoomLevels().stream().min(Integer::compareTo).orElse(0));
        config.put("maxZoom", request.getZoomLevels().stream().max(Integer::compareTo).orElse(18));
        config.put("bounds", request.getBounds());
        config.put("format", request.getFormat() != null ? request.getFormat() : "pbf");

        log.info("离线包生成任务 {} 已提交后台处理", jobId);
    }

    /**
     * 获取离线包生成状态
     */
    public OfflinePackageResult getPackageStatus(String jobId) {
        // 占位实现，实际应查询数据库
        return OfflinePackageResult.builder()
                .jobId(jobId)
                .status("PROCESSING")
                .build();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @lombok.Data
    @lombok.Builder
    public static class OfflinePackageRequest {
        private String name;
        private String tileSource;  // 瓦片源 URL 模板
        private double[] bounds;     // [minLon, minLat, maxLon, maxLat]
        private List<Integer> zoomLevels;
        private String format;       // pbf / png / jpg
    }

    @lombok.Data
    @lombok.Builder
    public static class OfflinePackageResult {
        private String jobId;
        private int tileCount;
        private String estimatedSize;
        private String status;   // PROCESSING / READY / FAILED
        private String downloadUrl;
    }
}
