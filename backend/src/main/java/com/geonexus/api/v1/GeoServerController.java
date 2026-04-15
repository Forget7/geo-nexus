package com.geonexus.api.v1;

import com.geonexus.service.GeoServerService;
import com.geonexus.service.GeoServerStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * GeoServer控制器 - OGC标准服务
 */
@Tag(name = "GeoServer", description = "GeoServer集成 - WMS/WFS/WMTS服务")
@RestController
@RequestMapping("/api/v1/geoserver")
@RequiredArgsConstructor
public class GeoServerController {
    
    private final GeoServerService geoServerService;
    private final GeoServerStyleService styleService;
    
    @GetMapping("/health")
    @Operation(summary = "GeoServer健康检查")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "健康检查结果")
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean healthy = geoServerService.isHealthy();
        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DOWN",
                "service", "GeoServer"
        ));
    }
    
    @GetMapping("/wms/capabilities")
    @Operation(summary = "获取WMS GetCapabilities URL")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<Map<String, String>> getWMSCapabilities() {
        return ResponseEntity.ok(Map.of(
                "url", geoServerService.getWMSCapabilitiesUrl(),
                "type", "WMS",
                "version", "1.3.0"
        ));
    }
    
    @GetMapping("/wfs/capabilities")
    @Operation(summary = "获取WFS GetCapabilities URL")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<Map<String, String>> getWFSCapabilities() {
        return ResponseEntity.ok(Map.of(
                "url", geoServerService.getWFSCapabilitiesUrl(),
                "type", "WFS",
                "version", "2.0.0"
        ));
    }
    
    @PostMapping("/layers")
    @Operation(summary = "发布图层", description = "将GeoJSON数据发布为GeoServer图层")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发布成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public ResponseEntity<Map<String, Object>> publishLayer(
            @RequestBody Map<String, Object> request) {
        
        String layerName = (String) request.get("layerName");
        String geoJson = (String) request.get("geoJson");
        String style = (String) request.get("style");
        
        if (layerName == null || layerName.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "layerName is required"));
        }
        
        // 上传GeoJSON
        boolean success = geoServerService.uploadGeoJSON(layerName, geoJson);
        
        if (success && style != null && !style.isEmpty()) {
            // 应用样式
            geoServerService.createStyle(layerName + "_style", style);
            geoServerService.setLayerStyle(layerName, layerName + "_style");
        }
        
        return ResponseEntity.ok(Map.of(
                "success", success,
                "layerName", layerName,
                "wmsUrl", geoServerService.getWMSCapabilitiesUrl(),
                "message", success ? "Layer published successfully" : "Failed to publish layer"
        ));
    }
    
    @GetMapping("/layers/{layerName}")
    @Operation(summary = "获取图层信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "图层不存在")
    })
    public ResponseEntity<Map<String, Object>> getLayerInfo(
            @Parameter(description = "图层名称") @PathVariable String layerName) {
        Map<String, Object> info = geoServerService.getLayerInfo(layerName);
        
        if (info.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(info);
    }
    
    @GetMapping("/layers/{layerName}/wms-url")
    @Operation(summary = "获取图层WMS URL", description = "生成图层WMS GetMap请求URL")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<Map<String, String>> getLayerWMSUrl(
            @Parameter(description = "图层名称") @PathVariable String layerName,
            @Parameter(description = "边界框") @RequestParam(defaultValue = "-180,-90,180,90") String bbox,
            @Parameter(description = "图片宽度") @RequestParam(defaultValue = "800") int width,
            @Parameter(description = "图片高度") @RequestParam(defaultValue = "600") int height,
            @Parameter(description = "图片格式") @RequestParam(defaultValue = "image/png") String format) {
        
        String url = geoServerService.getWMSGetMapUrl(layerName, bbox, width, height, format);
        
        return ResponseEntity.ok(Map.of(
                "layerName", layerName,
                "wmsUrl", url
        ));
    }
    
    @PostMapping("/styles")
    @Operation(summary = "创建样式", description = "创建点、线、面类型的SLD样式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public ResponseEntity<Map<String, Object>> createStyle(
            @RequestBody Map<String, Object> request) {
        
        String styleName = (String) request.get("styleName");
        String styleType = (String) request.get("type"); // point, line, polygon
        String color = (String) request.getOrDefault("color", "#2563EB");
        double size = ((Number) request.getOrDefault("size", 6)).doubleValue();
        
        if (styleName == null || styleType == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "styleName and type are required"));
        }
        
        String sldContent;
        switch (styleType.toLowerCase()) {
            case "point" -> sldContent = GeoServerStyleService.createDefaultPointStyle(color, size);
            case "line" -> sldContent = GeoServerStyleService.createDefaultLineStyle(color, size);
            case "polygon" -> sldContent = GeoServerStyleService.createDefaultPolygonStyle(
                    color, color, 0.7);
            default -> sldContent = GeoServerStyleService.createDefaultPointStyle(color, size);
        }
        
        boolean success = geoServerService.createStyle(styleName, sldContent);
        
        return ResponseEntity.ok(Map.of(
                "success", success,
                "styleName", styleName,
                "message", success ? "Style created" : "Failed to create style"
        ));
    }
    
    @GetMapping("/workspaces")
    @Operation(summary = "获取所有Workspace")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<Map<String, Object>> getWorkspaces() {
        return ResponseEntity.ok(Map.of(
                "workspaces", geoServerService.getWorkspaces()
        ));
    }
    
    @PostMapping("/workspaces")
    @Operation(summary = "创建Workspace")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public ResponseEntity<Map<String, Object>> createWorkspace(
            @RequestBody Map<String, String> request) {
        
        String name = request.get("name");
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "name is required"));
        }
        
        boolean success = geoServerService.createWorkspace(name);
        
        return ResponseEntity.ok(Map.of(
                "success", success,
                "workspace", name,
                "message", success ? "Workspace created" : "Failed to create workspace"
        ));
    }

    // ─── SLD 样式编辑器 API ───────────────────────────────────────────────

    @GetMapping("/styles/{styleName}")
    @Operation(summary = "获取SLD样式XML")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "样式不存在")
    })
    public ResponseEntity<Map<String, Object>> getStyle(
            @Parameter(description = "样式名称") @PathVariable String styleName) {
        String sldContent = geoServerService.getStyleContent(styleName);
        if (sldContent == null) {
            return ResponseEntity.notFound().build();
        }
        String sldVersion = sldContent.contains("version=\"1.1.0\"") ? "SLD 1.1" : "SLD 1.0";
        return ResponseEntity.ok(Map.of(
                "styleName", styleName,
                "sldContent", sldContent,
                "sldVersion", sldVersion
        ));
    }

    @PutMapping("/styles/{styleName}")
    @Operation(summary = "更新SLD样式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public ResponseEntity<Map<String, Object>> updateStyle(
            @Parameter(description = "样式名称") @PathVariable String styleName,
            @RequestBody Map<String, Object> request) {
        String sldContent = (String) request.get("sldContent");
        if (sldContent == null || sldContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sldContent is required"));
        }
        boolean success = geoServerService.createStyle(styleName, sldContent);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "styleName", styleName,
                "message", success ? "Style updated" : "Failed to update style"
        ));
    }

    @GetMapping("/styles/templates")
    @Operation(summary = "获取SLD预设模板")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<Map<String, Object>> getStyleTemplates() {
        return ResponseEntity.ok(Map.of(
                "templates", geoServerService.getStyleTemplates()
        ));
    }
}
