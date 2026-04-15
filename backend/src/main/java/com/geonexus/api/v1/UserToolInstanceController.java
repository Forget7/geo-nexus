package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.domain.tool.ToolInstance;
import com.geonexus.service.UserToolInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户工具实例控制器
 * 职责：用户工具实例的 CRUD 管理
 */
@RestController
@RequestMapping("/api/v1/user-tools")
@Tag(name = "用户工具实例", description = "用户工具实例的创建、查询、更新与删除")
@RequiredArgsConstructor
public class UserToolInstanceController {

    private final UserToolInstanceService toolInstanceService;

    @PostMapping
    @Operation(summary = "创建工具实例", description = "为用户创建一个工具实例")
    public ResponseEntity<ApiResponse<ToolInstance>> createInstance(
            @RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        String toolId = (String) request.get("toolId");
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) request.get("config");

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("userId is required"));
        }
        if (toolId == null || toolId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("toolId is required"));
        }
        if (config == null) {
            config = Map.of();
        }

        try {
            ToolInstance instance = toolInstanceService.createUserToolInstance(userId, toolId, config);
            return ResponseEntity.ok(ApiResponse.success(instance, "Tool instance created"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户工具实例列表", description = "获取指定用户的所有工具实例")
    public ResponseEntity<ApiResponse<List<ToolInstance>>> getUserInstances(
            @PathVariable String userId) {
        List<ToolInstance> instances = toolInstanceService.getUserToolInstances(userId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @PatchMapping("/{instanceId}")
    @Operation(summary = "更新工具实例", description = "更新指定工具实例的配置")
    public ResponseEntity<ApiResponse<ToolInstance>> updateInstance(
            @PathVariable String instanceId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) request.get("config");
        try {
            ToolInstance instance = toolInstanceService.updateToolInstance(instanceId, config);
            return ResponseEntity.ok(ApiResponse.success(instance, "Tool instance updated"));
        } catch (UserToolInstanceService.ToolInstanceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{instanceId}")
    @Operation(summary = "删除工具实例", description = "删除指定的工具实例")
    public ResponseEntity<ApiResponse<Void>> deleteInstance(@PathVariable String instanceId) {
        toolInstanceService.deleteToolInstance(instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tool instance deleted"));
    }

    @GetMapping("/{instanceId}")
    @Operation(summary = "获取工具实例", description = "根据ID获取工具实例详情")
    public ResponseEntity<ApiResponse<ToolInstance>> getInstance(@PathVariable String instanceId) {
        ToolInstance instance = toolInstanceService.findInstanceById(instanceId);
        if (instance == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Tool instance not found: " + instanceId));
        }
        return ResponseEntity.ok(ApiResponse.success(instance));
    }
}