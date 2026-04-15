package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器 - 暴露知识库与知识图谱服务
 */
@RestController
@RequestMapping("/api/v1/knowledge")
@Tag(name = "知识库", description = "GIS知识库与知识图谱管理")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    // ==================== 知识库管理 ====================

    @PostMapping("/entries")
    @Operation(summary = "添加知识条目")
    public ResponseEntity<ApiResponse<KnowledgeBaseService.KnowledgeEntry>> addEntry(
            @RequestBody KnowledgeBaseService.KnowledgeEntry entry) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.addEntry(entry)));
    }

    @GetMapping("/entries/{id}")
    @Operation(summary = "获取知识条目")
    public ResponseEntity<ApiResponse<KnowledgeBaseService.KnowledgeEntry>> getEntry(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.getEntry(id)));
    }

    @PutMapping("/entries/{id}")
    @Operation(summary = "更新知识条目")
    public ResponseEntity<ApiResponse<KnowledgeBaseService.KnowledgeEntry>> updateEntry(
            @PathVariable String id, @RequestBody KnowledgeBaseService.KnowledgeEntry entry) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.updateEntry(id, entry)));
    }

    @DeleteMapping("/entries/{id}")
    @Operation(summary = "删除知识条目")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable String id) {
        knowledgeBaseService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/search")
    @Operation(summary = "搜索知识")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseService.KnowledgeEntry>>> search(
            @RequestBody KnowledgeBaseService.SearchQuery query) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.search(query)));
    }

    // ==================== 知识图谱管理 ====================

    @PostMapping("/kg/entities")
    @Operation(summary = "添加实体")
    public ResponseEntity<ApiResponse<KnowledgeBaseService.KGEntity>> addEntity(
            @RequestBody KnowledgeBaseService.KGEntity entity) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.addEntity(entity)));
    }

    @PostMapping("/kg/relations")
    @Operation(summary = "添加关系")
    public ResponseEntity<ApiResponse<KnowledgeBaseService.KGRelation>> addRelation(
            @RequestBody KnowledgeBaseService.KGRelation relation) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.addRelation(relation)));
    }

    @GetMapping("/kg/entities/{id}/relations")
    @Operation(summary = "查询实体关系")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseService.KGRelation>>> getEntityRelations(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.getEntityRelations(id)));
    }

    @PostMapping("/kg/link")
    @Operation(summary = "实体链接")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseService.EntityLink>>> linkEntities(
            @RequestParam String text,
            @RequestParam(required = false) String entityType) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.linkEntities(text, entityType)));
    }

    @PostMapping("/kg/infer")
    @Operation(summary = "知识推理")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseService.InferenceResult>>> infer(
            @RequestParam String sourceEntityId,
            @RequestParam String targetType,
            @RequestParam(defaultValue = "3") int depth) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeBaseService.infer(sourceEntityId, targetType, depth)));
    }
}