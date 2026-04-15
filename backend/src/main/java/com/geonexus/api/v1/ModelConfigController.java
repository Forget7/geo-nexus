package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.ModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型配置控制器 - 包装 ModelConfigService
 */
@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "模型配置", description = "LLM提供商配置与模型管理")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @GetMapping("/providers")
    @Operation(summary = "列出所有LLM提供商")
    public ResponseEntity<ApiResponse<List<ModelConfigService.ProviderConfig>>> getAllProviders() {
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.getAllProviders()));
    }

    @GetMapping("/providers/{id}")
    @Operation(summary = "获取提供商详情")
    public ResponseEntity<ApiResponse<ModelConfigService.ProviderConfig>> getProvider(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.getProvider(id)));
    }

    @PostMapping("/providers")
    @Operation(summary = "保存提供商配置")
    public ResponseEntity<ApiResponse<ModelConfigService.ProviderConfig>> saveProvider(
            @RequestBody ModelConfigService.ProviderConfig provider) {
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.saveProvider(provider)));
    }

    @DeleteMapping("/providers/{id}")
    @Operation(summary = "删除提供商")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(@PathVariable String id) {
        modelConfigService.deleteProvider(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/ollama/models")
    @Operation(summary = "从Ollama获取可用模型列表")
    public ResponseEntity<ApiResponse<List<String>>> fetchOllamaModels(
            @RequestParam String baseUrl) {
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.fetchOllamaModels(baseUrl)));
    }

    @PostMapping("/configs")
    @Operation(summary = "创建模型配置")
    public ResponseEntity<ApiResponse<ModelConfigService.ModelConfig>> createConfig(
            @RequestBody ModelConfigService.ModelConfig config) {
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.createConfig(config)));
    }

    @GetMapping("/configs")
    @Operation(summary = "列出模型配置")
    public ResponseEntity<ApiResponse<List<ModelConfigService.ModelConfig>>> getAllConfigs() {
        // ModelConfigService.getUserConfigs(userId) returns configs for a specific user;
        // pass null to get all configs (no user filter)
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.getUserConfigs(null)));
    }

    @GetMapping("/configs/{id}")
    @Operation(summary = "获取模型配置")
    public ResponseEntity<ApiResponse<ModelConfigService.ModelConfig>> getConfig(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.getConfig(id)));
    }

    @PutMapping("/configs/{id}")
    @Operation(summary = "更新模型配置")
    public ResponseEntity<ApiResponse<ModelConfigService.ModelConfig>> updateConfig(
            @PathVariable String id, @RequestBody ModelConfigService.ModelConfig config) {
        return ResponseEntity.ok(ApiResponse.success(modelConfigService.updateConfig(id, config)));
    }

    @DeleteMapping("/configs/{id}")
    @Operation(summary = "删除模型配置")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable String id) {
        modelConfigService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
