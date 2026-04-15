package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能地图服务 - 自然语言控制 + 配置驱动
 */
@Slf4j
@Service
public class IntelligentMapService {
    
    private final CacheService cacheService;
    private final GISService gisService;
    private final LLMService llmService;
    
    // 地图配置
    private final Map<String, MapConfig> mapConfigs = new ConcurrentHashMap<>();
    
    // 地图状态
    private final Map<String, MapState> mapStates = new ConcurrentHashMap<>();
    
    public IntelligentMapService(CacheService cacheService, GISService gisService, LLMService llmService) {
        this.cacheService = cacheService;
        this.gisService = gisService;
        this.llmService = llmService;
    }
    
    // ==================== 地图配置 ====================
    
    /**
     * 创建地图配置
     */
    public MapConfig createConfig(String name, MapConfig config) {
        config.setId(UUID.randomUUID().toString());
        config.setName(name);
        config.setCreatedAt(System.currentTimeMillis());
        config.setUpdatedAt(config.getCreatedAt());
        
        mapConfigs.put(config.getId(), config);
        
        log.info("创建地图配置: id={}, name={}", config.getId(), name);
        
        return config;
    }
    
    /**
     * 获取地图配置
     */
    public MapConfig getConfig(String configId) {
        MapConfig config = mapConfigs.get(configId);
        if (config == null) {
            throw new ConfigNotFoundException("配置不存在: " + configId);
        }
        return config;
    }
    
    /**
     * 应用配置到地图
     */
    public MapState applyConfig(String sessionId, String configId) {
        MapConfig config = getConfig(configId);
        MapState state = mapStates.get(sessionId);
        
        if (state == null) {
            state = new MapState();
            state.setSessionId(sessionId);
        }
        
        // 应用配置
        state.setMode(config.getDefaultMode());
        state.setCenter(config.getCenter());
        state.setZoom(config.getZoom());
        state.setPitch(config.getPitch());
        state.setBearing(config.getBearing());
        state.setBaseLayer(config.getBaseLayer());
        state.setLayers(new ArrayList<>(config.getLayers()));
        state.setEffects(new ArrayList<>(config.getEffects()));
        
        mapStates.put(sessionId, state);
        
        log.info("应用地图配置: sessionId={}, configId={}", sessionId, configId);
        
        return state;
    }
    
