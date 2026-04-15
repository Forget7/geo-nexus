package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 三维模型管理服务 - 3DTiles模型库
 */
@Slf4j
@Service
public class Model3DService {
    
    private final CacheService cacheService;
    
    // 模型库
    private final Map<String, Model3D> models = new ConcurrentHashMap<>();
    
    // 模型实例
    private final Map<String, ModelInstance> instances = new ConcurrentHashMap<>();
    
    // 瓦片集
    private final Map<String, Tileset> tilesets = new ConcurrentHashMap<>();
    
    public Model3DService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeSampleModels();
    }
    
    // ==================== 初始化示例数据 ====================
    
    private void initializeSampleModels() {
        // 添加示例3D模型
        createModel(Model3D.builder()
                .id("building-template-1")
                .name("通用建筑模板")
                .description("标准写字楼建筑模型")
                .category("building")
                .type("glb")
                .url("/models/buildings/template1.glb")
                .boundingBox(new double[]{-50, -50, 100, 100, 200, 0})
                .lodLevels(3)
                .properties(Map.of(
                        "height", "建筑高度",
                        "floors", "楼层数",
                        "type", "建筑类型"
                ))
                .tags(List.of("建筑", "写字楼", "标准"))
                .build());
        
        createModel(Model3D.builder()
                .id("tree-template-1")
                .name("树木植被模板")
                .description("各类树木植被模型")
                .category("vegetation")
                .type("glb")
                .url("/models/vegetation/trees.glb")
                .boundingBox(new double[]{-10, -10, 20, 20, 30, 0})
                .lodLevels(2)
                .properties(Map.of("height", "高度", "species", "树种"))
                .tags(List.of("植被", "树木", "绿化"))
                .build());
        
        createTileset(Tileset.builder()
                .id("city-lod")
                .name("城市LOD模型")
                .description("分层次细节城市模型")
                .category("city")
                .url("https://assets.cesium.com/8568/3d-tiles-test.zip")
                .boundingBox(new double[]{-1000, -1000, 1000, 1000, 500, 0})
                .availableLod(List.of("lod0", "lod1", "lod2", "lod3"))
                .build());
    }
    
    // ==================== 3D模型管理 ====================
    
    /**
     * 创建3D模型
     */
    public Model3D createModel(Model3D model) {
        model.setId(model.getId() != null ? model.getId() : UUID.randomUUID().toString());
        model.setCreatedAt(System.currentTimeMillis());
        model.setUpdatedAt(model.getCreatedAt());
        
        models.put(model.getId(), model);
        
        log.info("创建3D模型: id={}, name={}", model.getId(), model.getName());
        
        return model;
    }
    
    /**
     * 获取模型
     */
    public Model3D getModel(String modelId) {
        return models.get(modelId);
    }
    
    /**
     * 更新模型
     */
    public Model3D updateModel(String modelId, Model3D updates) {
        Model3D existing = models.get(modelId);
        if (existing == null) {
            throw new ModelNotFoundException("模型不存在: " + modelId);
        }
        
        updates.setId(modelId);
        updates.setCreatedAt(existing.getCreatedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        
        models.put(modelId, updates);
        
        return updates;
    }
    
    /**
     * 删除模型
     */
    public void deleteModel(String modelId) {
        models.remove(modelId);
        log.info("删除3D模型: id={}", modelId);
    }
    
    /**
     * 列出模型（支持分类过滤）
     */
    public List<Model3D> listModels(String category, int limit) {
        return models.values().stream()
                .filter(m -> category == null || category.equals(m.getCategory()))
                .limit(limit)
                .toList();
    }

    /**
     * 搜索模型
     */
    public List<Model3D> searchModels(String keyword, String category, String type) {
        return models.values().stream()
                .filter(m -> {
                    if (keyword != null && !keyword.isEmpty()) {
                        String k = keyword.toLowerCase();
                        if (!m.getName().toLowerCase().contains(k) &&
                                !m.getDescription().toLowerCase().contains(k) &&
                                !m.getTags().stream().anyMatch(t -> t.toLowerCase().contains(k))) {
                            return false;
                        }
                    }
                    if (category != null && !category.equals(m.getCategory())) {
                        return false;
                    }
                    if (type != null && !type.equals(m.getType())) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }
    
    // ==================== 模型实例 ====================
    
    /**
     * 创建模型实例
     */
    public ModelInstance createInstance(String modelId, InstanceConfig config) {
        Model3D model = getModel(modelId);
        if (model == null) {
            throw new ModelNotFoundException("模型不存在: " + modelId);
        }
        
        String instanceId = UUID.randomUUID().toString();
        
        ModelInstance instance = ModelInstance.builder()
                .id(instanceId)
                .modelId(modelId)
                .modelName(model.getName())
                .position(config.getPosition())
                .rotation(config.getRotation())
                .scale(config.getScale())
                .properties(config.getProperties())
                .lodLevel(config.getLodLevel())
                .visible(true)
                .createdAt(System.currentTimeMillis())
                .build();
        
        instances.put(instanceId, instance);
        
        log.info("创建模型实例: instanceId={}, modelId={}", instanceId, modelId);
        
        return instance;
    }
    
    /**
     * 获取实例
     */
    public ModelInstance getInstance(String instanceId) {
        return instances.get(instanceId);
    }
    
    /**
     * 更新实例
     */
    public ModelInstance updateInstance(String instanceId, InstanceConfig updates) {
        ModelInstance existing = instances.get(instanceId);
        if (existing == null) {
            throw new ModelNotFoundException("实例不存在: " + instanceId);
        }
        
        if (updates.getPosition() != null) existing.setPosition(updates.getPosition());
        if (updates.getRotation() != null) existing.setRotation(updates.getRotation());
        if (updates.getScale() != null) existing.setScale(updates.getScale());
        if (updates.getProperties() != null) existing.setProperties(updates.getProperties());
        if (updates.getLodLevel() != null) existing.setLodLevel(updates.getLodLevel());
        if (updates.getVisible() != null) existing.setVisible(updates.getVisible());
        
        existing.setUpdatedAt(System.currentTimeMillis());
        
        return existing;
    }
    
    /**
     * 删除实例
     */
    public void deleteInstance(String instanceId) {
        instances.remove(instanceId);
        log.info("删除模型实例: id={}", instanceId);
    }
    
    /**
     * 批量创建实例
     */
    public List<ModelInstance> batchCreateInstances(String modelId, List<InstanceConfig> configs) {
        List<ModelInstance> results = new ArrayList<>();
        for (InstanceConfig config : configs) {
            results.add(createInstance(modelId, config));
        }
        return results;
    }
    
    /**
     * 按位置查询实例
     */
    public List<ModelInstance> queryInstances(double[] bounds) {
        return instances.values().stream()
                .filter(i -> {
                    double[] pos = i.getPosition();
                    return pos[0] >= bounds[0] && pos[0] <= bounds[2] &&
                            pos[1] >= bounds[1] && pos[1] <= bounds[3];
                })
                .toList();
    }
    
    // ==================== 3DTileset管理 ====================
    
    /**
     * 创建Tileset
     */
    public Tileset createTileset(Tileset tileset) {
        tileset.setId(tileset.getId() != null ? tileset.getId() : UUID.randomUUID().toString());
        tileset.setCreatedAt(System.currentTimeMillis());
        tileset.setUpdatedAt(tileset.getCreatedAt());
        
        tilesets.put(tileset.getId(), tileset);
        
        log.info("创建Tileset: id={}, name={}", tileset.getId(), tileset.getName());
        
        return tileset;
    }
    
    /**
     * 获取Tileset
     */
    public Tileset getTileset(String tilesetId) {
        return tilesets.get(tilesetId);
    }
    
    /**
     * 获取Tileset元数据
     */
    public TilesetMetadata getTilesetMetadata(String tilesetId) {
        Tileset tileset = getTileset(tilesetId);
        if (tileset == null) {
            throw new TilesetNotFoundException("Tileset不存在: " + tilesetId);
        }
        
        return TilesetMetadata.builder()
                .id(tileset.getId())
                .name(tileset.getName())
                .description(tileset.getDescription())
                .boundingBox(tileset.getBoundingBox())
                .availableLod(tileset.getAvailableLod())
                .totalInstances(0) // 简化
                .bounds(tileset.getBoundingBox())
                .build();
    }
    
    // ==================== 导出/导入 ====================
    
    /**
     * 导出实例配置
     */
    public String exportInstances(List<String> instanceIds, String format) {
        List<ModelInstance> toExport = instanceIds.stream()
                .map(instances::get)
                .filter(i -> i != null)
                .toList();
        
        if ("json".equals(format)) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"instances\": [\n");
            for (int i = 0; i < toExport.size(); i++) {
                ModelInstance inst = toExport.get(i);
                json.append("    {\n");
                json.append("      \"id\": \"").append(inst.getId()).append("\",\n");
                json.append("      \"modelId\": \"").append(inst.getModelId()).append("\",\n");
                json.append("      \"position\": [").append(inst.getPosition()[0])
                        .append(", ").append(inst.getPosition()[1])
                        .append(", ").append(inst.getPosition()[2]).append("],\n");
                json.append("      \"rotation\": [").append(inst.getRotation()[0])
                        .append(", ").append(inst.getRotation()[1])
                        .append(", ").append(inst.getRotation()[2]).append("],\n");
                json.append("      \"scale\": [").append(inst.getScale()[0])
                        .append(", ").append(inst.getScale()[1])
                        .append(", ").append(inst.getScale()[2]).append("]\n");
                json.append("    }");
                if (i < toExport.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");
            json.append("}");
            return json.toString();
        }
        
        return "";
    }
    
    /**
     * 导入实例
     */
    public List<ModelInstance> importInstances(String content, String format) {
        List<ModelInstance> results = new ArrayList<>();
        
        // 简化实现：解析JSON
        // 实际应使用Jackson解析
        log.info("导入实例: format={}", format);
        
        return results;
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class Model3D {
        private String id;
        private String name;
        private String description;
        private String category; // building, vegetation, vehicle, furniture, terrain
        private String type; // glb, gltf, b3dm, i3dm
        private String url;
        private double[] boundingBox;
        private int lodLevels;
        private Map<String, String> properties;
        private List<String> tags;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ModelInstance {
        private String id;
        private String modelId;
        private String modelName;
        private double[] position; // [lon, lat, height]
        private double[] rotation; // [heading, pitch, roll]
        private double[] scale; // [x, y, z]
        private Map<String, Object> properties;
        private String lodLevel;
        private boolean visible;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class InstanceConfig {
        private double[] position;
        private double[] rotation;
        private double[] scale;
        private Map<String, Object> properties;
        private String lodLevel;
        private Boolean visible;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Tileset {
        private String id;
        private String name;
        private String description;
        private String category;
        private String url;
        private double[] boundingBox;
        private List<String> availableLod;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TilesetMetadata {
        private String id;
        private String name;
        private String description;
        private double[] boundingBox;
        private List<String> availableLod;
        private int totalInstances;
        private double[] bounds;
    }
    
    public static class ModelNotFoundException extends RuntimeException {
        public ModelNotFoundException(String msg) { super(msg); }
    }
    
    public static class TilesetNotFoundException extends RuntimeException {
        public TilesetNotFoundException(String msg) { super(msg); }
    }
}
