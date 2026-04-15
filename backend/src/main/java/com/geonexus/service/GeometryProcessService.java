package com.geonexus.service;

import com.geonexus.common.exception.GeometryProcessException;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.springframework.stereotype.Service;

/**
 * 几何处理服务
 * 职责：几何处理（简化/凸包/边界）
 */
@Slf4j
@Service
public class GeometryProcessService {
    
    private final GeometryFactory geometryFactory;
    
    public GeometryProcessService() {
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
    }
    
    // ==================== 几何简化 ====================
    
    /**
     * 几何简化 - Douglas-Peucker算法
     */
    public Geometry simplify(Geometry geom, double tolerance) {
        if (geom == null) {
            throw new GeometryProcessException("几何对象为空");
        }
        
        if (tolerance < 0) {
            throw new GeometryProcessException("简化容差不能为负数");
        }
        
        try {
            return DouglasPeuckerSimplifier.simplify(geom, tolerance);
        } catch (Exception e) {
            throw new GeometryProcessException("几何简化失败", e);
        }
    }
    
    // ==================== 凸包 ====================
    
    /**
     * 凸包计算
     */
    public Geometry convexHull(Geometry geom) {
        if (geom == null) {
            throw new GeometryProcessException("几何对象为空");
        }
        
        try {
            return geom.convexHull();
        } catch (Exception e) {
            throw new GeometryProcessException("凸包计算失败", e);
        }
    }
    
    // ==================== 边界 ====================
    
    /**
     * 计算几何边界
     */
    public Geometry boundary(Geometry geom) {
        if (geom == null) {
            throw new GeometryProcessException("几何对象为空");
        }
        
        try {
            return geom.getBoundary();
        } catch (Exception e) {
            throw new GeometryProcessException("边界计算失败", e);
        }
    }
    
    /**
     * 计算边界框
     */
    public Envelope boundingBox(Geometry geom) {
        if (geom == null) {
            throw new GeometryProcessException("几何对象为空");
        }
        
        return geom.getEnvelopeInternal();
    }
    
    /**
     * 获取边界框信息
     */
    public Map<String, Object> getBoundingBoxInfo(Geometry geom) {
        Envelope envelope = boundingBox(geom);
        
        return Map.of(
            "south", envelope.getMinY(),
            "west", envelope.getMinX(),
            "north", envelope.getMaxY(),
            "east", envelope.getMaxX(),
            "center", List.of(
                (envelope.getMinY() + envelope.getMaxY()) / 2,
                (envelope.getMinX() + envelope.getMaxX()) / 2
            )
        );
    }
}
