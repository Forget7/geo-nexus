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
 * 用户认证控制器 - 包装 UserManagementService
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "用户认证", description = "注册/登录/Token管理")
@RequiredArgsConstructor
public class AuthController {

    private final UserManagementService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ResponseEntity<ApiResponse<UserManagementService.User>> register(
            @RequestBody UserManagementService.RegisterRequest request) {
        UserManagementService.User user = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ApiResponse<UserManagementService.LoginResult>> login(
            @RequestBody UserManagementService.LoginRequest request) {
        UserManagementService.LoginResult result = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public ResponseEntity<ApiResponse<UserManagementService.LoginResult>> refresh(
            @RequestBody Map<String, String> body) {
        UserManagementService.LoginResult result = userService.refreshToken(body.get("refreshToken"));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody Map<String, String> body) {
        userService.logout(body.get("accessToken"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/validate")
    @Operation(summary = "验证Token")
    public ResponseEntity<ApiResponse<UserManagementService.User>> validate(
            @RequestHeader("Authorization") String auth) {
        String token = auth.replace("Bearer ", "");
        UserManagementService.User user = userService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
