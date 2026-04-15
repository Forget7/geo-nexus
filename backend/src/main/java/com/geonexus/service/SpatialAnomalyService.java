package com.geonexus.service;

import com.geonexus.model.dto.AnomalyDetectionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.STRtree;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 空间异常检测服务
 * 职责：基于统计和密度的空间数据异常检测
 * 支持：IQR、LOF、DBSCAN、GRID_ZSCORE 四种检测算法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpatialAnomalyService {

    private final SpatialIndexService spatialIndexService;
    private final CacheService cacheService;

    /**
     * 基于 IQR（四分位距）的点状异常检测
     * 适用于：PM2.5、水质、噪音等环境监测数据
     */
    public AnomalyDetectionResult detectPointAnomalies(
            List<AnomalyPoint> points,
            String metricField,
            double iqrMultiplier) {

        // 提取数值字段
        List<Double> values = points.stream()
                .map(p -> getDoubleValue(p, metricField))
                .filter(v -> v != null && !v.isNaN())
                .sorted()
                .collect(Collectors.toList());

        if (values.size() < 4) {
            return AnomalyDetectionResult.builder()
                    .totalPoints(values.size())
                    .anomalyCount(0)
                    .anomalies(Collections.emptyList())
                    .method("IQR")
                    .build();
        }

        // 计算四分位数
        double q1 = values.get(values.size() / 4);
        double q3 = values.get(values.size() * 3 / 4);
        double iqr = q3 - q1;
        double lowerBound = q1 - iqrMultiplier * iqr;
        double upperBound = q3 + iqrMultiplier * iqr;

        // 标记异常点
        List<AnomalyPoint> anomalies = points.stream()
                .filter(p -> {
                    Double v = getDoubleValue(p, metricField);
                    return v != null && (v < lowerBound || v > upperBound);
                })
                .map(p -> {
                    Double v = getDoubleValue(p, metricField);
                    String severity = v < lowerBound ? "LOW" : "HIGH";
                    double deviation = iqr > 0 ? Math.abs(v - (v < lowerBound ? lowerBound : upperBound)) / iqr : 0;
                    return AnomalyPoint.builder()
                            .lat(p.getLat())
                            .lng(p.getLng())
                            .properties(p.getProperties())
                            .anomalyScore(deviation)
                            .anomalyType("IQR_OUTLIER")
                            .severity(severity)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("[IQR异常检测] 总点数={}, 异常点={}, lowerBound={}, upperBound={}",
                points.size(), anomalies.size(), lowerBound, upperBound);

        return AnomalyDetectionResult.builder()
                .totalPoints(points.size())
                .anomalyCount(anomalies.size())
                .anomalies(anomalies)
                .method("IQR")
                .thresholdLower(lowerBound)
                .thresholdUpper(upperBound)
                .build();
    }

    /**
     * 基于密度的局部异常因子（LOF）简化版
     * 适用于：人口密度突变、轨迹异常检测
     */
    public AnomalyDetectionResult detectDensityAnomalies(
            List<AnomalyPoint> points,
            int kNeighbors,
            double threshold) {

        List<AnomalyPoint> anomalies = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            AnomalyPoint p = points.get(i);

            // 找 k 个最近邻
            List<AnomalyPoint> neighbors = findKNearest(points, i, kNeighbors);
            if (neighbors.isEmpty()) continue;

            // 计算到邻居的平均距离
            double avgDist = neighbors.stream()
                    .mapToDouble(n -> haversineDistance(p.getLat(), p.getLng(), n.getLat(), n.getLng()))
                    .average()
                    .orElse(0);

            // 全局平均距离
            double globalAvg = points.stream()
                    .filter(other -> !other.equals(p))
                    .mapToDouble(other -> haversineDistance(p.getLat(), p.getLng(), other.getLat(), other.getLng()))
                    .average()
                    .orElse(1);

            double lof = globalAvg > 0 ? avgDist / globalAvg : 1;

            if (lof > threshold) {
                anomalies.add(AnomalyPoint.builder()
                        .lat(p.getLat())
                        .lng(p.getLng())
                        .properties(p.getProperties())
                        .anomalyScore(lof)
                        .anomalyType("LOF")
                        .severity(lof > threshold * 2 ? "HIGH" : "MEDIUM")
                        .build());
            }
        }

        log.info("[LOF异常检测] 总点数={}, 异常点={}, k={}, threshold={}",
                points.size(), anomalies.size(), kNeighbors, threshold);

        return AnomalyDetectionResult.builder()
                .totalPoints(points.size())
                .anomalyCount(anomalies.size())
                .anomalies(anomalies)
                .method("LOF")
                .thresholdLower(threshold)
                .build();
    }

    /**
     * 空间聚类异常检测（DBSCAN 简化版）
     * 落在小聚类或边缘的点为异常
     * 适用于：基础设施异常段检测
     */
    public AnomalyDetectionResult detectClusterAnomalies(
            List<AnomalyPoint> points,
            double epsKm,
            int minPts) {

        // 构建 R-Tree 索引
        List<Geometry> geometries = points.stream()
                .map(p -> {
                    GeometryFactory gf = new GeometryFactory();
                    return (Geometry) gf.createPoint(new Coordinate(p.getLng(), p.getLat()));
                })
                .collect(Collectors.toList());

        STRtree tree = spatialIndexService.buildIndex(geometries);

        List<AnomalyPoint> anomalies = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        List<List<AnomalyPoint>> clusters = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            if (visited.contains(i)) continue;
            visited.add(i);

            Envelope searchEnv = createEnvelope(points.get(i), epsKm);
            Envelope jtsEnv = new Envelope(searchEnv.getMinX(), searchEnv.getMaxX(),
                    searchEnv.getMinY(), searchEnv.getMaxY());
            List<SpatialIndexEntry> neighbors = spatialIndexService.queryByEnvelope(tree, jtsEnv);

            if (neighbors.size() >= minPts) {
                // 形成聚类
                List<AnomalyPoint> cluster = new ArrayList<>();
                cluster.add(points.get(i));
                clusters.add(cluster);
            } else {
                // 噪声点
                anomalies.add(AnomalyPoint.builder()
                        .lat(points.get(i).getLat())
                        .lng(points.get(i).getLng())
                        .properties(points.get(i).getProperties())
                        .anomalyScore(1.0)
                        .anomalyType("DBSCAN_NOISE")
                        .severity("LOW")
                        .build());
            }
        }

        log.info("[DBSCAN异常检测] 总点数={}, 噪声点={}, 聚类数={}, epsKm={}, minPts={}",
                points.size(), anomalies.size(), clusters.size(), epsKm, minPts);

        return AnomalyDetectionResult.builder()
                .totalPoints(points.size())
                .anomalyCount(anomalies.size())
                .anomalies(anomalies)
                .clusterCount(clusters.size())
                .method("DBSCAN")
                .build();
    }

    /**
     * 网格聚合异常检测（适合大规模点数据）
     * 基于 Z-score 统计显著性与否判断异常
     * 适用于：环境监测、人口密度的大规模网格分析
     */
    public AnomalyDetectionResult detectGridAnomalies(
            List<AnomalyPoint> points,
            String valueField,
            double cellSizeKm,
            double zScoreThreshold) {

        // 网格聚合
        Map<String, List<AnomalyPoint>> grid = new HashMap<>();
        for (AnomalyPoint p : points) {
            String key = gridKey(p.getLat(), p.getLng(), cellSizeKm);
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        // 计算每个网格的统计量
        Map<String, Double> cellStats = new HashMap<>();
        for (Map.Entry<String, List<AnomalyPoint>> entry : grid.entrySet()) {
            double avg = entry.getValue().stream()
                    .mapToDouble(pt -> getDoubleValue(pt, valueField))
                    .average()
                    .orElse(0);
            cellStats.put(entry.getKey(), avg);
        }

        // 全局统计
        DoubleSummaryStatistics stats = cellStats.values().stream()
                .mapToDouble(v -> v)
                .summaryStatistics();
        double mean = stats.getAverage();
        double std = Math.sqrt(cellStats.values().stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average()
                .orElse(0));

        // 找异常网格
        List<AnomalyPoint> anomalies = new ArrayList<>();
        for (Map.Entry<String, List<AnomalyPoint>> entry : grid.entrySet()) {
            double cellMean = entry.getValue().stream()
                    .mapToDouble(pt -> getDoubleValue(pt, valueField))
                    .average()
                    .orElse(0);
            double zScore = std > 0 ? Math.abs((cellMean - mean) / std) : 0;

            if (zScore > zScoreThreshold) {
                for (AnomalyPoint p : entry.getValue()) {
                    anomalies.add(AnomalyPoint.builder()
                            .lat(p.getLat())
                            .lng(p.getLng())
                            .properties(p.getProperties())
                            .anomalyScore(zScore)
                            .anomalyType("GRID_ZSCORE")
                            .severity(zScore > zScoreThreshold * 2 ? "HIGH" : "MEDIUM")
                            .build());
                }
            }
        }

        log.info("[GRID_ZSCORE异常检测] 总点数={}, 异常点={}, 网格数={}, cellSizeKm={}, zScoreThreshold={}",
                points.size(), anomalies.size(), grid.size(), cellSizeKm, zScoreThreshold);

        return AnomalyDetectionResult.builder()
                .totalPoints(points.size())
                .anomalyCount(anomalies.size())
                .anomalies(anomalies)
                .method("GRID_ZSCORE")
                .thresholdLower(zScoreThreshold)
                .build();
    }

    // ===== 工具方法 =====

    private List<AnomalyPoint> findKNearest(List<AnomalyPoint> points, int idx, int k) {
        AnomalyPoint target = points.get(idx);
        return points.stream()
                .filter(p -> !p.equals(target))
                .sorted(Comparator.comparingDouble(p ->
                        haversineDistance(target.getLat(), target.getLng(), p.getLat(), p.getLng())))
                .limit(k)
                .collect(Collectors.toList());
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String gridKey(double lat, double lng, double cellSizeKm) {
        double degPerKm = 1.0 / 111.0;
        int latCell = (int) Math.floor(lat / (cellSizeKm * degPerKm));
        int lngCell = (int) Math.floor(lng / (cellSizeKm * degPerKm));
        return latCell + "," + lngCell;
    }

    private Envelope createEnvelope(AnomalyPoint p, double km) {
        double deg = km / 111.0;
        return new Envelope(p.getLng() - deg, p.getLng() + deg,
                p.getLat() - deg, p.getLat() + deg);
    }

    private Double getDoubleValue(AnomalyPoint p, String field) {
        if (p.getProperties() == null) return null;
        Object v = p.getProperties().get(field);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // ===== 内嵌类 =====

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnomalyPoint {
        private Double lat;
        private Double lng;
        private Map<String, Object> properties;
        private Double anomalyScore;
        private String anomalyType;
        private String severity;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnomalyDetectionResult {
        private int totalPoints;
        private int anomalyCount;
        private List<AnomalyPoint> anomalies;
        private String method;
        private Double thresholdLower;
        private Double thresholdUpper;
        private Integer clusterCount;
    }
}
