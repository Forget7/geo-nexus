package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operationoverlay.snap.SnapOverlayOp;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * 高级地理空间分析服务
 * 包含复杂空间分析算法：三角剖分、Voronoi图、空间插值等
 */
@Slf4j
@Service
public class AdvancedGeoProcessingService {
    
    private final GeometryFactory geometryFactory;
    private final ObjectMapper objectMapper;
    
    public AdvancedGeoProcessingService(ObjectMapper objectMapper) {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.objectMapper = objectMapper;
    }
    
    /**
     * Delaunay三角剖分
     */
    public Map<String, Object> delaunayTriangulation(List<List<Double>> points) {
        log.info("执行Delaunay三角剖分，点数: {}", points.size());
        
        if (points.size() < 3) {
            return Map.of("error", "Need at least 3 points");
        }
        
        Coordinate[] coords = points.stream()
                .map(p -> new Coordinate(p.get(0), p.get(1)))
                .toArray(Coordinate[]::new);
        
        Point[] pts = new Point[coords.length];
        for (int i = 0; i < coords.length; i++) {
            pts[i] = geometryFactory.createPoint(coords[i]);
        }
        
        GeometryCollection pointCollection = geometryFactory.createGeometryCollection(pts);
        
        DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
        builder.setInputGeometry(pointCollection);
        
        Geometry triangles = builder.getTriangles(geometryFactory);
        
        // 转换为GeoJSON格式
        List<Map<String, Object>> features = new ArrayList<>();
        for (int i = 0; i < triangles.getNumGeometries(); i++) {
            Geometry triangle = triangles.getGeometryN(i);
            features.add(Map.of(
                    "type", "Feature",
                    "properties", Map.of("id", i, "area", triangle.getArea()),
                    "geometry", toGeoJSON(triangle)
            ));
        }
        
        return Map.of(
                "type", "FeatureCollection",
                "count", triangles.getNumGeometries(),
                "totalArea", triangles.getArea(),
                "features", features
        );
    }
    
    /**
     * Voronoi图生成
     */
    public Map<String, Object> voronoiDiagram(List<List<Double>> points, 
                                               List<Double> boundingBox) {
        log.info("生成Voronoi图，点数: {}", points.size());
        
        if (points.size() < 2) {
            return Map.of("error", "Need at least 2 points");
        }
        
        Coordinate[] coords = points.stream()
                .map(p -> new Coordinate(p.get(0), p.get(1)))
                .toArray(Coordinate[]::new);
        
        Point[] pts = new Point[coords.length];
        for (int i = 0; i < coords.length; i++) {
            pts[i] = geometryFactory.createPoint(coords[i]);
        }
        
        GeometryCollection pointCollection = geometryFactory.createGeometryCollection(pts);
        
        // 设置外包络
        Envelope envelope;
        if (boundingBox != null && boundingBox.size() >= 4) {
            envelope = new Envelope(
                    boundingBox.get(0), boundingBox.get(1), // minX, maxX
                    boundingBox.get(2), boundingBox.get(3)  // minY, maxY
            );
        } else {
            envelope = pointCollection.getEnvelopeInternal();
            envelope.expandBy(0.1); // 默认扩展10%
        }
        
        VoronoiDiagramBuilder builder = new VoronoiDiagramBuilder();
        builder.setInputGeometry(pointCollection);
        builder.setClipEnvelope(envelope);
        
        Geometry voronoi = builder.getDiagram(geometryFactory);
        
        List<Map<String, Object>> features = new ArrayList<>();
        for (int i = 0; i < voronoi.getNumGeometries(); i++) {
            Geometry cell = voronoi.getGeometryN(i);
            Point nearestPoint = findNearestPoint(cell, pts);
            
            features.add(Map.of(
                    "type", "Feature",
                    "properties", Map.of(
                            "id", i,
                            "area", cell.getArea(),
                            "point", List.of(nearestPoint.getX(), nearestPoint.getY())
                    ),
                    "geometry", toGeoJSON(cell)
            ));
        }
        
        return Map.of(
                "type", "FeatureCollection",
                "count", voronoi.getNumGeometries(),
                "features", features
        );
    }
    
    /**
     * 线段合并
     */
    public Map<String, Object> mergeLines(List<List<List<Double>>> lineStrings) {
        log.info("合并线段，数量: {}", lineStrings.size());
        
        List<LineString> lines = new ArrayList<>();
        for (List<List<Double>> ls : lineStrings) {
            Coordinate[] coords = ls.stream()
                    .map(p -> new Coordinate(p.get(0), p.get(1)))
                    .toArray(Coordinate[]::new);
            lines.add(geometryFactory.createLineString(coords));
        }
        
        LineMerger merger = new LineMerger();
        merger.add(lines);
        
        Collection<Geometry> merged = merger.getMergedLineStrings();
        
        List<Map<String, Object>> features = new ArrayList<>();
        int i = 0;
        for (Geometry g : merged) {
            features.add(Map.of(
                    "type", "Feature",
                    "properties", Map.of("id", i++, "length", g.getLength()),
                    "geometry", toGeoJSON(g)
            ));
        }
        
        return Map.of(
                "type", "FeatureCollection",
                "originalCount", lineStrings.size(),
                "mergedCount", merged.size(),
                "features", features
        );
    }
    
