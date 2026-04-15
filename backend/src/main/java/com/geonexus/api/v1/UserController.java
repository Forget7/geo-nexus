package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器 - 包装 UserManagementService
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户信息/角色/权限")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户信息")
    public ResponseEntity<ApiResponse<UserManagementService.User>> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(userId)));
    }

    @GetMapping("/{userId}/public")
    @Operation(summary = "获取用户公开信息")
    public ResponseEntity<ApiResponse<UserManagementService.UserDTO>> getUserPublicInfo(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserPublicInfo(userId)));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "更新用户信息")
    public ResponseEntity<ApiResponse<UserManagementService.User>> updateUser(
            @PathVariable String userId, @RequestBody UserManagementService.UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userId, request)));
    }

    @PostMapping("/{userId}/password")
    @Operation(summary = "修改密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable String userId, @RequestBody Map<String, String> body) {
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{userId}/password/reset")
    @Operation(summary = "重置密码")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable String userId, @RequestBody Map<String, String> body) {
        userService.resetPassword(userId, body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "设置用户状态")
    public ResponseEntity<ApiResponse<Void>> setUserStatus(
            @PathVariable String userId, @RequestBody Map<String, String> body) {
        userService.setUserStatus(userId, body.get("status"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "分配角色")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable String userId, @PathVariable String roleId) {
        userService.assignRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "移除角色")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable String userId, @PathVariable String roleId) {
        userService.removeRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{userId}/permissions/{permission}")
    @Operation(summary = "检查权限")
    public ResponseEntity<ApiResponse<Boolean>> hasPermission(
            @PathVariable String userId, @PathVariable String permission) {
        return ResponseEntity.ok(ApiResponse.success(userService.hasPermission(userId, permission)));
    }
}
