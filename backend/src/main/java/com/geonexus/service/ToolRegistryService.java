package com.geonexus.service;

import com.geonexus.domain.tool.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具注册与发现服务
 */
@Slf4j
@Service
public class ToolRegistryService {

    private final CacheService cacheService;
    private final Map<String, ToolDefinition> toolRegistry = new ConcurrentHashMap<>();

    private static final String TOOL_PREFIX = "tool:def:";

    public ToolRegistryService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeBuiltinTools();
    }

    // ==================== 初始化内置工具 ====================

    private void initializeBuiltinTools() {
        // 地图工具
        registerTool(ToolDefinition.builder()
                .id("map-viewer")
                .name("地图查看器")
                .description("查看和浏览2D/3D地图")
                .category("map")
                .type("builtin")
                .icon("🗺️")
                .capabilities(List.of("2d", "3d", "satellite", "terrain"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("layer-manager")
                .name("图层管理器")
                .description("管理地图图层")
                .category("map")
                .type("builtin")
                .icon("📚")
                .capabilities(List.of("add", "remove", "reorder", "style", "opacity"))
                .build());

        // GIS分析工具
        registerTool(ToolDefinition.builder()
                .id("spatial-query")
                .name("空间查询")
                .description("执行空间查询操作")
                .category("analysis")
                .type("builtin")
                .icon("🔍")
                .capabilities(List.of("point-in-polygon", "spatial-join", "intersection"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("buffer-analysis")
                .name("缓冲区分析")
                .description("创建指定要素的缓冲区域")
                .category("analysis")
                .type("builtin")
                .icon("🔵")
                .capabilities(List.of("point-buffer", "line-buffer", "polygon-buffer", "multiple-ring"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("overlay-analysis")
                .name("叠加分析")
                .description("执行图层叠加分析")
                .category("analysis")
                .type("builtin")
                .icon("🔄")
                .capabilities(List.of("intersect", "union", "erase", "update"))
                .build());

        // 数据工具
        registerTool(ToolDefinition.builder()
                .id("data-import")
                .name("数据导入")
                .description("导入各种格式的GIS数据")
                .category("data")
                .type("builtin")
                .icon("📥")
                .capabilities(List.of("geojson", "shapefile", "kml", "gpx", "csv"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("data-export")
                .name("数据导出")
                .description("导出数据为各种格式")
                .category("data")
                .type("builtin")
                .icon("📤")
                .capabilities(List.of("geojson", "shapefile", "kml", "csv", "png", "pdf"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("data-validate")
                .name("数据验证")
                .description("验证GIS数据质量")
                .category("data")
                .type("builtin")
                .icon("✅")
                .capabilities(List.of("geometry", "topology", "attribute", "schema"))
                .build());

        // 第三方AI工具示例
        registerTool(ToolDefinition.builder()
                .id("openai-dalle")
                .name("DALL-E 图像生成")
                .description("使用OpenAI DALL-E生成地图相关图像")
                .category("ai")
                .type("external")
                .icon("🎨")
                .apiConfig(Map.of(
                        "provider", "openai",
                        "endpoint", "https://api.openai.com/v1/images/generations",
                        "method", "POST"
                ))
                .capabilities(List.of("text-to-image", "image-edit", "variation"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("google-maps")
                .name("Google地图")
                .description("集成Google地图服务")
                .category("external")
                .type("external")
                .icon("🌍")
                .apiConfig(Map.of(
                        "provider", "google",
                        "services", List.of("maps", "places", "geocoding", "directions")
                ))
                .capabilities(List.of("geocoding", "directions", "places-search", "elevation"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("amap")
                .name("高德地图")
                .description("集成高德地图服务（中国）")
                .category("external")
                .type("external")
                .icon("🏠")
                .apiConfig(Map.of(
                        "provider", "amap",
                        "baseUrl", "https://restapi.amap.com/v3"
                ))
                .capabilities(List.of("geocoding", "navigation", "weather", "search"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("mapbox")
                .name("Mapbox")
                .description("集成Mapbox地图服务")
                .category("external")
                .type("external")
                .icon("📍")
                .apiConfig(Map.of(
                        "provider", "mapbox",
                        "baseUrl", "https://api.mapbox.com"
                ))
                .capabilities(List.of("geocoding", "directions", "matrix", "tiles"))
                .build());

        registerTool(ToolDefinition.builder()
                .id("weather-api")
                .name("天气API")
                .description("获取天气预报数据")
                .category("external")
                .type("external")
                .icon("🌤️")
                .apiConfig(Map.of(
                        "providers", List.of("openweather", "weatherapi", "tomorrow")
                ))
                .capabilities(List.of("current", "forecast", "historical", "alerts"))
                .build());
    }

    // ==================== 工具注册 ====================

    /**
     * 注册工具
     */
    public ToolDefinition registerTool(ToolDefinition tool) {
        tool.setId(tool.getId() != null ? tool.getId() : UUID.randomUUID().toString());
        tool.setRegisteredAt(System.currentTimeMillis());

        toolRegistry.put(tool.getId(), tool);

        String key = TOOL_PREFIX + tool.getId();
        cacheService.set(key, tool);

        log.info("注册工具: id={}, name={}, type={}",
                tool.getId(), tool.getName(), tool.getType());

        return tool;
    }

    /**
     * 获取工具定义
     */
    public ToolDefinition getTool(String toolId) {
        ToolDefinition tool = toolRegistry.get(toolId);
        if (tool == null) {
            String key = TOOL_PREFIX + toolId;
            tool = (ToolDefinition) cacheService.get(key);
        }

        if (tool == null) {
            throw new ToolNotFoundException("工具不存在: " + toolId);
        }

        return tool;
    }

    /**
     * 获取所有工具
     */
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(toolRegistry.values());
    }

    /**
     * 按分类获取工具
     */
    public List<ToolDefinition> getToolsByCategory(String category) {
        return toolRegistry.values().stream()
                .filter(tool -> tool.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    /**
     * 搜索工具
     */
    public List<ToolDefinition> searchTools(String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        return toolRegistry.values().stream()
                .filter(tool -> tool.getName().toLowerCase().contains(lowerKeyword)
                        || tool.getDescription().toLowerCase().contains(lowerKeyword)
                        || (tool.getTags() != null && tool.getTags().stream()
                                .anyMatch(t -> t.toLowerCase().contains(lowerKeyword))))
                .collect(Collectors.toList());
    }

    /**
     * 更新工具
     */
    public ToolDefinition updateTool(String toolId, ToolDefinition tool) {
        ToolDefinition existing = getTool(toolId);

        if ("builtin".equals(existing.getType())) {
            throw new IllegalArgumentException("不能修改内置工具");
        }

        tool.setId(toolId);
        tool.setType(existing.getType());
        tool.setRegisteredAt(existing.getRegisteredAt());
        tool.setUpdatedAt(System.currentTimeMillis());

        toolRegistry.put(toolId, tool);

        String key = TOOL_PREFIX + toolId;
        cacheService.set(key, tool);

        return tool;
    }

    /**
     * 删除工具
     */
    public void unregisterTool(String toolId) {
        ToolDefinition tool = getTool(toolId);

        if ("builtin".equals(tool.getType())) {
            throw new IllegalArgumentException("不能删除内置工具");
        }

        toolRegistry.remove(toolId);

        String key = TOOL_PREFIX + toolId;
        cacheService.delete(key);

        log.info("注销工具: id={}", toolId);
    }

    // ==================== 异常类 ====================

    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String message) { super(message); }
    }
}