    /**
     * 几何融合（合并相邻多边形）
     */
    public Map<String, Object> dissolvePolygons(List<List<List<Double>>> polygons,
                                                 String dissolveField,
                                                 List<Object> fieldValues) {
        log.info("融合多边形，数量: {}", polygons.size());
        
        List<Polygon> polygonList = new ArrayList<>();
        Map<Object, List<Polygon>> grouped = new HashMap<>();
        
        for (int i = 0; i < polygons.size(); i++) {
            Polygon polygon = createPolygon(polygons.get(i));
            polygonList.add(polygon);
            
            Object fieldValue = fieldValues != null && i < fieldValues.size() 
                    ? fieldValues.get(i) : "default";
            grouped.computeIfAbsent(fieldValue, k -> new ArrayList<>()).add(polygon);
        }
        
        List<Map<String, Object>> features = new ArrayList<>();
        
        if (dissolveField != null && !dissolveField.isEmpty()) {
            // 按字段值分组融合
            int fid = 0;
            for (Map.Entry<Object, List<Polygon>> entry : grouped.entrySet()) {
                Geometry dissolved = CascadedPolygonUnion.union(entry.getValue());
                
                for (int j = 0; j < dissolved.getNumGeometries(); j++) {
                    Geometry g = dissolved.getGeometryN(j);
                    features.add(Map.of(
                            "type", "Feature",
                            "properties", Map.of(
                                    "id", fid++,
                                    dissolveField, entry.getKey(),
                                    "area", g.getArea()
                            ),
                            "geometry", toGeoJSON(g)
                    ));
                }
            }
        } else {
            // 全部融合
            Geometry dissolved = CascadedPolygonUnion.union(polygonList);
            
            for (int j = 0; j < dissolved.getNumGeometries(); j++) {
                Geometry g = dissolved.getGeometryN(j);
                features.add(Map.of(
                        "type", "Feature",
                        "properties", Map.of("id", j, "area", g.getArea()),
                        "geometry", toGeoJSON(g)
                ));
            }
        }
        
        return Map.of(
                "type", "FeatureCollection",
                "originalCount", polygons.size(),
                "dissolvedCount", features.size(),
                "features", features
        );
    }
    
    /**
     * 几何裁剪
     */
    public Map<String, Object> clipGeometry(List<List<Double>> target,
                                              List<List<List<Double>>> clipPolygon) {
        log.info("几何裁剪操作");
        
        if (clipPolygon == null || clipPolygon.isEmpty()) {
            return Map.of("error", "Clip polygon required");
        }
        
        Geometry targetGeom;
        if (target.get(0).size() == 2) {
            targetGeom = createPolygon(target);
        } else {
            targetGeom = createMultiPoint(target);
        }
        
        Polygon clip = createPolygon(clipPolygon.get(0));
        
        Geometry clipped = targetGeom.intersection(clip);
        
        return Map.of(
                "type", "Feature",
                "properties", Map.of(
                        "originalArea", targetGeom.getArea(),
                        "clippedArea", clipped.getArea(),
                        "preservedRatio", clipped.getArea() / targetGeom.getArea()
                ),
                "geometry", toGeoJSON(clipped)
        );
    }
    
