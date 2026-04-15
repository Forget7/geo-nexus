package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * GIS批处理服务 - 高效处理大规模空间数据
 */
@Slf4j
@Service
public class GISProcessingService {
    
    private final GISService gisService;
    private final ObjectMapper objectMapper;
    
    public GISProcessingService(GISService gisService, ObjectMapper objectMapper) {
        this.gisService = gisService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 批量缓冲区分析
     */
    public Map<String, Object> batchBuffer(
            List<Map<String, Object>> geometries, 
            double distanceKm) {
        
        log.info("开始批量缓冲区分析，数量: {}", geometries.size());
        
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < geometries.size(); i++) {
            try {
                Map<String, Object> geometry = geometries.get(i);
                Map<String, Object> params = new HashMap<>();
                params.put("geometry", geometry);
                params.put("distanceKm", distanceKm);
                
                Map<String, Object> result = gisService.executeTool(
                        new com.geonexus.model.ToolExecuteRequest("buffer_analysis", params));
                
                if (!result.containsKey("error")) {
                    results.add(Map.of(
                            "index", i,
                            "success", true,
                            "buffer", result.get("buffer")
                    ));
                    successCount++;
                } else {
                    failCount++;
                    errors.add("Index " + i + ": " + result.get("error"));
                }
                
            } catch (Exception e) {
                failCount++;
                errors.add("Index " + i + ": " + e.getMessage());
            }
            
            // 每100条记录打印进度
            if ((i + 1) % 100 == 0) {
                log.info("进度: {}/{}", i + 1, geometries.size());
            }
        }
        
        log.info("批量缓冲区分析完成: 成功={}, 失败={}", successCount, failCount);
        
        return Map.of(
                "total", geometries.size(),
                "success", successCount,
                "failed", failCount,
                "results", results,
                "errors", errors
        );
    }
    
    /**
     * 批量距离计算
     */
    public Map<String, Object> batchDistance(
            List<List<Double>> points1,
            List<List<Double>> points2) {
        
        if (points1.size() != points2.size()) {
            return Map.of("error", "points1 and points2 must have same size");
        }
        
        log.info("开始批量距离计算，数量: {}", points1.size());
        
        List<Map<String, Object>> results = new ArrayList<>();
        double totalDistance = 0;
        
        for (int i = 0; i < points1.size(); i++) {
            List<Double> p1 = points1.get(i);
            List<Double> p2 = points2.get(i);
            
            Map<String, Object> result = gisService.calculateDistance(
                    p1.get(0), p1.get(1), p2.get(0), p2.get(1), "km");
            
            double dist = (double) result.get("distance");
            totalDistance += dist;
            
            results.add(Map.of(
                    "index", i,
                    "from", p1,
                    "to", p2,
                    "distance", dist,
                    "unit", "km"
            ));
        }
        
        return Map.of(
                "count", points1.size(),
                "totalDistance", Math.round(totalDistance * 1000.0) / 1000.0,
                "averageDistance", Math.round(totalDistance / points1.size() * 1000.0) / 1000.0,
                "results", results
        );
    }
    
    /**
     * 点位聚合（网格聚合）
     */
    public Map<String, Object> gridAggregation(
            List<Map<String, Object>> points,
            double gridSizeDegrees) {
        
        log.info("开始网格聚合，点位数量: {}, 网格大小: {}°", points.size(), gridSizeDegrees);
        
        // 按网格分组
        Map<String, List<Map<String, Object>>> gridMap = new HashMap<>();
        
        for (Map<String, Object> point : points) {
            Map<String, Object> geometry = (Map<String, Object>) point.get("geometry");
            if (geometry == null) continue;
            
            List<Number> coords = (List<Number>) geometry.get("coordinates");
            if (coords == null || coords.size() < 2) continue;
            
            double lon = coords.get(0).doubleValue();
            double lat = coords.get(1).doubleValue();
            
            // 计算网格索引
            String gridKey = String.format("%.4f,%.4f", 
                    Math.floor(lat / gridSizeDegrees) * gridSizeDegrees,
                    Math.floor(lon / gridSizeDegrees) * gridSizeDegrees);
            
            gridMap.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(point);
        }
        
        // 构建聚合结果
        List<Map<String, Object>> clusters = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : gridMap.entrySet()) {
            String[] parts = entry.getKey().split(",");
            double gridLat = Double.parseDouble(parts[0]);
            double gridLon = Double.parseDouble(parts[1]);
            
            // 计算网格中心
            double centerLat = gridLat + gridSizeDegrees / 2;
            double centerLon = gridLon + gridSizeDegrees / 2;
            
            clusters.add(Map.of(
                    "gridKey", entry.getKey(),
                    "count", entry.getValue().size(),
                    "center", List.of(centerLon, centerLat),
                    "bounds", Map.of(
                            "south", gridLat,
                            "west", gridLon,
                            "north", gridLat + gridSizeDegrees,
                            "east", gridLon + gridSizeDegrees
                    )
            ));
        }
        
