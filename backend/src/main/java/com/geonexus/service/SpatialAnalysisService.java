package com.geonexus.service;

import com.geonexus.common.exception.GeometryProcessException;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 空间分析服务
 * 职责：空间分析（缓冲区/叠加/空间连接）
 */
@Slf4j
@Service
public class SpatialAnalysisService {
    
    private final GeometryFactory geometryFactory;
    private final GeometryCalcService geometryCalcService;
    
    public SpatialAnalysisService(GeometryCalcService geometryCalcService) {
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
        this.geometryCalcService = geometryCalcService;
    }
    
    // ==================== 缓冲区分析 ====================
    
    /**
     * 缓冲区分析 - 创建给定距离的缓冲区
     */
    public Geometry buffer(Geometry geom, double distanceKm) {
        try {
            // 转换为米为单位的投影坐标系
            Geometry projected = geometryCalcService.transformToEqualDistance(geom);
            
            // 创建缓冲区
            BufferOp bufOp = new BufferOp(projected);
            bufOp.setEndCapStyle(BufferOp.CAP_ROUND);
            Geometry buffer = bufOp.getResultGeometry(distanceKm * 1000);
            
            // 转回WGS84
            return transformToWGS84(buffer);
        } catch (Exception e) {
            throw new GeometryProcessException("缓冲区分析失败", e);
        }
    }
    
    /**
     * 转换到米为单位的投影坐标系
     */
    private Geometry transformToMeter(Geometry geom) {
        return geometryCalcService.transformToEqualDistance(geom);
    }
    
    /**
     * 转换到WGS84
     */
    private Geometry transformToWGS84(Geometry geom) {
        try {
            return geometryCalcService.reprojectGeometry(geom, "EPSG:3857", "EPSG:4326");
        } catch (Exception e) {
            return geom;
        }
    }
    
    // ==================== 叠加分析 ====================
    
    /**
     * 叠加分析 - Union
     */
    public Geometry union(Geometry geom1, Geometry geom2) {
        try {
            List<Geometry> geoms = new ArrayList<>();
            geoms.add(geom1);
            geoms.add(geom2);
            return new UnaryUnionOp(geoms).union();
        } catch (Exception e) {
            throw new GeometryProcessException("Union分析失败", e);
        }
    }
    
    /**
     * 叠加分析 - Intersection
     */
    public Geometry intersection(Geometry geom1, Geometry geom2) {
        try {
            return geom1.intersection(geom2);
        } catch (Exception e) {
            throw new GeometryProcessException("Intersection分析失败", e);
        }
    }
    
    /**
     * 叠加分析 - Difference
     */
    public Geometry difference(Geometry geom1, Geometry geom2) {
        try {
            return geom1.difference(geom2);
        } catch (Exception e) {
            throw new GeometryProcessException("Difference分析失败", e);
        }
    }
    
    /**
     * 叠加分析 - Symmetric Difference
     */
    public Geometry symDifference(Geometry geom1, Geometry geom2) {
        try {
            return geom1.symDifference(geom2);
        } catch (Exception e) {
            throw new GeometryProcessException("Symmetric Difference分析失败", e);
        }
    }
    
    // ==================== 空间过滤 ====================
    
    /**
     * 空间过滤 - 按边界框过滤要素
     */
    public List<Geometry> spatialFilter(List<Geometry> geometries, Envelope envelope) {
        Geometry filterGeom = JTS.toGeometry(envelope);
        List<Geometry> filtered = new ArrayList<>();
        
        for (Geometry geom : geometries) {
            if (geom.intersects(filterGeom)) {
                filtered.add(geom);
            }
        }
        
        return filtered;
    }
    
