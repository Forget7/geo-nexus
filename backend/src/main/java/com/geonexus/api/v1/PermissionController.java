package com.geonexus.api.v1;

import com.geonexus.domain.ResourcePermission;
import com.geonexus.domain.ResourcePermission.Permission;
import com.geonexus.service.ResourcePermissionService;
import com.geonexus.service.ResourcePermissionService.ResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源权限控制器
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "权限管理", description = "GIS资源权限授予、撤销与查询接口")
public class PermissionController {

    private final ResourcePermissionService permissionService;
    private final com.geonexus.api.config.SecurityConfig securityConfig;

    /**
     * 获取资源的权限列表
     * GET /api/v1/permissions/{type}/{id}
     */
    @Operation(summary = "获取资源权限", description = "获取指定资源的权限列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/permissions/{type}/{id}")
    public ResponseEntity<List<ResourcePermission>> getResourcePermissions(
            @Parameter(description = "资源类型：LAYER/DATA/MAP等") @PathVariable String type,
            @Parameter(description = "资源ID") @PathVariable String id) {
        ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        List<ResourcePermission> permissions = permissionService.getResourcePermissions(resourceType, id);
        return ResponseEntity.ok(permissions);
    }

    /**
     * 授予权限
     * POST /api/v1/permissions
     * Body: { "userId": "...", "resourceType": "LAYER", "resourceId": "...", "permission": "READ" }
     */
    @Operation(summary = "授予权限", description = "向指定用户授予资源的访问权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "授权成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/permissions")
    public ResponseEntity<ResourcePermission> grantPermission(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        ResourceType resourceType = ResourceType.valueOf(request.get("resourceType").toUpperCase());
        String resourceId = request.get("resourceId");
        Permission permission = Permission.valueOf(request.get("permission").toUpperCase());

        ResourcePermission granted = permissionService.grantPermission(userId, resourceType, resourceId, permission);
        return ResponseEntity.ok(granted);
    }

    /**
     * 撤销权限
     * DELETE /api/v1/permissions/{id}
     */
    @Operation(summary = "撤销权限", description = "根据权限ID撤销指定用户的资源权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "撤销成功"),
        @ApiResponse(responseCode = "404", description = "权限不存在")
    })
    @DeleteMapping("/permissions/{id}")
    public ResponseEntity<Map<String, Object>> revokePermission(
            @Parameter(description = "权限记录ID") @PathVariable String id) {
        boolean removed = permissionService.revokeById(id);
        if (removed) {
            return ResponseEntity.ok(Map.of("success", true, "message", "权限已撤销"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取当前用户的资源权限
     * GET /api/v1/permissions/user/me
     * Header: Authorization: Bearer <token>
     */
    @Operation(summary = "获取我的权限", description = "获取当前认证用户的所有资源权限列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/permissions/user/me")
    public ResponseEntity<List<ResourcePermission>> getMyPermissions(
            HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<ResourcePermission> permissions = permissionService.getCurrentUserPermissions(userId);
        return ResponseEntity.ok(permissions);
    }

    // ── 内部工具 ─────────────────────────────────────

    /**
     * 从Authorization header获取当前用户ID（从JWT token解析）
     * 不再信任 X-User-Id 请求头（可被伪造）
     */
    private String getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null; // 未认证返回null，由调用方决定如何处理
        }
        try {
            return securityConfig.extractUserIdFromAuth(authHeader);
        } catch (Exception e) {
            return null;
        }
    }
}
