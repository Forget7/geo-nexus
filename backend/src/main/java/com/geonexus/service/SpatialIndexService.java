package com.geonexus.service;

import com.geonexus.common.exception.SpatialIndexException;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.STRtree;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间索引服务
 * 职责：空间索引管理（R-Tree索引构建与查询）
 */
@Slf4j
@Service
public class SpatialIndexService {
    
    private final GeometryFactory geometryFactory;
    
    public SpatialIndexService() {
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
    }
    
    // ==================== 索引构建 ====================
    
    /**
     * 为几何集合构建R-Tree索引
     */
    public STRtree buildIndex(List<Geometry> geometries) {
        if (geometries == null || geometries.isEmpty()) {
            throw new SpatialIndexException("几何集合为空");
        }
        
        try {
            STRtree tree = new STRtree();
            
            for (int i = 0; i < geometries.size(); i++) {
                Geometry geom = geometries.get(i);
                if (geom != null) {
                    tree.insert(geom.getEnvelopeInternal(), i);
                }
            }
            
            tree.build();
            log.info("构建R-Tree索引，包含 {} 个几何", geometries.size());
            
            return tree;
        } catch (Exception e) {
            throw new SpatialIndexException("构建空间索引失败", e);
        }
    }
    
    /**
     * 为几何包构建R-Tree索引（带数据对象）
     */
    public STRtree buildIndexWithData(List<? extends SpatialIndexEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new SpatialIndexException("索引条目为空");
        }
        
        try {
            STRtree tree = new STRtree();
            
            for (SpatialIndexEntry entry : entries) {
                Geometry geom = entry.getGeometry();
                if (geom != null) {
                    tree.insert(geom.getEnvelopeInternal(), entry);
                }
            }
            
            tree.build();
            log.info("构建R-Tree索引（带数据），包含 {} 个条目", entries.size());
            
            return tree;
        } catch (Exception e) {
            throw new SpatialIndexException("构建空间索引失败", e);
        }
    }
    
    // ==================== 空间查询 ====================
    
    /**
     * 查询与给定几何相交的所有索引项
     */
    public List<SpatialIndexEntry> query(STRtree tree, Geometry queryGeom) {
        if (tree == null || queryGeom == null) {
            throw new SpatialIndexException("索引或查询几何为空");
        }
        
        try {
            List<SpatialIndexEntry> results = new ArrayList<>();
            
            tree.query(queryGeom.getEnvelopeInternal(), item -> {
                if (item instanceof SpatialIndexEntry entry) {
                    Geometry geom = entry.getGeometry();
                    if (geom != null && geom.intersects(queryGeom)) {
                        results.add(entry);
                    }
                }
            });
            
            return results;
        } catch (Exception e) {
            throw new SpatialIndexException("空间查询失败", e);
        }
    }
    
    /**
     * 查询与给定包络范围相交的所有索引项
     */
    public List<SpatialIndexEntry> queryByEnvelope(STRtree tree, Envelope envelope) {
        if (tree == null || envelope == null) {
            throw new SpatialIndexException("索引或查询范围为空");
        }
        
        try {
            List<SpatialIndexEntry> results = new ArrayList<>();
            
            tree.query(envelope, item -> {
                if (item instanceof SpatialIndexEntry entry) {
                    results.add(entry);
                }
            });
            
            return results;
        } catch (Exception e) {
            throw new SpatialIndexException("空间查询失败", e);
        }
    }
    
    /**
     * 查询最近的N个邻域
     */
    public List<SpatialIndexEntry> nearestNeighbors(STRtree tree, Geometry queryGeom, int k) {
        if (tree == null || queryGeom == null) {
            throw new SpatialIndexException("索引或查询几何为空");
        }
        
        if (k <= 0) {
            throw new SpatialIndexException("k必须大于0");
        }
        
        try {
            Envelope searchEnvelope = queryGeom.getEnvelopeInternal();
            double bufferDistance = Math.max(searchEnvelope.getWidth(), searchEnvelope.getHeight());
            searchEnvelope.expandBy(bufferDistance);
            
            List<SpatialIndexEntry> candidates = queryByEnvelope(tree, searchEnvelope);
            
            // 按距离排序
            candidates.sort((a, b) -> {
                double distA = a.getGeometry().distance(queryGeom);
                double distB = b.getGeometry().distance(queryGeom);
                return Double.compare(distA, distB);
            });
            
            return candidates.subList(0, Math.min(k, candidates.size()));
        } catch (Exception e) {
            throw new SpatialIndexException("最近邻查询失败", e);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 创建索引条目
     */
    public SpatialIndexEntry createEntry(Geometry geometry, Object data) {
        return new SpatialIndexEntry(geometry, data);
    }
    
    /**
     * 索引条目类
     */
    public static class SpatialIndexEntry {
        private final Geometry geometry;
        private final Object data;
        
        public SpatialIndexEntry(Geometry geometry, Object data) {
            this.geometry = geometry;
            this.data = data;
        }
        
        public Geometry getGeometry() {
            return geometry;
        }
        
        public Object getData() {
            return data;
        }
    }
}