    /**
     * 空间过滤 - 按边界框过滤几何列表
     */
    public List<Map<String, Object>> filterByBounds(List<Map<String, Object>> features, 
                                                     double south, double west, 
                                                     double north, double east) {
        Envelope envelope = new Envelope(west, east, south, north);
        Geometry filterGeom = JTS.toGeometry(envelope);
        
        List<Map<String, Object>> filtered = new ArrayList<>();
        
        for (Map<String, Object> feature : features) {
            @SuppressWarnings("unchecked")
            Map<String, Object> geomMap = (Map<String, Object>) feature.get("geometry");
            Geometry geom = parseGeometry(geomMap);
            
            if (geom != null && geom.intersects(filterGeom)) {
                filtered.add(feature);
            }
        }
        
        return filtered;
    }
    
    // ==================== 空间连接 ====================
    
    /**
     * 空间连接 - 基于空间关系合并数据
     */
    public List<Map<String, Object>> spatialJoin(List<Map<String, Object>> features1,
                                                   List<Map<String, Object>> features2,
                                                   String predicate) {
        List<Map<String, Object>> joined = new ArrayList<>();
        
        for (Map<String, Object> f1 : features1) {
            @SuppressWarnings("unchecked")
            Map<String, Object> g1 = (Map<String, Object>) f1.get("geometry");
            Geometry geom1 = parseGeometry(g1);
            
            for (Map<String, Object> f2 : features2) {
                @SuppressWarnings("unchecked")
                Map<String, Object> g2 = (Map<String, Object>) f2.get("geometry");
                Geometry geom2 = parseGeometry(g2);
                
                boolean matches = switch (predicate != null ? predicate.toLowerCase() : "intersects") {
                    case "contains" -> geom1.contains(geom2);
                    case "within" -> geom1.within(geom2);
                    case "equals" -> geom1.equals(geom2);
                    case "distance" -> geom1.distance(geom2) < 0.01; // 约1km
                    default -> geom1.intersects(geom2);
                };
                
                if (matches) {
                    Map<String, Object> merged = new java.util.HashMap<>(f1);
                    merged.put("joined", f2);
                    joined.add(merged);
                }
            }
        }
        
        return joined;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 从GeoJSON解析Geometry
     */
    public Geometry parseGeometry(Map<String, Object> geojson) {
        if (geojson == null) {
            return null;
        }
        
        String type = (String) geojson.get("type");
        Object coords = geojson.get("coordinates");
        
        if (type == null || coords == null) {
            return null;
        }
        
        return switch (type.toLowerCase()) {
            case "point" -> {
                @SuppressWarnings("unchecked")
                List<Number> c = (List<Number>) coords;
                yield geometryFactory.createPoint(new Coordinate(c.get(0).doubleValue(), c.get(1).doubleValue()));
            }
            case "linestring" -> {
                @SuppressWarnings("unchecked")
                List<List<Number>> coordinates = (List<List<Number>>) coords;
                Coordinate[] coords2 = new Coordinate[coordinates.size()];
                for (int i = 0; i < coordinates.size(); i++) {
                    @SuppressWarnings("unchecked")
                    List<Number> c = coordinates.get(i);
                    coords2[i] = new Coordinate(c.get(0).doubleValue(), c.get(1).doubleValue());
                }
                yield geometryFactory.createLineString(coords2);
            }
            case "polygon" -> createPolygon((List<List<List<Number>>>) coords);
            default -> null;
        };
    }
    
    private Polygon createPolygon(List<List<List<Number>>> rings) {
        List<LineString> shells = new ArrayList<>();
        List<LineString> holes = new ArrayList<>();
        
        for (int i = 0; i < rings.size(); i++) {
            List<List<Number>> ring = rings.get(i);
            Coordinate[] coords2 = new Coordinate[ring.size()];
            for (int j = 0; j < ring.size(); j++) {
                @SuppressWarnings("unchecked")
                List<Number> c = ring.get(j);
                coords2[j] = new Coordinate(c.get(0).doubleValue(), c.get(1).doubleValue());
            }
            LineString ls = geometryFactory.createLineString(coords2);
            if (i == 0) {
                shells.add(ls);
            } else {
                holes.add(ls);
            }
        }
        
        if (shells.isEmpty()) {
            return null;
        }
        
        return geometryFactory.createPolygon(shells.get(0), holes.toArray(new LineString[0]));
    }
}
