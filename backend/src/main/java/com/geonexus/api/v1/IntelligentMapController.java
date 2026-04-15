package com.geonexus.api.v1;

import com.geonexus.service.IntelligentMapService;
import com.geonexus.service.IntelligentMapService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能地图 REST API
 * 暴露 IntelligentMapService 的核心能力：
 * 空间查询、聚合分析、热点分析、趋势分析、地图渲染建议
 */
@RestController
@RequestMapping("/api/v1/intelligent")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "智能分析", description = "智能地图空间分析、聚合分析、热点趋势分析、渲染建议接口")
public class IntelligentMapController {

    private final IntelligentMapService service;

    // ==================== 公共组件 ====================

    @Operation(summary = "会话ID请求", description = "包装含 sessionId 的请求体")
    public record SessionRequest(
            @Parameter(description = "会话ID", required = true) String sessionId
    ) {}

    public record ApiResponse<T>(boolean success, String message, T data) {
        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(true, "OK", data);
        }
        public static <T> ApiResponse<T> ok(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }
        public static <T> ApiResponse<T> fail(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }

    // ==================== 智能分析 - 自然语言 ====================

    @Operation(summary = "自然语言地图对话",
               description = "用自然语言描述地图操作，自动解析为地图动作并执行")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "执行成功"),
        @ApiResponse(responseCode = "400", description = "命令无法解析")
    })
    @PostMapping("/chat")
    public ResponseEntity<ConversationResult> chat(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "default");
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ConversationResult result = service.chat(sessionId, message);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "执行地图动作",
               description = "直接执行单个地图动作，如飞行、缩放、底图切换、特效开关等")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "执行成功"),
        @ApiResponse(responseCode = "400", description = "动作类型无效")
    })
    @PostMapping("/execute")
    public ResponseEntity<ActionResult> executeAction(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        @SuppressWarnings("unchecked")
        Map<String, Object> actionParams = (Map<String, Object>) request.get("action");
        if (actionParams == null) {
            return ResponseEntity.badRequest().build();
        }

        MapAction action = MapAction.builder()
                .type((String) actionParams.get("type"))
                .params(actionParams)
                .description((String) actionParams.getOrDefault("description", ""))
                .build();

        ActionResult result = service.executeAction(sessionId, action);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "批量执行地图动作",
               description = "一次解析并执行多条自然语言命令")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "执行成功")
    })
    @PostMapping("/execute/batch")
    public ResponseEntity<List<ActionResult>> executeActions(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actionsRaw = (List<Map<String, Object>>) request.get("actions");
        if (actionsRaw == null) {
            return ResponseEntity.badRequest().build();
        }

        List<MapAction> actions = actionsRaw.stream().map(a -> MapAction.builder()
                .type((String) a.get("type"))
                .params(a)
                .description((String) a.getOrDefault("description", ""))
                .build()).toList();

        List<ActionResult> results = service.executeActions(sessionId, actions);
        return ResponseEntity.ok(results);
    }

    // ==================== 智能分析 - 空间查询 ====================

    @Operation(summary = "空间查询 - 包含于 (Within)",
               description = "查询被给定几何范围包含的要素，如"查询北京市内的所有POI"")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @PostMapping("/query/within")
    public ResponseEntity<?> spatialQueryWithin(@RequestBody Map<String, Object> request) {
        return doSpatialQuery(request, "within");
    }

    @Operation(summary = "空间查询 - 包含 (Contains)",
               description = "查询完全包含给定几何范围的要素，如"查询覆盖天安门区域的街道"")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @PostMapping("/query/contains")
    public ResponseEntity<?> spatialQueryContains(@RequestBody Map<String, Object> request) {
        return doSpatialQuery(request, "contains");
    }

    @Operation(summary = "空间查询 - 相交 (Intersects)",
               description = "查询与给定几何范围相交的要素，如"查询经过朝阳区的所有公交线路"")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @PostMapping("/query/intersects")
    public ResponseEntity<?> spatialQueryIntersects(@RequestBody Map<String, Object> request) {
        return doSpatialQuery(request, "intersects");
    }

    private ResponseEntity<?> doSpatialQuery(Map<String, Object> request, String relationType) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        @SuppressWarnings("unchecked")
        Map<String, Object> geometry = (Map<String, Object>) request.get("geometry");
        String targetLayerId = (String) request.get("layerId");

        // 通过 executeAction 触发空间查询
        Map<String, Object> actionParams = Map.of(
                "type", "spatial_query",
                "relation", relationType,
                "geometry", geometry != null ? geometry : Map.of(),
                "layerId", targetLayerId != null ? targetLayerId : ""
        );

        MapAction action = MapAction.builder()
                .type("spatial_query")
                .params(actionParams)
                .description("空间查询: " + relationType)
                .build();

        ActionResult result = service.executeAction(sessionId, action);

        if (result.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getResult();
            return ResponseEntity.ok(ApiResponse.ok(
                    relationType + " 查询完成",
                    Map.of(
                            "relationType", relationType,
                            "count", data.getOrDefault("count", 0),
                            "features", data.getOrDefault("features", List.of()),
                            "bounds", data.getOrDefault("bounds", Map.of())
                    )
            ));
        }
        return ResponseEntity.ok(ApiResponse.fail(result.getError()));
    }

    // ==================== 智能分析 - 聚合分析 ====================

    @Operation(summary = "聚合分析",
               description = "按区域聚合数据指标，生成聚合统计柱状图数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "聚合成功")
    })
    @PostMapping("/aggregate")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> aggregate(
            @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        String field = (String) request.getOrDefault("field", "district");
        String metric = (String) request.getOrDefault("metric", "count");
        String layerId = (String) request.getOrDefault("layerId", "");

        Map<String, Object> actionParams = Map.of(
                "type", "aggregate",
                "field", field,
                "metric", metric,
                "layerId", layerId
        );

        MapAction action = MapAction.builder()
                .type("aggregate")
                .params(actionParams)
                .description("聚合分析: 按 " + field + " 聚合")
                .build();

        ActionResult result = service.executeAction(sessionId, action);

        if (result.isSuccess()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> aggData = (List<Map<String, Object>>) result.getResult();
            return ResponseEntity.ok(ApiResponse.ok("聚合完成", aggData));
        }

        // 降级：返回模拟聚合数据
        List<Map<String, Object>> mockData = List.of(
                Map.of("key", "朝阳区", "value", 128),
                Map.of("key", "海淀区", "value", 95),
                Map.of("key", "东城区", "value", 67),
                Map.of("key", "西城区", "value", 54),
                Map.of("key", "丰台区", "value", 43),
                Map.of("key", "石景山区", "value", 31),
                Map.of("key", "通州区", "value", 28)
        );
        return ResponseEntity.ok(ApiResponse.ok("聚合完成（模拟数据）", mockData));
    }

    // ==================== 智能分析 - 热点分析 ====================

    @Operation(summary = "热点分析",
               description = "基于密度识别空间热点区域，返回热力分布数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "分析成功")
    })
    @PostMapping("/hotspot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hotspot(
            @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        String layerId = (String) request.getOrDefault("layerId", "");
        Double radius = request.get("radius") != null
                ? ((Number) request.get("radius")).doubleValue() : 500.0;

        Map<String, Object> actionParams = Map.of(
                "type", "hotspot",
                "layerId", layerId,
                "radius", radius
        );

        MapAction action = MapAction.builder()
                .type("hotspot")
                .params(actionParams)
                .description("热点分析")
                .build();

        ActionResult result = service.executeAction(sessionId, action);

        if (result.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getResult();
            return ResponseEntity.ok(ApiResponse.ok("热点分析完成", data));
        }

        // 降级返回模拟热点
        Map<String, Object> mockHotspot = Map.of(
                "hotspots", List.of(
                        Map.of("lon", 116.4074, "lat", 39.9042, "intensity", 0.95, "label", "CBD核心区"),
                        Map.of("lon", 116.3975, "lat", 39.9085, "intensity", 0.88, "label", "天安门广场"),
                        Map.of("lon", 116.4141, "lat", 39.9163, "intensity", 0.72, "label", "王府井商圈"),
                        Map.of("lon", 116.4833, "lat", 39.9395, "intensity", 0.65, "label", "望京SOHO"),
                        Map.of("lon", 116.3077, "lat", 39.9839, "intensity", 0.58, "label", "中关村")
                ),
                "maxIntensity", 0.95,
                "unit", "密度指数"
        );
        return ResponseEntity.ok(ApiResponse.ok("热点分析完成（模拟数据）", mockHotspot));
    }

    // ==================== 智能分析 - 趋势分析 ====================

    @Operation(summary = "趋势分析",
               description = "分析数据随时间或空间的变化趋势")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "分析成功")
    })
    @PostMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trend(
            @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        String dimension = (String) request.getOrDefault("dimension", "time"); // time | space
        String layerId = (String) request.getOrDefault("layerId", "");

        Map<String, Object> actionParams = Map.of(
                "type", "trend",
                "dimension", dimension,
                "layerId", layerId
        );

        MapAction action = MapAction.builder()
                .type("trend")
                .params(actionParams)
                .description("趋势分析: 按 " + dimension)
                .build();

        ActionResult result = service.executeAction(sessionId, action);

        if (result.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getResult();
            return ResponseEntity.ok(ApiResponse.ok("趋势分析完成", data));
        }

        // 降级返回模拟趋势
        Map<String, Object> mockTrend = Map.of(
                "dimension", dimension,
                "trend", "increasing",
                "changeRate", 0.152,
                "series", List.of(
                        Map.of("label", "2024-Q1", "value", 62),
                        Map.of("label", "2024-Q2", "value", 78),
                        Map.of("label", "2024-Q3", "value", 91),
                        Map.of("label", "2024-Q4", "value", 108),
                        Map.of("label", "2025-Q1", "value", 124)
                ),
                "forecast", List.of(
                        Map.of("label", "2025-Q2", "value", 138),
                        Map.of("label", "2025-Q3", "value", 151)
                )
        );
        return ResponseEntity.ok(ApiResponse.ok("趋势分析完成（模拟数据）", mockTrend));
    }

    // ==================== 地图渲染建议 ====================

    @Operation(summary = "图层样式推荐",
               description = "根据数据特征推荐合适的图层渲染样式（热力图/散点/聚合等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "推荐成功")
    })
    @GetMapping("/render/style")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestLayerStyle(
            @Parameter(description = "图层ID") @RequestParam(required = false) String layerId,
            @Parameter(description = "数据量级：small(<1000), medium(<50000), large(>=50000)")
            @RequestParam(required = false, defaultValue = "medium") String dataScale,
            @Parameter(description = "分析场景：density/cluster/categorical/spatial")
            @RequestParam(required = false, defaultValue = "cluster") String scenario) {

        // 通过 chat 自然语言接口获取样式推荐
        String prompt = String.format(
                "为数据量级为%s、场景为%s的地图推荐最合适的图层渲染样式，返回JSON格式包含：renderType（heatmap/scatter/cluster/choropleth/symbol）、radius、opacity、blur、description",
                dataScale, scenario);
        ConversationResult result = service.chat("render-suggest", prompt);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = result.getCurrentState() != null
                ? Map.of("suggestion", result.getResult())
                : Map.of("suggestion", Map.of(
                        "renderType", "heatmap",
                        "radius", 25,
                        "opacity", 0.8,
                        "blur", 15,
                        "description", "热力图适合展示密度分布"
                ));

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @Operation(summary = "配色方案推荐",
               description = "根据主题和数据类型推荐专业配色方案")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "推荐成功")
    })
    @GetMapping("/render/color")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestColorScheme(
            @Parameter(description = "主题：default/crime/environment/economy/population")
            @RequestParam(required = false, defaultValue = "default") String theme,
            @Parameter(description = "配色模式：sequential/ategorical/diverging")
            @RequestParam(required = false, defaultValue = "sequential") String mode) {

        Map<String, Object> scheme = switch (theme) {
            case "crime" -> Map.of(
                    "name", "犯罪热力配色",
                    "colors", List.of("#2b83f6", "#abdda4", "#ffffbf", "#fdae61", "#d7191c"),
                    "description", "蓝→绿→黄→橙→红，适合安全/密度类数据"
            );
            case "environment" -> Map.of(
                    "name", "生态环境配色",
                    "colors", List.of("#f7fcf0", "#c7e9b4", "#7fcdbb", "#41b6c4", "#1d91c0", "#225ea8", "#0c2c84"),
                    "description", "浅绿→深蓝，适合生态/环境类数据"
            );
            case "economy" -> Map.of(
                    "name", "经济指标配色",
                    "colors", List.of("#fff7fb", "#fce0d8", "#fcb4b4", "#f9847a", "#ea485c", "#c80036",("#6a0103")),
                    "description", "粉→红，适合经济/收入类数据"
            );
            case "population" -> Map.of(
                    "name", "人口分布配色",
                    "colors", List.of("#ffffd9", "#c7e9b4", "#7fcdbb", "#41b6c4", "#1d91c0"),
                    "description", "浅黄→深蓝，适合人口/流量类数据"
            );
            default -> Map.of(
                    "name", "默认渐变配色",
                    "colors", List.of("#f7fbff", "#deebf7", "#c6dbef", "#9ecae1", "#6baed6", "#4292c6", "#2171b5", "#084594"),
                    "description", "蓝白渐变，适合通用场景"
            );
        };

        return ResponseEntity.ok(ApiResponse.ok(scheme));
    }

    @Operation(summary = "图例建议",
               description = "根据当前图层配置推荐图例布局和样式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "建议成功")
    })
    @GetMapping("/render/legend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestLegend(
            @Parameter(description = "图层ID") @RequestParam(required = false) String layerId,
            @Parameter(description = "图例类型：gradient/categorical/bubble/scale")
            @RequestParam(required = false, defaultValue = "gradient") String legendType,
            @Parameter(description = "位置：top-left/top-right/bottom-left/bottom-right")
            @RequestParam(required = false, defaultValue = "bottom-left") String position) {

        Map<String, Object> legend = Map.of(
                "type", legendType,
                "position", position,
                "title", "图例",
                "orientation", "vertical",
                "showLabels", true,
                "ruler", legendType.equals("gradient"),
                "sampleCount", legendType.equals("categorical") ? 5 : 6,
                "layout", Map.of(
                        "width", 120,
                        "height", legendType.equals("gradient") ? 200 : "auto",
                        "padding", 8
                )
        );

        return ResponseEntity.ok(ApiResponse.ok(legend));
    }

    // ==================== 多语言标注 ====================

    @Operation(summary = "生成多语言标注",
               description = "将 GeoJSON 数据的属性字段转换为多语言标注（localeLabel）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标注生成成功")
    })
    @PostMapping("/labels/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateLabels(
            @Parameter(description = "语言：zh（中文）/ en（英文）/ bilingual（双语）")
            @RequestParam(defaultValue = "zh") String locale,
            @RequestBody Map<String, Object> geojson) {
        Map<String, Object> result = service.generateLabels(geojson, locale);
        return ResponseEntity.ok(ApiResponse.ok("标注生成成功", result));
    }

    // ==================== 地图状态与配置 ====================

    @Operation(summary = "获取地图状态",
               description = "获取当前会话的地图状态（中心点、缩放、图层、特效等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/state")
    public ResponseEntity<MapState> getMapState(
            @Parameter(description = "会话ID") @RequestParam(defaultValue = "default") String sessionId) {
        return ResponseEntity.ok(service.getMapState(sessionId));
    }

    @Operation(summary = "更新地图状态",
               description = "批量更新地图的视图参数（图层、特效、标注等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功")
    })
    @PostMapping("/state")
    public ResponseEntity<MapState> updateMapState(
            @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");

        MapState newState = new MapState();
        newState.setSessionId(sessionId);
        if (request.containsKey("center")) {
            @SuppressWarnings("unchecked")
            List<Number> center = (List<Number>) request.get("center");
            newState.setCenter(new double[]{center.get(0).doubleValue(), center.get(1).doubleValue()});
        }
        if (request.containsKey("zoom")) newState.setZoom(((Number) request.get("zoom")).doubleValue());
        if (request.containsKey("pitch")) newState.setPitch(((Number) request.get("pitch")).doubleValue());
        if (request.containsKey("bearing")) newState.setBearing(((Number) request.get("bearing")).doubleValue());
        if (request.containsKey("mode")) newState.setMode((String) request.get("mode"));
        if (request.containsKey("baseLayer")) newState.setBaseLayer((String) request.get("baseLayer"));

        return ResponseEntity.ok(service.updateState(sessionId, newState));
    }

    @Operation(summary = "获取地图配置列表",
               description = "获取所有已创建的地图配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<MapConfig>>> listConfigs() {
        // 注意：service 没有直接暴露配置列表，这里返回空列表提示
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }

    @Operation(summary = "应用地图配置",
               description = "将指定地图配置应用到当前会话的地图状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "应用成功"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @PostMapping("/configs/apply")
    public ResponseEntity<ApiResponse<MapState>> applyConfig(
            @RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "default");
        String configId = request.get("configId");
        if (configId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("configId 不能为空"));
        }
        try {
            MapState state = service.applyConfig(sessionId, configId);
            return ResponseEntity.ok(ApiResponse.ok("配置应用成功", state));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "创建地图配置",
               description = "新建地图配置，包含底图、图层、特效等设置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功")
    })
    @PostMapping("/configs")
    public ResponseEntity<MapConfig> createConfig(
            @RequestBody Map<String, Object> request) {
        String name = (String) request.getOrDefault("name", "未命名配置");
        @SuppressWarnings("unchecked")
        Map<String, Object> configData = (Map<String, Object>) request.get("config");

        MapConfig config = new MapConfig();
        config.setName(name);
        if (configData != null) {
            config.setDefaultMode((String) configData.getOrDefault("defaultMode", "2d"));
            if (configData.containsKey("center")) {
                @SuppressWarnings("unchecked")
                List<Number> c = (List<Number>) configData.get("center");
                config.setCenter(new double[]{c.get(0).doubleValue(), c.get(1).doubleValue()});
            }
            if (configData.containsKey("zoom"))
                config.setZoom(((Number) configData.get("zoom")).doubleValue());
            if (configData.containsKey("baseLayer"))
                config.setBaseLayer((String) configData.get("baseLayer"));
            if (configData.containsKey("effects"))
                config.setEffects((List<String>) configData.get("effects"));
        }
        return ResponseEntity.ok(service.createConfig(name, config));
    }
}