        // 按数量排序
        clusters.sort((a, b) -> Integer.compare(
                (Integer) b.get("count"), (Integer) a.get("count")));
        
        return Map.of(
                "totalPoints", points.size(),
                "gridCount", clusters.size(),
                "gridSizeDegrees", gridSizeDegrees,
                "clusters", clusters
        );
    }
    
    /**
     * 空间连接（点与多边形）
     */
    public Map<String, Object> spatialJoinPointsToPolygon(
            List<Map<String, Object>> points,
            List<Map<String, Object>> polygons) {
        
        log.info("开始空间连接: {}个点, {}个多边形", points.size(), polygons.size());
        
        // 解析多边形几何
        Map<Integer, org.locationtech.jts.geom.Geometry> polygonGeomMap = new HashMap<>();
        for (int i = 0; i < polygons.size(); i++) {
            try {
                Map<String, Object> geom = (Map<String, Object>) polygons.get(i).get("geometry");
                if (geom != null) {
                    polygonGeomMap.put(i, gisService.parseGeometry(geom));
                }
            } catch (Exception e) {
                log.warn("多边形 {} 解析失败: {}", i, e.getMessage());
            }
        }
        
        // 对每个点进行空间连接
        List<Map<String, Object>> results = new ArrayList<>();
        int matchedCount = 0;
        
        for (Map<String, Object> point : points) {
            Map<String, Object> pointGeom = (Map<String, Object>) point.get("geometry");
            if (pointGeom == null) continue;
            
            try {
                org.locationtech.jts.geom.Geometry pointJts = gisService.parseGeometry(pointGeom);
                
                // 查找匹配的多边形
                for (Map.Entry<Integer, org.locationtech.jts.geom.Geometry> entry : polygonGeomMap.entrySet()) {
                    if (entry.getValue().contains(pointJts)) {
                        results.add(Map.of(
                                "point", point,
                                "matchedPolygonIndex", entry.getKey(),
                                "matchedPolygon", polygons.get(entry.getKey())
                        ));
                        matchedCount++;
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("点处理失败: {}", e.getMessage());
            }
        }
        
        return Map.of(
                "totalPoints", points.size(),
                "matchedPoints", matchedCount,
                "unmatchedPoints", points.size() - matchedCount,
                "matchedPolygons", polygonGeomMap.size(),
                "results", results
        );
    }
    
    /**
     * 路径简化（保留关键点）
     */
    public Map<String, Object> simplifyPath(
            List<List<Double>> coordinates,
            double tolerance) {
        
        if (coordinates.size() < 3) {
            return Map.of("error", "Need at least 3 points");
        }
        
        // 构建线字符串
        org.locationtech.jts.geom.Coordinate[] coords = 
                coordinates.stream()
                        .map(c -> new org.locationtech.jts.geom.Coordinate(c.get(0), c.get(1)))
                        .toArray(org.locationtech.jts.geom.Coordinate[]::new);
        
        org.locationtech.jts.geom.LineString line = 
                new org.locationtech.jts.geom.GeometryFactory()
                        .createLineString(coords);
        
        // 简化
        org.locationtech.jts.geom.Geometry simplified = 
                org.locationtech.jts.simplify.DouglasPeuckerSimplifier.simplify(line, tolerance);
        
        // 提取简化后的坐标
        List<List<Double>> simplifiedCoords = new ArrayList<>();
        for (org.locationtech.jts.geom.Coordinate c : simplified.getCoordinates()) {
            simplifiedCoords.add(List.of(c.x, c.y));
        }
        
        return Map.of(
                "originalPoints", coordinates.size(),
                "simplifiedPoints", simplifiedCoords.size(),
                "reduction", String.format("%.1f%%", 
                        (1 - (double) simplifiedCoords.size() / coordinates.size()) * 100),
                "tolerance", tolerance,
                "simplifiedCoordinates", simplifiedCoords
        );
    }
}
