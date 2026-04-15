package com.geonexus.service;

import com.geonexus.common.exception.GeometryProcessException;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.springframework.stereotype.Service;

/**
 * 几何验证服务
 * 职责：几何有效性验证
 */
@Slf4j
@Service
public class GeometryValidationService {
    
    private final GeometryFactory geometryFactory;
    
    public GeometryValidationService() {
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
    }
    
    // ==================== 有效性验证 ====================
    
    /**
     * 检查几何是否有效
     */
    public boolean isValid(Geometry geom) {
        if (geom == null) {
            return false;
        }
        
        try {
            return geom.isValid();
        } catch (Exception e) {
            log.warn("几何验证异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查几何是否有效（详细）
     */
    public ValidationResult validate(Geometry geom) {
        if (geom == null) {
            return new ValidationResult(false, "几何对象为空", null);
        }
        
        try {
            IsValidOp op = new IsValidOp(geom);
            boolean valid = op.isValid();
            
            if (valid) {
                return new ValidationResult(true, "几何有效", null);
            } else {
                TopologyValidationError error = op.getValidationError();
                String message = error != null ? error.getMessage() : "未知错误";
                return new ValidationResult(false, message, error);
            }
        } catch (Exception e) {
            return new ValidationResult(false, "验证异常: " + e.getMessage(), null);
        }
    }
    
    // ==================== 修复几何 ====================
    
    /**
     * 修复无效几何使其有效
     */
    public Geometry makeValid(Geometry geom) {
        if (geom == null) {
            throw new GeometryProcessException("几何对象为空");
        }
        
        if (geom.isValid()) {
            return geom;
        }
        
        try {
            // 使用 buffer(0) 技术修复自相交等常见问题
            Geometry fixed = geom.buffer(0);
            
            if (!fixed.isValid()) {
                log.warn("buffer(0) 修复失败，尝试其他方法");
                // 尝试提取有效部分
                fixed = extractValidPart(geom);
            }
            
            log.info("几何修复成功: {} -> {}", geom.getGeometryType(), fixed.getGeometryType());
            return fixed;
        } catch (Exception e) {
            throw new GeometryProcessException("几何修复失败", e);
        }
    }
    
    /**
     * 提取几何的有效部分
     */
    private Geometry extractValidPart(Geometry geom) {
        try {
            if (geom instanceof Polygon || geom instanceof MultiPolygon) {
                return extractValidPolygonPart((Geometry) geom);
            } else if (geom instanceof LineString) {
                return geom.isSimple() ? geom : geometryFactory.createLineString(null);
            } else if (geom instanceof Point) {
                return geom;
            }
            
            return geom;
        } catch (Exception e) {
            return geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }
    
    private Geometry extractValidPolygonPart(Geometry geom) {
        try {
            Geometry[] geoms = geom.getNumGeometries() > 1 
                ? geom.getNumGeometries() > 0 ? new Geometry[geom.getNumGeometries()] : new Geometry[0]
                : new Geometry[] { geom };
            
            if (geom.getNumGeometries() <= 1) {
                geoms = new Geometry[] { geom };
            } else {
                for (int i = 0; i < geom.getNumGeometries(); i++) {
                    geoms[i] = geom.getGeometryN(i);
                }
            }
            
            java.util.List<Polygon> validPolygons = new java.util.ArrayList<>();
            
            for (Geometry g : geoms) {
                if (g.isValid()) {
                    validPolygons.add((Polygon) g);
                }
            }
            
            if (validPolygons.isEmpty()) {
                return geometryFactory.createPoint(new Coordinate(0, 0));
            }
            
            if (validPolygons.size() == 1) {
                return validPolygons.get(0);
            }
            
            return geometryFactory.createMultiPolygon(validPolygons.toArray(new Polygon[0]));
        } catch (Exception e) {
            return geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }
    
    // ==================== 简单性检查 ====================
    
    /**
     * 检查几何是否简单（无自相交）
     */
    public boolean isSimple(Geometry geom) {
        if (geom == null) {
            return false;
        }
        
        try {
            return geom.isSimple();
        } catch (Exception e) {
            log.warn("简单性检查异常: {}", e.getMessage());
            return false;
        }
    }
    
    // ==================== 验证结果类 ====================
    
    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final TopologyValidationError error;
        
        public ValidationResult(boolean valid, String message, TopologyValidationError error) {
            this.valid = valid;
            this.message = message;
            this.error = error;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public TopologyValidationError getError() {
            return error;
        }
    }
}
