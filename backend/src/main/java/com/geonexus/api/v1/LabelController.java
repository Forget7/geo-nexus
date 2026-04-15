package com.geonexus.api.v1;

import com.geonexus.service.LabelPlacementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/labels")
@Tag(name = "标签布局", description = "智能标签规则与位置计算")
@RequiredArgsConstructor
public class LabelController {

    private final LabelPlacementService labelService;

    // ===== Rules =====

    @PostMapping("/rules")
    @Operation(summary = "创建标签规则")
    public ResponseEntity<ApiResponse<LabelPlacementService.LabelRule>> createRule(
            @RequestBody LabelPlacementService.LabelRule rule) {
        return ResponseEntity.ok(ApiResponse.success(labelService.createRule(rule)));
    }

    @GetMapping("/rules")
    @Operation(summary = "列出所有标签规则")
    public ResponseEntity<ApiResponse<List<LabelPlacementService.LabelRule>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success(labelService.getAllRules()));
    }

    @GetMapping("/rules/{id}")
    @Operation(summary = "获取标签规则")
    public ResponseEntity<ApiResponse<LabelPlacementService.LabelRule>> getRule(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(labelService.getRule(id)));
    }

    @GetMapping("/rules/layer/{layerType}")
    @Operation(summary = "按图层类型获取规则")
    public ResponseEntity<ApiResponse<LabelPlacementService.LabelRule>> getRuleByLayerType(@PathVariable String layerType) {
        return ResponseEntity.ok(ApiResponse.success(labelService.getRuleByLayerType(layerType)));
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除标签规则")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable String id) {
        labelService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== Label Calculation =====

    @PostMapping("/calculate")
    @Operation(summary = "计算标签位置")
    public ResponseEntity<ApiResponse<List<LabelPlacementService.PlacedLabel>>> calculateLabels(
            @RequestBody LabelPlacementService.LabelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(labelService.calculateLabels(request)));
    }

    @PostMapping("/calculate/batch")
    @Operation(summary = "批量计算标签位置")
    public ResponseEntity<ApiResponse<Map<String, List<LabelPlacementService.PlacedLabel>>>> batchCalculateLabels(
            @RequestBody List<LabelPlacementService.LabelRequest> requests) {
        return ResponseEntity.ok(ApiResponse.success(labelService.batchCalculateLabels(requests)));
    }

    // ===== Label Editing =====

    @PutMapping("/{id}")
    @Operation(summary = "更新已放置标签")
    public ResponseEntity<ApiResponse<LabelPlacementService.PlacedLabel>> updateLabel(
            @PathVariable String id, @RequestBody LabelPlacementService.PlacedLabel updates) {
        return ResponseEntity.ok(ApiResponse.success(labelService.updateLabel(id, updates)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除已放置标签")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(@PathVariable String id) {
        labelService.deleteLabel(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== Export =====

    @PostMapping("/export")
    @Operation(summary = "导出标签", description = "支持 GeoJSON / KML / CSV 格式")
    public ResponseEntity<String> exportLabels(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<LabelPlacementService.PlacedLabel> labels = (List<LabelPlacementService.PlacedLabel>) body.get("labels");
        String format = (String) body.getOrDefault("format", "geojson");
        String exported = labelService.exportLabels(labels, format);
        return ResponseEntity.ok(exported);
    }
}
