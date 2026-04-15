package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.MapPublishingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publish")
@Tag(name = "地图发布", description = "地图分享与权限管理")
@RequiredArgsConstructor
public class PublishController {

    private final MapPublishingService publishService;

    @PostMapping
    @Operation(summary = "发布地图")
    public ResponseEntity<ApiResponse<MapPublishingService.MapPublishResult>> publish(
            @RequestBody MapPublishingService.MapPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(publishService.publish(request)));
    }

    @GetMapping("/{shareId}")
    @Operation(summary = "获取分享地图")
    public ResponseEntity<ApiResponse<MapPublishingService.MapShareConfig>> getSharedMap(
            @PathVariable String shareId,
            @RequestParam(required = false) String accessToken) {
        return ResponseEntity.ok(ApiResponse.success(
            publishService.getSharedMap(shareId, accessToken)));
    }

    @PutMapping("/{shareId}")
    @Operation(summary = "更新分享设置")
    public ResponseEntity<ApiResponse<MapPublishingService.MapPublishResult>> updateShare(
            @PathVariable String shareId,
            @RequestBody MapPublishingService.MapPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(publishService.updateShare(shareId, request)));
    }

    @DeleteMapping("/{shareId}")
    @Operation(summary = "取消发布")
    public ResponseEntity<ApiResponse<Void>> unpublish(
            @PathVariable String shareId,
            @RequestParam String accessToken) {
        publishService.unpublish(shareId, accessToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
