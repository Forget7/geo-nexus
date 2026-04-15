package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.GeoServerWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GeoServer Workspace 管理控制器
 * 职责：Workspace CRUD
 */
@RestController
@RequestMapping("/api/v1/geoserver/workspaces")
@Tag(name = "GeoServer工作区", description = "GeoServer工作区管理")
@RequiredArgsConstructor
public class GeoServerWorkspaceController {

    private final GeoServerWorkspaceService workspaceService;

    @PostMapping
    @Operation(summary = "创建Workspace", description = "在GeoServer中创建一个新的工作区")
    public ResponseEntity<ApiResponse<Boolean>> createWorkspace(
            @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("name is required"));
        }
        boolean success = workspaceService.createWorkspace(name);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(true, "Workspace created: " + name));
        } else {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create workspace: " + name));
        }
    }

    @GetMapping
    @Operation(summary = "获取所有Workspace", description = "列出GeoServer中所有工作区")
    public ResponseEntity<ApiResponse<List<String>>> listWorkspaces() {
        List<String> workspaces = workspaceService.getWorkspaces();
        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }
}