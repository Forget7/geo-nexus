package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流控制器 - REST API
 */
@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "工作流编排", description = "分析工作流定义与执行")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    // ===== Workflow Definitions =====

    @PostMapping
    @Operation(summary = "创建工作流")
    public ResponseEntity<ApiResponse<WorkflowService.WorkflowDefinition>> createWorkflow(
            @RequestBody WorkflowService.WorkflowDefinition definition) {
        WorkflowService.WorkflowDefinition created = workflowService.createWorkflow(definition);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping
    @Operation(summary = "列出工作流")
    public ResponseEntity<ApiResponse<List<WorkflowService.WorkflowDefinition>>> listWorkflows(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(
            workflowService.listWorkflows(category)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取工作流详情")
    public ResponseEntity<ApiResponse<WorkflowService.WorkflowDefinition>> getWorkflow(@PathVariable String id) {
        WorkflowService.WorkflowDefinition wf = workflowService.getWorkflow(id);
        if (wf == null) return ResponseEntity.status(404)
            .body(ApiResponse.error("Workflow not found"));
        return ResponseEntity.ok(ApiResponse.success(wf));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工作流")
    public ResponseEntity<ApiResponse<WorkflowService.WorkflowDefinition>> updateWorkflow(
            @PathVariable String id, @RequestBody WorkflowService.WorkflowDefinition updates) {
        WorkflowService.WorkflowDefinition updated = workflowService.updateWorkflow(id, updates);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作流")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable String id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== Execution =====

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行工作流")
    public ResponseEntity<?> executeWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            WorkflowService.WorkflowExecution exec = workflowService.executeWorkflow(id, params);
            return ResponseEntity.accepted()
                .body(ApiResponse.success(exec));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/executions/{execId}")
    @Operation(summary = "查询执行状态")
    public ResponseEntity<ApiResponse<WorkflowService.WorkflowExecution>> getExecution(
            @PathVariable String execId) {
        WorkflowService.WorkflowExecution exec = workflowService.getExecution(execId);
        if (exec == null) return ResponseEntity.status(404)
            .body(ApiResponse.error("Execution not found"));
        return ResponseEntity.ok(ApiResponse.success(exec));
    }

    @GetMapping("/executions/{execId}/result")
    @Operation(summary = "获取执行结果")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionResult(
            @PathVariable String execId) {
        Map<String, Object> result = workflowService.getExecutionResult(execId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/executions/{execId}/cancel")
    @Operation(summary = "取消执行")
    public ResponseEntity<ApiResponse<Void>> cancelExecution(@PathVariable String execId) {
        workflowService.cancelExecution(execId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
