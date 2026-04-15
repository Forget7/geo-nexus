package com.geonexus.api.v1;

import com.geonexus.model.MapGenerateRequest;
import com.geonexus.model.MapGenerateResponse;
import com.geonexus.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "地图管理", description = "地图生成、查询与渲染接口")
public class MapController {
    
    private final MapService mapService;
    
    @Operation(summary = "生成地图", description = "根据GeoJSON和配置参数生成交互式地图")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "生成成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/map/generate")
    public ResponseEntity<MapGenerateResponse> generateMap(@RequestBody MapGenerateRequest request) {
        MapGenerateResponse response = mapService.generateMap(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "获取地图信息", description = "根据ID获取地图元数据信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "地图不存在")
    })
    @GetMapping("/map/{mapId}")
    public ResponseEntity<?> getMapInfo(
            @Parameter(description = "地图ID") @PathVariable String mapId) {
        return ResponseEntity.ok(mapService.getMapInfo(mapId));
    }
    
    @Operation(summary = "获取地图HTML", description = "获取地图的HTML渲染页面，支持2D/3D模式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "地图不存在")
    })
    @GetMapping(value = "/map/{mapId}/html", produces = "text/html")
    public ResponseEntity<String> getMapHtml(
            @Parameter(description = "地图ID") @PathVariable String mapId, 
            @Parameter(description = "渲染模式：2d或3d") @RequestParam(defaultValue = "2d") String mode) {
        String html = mapService.getMapHtml(mapId, mode);
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(html);
    }
    
    @Operation(summary = "获取2D地图", description = "获取2D模式地图HTML页面")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "地图不存在")
    })
    @GetMapping("/map/{mapId}/2d")
    public ResponseEntity<String> getMap2D(
            @Parameter(description = "地图ID") @PathVariable String mapId) {
        String html = mapService.getMapHtml(mapId, "2d");
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(html);
    }
    
    @Operation(summary = "获取3D地图", description = "获取3D模式地图HTML页面（Cesium）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "地图不存在")
    })
    @GetMapping("/map/{mapId}/3d")
    public ResponseEntity<String> getMap3D(
            @Parameter(description = "地图ID") @PathVariable String mapId) {
        String html = mapService.getMapHtml(mapId, "3d");
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(html);
    }
}
