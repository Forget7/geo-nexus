package com.geonexus.api.v1;

import com.geonexus.model.TaskResponse;
import com.geonexus.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Task", description = "异步任务管理")
public class TaskController {
    
    private final TaskService taskService;
    
    @PostMapping("/tasks/{type}")
    @Operation(summary = "创建异步任务")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "任务已创建"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public ResponseEntity<Map<String, String>> createTask(
            @Parameter(description = "任务类型") @PathVariable String type,
            @RequestBody Map<String, Object> params) {
        
        String taskId = taskService.createTask(type, params);
        return ResponseEntity.accepted()
                .body(Map.of("taskId", taskId, "statusUrl", "/api/v1/tasks/" + taskId));
    }
    
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "获取任务状态")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<TaskResponse> getTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        TaskResponse response = taskService.getTask(taskId);
        return ResponseEntity.ok(response);
    }
}
