package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.domain.ProcessDefinitionEntity;
import com.geonexus.service.OGCProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OGC API - Processes Part 1 (Core) 控制器
 * 提供标准化空间处理接口
 */
@RestController
@RequestMapping("/api/v1/processes")
@Tag(name = "OGC API - Processes", description = "标准化空间处理接口 (OGC API - Processes Part 1)")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
public class OGCProcessController {

    private final OGCProcessService processService;

    // ===== OGC API - Processes conformance =====

    @GetMapping
    @Operation(summary = "列出所有可用处理过程", description = "返回所有注册的处理过程列表，按分类和过程ID排序")
    public ResponseEntity<ApiResponse<ProcessListResponse>> listProcesses() {
        List<ProcessDefinitionEntity> processes = processService.getAllProcesses();
        var response = new ProcessListResponse(
            "https://ogc.org/standard/ogcapi-processes/",
            "1.0",
            processes.size(),
            processes
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{processId}")
    @Operation(summary = "获取处理过程描述", description = "返回指定过程的完整元数据描述")
    public ResponseEntity<ApiResponse<ProcessDefinitionEntity>> getProcess(
            @PathVariable String processId) {
        ProcessDefinitionEntity def = processService.getProcess(processId);
        if (def == null) {
            return ResponseEntity.status(404)
                .body(ApiResponse.error("Process not found: " + processId));
        }
        return ResponseEntity.ok(ApiResponse.success(def));
    }

    @PostMapping("/{processId}/execute")
    @Operation(summary = "执行处理过程", description = "同步或异步执行指定的空间处理过程")
    public ResponseEntity<?> executeProcess(
            @PathVariable String processId,
            @RequestBody(required = false) Map<String, Object> inputs,
            @RequestParam(defaultValue = "sync") String mode) {
        try {
            if ("async".equalsIgnoreCase(mode)) {
                String jobId = processService.submitJob(
                    processId, inputs != null ? inputs : Map.of()
                );
                return ResponseEntity.accepted()
                    .body(ApiResponse.success(Map.of(
                        "status", "accepted",
                        "jobId", jobId,
                        "location", "/api/v1/processes/jobs/" + jobId
                    )));
            }
            Map<String, Object> result = processService.executeProcess(
                processId, inputs != null ? inputs : Map.of()
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "查询异步任务状态", description = "查询异步执行任务的状态和结果")
    public ResponseEntity<ApiResponse<JobStatusResponse>> getJobStatus(
            @PathVariable String jobId) {
        // In a full impl, query job store; here return accepted
        return ResponseEntity.ok(ApiResponse.success(new JobStatusResponse(
            jobId, "accepted", null, null
        )));
    }

    // ===== DTOs =====

    public record ProcessListResponse(
        String conformance,
        String version,
        int total,
        List<ProcessDefinitionEntity> processes
    ) {}

    public record JobStatusResponse(
        String jobId,
        String status,      // accepted / running / successful / failed
        String message,
        Object result
    ) {}
}
