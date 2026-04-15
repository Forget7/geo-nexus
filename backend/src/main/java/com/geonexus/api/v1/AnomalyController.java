package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.SpatialAnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 空间异常检测API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
@Tag(name = "空间异常", description = "空间数据异常检测与分析")
public class AnomalyController {

    private final SpatialAnomalyService anomalyService;

    // 内存存储已检测的异常（简化实现）
    private final Map<String, Map<String, Object>> anomalyStore = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/detect")
    @Operation(summary = "检测异常")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> detectAnomalies(
            @RequestBody Map<String, Object> body) {

        String method = (String) body.getOrDefault("method", "IQR");
        List<Map<String, Object>> pointsRaw = (List<Map<String, Object>>) body.get("points");
        String valueField = (String) body.getOrDefault("valueField", "value");
        double iqrMultiplier = ((Number) body.getOrDefault("iqrMultiplier", 1.5)).doubleValue();
        int kNeighbors = ((Number) body.getOrDefault("kNeighbors", 5)).intValue();
        double threshold = ((Number) body.getOrDefault("threshold", 2.0)).doubleValue();
        double epsKm = ((Number) body.getOrDefault("epsKm", 0.5)).doubleValue();
        int minPts = ((Number) body.getOrDefault("minPts", 5)).intValue();
        double cellSizeKm = ((Number) body.getOrDefault("cellSizeKm", 0.1)).doubleValue();
        double zScoreThreshold = ((Number) body.getOrDefault("zScoreThreshold", 2.0)).doubleValue();

        List<SpatialAnomalyService.AnomalyPoint> points = pointsRaw.stream()
                .map(m -> {
                    Double lat = m.get("lat") != null ? ((Number) m.get("lat")).doubleValue() : 0;
                    Double lng = m.get("lng") != null ? ((Number) m.get("lng")).doubleValue() : 0;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> props = (Map<String, Object>) m.get("properties");
                    return SpatialAnomalyService.AnomalyPoint.builder()
                            .lat(lat)
                            .lng(lng)
                            .properties(props)
                            .build();
                })
                .toList();

        SpatialAnomalyService.AnomalyDetectionResult result;
        switch (method.toUpperCase()) {
            case "LOF":
                result = anomalyService.detectDensityAnomalies(points, kNeighbors, threshold);
                break;
            case "DBSCAN":
                result = anomalyService.detectClusterAnomalies(points, epsKm, minPts);
                break;
            case "GRID":
                result = anomalyService.detectGridAnomalies(points, valueField, cellSizeKm, zScoreThreshold);
                break;
            default:
                result = anomalyService.detectPointAnomalies(points, valueField, iqrMultiplier);
                break;
        }

        List<Map<String, Object>> anomalies = result.getAnomalies().stream()
                .map(a -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", UUID.randomUUID().toString());
                    m.put("lat", a.getLat());
                    m.put("lng", a.getLng());
                    m.put("severity", a.getSeverity());
                    m.put("score", a.getAnomalyScore());
                    m.put("type", a.getAnomalyType());
                    m.put("properties", a.getProperties());
                    m.put("resolved", false);
                    return m;
                })
                .peek(m -> anomalyStore.put((String) m.get("id"), m))
                .toList();

        log.info("[异常检测] method={}, total={}, detected={}", method, points.size(), anomalies.size());
        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    @GetMapping
    @Operation(summary = "列出异常")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAnomalies(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> all = anomalyStore.values().stream()
                .sorted((a, b) -> {
                    Object tsA = a.get("timestamp");
                    Object tsB = b.get("timestamp");
                    if (tsA == null || tsB == null) return 0;
                    return ((Number) tsB).compareTo((Number) tsA);
                })
                .limit(limit)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(all));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取异常详情")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnomaly(@PathVariable String id) {
        Map<String, Object> anomaly = anomalyStore.get(id);
        if (anomaly == null) {
            return ResponseEntity.ok(ApiResponse.error("异常不存在: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(anomaly));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "标记已解决")
    public ResponseEntity<ApiResponse<Void>> resolveAnomaly(@PathVariable String id) {
        Map<String, Object> anomaly = anomalyStore.get(id);
        if (anomaly == null) {
            return ResponseEntity.ok(ApiResponse.error("异常不存在: " + id));
        }
        anomaly.put("resolved", true);
        anomaly.put("resolvedAt", System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
