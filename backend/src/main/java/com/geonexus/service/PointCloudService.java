package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 点云数据服务 - PNTS / 3D Point Cloud Tile Set
 */
@Slf4j
@Service
public class PointCloudService {

    private final CacheService cacheService;

    // 支持的点云格式
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "PNTS",    // Cesium Point Cloud Tile Set
            "PCT",     // Potree Point Cloud
            "LAS",     // ASPRS LASer (需要转换)
            "LAZ",     // Compressed LAS
            "EPT"      // Entwine Point Table
    );

    // Cesium Ion 预置点云资源
    private static final Map<String, PointCloudPreset> ION_PRESETS = new LinkedHashMap<>();
    static {
        ION_PRESETS.put("melbourne", new PointCloudPreset(
                "Melbourne Point Cloud",
                null, // uses ion asset
                43978,
                "Melbourne, Australia"
        ));
        ION_PRESETS.put("montreal", new PointCloudPreset(
                "Montreal Point Cloud",
                null,
                43978,
                "Montreal, Canada"
        ));
    }

    public PointCloudService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    // ==================== 格式查询 ====================

    /**
     * 获取支持的所有点云格式
     */
    public List<String> getSupportedFormats() {
        return new ArrayList<>(SUPPORTED_FORMATS);
    }

    // ==================== 点云加载参数 ====================

    /**
     * 返回点云加载参数（URL + 配置）
     */
    public PointCloudLoadResult loadPointCloud(PointCloudLoadRequest request) {
        log.info("加载点云: name={}, url={}, ionAssetId={}",
                request.getName(), request.getUrl(), request.getIonAssetId());

        PointCloudLoadResult result = new PointCloudLoadResult();
        result.setName(request.getName() != null ? request.getName() : "Point Cloud");
        result.setSuccess(false);

        try {
            if (request.getIonAssetId() != null) {
                // Cesium Ion asset
                result.setIonAssetId(request.getIonAssetId());
                result.setLoadType("ion");
                result.setSuccess(true);
                result.setMessage("Use Cesium.PointCloud.fromIon(" + request.getIonAssetId() + ") with your Ion token");
            } else if (request.getUrl() != null && !request.getUrl().isEmpty()) {
                // Custom URL
                result.setUrl(request.getUrl());
                result.setLoadType("url");
                result.setFormat(detectFormat(request.getUrl()));
                result.setSuccess(true);
                result.setMessage("Point cloud URL configured");
            } else {
                result.setMessage("请提供 URL 或 Ion Asset ID");
            }
        } catch (Exception e) {
            log.error("点云加载参数获取失败", e);
            result.setMessage("加载失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从 URL 批量加载点云
     */
    public List<PointCloudLoadResult> batchLoadPointClouds(List<PointCloudLoadRequest> requests) {
        List<PointCloudLoadResult> results = new ArrayList<>();
        for (PointCloudLoadRequest request : requests) {
            results.add(loadPointCloud(request));
        }
        return results;
    }

    // ==================== 辅助方法 ====================

    private String detectFormat(String url) {
        if (url == null) return "UNKNOWN";
        String lower = url.toLowerCase();
        if (lower.endsWith(".pnt") || lower.endsWith(".pnts")) return "PNTS";
        if (lower.endsWith(".las")) return "LAS";
        if (lower.endsWith(".laz")) return "LAZ";
        if (lower.endsWith(".ept")) return "EPT";
        if (lower.contains("potree")) return "PCT";
        return "PNTS"; // default
    }

    // ==================== 内部类 ====================

    @lombok.Data
    public static class PointCloudLoadRequest {
        private String name;
        private String url;
        private Integer ionAssetId;
        private Map<String, Object> options;
    }

    @lombok.Data
    @lombok.Builder
    public static class PointCloudLoadResult {
        private String name;
        private boolean success;
        private String loadType;   // "ion" or "url"
        private String url;
        private Integer ionAssetId;
        private String format;
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PointCloudPreset {
        private String name;
        private String url;
        private Integer ionAssetId;
        private String description;
    }
}