    /**
     * 更新配置
     */
    public MapConfig updateConfig(String configId, MapConfig updates) {
        MapConfig existing = getConfig(configId);
        updates.setId(configId);
        updates.setCreatedAt(existing.getCreatedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        mapConfigs.put(configId, updates);
        return updates;
    }
    
    // ==================== 地图状态 ====================
    
    /**
     * 获取地图状态
     */
    public MapState getMapState(String sessionId) {
        return mapStates.computeIfAbsent(sessionId, k -> {
            MapState state = new MapState();
            state.setSessionId(sessionId);
            state.setMode("2d");
            state.setCenter(new double[]{116.4, 39.9}); // 默认北京
            state.setZoom(10);
            state.setPitch(0);
            state.setBearing(0);
            return state;
        });
    }
    
    /**
     * 更新地图状态
     */
    public MapState updateState(String sessionId, MapState newState) {
        newState.setSessionId(sessionId);
        newState.setUpdatedAt(System.currentTimeMillis());
        mapStates.put(sessionId, newState);
        return newState;
    }
    
    // ==================== 自然语言解析 ====================
    
    /**
     * 解析自然语言命令
     */
    public List<MapAction> parseNaturalCommand(String command) {
        List<MapAction> actions = new ArrayList<>();
        
        command = command.toLowerCase().trim();
        
        // 模式匹配
        Pattern flyToPattern = Pattern.compile("(飞到?|去|定位到?|切换到?|显示)(.*?)(市|区|县|省|地点|位置)?$");
        Matcher flyToMatcher = flyToPattern.matcher(command);
        if (flyToMatcher.find()) {
            String location = flyToMatcher.group(2);
            actions.add(MapAction.builder()
                    .type("flyto")
                    .params(Map.of("location", location))
                    .description("飞往 " + location)
                    .build());
        }
        
        // 缩放
        if (command.contains("放大") || command.contains("zoom in")) {
            actions.add(MapAction.builder()
                    .type("zoom")
                    .params(Map.of("delta", 2))
                    .description("放大")
                    .build());
        }
        if (command.contains("缩小") || command.contains("zoom out")) {
            actions.add(MapAction.builder()
                    .type("zoom")
                    .params(Map.of("delta", -2))
                    .description("缩小")
                    .build());
        }
        
        // 2D/3D切换
        if (command.contains("3d") || command.contains("三维")) {
            actions.add(MapAction.builder()
                    .type("mode")
                    .params(Map.of("mode", "3d"))
                    .description("切换到3D模式")
                    .build());
        }
        if (command.contains("2d") || command.contains("二维")) {
            actions.add(MapAction.builder()
                    .type("mode")
                    .params(Map.of("mode", "2d"))
                    .description("切换到2D模式")
                    .build());
        }
        
        // 底图切换
        String[] baseLayers = {"卫星", "街景", "暗色", "地形", "OSM"};
        for (String layer : baseLayers) {
            if (command.contains(layer) || command.contains("底图")) {
                actions.add(MapAction.builder()
                        .type("baselayer")
                        .params(Map.of("layer", layer))
                        .description("切换底图为 " + layer)
                        .build());
                break;
            }
        }
        
        // 特效
        if (command.contains("下雨") || command.contains("雨")) {
            actions.add(MapAction.builder()
                    .type("effect")
                    .params(Map.of("effect", "rain", "enabled", true))
                    .description("开启下雨特效")
                    .build());
        }
        if (command.contains("下雪") || command.contains("雪")) {
            actions.add(MapAction.builder()
                    .type("effect")
                    .params(Map.of("effect", "snow", "enabled", true))
                    .description("开启下雪特效")
                    .build());
        }
        if (command.contains("黑夜") || command.contains("夜间")) {
            actions.add(MapAction.builder()
                    .type("effect")
                    .params(Map.of("effect", "night", "enabled", true))
                    .description("开启夜间模式")
                    .build());
        }
        
        // 清除
        if (command.contains("清除") || command.contains("清空")) {
            actions.add(MapAction.builder()
                    .type("clear")
                    .params(Map.of())
                    .description("清除所有覆盖物")
                    .build());
        }
        
        // 添加标记
        Pattern markerPattern = Pattern.compile("(标记|标注|显示)(.*?)在(.*)");
        Matcher markerMatcher = markerPattern.matcher(command);
        if (markerMatcher.find()) {
            String label = markerMatcher.group(2);
            String location = markerMatcher.group(3);
            actions.add(MapAction.builder()
                    .type("marker")
                    .params(Map.of("label", label, "location", location))
                    .description("在 " + location + " 标记 " + label)
                    .build());
        }
        
        // 绘制
        if (command.contains("画线") || command.contains("绘制线")) {
            actions.add(MapAction.builder()
                    .type("draw")
                    .params(Map.of("geometry", "linestring"))
                    .description("开始绘制线")
                    .build());
        }
        if (command.contains("画面") || command.contains("绘制面")) {
            actions.add(MapAction.builder()
                    .type("draw")
                    .params(Map.of("geometry", "polygon"))
                    .description("开始绘制面")
                    .build());
        }
        
        // 测量
        if (command.contains("测距") || command.contains("测量距离")) {
            actions.add(MapAction.builder()
                    .type("measure")
                    .params(Map.of("type", "distance"))
                    .description("开始距离测量")
                    .build());
        }
        if (command.contains("测面") || command.contains("测量面积")) {
            actions.add(MapAction.builder()
                    .type("measure")
                    .params(Map.of("type", "area"))
                    .description("开始面积测量")
                    .build());
        }
        
        // 回家
        if (command.contains("回家") || command.contains("回到初始")) {
            actions.add(MapAction.builder()
                    .type("home")
                    .params(Map.of())
                    .description("回到初始视图")
                    .build());
        }
        
        // 全屏
        if (command.contains("全屏")) {
            actions.add(MapAction.builder()
                    .type("fullscreen")
                    .params(Map.of())
                    .description("全屏显示")
                    .build());
        }
        
        // 截图
        if (command.contains("截图") || command.contains("保存图片")) {
            actions.add(MapAction.builder()
                    .type("screenshot")
                    .params(Map.of())
                    .description("截取当前地图")
                    .build());
        }
        
        return actions;
    }
    
    /**
     * 执行地图动作
     */
    public ActionResult executeAction(String sessionId, MapAction action) {
        MapState state = getMapState(sessionId);
        
        ActionResult result = ActionResult.builder()
                .action(action)
                .success(true)
                .timestamp(System.currentTimeMillis())
                .build();
        
        try {
            switch (action.getType()) {
                case "flyto" -> {
                    String location = (String) action.getParams().get("location");
                    double[] coords = geocodeLocation(location);
                    state.setCenter(coords);
                    state.setZoom(14);
                    result.setResult(Map.of("center", coords, "zoom", 14));
                }
                case "zoom" -> {
                    int delta = (int) action.getParams().get("delta");
                    state.setZoom(Math.max(1, Math.min(20, state.getZoom() + delta)));
                    result.setResult(Map.of("zoom", state.getZoom()));
                }
                case "mode" -> {
                    String mode = (String) action.getParams().get("mode");
                    state.setMode(mode);
                    result.setResult(Map.of("mode", mode));
                }
                case "baselayer" -> {
                    String layer = (String) action.getParams().get("layer");
                    state.setBaseLayer(layer);
                    result.setResult(Map.of("baseLayer", layer));
                }
                case "effect" -> {
                    String effect = (String) action.getParams().get("effect");
                    boolean enabled = (boolean) action.getParams().getOrDefault("enabled", true);
                    
                    // 更新特效列表
                    if (enabled) {
                        if (!state.getEffects().contains(effect)) {
                            state.getEffects().add(effect);
                        }
                    } else {
                        state.getEffects().remove(effect);
                    }
                    result.setResult(Map.of("effect", effect, "enabled", enabled));
                }
                case "clear" -> {
                    state.setMarkers(new ArrayList<>());
                    state.setDrawings(new ArrayList<>());
                    state.setAnalysisResults(new ArrayList<>());
                    result.setResult(Map.of("cleared", true));
                }
                case "marker" -> {
                    String label = (String) action.getParams().get("label");
                    String location = (String) action.getParams().get("location");
                    double[] coords = geocodeLocation(location);
                    
                    Marker marker = new Marker();
                    marker.setId(UUID.randomUUID().toString());
                    marker.setPosition(coords);
                    marker.setLabel(label);
                    marker.setType("pin");
                    state.getMarkers().add(marker);
                    
                    result.setResult(Map.of("marker", marker));
                }
                case "draw" -> {
                    String geometry = (String) action.getParams().get("geometry");
                    state.setDrawingMode(geometry);
                    result.setResult(Map.of("drawingMode", geometry));
                }
                case "measure" -> {
                    String type = (String) action.getParams().get("type");
                    state.setMeasureMode(type);
                    result.setResult(Map.of("measureMode", type));
                }
                case "home" -> {
                    state.setCenter(new double[]{116.4, 39.9});
                    state.setZoom(10);
                    state.setPitch(0);
                    state.setBearing(0);
                    result.setResult(Map.of("center", state.getCenter(), "zoom", 10));
                }
                case "fullscreen" -> {
                    result.setResult(Map.of("fullscreen", true));
                }
                case "screenshot" -> {
                    result.setResult(Map.of("screenshot", true));
                }
                default -> {
                    result.setSuccess(false);
                    result.setError("未知动作: " + action.getType());
                }
            }
            
            state.setUpdatedAt(System.currentTimeMillis());
            mapStates.put(sessionId, state);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
            log.error("执行地图动作失败: {}", action.getType(), e);
        }
        
        return result;
    }
    
    /**
     * 批量执行动作
     */
    public List<ActionResult> executeActions(String sessionId, List<MapAction> actions) {
        List<ActionResult> results = new ArrayList<>();
        for (MapAction action : actions) {
            results.add(executeAction(sessionId, action));
        }
        return results;
    }
    
    /**
     * 自然语言对话执行
     */
    public ConversationResult chat(String sessionId, String message) {
        log.info("智能地图对话: sessionId={}, message={}", sessionId, message);
        
        // 解析命令
        List<MapAction> actions = parseNaturalCommand(message);
        
        if (actions.isEmpty()) {
            return ConversationResult.builder()
                    .success(false)
                    .message("抱歉，我不明白这个命令。支持的命令包括：\n" +
                            "- 飞到北京/上海\n" +
                            "- 放大/缩小\n" +
                            "- 切换到2D/3D模式\n" +
                            "- 切换到底图\n" +
                            "- 添加下雨/下雪特效\n" +
                            "- 标记XXX在XXX\n" +
                            "- 画线/画面\n" +
                            "- 测量距离/面积\n" +
                            "- 回家\n" +
                            "- 全屏\n" +
                            "- 截图")
                    .actions(Collections.emptyList())
                    .build();
        }
        
        // 执行动作
        List<ActionResult> results = executeActions(sessionId, actions);
        
        // 构建回复
        StringBuilder response = new StringBuilder();
        for (MapAction action : actions) {
            response.append(action.getDescription()).append(" ✓\n");
        }
        
        // 获取当前状态
        MapState state = getMapState(sessionId);
        
        return ConversationResult.builder()
                .success(true)
                .message(response.toString().trim())
                .actions(actions)
                .results(results)
                .currentState(state)
                .build();
    }
    
    // ==================== 多语言地图标注 ====================

    /**
     * 按 locale 生成多语言地图标注
     *
     * @param geojson 输入的 GeoJSON 数据
     * @param locale  语言标识："zh"（中文，默认）、"en"（英文）、" bilingual"（双语）
     * @return 带标注的 GeoJSON，其中 properties 包含 localeLabel 字段
     */
    public Map<String, Object> generateLabels(Map<String, Object> geojson, String locale) {
        if (geojson == null) {
            return Map.of("error", "GeoJSON is null");
        }

        String lang = (locale == null || locale.isBlank()) ? "zh" : locale.toLowerCase();
        Map<String, Object> result = new HashMap<>(geojson);

        // 中英双语标注映射（常见GIS字段）
        Map<String, Map<String, String>> labelMapping = buildLabelDictionary();

        // 处理 FeatureCollection
        if ("FeatureCollection".equals(geojson.get("type"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> features = (List<Map<String, Object>>) result.get("features");
            if (features != null) {
                for (Map<String, Object> feature : features) {
                    applyLabelsToFeature(feature, lang, labelMapping);
                }
            }
        }
        // 处理单个 Feature
        else if ("Feature".equals(geojson.get("type"))) {
            applyLabelsToFeature(result, lang, labelMapping);
        }

        result.put("_labelLocale", lang);
        result.put("_labelGeneratedAt", System.currentTimeMillis());

        log.info("生成多语言标注: locale={}, type={}", lang, geojson.get("type"));
        return result;
    }

    /**
     * 构建中英双语标注字典
     */
    private Map<String, Map<String, String>> buildLabelDictionary() {
        Map<String, Map<String, String>> dict = new HashMap<>();

        // 常见GIS字段中英对照
        String[][] pairs = {
            {"name", "名称", "Name"},
            {"city", "城市", "City"},
            {"province", "省份", "Province"},
            {"district", "区县", "District"},
            {"street", "街道", "Street"},
            {"address", "地址", "Address"},
            {"population", "人口", "Population"},
            {"area", "面积", "Area"},
            {"elevation", "海拔", "Elevation"},
            {"river", "河流", "River"},
            {"mountain", "山脉", "Mountain"},
            {"park", "公园", "Park"},
            {"school", "学校", "School"},
            {"hospital", "医院", "Hospital"},
            {" railway", "火车站", "Railway Station"},
            {"subway", "地铁站", "Subway Station"},
            {"hotel", "酒店", "Hotel"},
            {"restaurant", "餐厅", "Restaurant"},
            {"shop", "商店", "Shop"},
            {"building", "建筑物", "Building"},
            {"landuse", "土地利用", "Land Use"},
            {"highway", "道路", "Highway"},
            {"boundary", "边界", "Boundary"},
            {"water", "水域", "Water Body"},
            {"forest", "森林", "Forest"},
            {"farmland", "农田", "Farmland"},
        };

        for (String[] pair : pairs) {
            String field = pair[0];
            dict.put(field, Map.of("zh", pair[1], "en", pair[2], "bilingual", pair[1] + " / " + pair[2]));
        }

        return dict;
    }

    /**
     * 为单个 Feature 应用多语言标注
     */
    @SuppressWarnings("unchecked")
    private void applyLabelsToFeature(Map<String, Object> feature, String locale,
                                      Map<String, Map<String, String>> dict) {
        Map<String, Object> props = (Map<String, Object>) feature.get("properties");
        if (props == null) return;

        // 为每个已知字段生成 localeLabel
        for (Map.Entry<String, Map<String, String>> entry : dict.entrySet()) {
            String fieldKey = entry.getKey();
            if (props.containsKey(fieldKey)) {
                Object rawValue = props.get(fieldKey);
                String displayValue = (rawValue == null) ? "" : rawValue.toString();

                // 对于 name 字段，直接生成标注
                if ("name".equals(fieldKey) && displayValue != null && !displayValue.isBlank()) {
                    Map<String, String> translations = entry.getValue();
                    String label = switch (locale) {
                        case "en" -> displayValue + " (" + translations.get("en") + ")";
                        case "bilingual" -> translations.get("bilingual");
                        default -> translations.get("zh") + " " + displayValue;
                    };
                    props.put("localeLabel", label);
                }
            }
        }

        // 构建通用 localeLabel（取 name 字段，无则尝试第一个非空字符串字段）
        if (!props.containsKey("localeLabel")) {
            String fallbackLabel = props.values().stream()
                    .filter(v -> v != null && !v.toString().isBlank())
                    .map(Object::toString)
                    .findFirst()
                    .orElse("未命名");

            String suffix = switch (locale) {
                case "en" -> "";
                case "bilingual" -> " / " + fallbackLabel;
                default -> "";
            };

            props.put("localeLabel", fallbackLabel + suffix);
        }

        // 同时保留 locale 字段本身（供前端按需使用）
        props.put("_labelLocale", locale);
    }

    // ==================== 辅助方法 ====================
    
    private double[] geocodeLocation(String location) {
        // 简化的地理位置匹配
        Map<String, double[]> knownLocations = new HashMap<>();
        knownLocations.put("北京", new double[]{116.4, 39.9});
        knownLocations.put("天安门", new double[]{116.3975, 39.9085});
        knownLocations.put("上海", new double[]{121.4737, 31.2304});
        knownLocations.put("广州", new double[]{113.2644, 23.1291});
        knownLocations.put("深圳", new double[]{114.0579, 22.5431});
        knownLocations.put("成都", new double[]{104.0657, 30.6598});
        knownLocations.put("杭州", new double[]{120.1551, 30.2741});
        knownLocations.put("武汉", new double[]{114.3055, 30.5928});
        knownLocations.put("西安", new double[]{108.9402, 34.3416});
        knownLocations.put("南京", new double[]{118.7969, 32.0603});
        
        for (Map.Entry<String, double[]> entry : knownLocations.entrySet()) {
            if (location.contains(entry.getKey()) || entry.getKey().contains(location)) {
                return entry.getValue();
            }
        }
        
        // 默认返回北京
        return new double[]{116.4, 39.9};
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class MapConfig {
        private String id;
        private String name;
        private String description;
        private String defaultMode; // 2d, 3d
        private double[] center;
        private double zoom;
        private double pitch;
        private double bearing;
        private String baseLayer;
        private List<LayerConfig> layers;
        private List<String> effects;
        private Map<String, Object> style;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class LayerConfig {
        private String id;
        private String name;
        private String type; // vector, raster, wms, wfs
        private String url;
        private boolean visible;
        private double opacity;
        private String style;
    }
    
    @lombok.Data
    public static class MapState {
        private String sessionId;
        private String mode;
        private double[] center;
        private double zoom;
        private double pitch;
        private double bearing;
        private String baseLayer;
        private List<LayerConfig> layers = new ArrayList<>();
        private List<String> effects = new ArrayList<>();
        private List<Marker> markers = new ArrayList<>();
        private List<Drawing> drawings = new ArrayList<>();
        private List<Object> analysisResults = new ArrayList<>();
        private String drawingMode;
        private String measureMode;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Marker {
        private String id;
        private double[] position;
        private String label;
        private String type; // pin, popup, custom
        private String icon;
        private Map<String, Object> properties;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Drawing {
        private String id;
        private String type; // point, linestring, polygon, circle
        private Object geometry;
        private String style;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MapAction {
        private String type;
        private Map<String, Object> params;
        private String description;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ActionResult {
        private MapAction action;
        private boolean success;
        private Object result;
        private String error;
        private Long timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ConversationResult {
        private boolean success;
        private String message;
        private List<MapAction> actions;
        private List<ActionResult> results;
        private MapState currentState;
    }
    
    public static class ConfigNotFoundException extends RuntimeException {
        public ConfigNotFoundException(String msg) { super(msg); }
    }
}
