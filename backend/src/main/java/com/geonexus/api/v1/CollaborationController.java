package com.geonexus.api.v1;

import com.geonexus.service.CollaborationService;
import com.geonexus.service.CollaborationService.*;
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
import java.util.Set;

/**
 * 协作系统 REST API
 * 暴露 CollaborationService 的所有协作能力给前端
 */
@RestController
@RequestMapping("/api/v1/collab")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "协作", description = "协作文档、锁定、版本历史、评论与标注接口")
public class CollaborationController {

    private final CollaborationService collaborationService;

    // ==================== 协作文档 ====================

    @Operation(summary = "创建协作文档", description = "创建一个新的协作文档，初始化版本为1")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/documents")
    public ResponseEntity<CollaborationDoc> createDocument(@RequestBody CollaborationDoc doc) {
        return ResponseEntity.ok(collaborationService.createDocument(doc));
    }

    @Operation(summary = "获取协作文档", description = "根据文档ID获取协作文档详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    @GetMapping("/documents/{docId}")
    public ResponseEntity<CollaborationDoc> getDocument(
            @Parameter(description = "文档ID") @PathVariable String docId) {
        return ResponseEntity.ok(collaborationService.getDocument(docId));
    }

    @Operation(summary = "更新协作文档", description = "更新文档内容/标题，支持乐观锁版本检查")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "409", description = "版本冲突"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PutMapping("/documents/{docId}")
    public ResponseEntity<CollaborationDoc> updateDocument(
            @Parameter(description = "文档ID") @PathVariable String docId,
            @Parameter(description = "用户ID") @RequestParam String userId,
            @RequestBody UpdateRequest update) {
        return ResponseEntity.ok(collaborationService.updateDocument(docId, userId, update));
    }

    // ==================== 锁定管理 ====================

    @Operation(summary = "锁定文档", description = "锁定文档进行编辑，30分钟自动释放")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "锁定成功"),
        @ApiResponse(responseCode = "409", description = "文档已被锁定")
    })
    @PostMapping("/documents/{docId}/lock")
    public ResponseEntity<LockInfo> lockDocument(
            @Parameter(description = "文档ID") @PathVariable String docId,
            @Parameter(description = "用户ID") @RequestParam String userId) {
        return ResponseEntity.ok(collaborationService.lockDocument(docId, userId));
    }

    @Operation(summary = "解锁文档", description = "释放文档编辑锁，仅锁定者可以解锁")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "解锁成功"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @DeleteMapping("/documents/{docId}/lock")
    public ResponseEntity<Void> unlockDocument(
            @Parameter(description = "文档ID") @PathVariable String docId,
            @Parameter(description = "用户ID") @RequestParam String userId) {
        collaborationService.unlockDocument(docId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "获取当前锁定信息", description = "查询文档当前的锁定状态")
    @GetMapping("/documents/{docId}/lock")
    public ResponseEntity<LockInfo> getLockInfo(
            @Parameter(description = "文档ID") @PathVariable String docId) {
        CollaborationDoc doc = collaborationService.getDocument(docId);
        return ResponseEntity.ok(doc.getLockInfo());
    }

    // ==================== 参与者管理 ====================

    @Operation(summary = "添加参与者", description = "将用户添加到文档协作参与者列表")
    @PostMapping("/documents/{docId}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable String docId,
            @RequestParam String userId) {
        collaborationService.addParticipant(docId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "移除参与者", description = "将用户从文档协作参与者列表移除")
    @DeleteMapping("/documents/{docId}/participants")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable String docId,
            @RequestParam String userId) {
        collaborationService.removeParticipant(docId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "获取在线参与者", description = "获取当前在线的协作用户ID列表")
    @GetMapping("/documents/{docId}/participants")
    public ResponseEntity<Set<String>> getOnlineParticipants(
            @PathVariable String docId) {
        return ResponseEntity.ok(collaborationService.getOnlineParticipants(docId));
    }

    @Operation(summary = "检查编辑权限", description = "检查用户是否有权编辑文档")
    @GetMapping("/documents/{docId}/can-edit")
    public ResponseEntity<Map<String, Boolean>> canEdit(
            @PathVariable String docId,
            @RequestParam String userId) {
        return ResponseEntity.ok(Map.of("canEdit", collaborationService.canEdit(docId, userId)));
    }

    // ==================== 实时评论 ====================

    @Operation(summary = "添加评论", description = "向文档添加一条评论，可指定父评论ID实现回复")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "评论添加成功"),
        @ApiResponse(responseCode = "404", description = "文档不存在")
    })
    @PostMapping("/documents/{docId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable String docId,
            @RequestParam String userId,
            @RequestParam(required = false) String parentId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        return ResponseEntity.ok(collaborationService.addComment(docId, userId, content, parentId));
    }

    @Operation(summary = "回复评论", description = "对指定评论发起回复")
    @PostMapping("/documents/{docId}/comments/{commentId}/replies")
    public ResponseEntity<Comment> replyToComment(
            @PathVariable String docId,
            @PathVariable String commentId,
            @RequestParam String userId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        return ResponseEntity.ok(collaborationService.replyToComment(docId, commentId, userId, content));
    }

    @Operation(summary = "获取评论列表", description = "获取文档下的所有评论（含回复）")
    @GetMapping("/documents/{docId}/comments")
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable String docId) {
        return ResponseEntity.ok(collaborationService.getComments(docId));
    }

    // ==================== 地图标注 ====================

    @Operation(summary = "添加地图标注", description = "在文档地图上添加点/线/面/文字标注")
    @PostMapping("/documents/{docId}/annotations")
    public ResponseEntity<Annotation> addAnnotation(
            @PathVariable String docId,
            @RequestParam String userId,
            @RequestBody Annotation annotation) {
        return ResponseEntity.ok(collaborationService.addAnnotation(docId, userId, annotation));
    }

    @Operation(summary = "获取地图标注", description = "获取文档地图上的所有标注")
    @GetMapping("/documents/{docId}/annotations")
    public ResponseEntity<List<Annotation>> getAnnotations(
            @PathVariable String docId) {
        return ResponseEntity.ok(collaborationService.getAnnotations(docId));
    }

    @Operation(summary = "删除地图标注", description = "删除指定的地图标注，仅标注创建者可删除")
    @DeleteMapping("/documents/{docId}/annotations/{annotationId}")
    public ResponseEntity<Void> deleteAnnotation(
            @PathVariable String docId,
            @PathVariable String annotationId,
            @RequestParam String userId) {
        collaborationService.deleteAnnotation(docId, annotationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 版本历史 ====================

    @Operation(summary = "获取版本历史", description = "获取文档的所有更新历史记录")
    @GetMapping("/documents/{docId}/versions")
    public ResponseEntity<List<DocUpdate>> getVersionHistory(
            @PathVariable String docId) {
        return ResponseEntity.ok(collaborationService.getVersionHistory(docId));
    }

    @Operation(summary = "回滚到指定版本", description = "将文档回滚到指定历史版本")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "回滚成功"),
        @ApiResponse(responseCode = "404", description = "版本不存在"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PostMapping("/documents/{docId}/rollback")
    public ResponseEntity<CollaborationDoc> rollbackToVersion(
            @PathVariable String docId,
            @RequestParam String userId,
            @RequestParam Long version) {
        return ResponseEntity.ok(collaborationService.rollbackToVersion(docId, userId, version));
    }

    // ==================== 异常全局处理 ====================

    @ExceptionHandler(CollaborationService.DocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDocumentNotFound(DocumentNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CollaborationService.PermissionDeniedException.class)
    public ResponseEntity<Map<String, String>> handlePermissionDenied(PermissionDeniedException e) {
        return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CollaborationService.VersionConflictException.class)
    public ResponseEntity<Map<String, String>> handleVersionConflict(VersionConflictException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CollaborationService.DocumentLockedException.class)
    public ResponseEntity<Map<String, String>> handleDocumentLocked(DocumentLockedException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CollaborationService.VersionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleVersionNotFound(VersionNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
}
