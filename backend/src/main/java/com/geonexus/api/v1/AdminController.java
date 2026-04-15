package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器 - 系统统计、用户管理、数据集管理、审计日志
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "管理员", description = "系统管理与监控")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "系统统计", description = "获取系统全局统计数据")
    public ResponseEntity<ApiResponse<AdminService.SystemStats>> getSystemStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSystemStats()));
    }

    @GetMapping("/users")
    @Operation(summary = "用户列表", description = "分页获取所有用户")
    public ResponseEntity<ApiResponse<List<AdminService.UserSummary>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listUsers(page, size)));
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "设置用户角色")
    public ResponseEntity<ApiResponse<Void>> setUserRole(
            @PathVariable String userId,
            @RequestParam String role) {
        adminService.setUserRole(userId, role);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/users/{userId}/disable")
    @Operation(summary = "禁用用户")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable String userId) {
        adminService.disableUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/users/{userId}/enable")
    @Operation(summary = "启用用户")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable String userId) {
        adminService.enableUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/datasets")
    @Operation(summary = "数据集统计")
    public ResponseEntity<ApiResponse<List<AdminService.DatasetStats>>> getDatasetStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDatasetStats()));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "操作日志")
    public ResponseEntity<ApiResponse<List<AdminService.AuditLogEntry>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getAuditLogs(page, size, level, userId)));
    }
}
