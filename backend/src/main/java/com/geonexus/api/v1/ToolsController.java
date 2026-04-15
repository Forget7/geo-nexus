package com.geonexus.api.v1;

import com.geonexus.model.ToolExecuteRequest;
import com.geonexus.model.ToolInfo;
import com.geonexus.service.GISService;
import com.geonexus.service.IntelligentMapService;
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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "工具集", description = "GIS分析工具集：距离计算、缓冲区、叠加分析、地理编码等")
public class ToolsController {
    
    private final GISService gisService;
    private final IntelligentMapService intelligentMapService;
    
    @Operation(summary = "获取工具列表", description = "获取所有可用的GIS分析工具")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/tools")
    public ResponseEntity<List<ToolInfo>> listTools() {
        return ResponseEntity.ok(gisService.listTools());
    }
    
    @Operation(summary = "执行工具", description = "根据工具名称和参数执行指定的GIS分析工具")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "执行成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/tools/execute")
    public ResponseEntity<Map<String, Object>> executeTool(@RequestBody ToolExecuteRequest request) {
        return ResponseEntity.ok(gisService.executeTool(request));
    }
    
    // ── 已有便捷端点 ───────────────────────────────
    
    @Operation(summary = "计算距离", description = "根据两点坐标计算直线距离，支持km/mile/nmi单位")
    @ApiResponse(responseCode = "200", description = "计算成功")
    @GetMapping("/tools/distance")
    public ResponseEntity<Map<String, Object>> calculateDistance(
            @Parameter(description = "起点纬度") @RequestParam Double lat1,
            @Parameter(description = "起点经度") @RequestParam Double lon1,
            @Parameter(description = "终点纬度") @RequestParam Double lat2,
            @Parameter(description = "终点经度") @RequestParam Double lon2,
            @Parameter(description = "距离单位：km/mile/nmi") @RequestParam(defaultValue = "km") String unit) {
        return ResponseEntity.ok(gisService.calculateDistance(lat1, lon1, lat2, lon2, unit));
    }
    
    @Operation(summary = "缓冲区分析", description = "对几何对象创建指定半径的缓冲区多边形")
    @ApiResponse(responseCode = "200", description = "分析成功")
    @PostMapping("/tools/buffer")
    public ResponseEntity<Map<String, Object>> bufferAnalysis(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(gisService.bufferAnalysis(request));
    }
    
    @Operation(summary = "地理编码", description = "将地址转换为经纬度坐标")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "编码成功"),
        @ApiResponse(responseCode = "400", description = "地址格式错误")
    })
    @PostMapping("/tools/geocode")
    public ResponseEntity<Map<String, Object>> geocode(@RequestBody Map<String, String> request) {
        String address = request.get("address");
        return ResponseEntity.ok(gisService.geocode(address));
    }
    
    @Operation(summary = "逆地理编码", description = "根据经纬度坐标获取对应地址")
    @ApiResponse(responseCode = "200", description = "编码成功")
    @GetMapping("/tools/geocode/reverse")
    public ResponseEntity<Map<String, Object>> reverseGeocode(
            @Parameter(description = "纬度") @RequestParam Double lat,
            @Parameter(description = "经度") @RequestParam Double lon) {
        return ResponseEntity.ok(gisService.reverseGeocode(lat, lon));
    }
    
    // ── 新增工具端点 ───────────────────────────────
    
    @Operation(summary = "叠加分析：交集", description = "计算两个几何对象的交集")
    @ApiResponse(responseCode = "200", description = "分析成功")
    @PostMapping("/tools/intersect")
    public ResponseEntity<Map<String, Object>> intersect(@RequestBody Map<String, Object> request) {
        Map<String, Object> merged = new java.util.HashMap<>(request);
        merged.put("operation", "intersection");
        return ResponseEntity.ok(gisService.executeTool(
                ToolExecuteRequest.builder().tool("overlay_analysis").params(merged).build()));
    }
    
    @Operation(summary = "叠加分析：并集", description = "计算两个几何对象的并集")
    @ApiResponse(responseCode = "200", description = "分析成功")
    @PostMapping("/tools/union")
    public ResponseEntity<Map<String, Object>> union(@RequestBody Map<String, Object> request) {
        Map<String, Object> merged = new java.util.HashMap<>(request);
        merged.put("operation", "union");
        return ResponseEntity.ok(gisService.executeTool(
                ToolExecuteRequest.builder().tool("overlay_analysis").params(merged).build()));
    }
    
    @Operation(summary = "叠加分析：差集", description = "计算两个几何对象的差集")
    @ApiResponse(responseCode = "200", description = "分析成功")
    @PostMapping("/tools/difference")
    public ResponseEntity<Map<String, Object>> difference(@RequestBody Map<String, Object> request) {
        Map<String, Object> merged = new java.util.HashMap<>(request);
        merged.put("operation", "difference");
        return ResponseEntity.ok(gisService.executeTool(
                ToolExecuteRequest.builder().tool("overlay_analysis").params(merged).build()));
    }
    
    @Operation(summary = "凸包分析", description = "计算几何对象的凸包")
    @ApiResponse(responseCode = "200", description = "分析成功")
    @PostMapping("/tools/convexHull")
    public ResponseEntity<Map<String, Object>> convexHull(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(gisService.executeTool(
                ToolExecuteRequest.builder().tool("convex_hull").params(request).build()));
    }
    
    @Operation(summary = "几何简化", description = "使用Douglas-Peucker算法简化几何对象")
    @ApiResponse(responseCode = "200", description = "简化成功")
    @PostMapping("/tools/simplify")
    public ResponseEntity<Map<String, Object>> simplify(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(gisService.executeTool(
                ToolExecuteRequest.builder().tool("simplify_geometry").params(request).build()));
    }
    
    @Operation(summary = "点面包含判断", description = "判断点是否在指定面内")
    @ApiResponse(responseCode = "200", description = "判断成功")
    @PostMapping("/tools/within")
    public ResponseEntity<Map<String, Object>> within(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(gisService.within(request));
    }
    
    // ── 多语言标注 ───────────────────────────────
    
    @Operation(summary = "生成地图标注", description = "根据GeoJSON生成多语言地图标注")
    @ApiResponse(responseCode = "200", description = "生成成功")
    @PostMapping("/tools/labels/generate")
    public ResponseEntity<Map<String, Object>> generateLabels(
            @RequestBody Map<String, Object> geojson,
            @Parameter(description = "语言：zh/en/bilingual") @RequestParam(defaultValue = "zh") String locale) {
        return ResponseEntity.ok(intelligentMapService.generateLabels(geojson, locale));
    }
}
