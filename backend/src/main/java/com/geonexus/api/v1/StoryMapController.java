package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.domain.StoryMapEntity;
import com.geonexus.service.StoryMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 叙事地图控制器
 */
@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "叙事地图", description = "故事地图管理")
public class StoryMapController {

    private final StoryMapService storyMapService;

    @PostMapping
    @Operation(summary = "创建叙事地图")
    public ResponseEntity<ApiResponse<StoryMapEntity>> create(
            @RequestBody StoryMapEntity story,
            @RequestParam String userId) {
        story.setAuthorId(userId);
        StoryMapEntity created = storyMapService.create(story);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新叙事地图")
    public ResponseEntity<ApiResponse<StoryMapEntity>> update(
            @PathVariable String id,
            @RequestBody StoryMapEntity updates) {
        StoryMapEntity updated = storyMapService.update(id, updates);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除叙事地图")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        storyMapService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取叙事地图详情")
    public ResponseEntity<ApiResponse<StoryMapEntity>> getById(@PathVariable String id) {
        StoryMapEntity story = storyMapService.getById(id);
        if (story == null) return ResponseEntity.status(404)
                .body(ApiResponse.error("不存在"));
        return ResponseEntity.ok(ApiResponse.success(story));
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的叙事地图")
    public ResponseEntity<ApiResponse<List<StoryMapEntity>>> getMyStories(
            @RequestParam String userId) {
        return ResponseEntity.ok(ApiResponse.success(storyMapService.getByAuthor(userId)));
    }

    @GetMapping("/published")
    @Operation(summary = "获取已发布列表")
    public ResponseEntity<ApiResponse<List<StoryMapEntity>>> getPublished() {
        return ResponseEntity.ok(ApiResponse.success(storyMapService.getPublished()));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布叙事地图")
    public ResponseEntity<ApiResponse<StoryMapEntity>> publish(@PathVariable String id) {
        StoryMapEntity story = storyMapService.publish(id);
        return ResponseEntity.ok(ApiResponse.success(story));
    }

    @GetMapping("/shared/{token}")
    @Operation(summary = "通过分享码获取（公开访问）")
    public ResponseEntity<ApiResponse<StoryMapEntity>> getByShareToken(@PathVariable String token) {
        StoryMapEntity story = storyMapService.getByShareToken(token);
        if (story == null) return ResponseEntity.status(404)
                .body(ApiResponse.error("分享内容不存在或已下架"));
        return ResponseEntity.ok(ApiResponse.success(story));
    }
}
