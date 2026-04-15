package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.Model3DService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 三维模型控制器 - 3D模型上传与管理
 */
@RestController
@RequestMapping("/api/v1/models-3d")
@Tag(name = "三维模型", description = "3D模型上传与管理")
@RequiredArgsConstructor
public class Model3DController {

    private final Model3DService model3DService;

    @PostMapping("/upload")
    @Operation(summary = "上传3D模型")
    public ResponseEntity<ApiResponse<Model3DService.Model3D>> uploadModel(
            @RequestBody Model3DService.Model3D model) {
        return ResponseEntity.ok(ApiResponse.success(model3DService.createModel(model)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取3D模型")
    public ResponseEntity<ApiResponse<Model3DService.Model3D>> getModel(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(model3DService.getModel(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新3D模型")
    public ResponseEntity<ApiResponse<Model3DService.Model3D>> updateModel(
            @PathVariable String id, @RequestBody Model3DService.Model3D updates) {
        return ResponseEntity.ok(ApiResponse.success(model3DService.updateModel(id, updates)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除3D模型")
    public ResponseEntity<ApiResponse<Void>> deleteModel(@PathVariable String id) {
        model3DService.deleteModel(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @Operation(summary = "列出3D模型")
    public ResponseEntity<ApiResponse<List<Model3DService.Model3D>>> listModels(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(model3DService.listModels(category, limit)));
    }
}
