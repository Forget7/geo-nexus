package com.geonexus.api.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geonexus.domain.SceneTemplate;
import com.geonexus.repository.SceneTemplateRepository;
import com.geonexus.service.SceneTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 4D场景模板控制器
 * 
 * 提供场景模板的保存、加载、应用功能
 */
@RestController
@RequestMapping("/api/v1/scenes/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "场景模板", description = "4D场景模板的保存、加载、应用与删除接口")
public class SceneTemplateController {

    private final SceneTemplateRepository sceneTemplateRepository;
    private final SceneTemplateService sceneTemplateService;
    private final ObjectMapper objectMapper;

    // ========== 请求/响应模型 ==========

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "保存场景请求")
    public static class SaveSceneRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "场景名称")
        private String name;
        @io.swagger.v3.oas.annotations.media.Schema(description = "场景描述")
        private String description;
        @io.swagger.v3.oas.annotations.media.Schema(description = "相机状态")
        private CameraState camera;
        @io.swagger.v3.oas.annotations.media.Schema(description = "时间范围开始")
        private String timeRangeStart;
        @io.swagger.v3.oas.annotations.media.Schema(description = "时间范围结束")
        private String timeRangeEnd;
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层配置列表")
        private List<LayerConfig> layers;
        @io.swagger.v3.oas.annotations.media.Schema(description = "缩略图URL或Base64")
        private String thumbnail;
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "相机状态")
    public static class CameraState {
        @io.swagger.v3.oas.annotations.media.Schema(description = "相机经度")
        private Double lon;
        @io.swagger.v3.oas.annotations.media.Schema(description = "相机纬度")
        private Double lat;
        @io.swagger.v3.oas.annotations.media.Schema(description = "相机高度")
        private Double height;
        @io.swagger.v3.oas.annotations.media.Schema(description = "方位角")
        private Double heading;
        @io.swagger.v3.oas.annotations.media.Schema(description = "俯仰角")
        private Double pitch;
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "图层配置")
    public static class LayerConfig {
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层ID")
        private String id;
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层名称")
        private String name;
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层类型")
        private String type;
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否可见")
        private Boolean visible;
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层叠放顺序")
        private Integer zIndex;
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层资源URL")
        private String url;
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层样式配置")
        private String style;
        @io.swagger.v3.oas.annotations.media.Schema(description = "图层透明度")
        private Double opacity;
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "路径规划请求")
    public static class RoutePlanRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "起点坐标 [经度, 纬度]")
        private Double[] from; // [lon, lat]
        @io.swagger.v3.oas.annotations.media.Schema(description = "终点坐标 [经度, 纬度]")
        private Double[] to;   // [lon, lat]
        @io.swagger.v3.oas.annotations.media.Schema(description = "出行模式")
        private String mode;    // driving | walking | cycling
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "路径规划响应")
    public static class RoutePlanResponse {
        @io.swagger.v3.oas.annotations.media.Schema(description = "路线几何")
        private String geometry; // GeoJSON LineString
        @io.swagger.v3.oas.annotations.media.Schema(description = "距离（米）")
        private Double distance;  // meters
        @io.swagger.v3.oas.annotations.media.Schema(description = "时间（秒）")
        private Double duration;  // seconds
        @io.swagger.v3.oas.annotations.media.Schema(description = "警告信息")
        private String warning;   // optional warning message
    }

    // ========== 场景模板 CRUD ==========

    /**
     * 保存当前场景为模板
     * POST /api/v1/scenes/templates
     */
    @Operation(summary = "保存场景模板", description = "将当前地图场景保存为可复用的模板")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping
    public ResponseEntity<?> saveScene(@RequestBody SaveSceneRequest request) {
        try {
            String layersJson = objectMapper.writeValueAsString(request.getLayers());

            SceneTemplate template = SceneTemplate.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .cameraLon(request.getCamera() != null ? request.getCamera().getLon() : null)
                    .cameraLat(request.getCamera() != null ? request.getCamera().getLat() : null)
                    .cameraHeight(request.getCamera() != null ? request.getCamera().getHeight() : null)
                    .cameraHeading(request.getCamera() != null ? request.getCamera().getHeading() : null)
                    .cameraPitch(request.getCamera() != null ? request.getCamera().getPitch() : null)
                    .timeRangeStart(request.getTimeRangeStart())
                    .timeRangeEnd(request.getTimeRangeEnd())
                    .layersJson(layersJson)
                    .thumbnail(request.getThumbnail())
                    .templateType("custom")
                    .tenantId("default")
                    .createdBy("system")
                    .updatedBy("system")
                    .build();

            SceneTemplate saved = sceneTemplateRepository.save(template);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", saved.getId(),
                    "name", saved.getName(),
                    "createdAt", saved.getCreatedAt().toString()
            ));
        } catch (JsonProcessingException e) {
            log.error("图层配置序列化失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", "图层配置序列化失败"));
        } catch (Exception e) {
            log.error("保存场景模板失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "保存场景模板失败: " + e.getMessage()));
        }
    }

    /**
     * 列出所有模板（分页）
     * GET /api/v1/scenes/templates?page=0&size=20
     */
    @Operation(summary = "列出场景模板", description = "分页获取场景模板列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<Page<SceneTemplate>> listTemplates(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        Page<SceneTemplate> result = sceneTemplateRepository
                .findByTenantIdOrderByCreatedAtDesc("default", PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取模板详情
     * GET /api/v1/scenes/templates/{id}
     */
    @Operation(summary = "获取模板详情", description = "根据ID获取场景模板的完整配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplate(
            @Parameter(description = "模板ID") @PathVariable String id) {
        Optional<SceneTemplate> opt = sceneTemplateRepository.findByIdAndTenantId(id, "default");
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SceneTemplate template = opt.get();
        // 反序列化图层配置
        List<LayerConfig> layers = null;
        if (template.getLayersJson() != null && !template.getLayersJson().isEmpty()) {
            try {
                layers = objectMapper.readValue(
                        template.getLayersJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, LayerConfig.class)
                );
            } catch (JsonProcessingException e) {
                log.warn("图层配置反序列化失败 id={}", id, e);
            }
        }

        return ResponseEntity.ok(Map.of(
                "id", template.getId(),
                "name", template.getName(),
                "description", template.getDescription() != null ? template.getDescription() : "",
                "camera", Map.of(
                        "lon", template.getCameraLon() != null ? template.getCameraLon() : 0,
                        "lat", template.getCameraLat() != null ? template.getCameraLat() : 0,
                        "height", template.getCameraHeight() != null ? template.getCameraHeight() : 0,
                        "heading", template.getCameraHeading() != null ? template.getCameraHeading() : 0,
                        "pitch", template.getCameraPitch() != null ? template.getCameraPitch() : 0
                ),
                "timeRangeStart", template.getTimeRangeStart() != null ? template.getTimeRangeStart() : "",
                "timeRangeEnd", template.getTimeRangeEnd() != null ? template.getTimeRangeEnd() : "",
                "layers", layers != null ? layers : List.of(),
                "createdAt", template.getCreatedAt() != null ? template.getCreatedAt().toString() : ""
        ));
    }

    /**
     * 应用模板到当前视图
     * POST /api/v1/scenes/templates/{id}/apply
     * 
     * 返回完整的场景状态，供前端直接应用
     */
    @Operation(summary = "应用模板", description = "将模板配置应用到当前视图，返回完整场景状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "应用成功"),
        @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @PostMapping("/{id}/apply")
    public ResponseEntity<?> applyTemplate(
            @Parameter(description = "模板ID") @PathVariable String id) {
        Optional<SceneTemplate> opt = sceneTemplateRepository.findByIdAndTenantId(id, "default");
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SceneTemplate template = opt.get();
        List<LayerConfig> layers = null;
        if (template.getLayersJson() != null && !template.getLayersJson().isEmpty()) {
            try {
                layers = objectMapper.readValue(
                        template.getLayersJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, LayerConfig.class)
                );
            } catch (JsonProcessingException e) {
                log.warn("图层配置反序列化失败 id={}", id, e);
            }
        }

        return ResponseEntity.ok(Map.of(
                "id", template.getId(),
                "name", template.getName(),
                "camera", Map.of(
                        "lon", template.getCameraLon() != null ? template.getCameraLon() : 116.4,
                        "lat", template.getCameraLat() != null ? template.getCameraLat() : 39.9,
                        "height", template.getCameraHeight() != null ? template.getCameraHeight() : 10000,
                        "heading", template.getCameraHeading() != null ? template.getCameraHeading() : 0,
                        "pitch", template.getCameraPitch() != null ? template.getCameraPitch() : 0
                ),
                "timeRangeStart", template.getTimeRangeStart() != null ? template.getTimeRangeStart() : "",
                "timeRangeEnd", template.getTimeRangeEnd() != null ? template.getTimeRangeEnd() : "",
                "layers", layers != null ? layers : List.of()
        ));
    }

    /**
     * 删除模板
     * DELETE /api/v1/scenes/templates/{id}
     */
    @Operation(summary = "删除模板", description = "根据ID删除场景模板")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable String id) {
        Optional<SceneTemplate> opt = sceneTemplateRepository.findByIdAndTenantId(id, "default");
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        sceneTemplateRepository.delete(opt.get());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}
