package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.domain.PluginInstanceEntity;
import com.geonexus.domain.PluginManifestEntity;
import com.geonexus.service.PluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件控制器 - 插件市场与管理
 */
@RestController
@RequestMapping("/api/v1/plugins")
@Tag(name = "插件系统", description = "GeoNexus 插件市场与管理")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
public class PluginController {

    private final PluginService pluginService;

    @PostMapping("/register")
    @Operation(summary = "注册插件", description = "提交插件manifest，审核后上架")
    public ResponseEntity<ApiResponse<PluginManifestEntity>> register(
            @RequestBody PluginManifestEntity manifest) {
        PluginManifestEntity registered = pluginService.registerPlugin(manifest);
        return ResponseEntity.ok(ApiResponse.success(registered));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索插件")
    public ResponseEntity<ApiResponse<List<PluginManifestEntity>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(ApiResponse.success(
            pluginService.searchPlugins(q, category, tag)));
    }

    @GetMapping("/{pluginId}")
    @Operation(summary = "获取插件详情")
    public ResponseEntity<ApiResponse<PluginManifestEntity>> getById(@PathVariable String pluginId) {
        PluginManifestEntity p = pluginService.getByPluginId(pluginId);
        if (p == null) return ResponseEntity.status(404)
            .body(ApiResponse.error("Plugin not found"));
        return ResponseEntity.ok(ApiResponse.success(p));
    }

    @PostMapping("/{pluginId}/install")
    @Operation(summary = "安装插件")
    public ResponseEntity<ApiResponse<PluginInstanceEntity>> install(
            @PathVariable String pluginId,
            @RequestParam String userId,
            @RequestBody(required = false) Map<String, Object> config) {
        try {
            PluginInstanceEntity inst = pluginService.installPlugin(pluginId, userId, config);
            return ResponseEntity.ok(ApiResponse.success(inst));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{pluginId}/install")
    @Operation(summary = "卸载插件")
    public ResponseEntity<ApiResponse<Void>> uninstall(
            @PathVariable String pluginId,
            @RequestParam String userId) {
        pluginService.uninstallPlugin(pluginId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{pluginId}/enabled")
    @Operation(summary = "启用/禁用插件")
    public ResponseEntity<ApiResponse<Void>> setEnabled(
            @PathVariable String pluginId,
            @RequestParam String userId,
            @RequestParam boolean enabled) {
        pluginService.setPluginEnabled(pluginId, userId, enabled);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的已安装插件")
    public ResponseEntity<ApiResponse<List<PluginInstanceEntity>>> getMyPlugins(
            @RequestParam String userId) {
        return ResponseEntity.ok(ApiResponse.success(pluginService.getUserPlugins(userId)));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取插件分类")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
            "visualization", "ai", "data", "utility", "security", "export"
        )));
    }
}
