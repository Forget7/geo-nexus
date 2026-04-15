package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 标注服务 - 地图标注自动Placement
 */
@Slf4j
@Service
public class LabelPlacementService {
    
    private final CacheService cacheService;
    
    // 标注规则
    private final Map<String, LabelRule> rules = new ConcurrentHashMap<>();
    
    // 标注缓存
    private final Map<String, PlacedLabel> labelCache = new ConcurrentHashMap<>();
    
    public LabelPlacementService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeDefaultRules();
    }
    
    private void initializeDefaultRules() {
        // 道路标注
        createRule(LabelRule.builder()
                .id("road-label")
                .name("道路标注")
                .layerType("road")
                .priority(5)
                .fontSize(12)
                .color("#333333")
                .placement("line") // line, point, area
                .build());
        
        // 兴趣点标注
        createRule(LabelRule.builder()
                .id("poi-label")
                .name("POI标注")
                .layerType("poi")
                .priority(8)
                .fontSize(11)
                .color("#0066cc")
                .placement("point")
                .build());
        
        // 水系标注
        createRule(LabelRule.builder()
                .id("water-label")
                .name("水系标注")
                .layerType("water")
                .priority(4)
                .fontSize(13)
                .color("#0066ff")
                .placement("line")
                .build());
        
        // 建筑标注
        createRule(LabelRule.builder()
                .id("building-label")
                .name("建筑标注")
                .layerType("building")
                .priority(3)
                .fontSize(10)
                .color("#666666")
                .placement("area")
                .build());
        
        // 行政区划标注
        createRule(LabelRule.builder()
                .id("boundary-label")
                .name("行政区划标注")
                .layerType("boundary")
                .priority(9)
                .fontSize(14)
                .color("#333333")
                .placement("area")
                .build());
    }
    
    // ==================== 标注规则 ====================
    
    public LabelRule createRule(LabelRule rule) {
        rule.setId(rule.getId() != null ? rule.getId() : UUID.randomUUID().toString());
        rule.setCreatedAt(System.currentTimeMillis());
        rules.put(rule.getId(), rule);
        return rule;
    }
    
    public LabelRule getRule(String ruleId) {
        return rules.get(ruleId);
    }
    
    public void deleteRule(String ruleId) {
        rules.remove(ruleId);
    }
    
    public List<LabelRule> getAllRules() {
        return new ArrayList<>(rules.values());
    }
    
    public LabelRule getRuleByLayerType(String layerType) {
        return rules.values().stream()
                .filter(r -> r.getLayerType().equals(layerType))
                .findFirst()
                .orElse(null);
    }
    
    // ==================== 标注Placement ====================
    
    /**
     * 计算标注位置
     */
    public List<PlacedLabel> calculateLabels(LabelRequest request) {
        log.info("计算标注: features={}, layerType={}", 
                request.getFeatures().size(), request.getLayerType());
        
        LabelRule rule = getRuleByLayerType(request.getLayerType());
        if (rule == null) {
            rule = createDefaultRule(request.getLayerType());
        }
        
        List<PlacedLabel> labels = new ArrayList<>();
        
        switch (rule.getPlacement()) {
            case "point":
                labels = placePointLabels(request, rule);
                break;
            case "line":
                labels = placeLineLabels(request, rule);
                break;
            case "area":
                labels = placeAreaLabels(request, rule);
                break;
        }
        
        // 冲突检测与解决
        labels = resolveConflicts(labels, request.getMapBounds());
        
        log.info("标注计算完成: count={}", labels.size());
        
        return labels;
    }
    
    private List<PlacedLabel> placePointLabels(LabelRequest request, LabelRule rule) {
        List<PlacedLabel> labels = new ArrayList<>();
        
        for (Feature feature : request.getFeatures()) {
            double[] centroid = calculateCentroid(feature.getGeometry());
            
            PlacedLabel label = PlacedLabel.builder()
                    .id(UUID.randomUUID().toString())
                    .featureId(feature.getId())
                    .text(feature.getLabel())
                    .x(centroid[0])
                    .y(centroid[1])
                    .rotation(0)
                    .fontSize(rule.getFontSize())
                    .color(rule.getColor())
                    .priority(rule.getPriority())
                    .anchor(rule.getAnchor())
                    .visible(true)
                    .build();
            
            labels.add(label);
        }
        
        return labels;
    }
    
    private List<PlacedLabel> placeLineLabels(LabelRequest request, LabelRule rule) {
        List<PlacedLabel> labels = new ArrayList<>();
        
        for (Feature feature : request.getFeatures()) {
            double[] line = feature.getGeometry();
            double[] midpoint = calculateLineMidpoint(line);
            double angle = calculateLineAngle(line);
            
            // 调整角度使其可读
            if (angle > 90 || angle < -90) {
                angle += 180;
            }
            
            PlacedLabel label = PlacedLabel.builder()
                    .id(UUID.randomUUID().toString())
                    .featureId(feature.getId())
                    .text(feature.getLabel())
                    .x(midpoint[0])
                    .y(midpoint[1])
                    .rotation(angle)
                    .fontSize(rule.getFontSize())
                    .color(rule.getColor())
                    .priority(rule.getPriority())
                    .anchor(rule.getAnchor())
                    .visible(true)
                    .build();
            
            labels.add(label);
        }
    
        return labels;
    }
    
    private List<PlacedLabel> placeAreaLabels(LabelRequest request, LabelRule rule) {
        List<PlacedLabel> labels = new ArrayList<>();
        
        for (Feature feature : request.getFeatures()) {
            double[] centroid = calculateCentroid(feature.getGeometry());
            
            // 检查标注是否在多边形内
            if (isPointInPolygon(centroid, feature.getGeometry())) {
                PlacedLabel label = PlacedLabel.builder()
                        .id(UUID.randomUUID().toString())
                        .featureId(feature.getId())
                        .text(feature.getLabel())
                        .x(centroid[0])
                        .y(centroid[1])
                        .rotation(0)
                        .fontSize(rule.getFontSize())
                        .color(rule.getColor())
                        .priority(rule.getPriority())
                        .anchor("center")
                        .visible(true)
                        .build();
                
                labels.add(label);
            }
        }
        
        return labels;
    }
    
    // ==================== 冲突解决 ====================
    
    private List<PlacedLabel> resolveConflicts(List<PlacedLabel> labels, double[] bounds) {
        if (labels.size() <= 1) return labels;
        
        // 按优先级排序
        List<PlacedLabel> sorted = labels.stream()
                .sorted(Comparator.comparingInt(PlacedLabel::getPriority).reversed())
                .collect(Collectors.toList());
        
        List<PlacedLabel> resolved = new ArrayList<>();
        Set<String> occupiedPositions = new HashSet<>();
        
        for (PlacedLabel label : sorted) {
            String posKey = getPositionKey(label);
            
            if (!occupiedPositions.contains(posKey)) {
                resolved.add(label);
                markOccupied(label, occupiedPositions, bounds);
            } else {
                // 尝试备选位置
                List<double[]> alternatives = getAlternativePositions(label);
                boolean placed = false;
                
                for (double[] alt : alternatives) {
                    label.setX(alt[0]);
                    label.setY(alt[1]);
                    String altKey = getPositionKey(label);
                    
                    if (!occupiedPositions.contains(altKey)) {
                        resolved.add(label);
                        markOccupied(label, occupiedPositions, bounds);
                        placed = true;
                        break;
                    }
                }
                
                if (!placed) {
                    // 降低优先级或隐藏
                    label.setPriority(label.getPriority() - 1);
                    if (label.getPriority() <= 0) {
                        label.setVisible(false);
                    }
                    resolved.add(label);
                }
            }
        }
        
        return resolved;
    }
    
    private String getPositionKey(PlacedLabel label) {
        int gridX = (int) (label.getX() / 100);
        int gridY = (int) (label.getY() / 100);
        return gridX + ":" + gridY;
    }
    
    private void markOccupied(PlacedLabel label, Set<String> occupied, double[] bounds) {
        // 标记标签占用的区域
        double width = label.getText().length() * label.getFontSize() * 0.6;
        double height = label.getFontSize() * 1.5;
        
        for (double x = label.getX(); x < label.getX() + width; x += 50) {
            for (double y = label.getY(); y < label.getY() + height; y += 50) {
                int gridX = (int) (x / 50);
                int gridY = (int) (y / 50);
                occupied.add(gridX + ":" + gridY);
            }
        }
    }
    
    private List<double[]> getAlternativePositions(PlacedLabel label) {
        List<double[]> alternatives = new ArrayList<>();
        double offset = 20;
        
        // 八个方向
        alternatives.add(new double[]{label.getX() + offset, label.getY()});
        alternatives.add(new double[]{label.getX() - offset, label.getY()});
        alternatives.add(new double[]{label.getX(), label.getY() + offset});
        alternatives.add(new double[]{label.getX(), label.getY() - offset});
        alternatives.add(new double[]{label.getX() + offset, label.getY() + offset});
        alternatives.add(new double[]{label.getX() - offset, label.getY() + offset});
        alternatives.add(new double[]{label.getX() + offset, label.getY() - offset});
        alternatives.add(new double[]{label.getX() - offset, label.getY() - offset});
        
        return alternatives;
    }
    
    // ==================== 辅助方法 ====================
    
    private double[] calculateCentroid(double[] geometry) {
        // 简化：取外包框中心
        if (geometry.length == 4) {
            return new double[]{(geometry[0] + geometry[2]) / 2, 
                    (geometry[1] + geometry[3]) / 2};
        }
        return new double[]{0, 0};
    }
    
    private double[] calculateLineMidpoint(double[] line) {
        if (line.length >= 4) {
            int mid = line.length / 2;
            return new double[]{line[mid - 2], line[mid - 1]};
        }
        return new double[]{0, 0};
    }
    
    private double calculateLineAngle(double[] line) {
        if (line.length >= 4) {
            double dx = line[2] - line[0];
            double dy = line[3] - line[1];
            return Math.toDegrees(Math.atan2(dy, dx));
        }
        return 0;
    }
    
    private boolean isPointInPolygon(double[] point, double[] polygon) {
        // 简化实现
        return true;
    }
    
    private LabelRule createDefaultRule(String layerType) {
        LabelRule rule = LabelRule.builder()
                .id("default-" + layerType)
                .name(layerType + "标注")
                .layerType(layerType)
                .priority(5)
                .fontSize(12)
                .color("#000000")
                .placement("point")
                .build();
        
        return createRule(rule);
    }
    
    // ==================== 批量标注 ====================
    
    /**
     * 批量计算标注
     */
    public Map<String, List<PlacedLabel>> batchCalculateLabels(List<LabelRequest> requests) {
        Map<String, List<PlacedLabel>> results = new HashMap<>();
        
        for (LabelRequest request : requests) {
            results.put(request.getLayerType(), calculateLabels(request));
        }
        
        return results;
    }
    
    /**
     * 更新单个标注
     */
    public PlacedLabel updateLabel(String labelId, PlacedLabel updates) {
        PlacedLabel existing = labelCache.get(labelId);
        if (existing != null) {
            if (updates.getX() != 0) existing.setX(updates.getX());
            if (updates.getY() != 0) existing.setY(updates.getY());
            if (updates.getRotation() != 0) existing.setRotation(updates.getRotation());
            if (updates.getText() != null) existing.setText(updates.getText());
            existing.setVisible(updates.isVisible());
        }
        return existing;
    }
    
    /**
     * 删除标注
     */
    public void deleteLabel(String labelId) {
        labelCache.remove(labelId);
    }
    
    /**
     * 导出标注
     */
    public String exportLabels(List<PlacedLabel> labels, String format) {
        if ("geojson".equals(format)) {
            StringBuilder geojson = new StringBuilder();
            geojson.append("{\"type\":\"FeatureCollection\",\"features\":[");
            
            for (int i = 0; i < labels.size(); i++) {
                PlacedLabel label = labels.get(i);
                if (i > 0) geojson.append(",");
                
                geojson.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
                        .append(label.getX()).append(",").append(label.getY())
                        .append("]},\"properties\":{")
                        .append("\"text\":\"").append(label.getText()).append("\",")
                        .append("\"rotation\":").append(label.getRotation())
                        .append("}}");
            }
            
            geojson.append("]}");
            return geojson.toString();
        }
        
        return "[]";
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class LabelRule {
        private String id;
        private String name;
        private String layerType;
        private int priority;
        private double fontSize;
        private String color;
        private String placement; // point, line, area
        private String anchor;
        private boolean visible;
        private Long createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PlacedLabel {
        private String id;
        private String featureId;
        private String text;
        private double x;
        private double y;
        private double rotation;
        private double fontSize;
        private String color;
        private int priority;
        private String anchor;
        private boolean visible;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class LabelRequest {
        private String layerType;
        private List<Feature> features;
        private double[] mapBounds;
        private double scale;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Feature {
        private String id;
        private String label;
        private double[] geometry;
        private Map<String, Object> properties;
    }
}