    /**
     * 缓冲区分析（带端点处理）
     */
    public Map<String, Object> bufferWithEndcaps(List<List<Double>> points,
                                                  double distanceKm,
                                                  String endcapStyle) {
        log.info("缓冲区分析，距离: {}km, 端点样式: {}", distanceKm, endcapStyle);
        
        // 转换km到度（粗略）
        double distanceDeg = distanceKm / 111.0;
        
        Point[] pts = points.stream()
                .map(p -> geometryFactory.createPoint(new Coordinate(p.get(0), p.get(1))))
                .toArray(Point[]::new);
        
        Geometry[] buffers;
        
        switch (endcapStyle != null ? endcapStyle : "round") {
            case "flat":
                buffers = Arrays.stream(pts)
                        .map(p -> p.buffer(distanceDeg, 0, org.locationtech.jts.operation.buffer.BufferOp.SCAP_STYLE.FLAT))
                        .toArray(Geometry[]::new);
                break;
            case "square":
                buffers = Arrays.stream(pts)
                        .map(p -> p.buffer(distanceDeg, 0, org.locationtech.jts.operation.buffer.BufferOp.SCAP_STYLE.SQUARE))
                        .toArray(Geometry[]::new);
                break;
            case "round":
            default:
                buffers = Arrays.stream(pts)
                        .map(p -> p.buffer(distanceDeg))
                        .toArray(Geometry[]::new);
                break;
        }
        
        GeometryCollection collection = geometryFactory.createGeometryCollection(buffers);
        
        List<Map<String, Object>> features = new ArrayList<>();
        for (int i = 0; i < buffers.length; i++) {
            features.add(Map.of(
                    "type", "Feature",
                    "properties", Map.of("id", i, "area", buffers[i].getArea()),
                    "geometry", toGeoJSON(buffers[i])
            ));
        }
        
        // 合并所有缓冲区
        Geometry dissolved = CascadedPolygonUnion.union(Arrays.asList(buffers));
        
        return Map.of(
                "type", "FeatureCollection",
                "bufferCount", buffers.length,
                "dissolvedArea", dissolved.getArea(),
                "individualBuffers", features,
                "dissolvedGeometry", toGeoJSON(dissolved)
        );
    }
    
