package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.MetadataCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 元数据目录控制器 - 元数据目录 REST API (ISO 19115 标准)
 */
@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "元数据目录", description = "元数据目录管理 (ISO 19115)")
@RequiredArgsConstructor
public class MetadataCatalogController {

    private final MetadataCatalogService metadataCatalogService;

    @PostMapping
    @Operation(summary = "创建元数据")
    public ResponseEntity<ApiResponse<MetadataCatalogService.MetadataRecord>> createMetadata(
            @RequestBody MetadataCatalogService.MetadataRecord metadata) {
        MetadataCatalogService.MetadataRecord created = metadataCatalogService.createMetadata(metadata);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取元数据")
    public ResponseEntity<ApiResponse<MetadataCatalogService.MetadataRecord>> getMetadata(@PathVariable String id) {
        MetadataCatalogService.MetadataRecord metadata = metadataCatalogService.getMetadata(id);
        if (metadata == null) {
            return ResponseEntity.ok(ApiResponse.error("元数据不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(metadata));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新元数据")
    public ResponseEntity<ApiResponse<MetadataCatalogService.MetadataRecord>> updateMetadata(
            @PathVariable String id,
            @RequestBody MetadataCatalogService.MetadataRecord updates) {
        try {
            MetadataCatalogService.MetadataRecord updated = metadataCatalogService.updateMetadata(id, updates);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (MetadataCatalogService.MetadataNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除元数据")
    public ResponseEntity<ApiResponse<Void>> deleteMetadata(@PathVariable String id) {
        metadataCatalogService.deleteMetadata(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/search")
    @Operation(summary = "搜索元数据")
    public ResponseEntity<ApiResponse<List<MetadataCatalogService.MetadataRecord>>> searchMetadata(
            @RequestBody MetadataCatalogService.MetadataSearchQuery query) {
        List<MetadataCatalogService.MetadataRecord> results = metadataCatalogService.search(query);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/export/iso19115/{id}")
    @Operation(summary = "导出为ISO 19115 XML")
    public ResponseEntity<ApiResponse<String>> exportToISO19115(@PathVariable String id) {
        try {
            String xml = metadataCatalogService.exportToISO19115(id);
            return ResponseEntity.ok(ApiResponse.success(xml));
        } catch (MetadataCatalogService.MetadataNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "获取分类列表")
    public ResponseEntity<ApiResponse<List<MetadataCatalogService.Category>>> getCategories() {
        List<MetadataCatalogService.Category> categories = metadataCatalogService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping("/categories")
    @Operation(summary = "创建分类")
    public ResponseEntity<ApiResponse<MetadataCatalogService.Category>> createCategory(
            @RequestBody MetadataCatalogService.Category category) {
        MetadataCatalogService.Category created = metadataCatalogService.createCategory(category);
        return ResponseEntity.ok(ApiResponse.success(created));
    }
}
