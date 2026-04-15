package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.GeoServerStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GeoServer 样式 API
 */
@RestController
@RequestMapping("/api/v1/geoserver/styles")
@Tag(name = "GeoServer样式", description = "地图样式管理与发布")
@RequiredArgsConstructor
public class GeoServerStyleController {

    private final GeoServerStyleService styleService;

    @GetMapping
    @Operation(summary = "列出样式")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listStyles() {
        return ResponseEntity.ok(ApiResponse.success(styleService.listStyles()));
    }

    @GetMapping("/{workspace}/{styleName}")
    @Operation(summary = "获取样式")
    public ResponseEntity<ApiResponse<String>> getStyle(
            @PathVariable String workspace, @PathVariable String styleName) {
        return ResponseEntity.ok(ApiResponse.success(styleService.getStyle(workspace, styleName)));
    }

    @PostMapping
    @Operation(summary = "创建样式")
    public ResponseEntity<ApiResponse<Void>> createStyle(
            @RequestBody Map<String, Object> body) {
        String workspace = (String) body.get("workspace");
        String name = (String) body.get("name");
        String sld = (String) body.get("sld");
        styleService.createStyle(workspace, name, sld);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{workspace}/{styleName}")
    @Operation(summary = "更新样式")
    public ResponseEntity<ApiResponse<Void>> updateStyle(
            @PathVariable String workspace, @PathVariable String styleName,
            @RequestBody Map<String, Object> body) {
        styleService.updateStyle(workspace, styleName, (String) body.get("sld"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{workspace}/{styleName}")
    @Operation(summary = "删除样式")
    public ResponseEntity<ApiResponse<Void>> deleteStyle(
            @PathVariable String workspace, @PathVariable String styleName) {
        styleService.deleteStyle(workspace, styleName);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/templates")
    @Operation(summary = "获取样式模板")
    public ResponseEntity<ApiResponse<List<String>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(styleService.getTemplates()));
    }
}
