package com.geonexus.api.v1;

import com.geonexus.model.MapGenerateResponse;
import com.geonexus.model.dto.MapTemplateDTO;
import com.geonexus.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地图模板控制器
 */
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "地图模板", description = "地图模板市场")
@RequiredArgsConstructor
public class MapTemplateController {

    private final Map<String, MapTemplateDTO> templates;
    private final MapService mapService;

    @GetMapping
    @Operation(summary = "获取所有模板", description = "返回模板市场所有可用模板")
    public ResponseEntity<List<MapTemplateDTO>> getAllTemplates(
            @RequestParam(required = false) String category) {
        List<MapTemplateDTO> list = templates.values().stream()
                .filter(t -> category == null || category.equals(t.getCategory()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    public ResponseEntity<MapTemplateDTO> getTemplate(@PathVariable String id) {
        MapTemplateDTO t = templates.get(id);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(t);
    }

    @PostMapping("/{id}/apply")
    @Operation(summary = "应用模板创建地图", description = "基于模板创建一个新的地图文档")
    public ResponseEntity<MapGenerateResponse> applyTemplate(
            @PathVariable String id,
            @RequestParam String userId) {
        MapTemplateDTO t = templates.get(id);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }
        MapGenerateResponse response = mapService.createFromTemplate(t, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取所有分类")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        List<Map<String, Object>> categories = List.of(
                Map.of("id", "government", "name", "政务"),
                Map.of("id", "emergency", "name", "应急"),
                Map.of("id", "traffic", "name", "交通"),
                Map.of("id", "environment", "name", "环境"),
                Map.of("id", "population", "name", "人口")
        );
        return ResponseEntity.ok(categories);
    }
}
