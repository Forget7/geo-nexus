package com.geonexus.api.v1;

import com.geonexus.domain.tool.ToolDefinition;
import com.geonexus.service.ToolRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工具注册与目录控制器
 */
@Tag(name = "Tool Registry", description = "工具注册、发现与管理")
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolRegistryController {

    private final ToolRegistryService toolRegistryService;

    @GetMapping
    @Operation(summary = "获取所有工具", description = "获取所有已注册的工具列表，支持按分类过滤")
    public ResponseEntity<List<ToolDefinition>> getAllTools(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(toolRegistryService.getToolsByCategory(category));
        }
        return ResponseEntity.ok(toolRegistryService.getAllTools());
    }

    @GetMapping("/{toolId}")
    @Operation(summary = "获取工具详情")
    public ResponseEntity<ToolDefinition> getTool(@PathVariable String toolId) {
        try {
            return ResponseEntity.ok(toolRegistryService.getTool(toolId));
        } catch (ToolRegistryService.ToolNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    @Operation(summary = "搜索工具", description = "按关键词搜索工具名称、描述和标签")
    public ResponseEntity<List<ToolDefinition>> searchTools(@RequestParam String keyword) {
        return ResponseEntity.ok(toolRegistryService.searchTools(keyword));
    }

    @PostMapping
    @Operation(summary = "注册工具", description = "注册一个新工具")
    public ResponseEntity<ToolDefinition> registerTool(@RequestBody ToolDefinition tool) {
        ToolDefinition registered = toolRegistryService.registerTool(tool);
        return ResponseEntity.ok(registered);
    }

    @PutMapping("/{toolId}")
    @Operation(summary = "更新工具", description = "更新已注册的工具（内置工具不可修改）")
    public ResponseEntity<?> updateTool(@PathVariable String toolId, @RequestBody ToolDefinition tool) {
        try {
            return ResponseEntity.ok(toolRegistryService.updateTool(toolId, tool));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ToolRegistryService.ToolNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{toolId}")
    @Operation(summary = "删除工具", description = "注销工具（内置工具不可删除）")
    public ResponseEntity<?> unregisterTool(@PathVariable String toolId) {
        try {
            toolRegistryService.unregisterTool(toolId);
            return ResponseEntity.ok(Map.of("success", true, "message", "工具已注销"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ToolRegistryService.ToolNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
