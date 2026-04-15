package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.domain.NotificationEntity;
import com.geonexus.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "通知中心", description = "用户通知管理")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取通知列表")
    public ResponseEntity<ApiResponse<Page<NotificationEntity>>> getNotifications(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            notificationService.getUserNotifications(userId, page, size)));
    }

    @GetMapping("/unread")
    @Operation(summary = "获取未读通知")
    public ResponseEntity<ApiResponse<Page<NotificationEntity>>> getUnread(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            notificationService.getUnread(userId, page, size)));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "未读数量")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@RequestParam String userId) {
        return ResponseEntity.ok(ApiResponse.success(
            notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "标记已读")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@RequestParam String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/all")
    @Operation(summary = "清空所有通知")
    public ResponseEntity<ApiResponse<Void>> deleteAll(@RequestParam String userId) {
        notificationService.deleteAllForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
