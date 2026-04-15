package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.SkillManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 技能管理控制器 - 包装 SkillManagementService
 */
@RestController
@RequestMapping("/api/v1/skills")
@Tag(name = "技能管理", description = "AI技能注册与分类管理")
@RequiredArgsConstructor
public class SkillController {

    private final SkillManagementService skillService;

    @PostMapping
    @Operation(summary = "注册技能")
    public ResponseEntity<ApiResponse<SkillManagementService.SkillDefinition>> registerSkill(
            @RequestBody SkillManagementService.SkillDefinition skill) {
        return ResponseEntity.ok(ApiResponse.success(skillService.registerSkill(skill)));
    }

    @GetMapping
    @Operation(summary = "列出所有技能")
    public ResponseEntity<ApiResponse<List<SkillManagementService.SkillDefinition>>> getAllSkills() {
        return ResponseEntity.ok(ApiResponse.success(skillService.getAllSkills()));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "按分类获取技能")
    public ResponseEntity<ApiResponse<List<SkillManagementService.SkillDefinition>>> getSkillsByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(skillService.getSkillsByCategory(category)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取技能详情")
    public ResponseEntity<ApiResponse<SkillManagementService.SkillDefinition>> getSkill(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(skillService.getSkill(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新技能")
    public ResponseEntity<ApiResponse<SkillManagementService.SkillDefinition>> updateSkill(
            @PathVariable String id, @RequestBody SkillManagementService.SkillDefinition skill) {
        return ResponseEntity.ok(ApiResponse.success(skillService.updateSkill(id, skill)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "注销技能")
    public ResponseEntity<ApiResponse<Void>> unregisterSkill(@PathVariable String id) {
        skillService.unregisterSkill(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