    /**
     * 空间插值（IDW - 反距离加权）
     */
    public Map<String, Object> interpolateIDW(List<List<Double>> samplePoints,
                                                List<Double> values,
                                                List<List<Double>> grid,
                                                double power) {
        log.info("IDW空间插值，样本点: {}, 网格点: {}, 幂次: {}", 
                samplePoints.size(), grid.size(), power);
        
        if (samplePoints.size() != values.size()) {
            return Map.of("error", "Points and values count mismatch");
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (List<Double> gridPoint : grid) {
            double interpolatedValue = 0;
            double totalWeight = 0;
            
            for (int i = 0; i < samplePoints.size(); i++) {
                List<Double> sample = samplePoints.get(i);
                
                double distance = haversineDistance(
                        gridPoint.get(0), gridPoint.get(1),
                        sample.get(0), sample.get(1)
                );
                
                if (distance < 0.0001) {
                    interpolatedValue = values.get(i);
                    totalWeight = 1;
                    break;
                }
                
                double weight = 1.0 / Math.pow(distance, power);
                interpolatedValue += values.get(i) * weight;
                totalWeight += weight;
            }
            
            if (totalWeight > 0) {
                interpolatedValue /= totalWeight;
            }
            
            results.add(Map.of(
                    "location", gridPoint,
                    "value", interpolatedValue
            ));
        }
        
        return Map.of(
                "method", "IDW",
                "power", power,
                "sampleCount", samplePoints.size(),
                "gridCount", grid.size(),
                "results", results
        );
    }
    
    /**
     * 等高线生成（简化的基于网格的方法）
     */
    public Map<String, Object> generateContours(List<List<Double>> grid,
                                                  List<Double> levels) {
        log.info("生成等高线，网格: {}x{}, 分级: {}", 
                grid.size(), grid.get(0).size(), levels.size());
        
        // 简化的等高线生成
        List<Map<String, Object>> contours = new ArrayList<>();
        
        int rows = grid.size();
        int cols = grid.get(0).size();
        
        for (double level : levels) {
            List<List<Double>> contourPoints = new ArrayList<>();
            
            // 扫描网格查找等值线点
            for (int i = 0; i < rows - 1; i++) {
                for (int j = 0; j < cols - 1; j++) {
                    double v00 = grid.get(i).get(j);
                    double v10 = grid.get(i + 1).get(j);
                    double v01 = grid.get(i).get(j + 1);
                    double v11 = grid.get(i + 1).get(j + 1);
                    
                    // 检查是否跨越等值线
                    if ((v00 <= level && v10 > level) || (v00 > level && v10 <= level)) {
                        double t = (level - v00) / (v10 - v00);
                        contourPoints.add(List.of((double) i + t, (double) j));
                    }
                    
                    if ((v00 <= level && v01 > level) || (v00 > level && v01 <= level)) {
                        double t = (level - v00) / (v01 - v00);
                        contourPoints.add(List.of((double) i, (double) j + t));
                    }
                }
            }
            
            if (!contourPoints.isEmpty()) {
                contours.add(Map.of(
                        "level", level,
                        "points", contourPoints,
                        "pointCount", contourPoints.size()
                ));
            }
        }
        
        return Map.of(
                "type", "Contours",
                "gridSize", Map.of("rows", rows, "cols", cols),
                "levelCount", levels.size(),
                "contours", contours
        );
    }
    
    /**
     * 几何验证与修复
     */
    public Map<String, Object> validateAndRepair(List<List<List<Double>>> polygons) {
        log.info("几何验证与修复，数量: {}", polygons.size());
        
        List<Map<String, Object>> results = new ArrayList<>();
        int validCount = 0;
        int repairedCount = 0;
        int invalidCount = 0;
        
        for (int i = 0; i < polygons.size(); i++) {
            List<List<Double>> poly = polygons.get(i);
            Map<String, Object> result = new HashMap<>();
            result.put("index", i);
            
            try {
                Polygon polygon = createPolygon(poly);
                
                // 检查有效性
                if (polygon.isValid()) {
                    result.put("valid", true);
                    result.put("repaired", false);
                    validCount++;
                } else {
                    result.put("valid", false);
                    
                    // 尝试修复
                    Geometry repaired = repairGeometry(polygon);
                    if (repaired.isValid()) {
                        result.put("repaired", true);
                        result.put("geometry", toGeoJSON(repaired));
                        repairedCount++;
                    } else {
                        result.put("repaired", false);
                        result.put("error", "Could not repair");
                        invalidCount++;
                    }
                }
                
                result.put("area", polygon.getArea());
                result.put("perimeter", polygon.getLength());
                result.put("numHoles", polygon.getNumInteriorRing());
                
            } catch (Exception e) {
                result.put("valid", false);
                result.put("error", e.getMessage());
                invalidCount++;
            }
            
            results.add(result);
        }
        
        return Map.of(
                "total", polygons.size(),
                "valid", validCount,
                "repaired", repairedCount,
                "invalid", invalidCount,
                "details", results
        );
    }
    
    // ==================== 辅助方法 ====================
    
    private Point findNearestPoint(Geometry cell, Point[] points) {
        Point nearest = null;
        double minDist = Double.MAX_VALUE;
        Point cellCentroid = cell.getCentroid();
        
        for (Point p : points) {
            double dist = cellCentroid.distance(p);
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }
        
        return nearest;
    }
    
    private Polygon createPolygon(List<List<Double>> coords) {
        Coordinate[] jtsCoords = coords.stream()
                .map(c -> new Coordinate(c.get(0), c.get(1)))
                .toArray(Coordinate[]::new);
        
        // 确保闭合
        if (!jtsCoords[0].equals2D(jtsCoords[jtsCoords.length - 1])) {
            jtsCoords = Arrays.copyOf(jtsCoords, jtsCoords.length + 1);
            jtsCoords[jtsCoords.length - 1] = jtsCoords[0];
        }
        
        LinearRing shell = geometryFactory.createLinearRing(jtsCoords);
        return geometryFactory.createPolygon(shell);
    }
    
    private MultiPoint createMultiPoint(List<List<Double>> coords) {
        Point[] points = coords.stream()
                .map(c -> geometryFactory.createPoint(new Coordinate(c.get(0), c.get(1))))
                .toArray(Point[]::new);
        return geometryFactory.createMultiPoint(points);
    }
    
    private Geometry repairGeometry(Geometry input) {
        try {
            // 尝试使用SnapOverlay进行修复
            Geometry snapped = SnapOverlayOp.snapToSelf(input, 0.0001, true);
            if (snapped.isValid()) {
                return snapped;
            }
        } catch (Exception ignored) {}
        
        // 尝试缓冲0
        Geometry buffered = input.buffer(0);
        if (buffered.isValid()) {
            return buffered;
        }
        
        return input;
    }
    
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 地球半径km
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
    
    private Map<String, Object> toGeoJSON(Geometry geometry) {
        Map<String, Object> geojson = new HashMap<>();
        geojson.put("type", geometry.getGeometryType());
        geojson.put("coordinates", toGeoJSONCoordinates(geometry));
        return geojson;
    }
    
    private Object toGeoJSONCoordinates(Geometry geometry) {
        if (geometry instanceof Point) {
            Point p = (Point) geometry;
            return List.of(p.getX(), p.getY());
        } else if (geometry instanceof LineString) {
            LineString ls = (LineString) geometry;
            List<List<Double>> coords = new ArrayList<>();
            for (Coordinate c : ls.getCoordinates()) {
                coords.add(List.of(c.x, c.y));
            }
            return coords;
        } else if (geometry instanceof Polygon) {
            Polygon poly = (Polygon) geometry;
            List<List<List<Double>>> rings = new ArrayList<>();
            
            // 外环
            List<List<Double>> exterior = new ArrayList<>();
            for (Coordinate c : poly.getExteriorRing().getCoordinates()) {
                exterior.add(List.of(c.x, c.y));
            }
            rings.add(exterior);
            
            // 内环
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                List<List<Double>> interior = new ArrayList<>();
                for (Coordinate c : poly.getInteriorRingN(i).getCoordinates()) {
                    interior.add(List.of(c.x, c.y));
                }
                rings.add(interior);
            }
            
            return rings;
        }
        
        return geometry.toText();
    }
}
