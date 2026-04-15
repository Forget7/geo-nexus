package com.geonexus.api.v1;

import com.geonexus.model.DataConvertRequest;
import com.geonexus.model.DataUploadResponse;
import com.geonexus.service.DataSanitizationService;
import com.geonexus.service.DataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "数据管理", description = "GIS数据上传、转换、查询与脱敏接口")
public class DataController {
    
    private final DataService dataService;
    private final DataSanitizationService sanitizationService;
    
    @Operation(summary = "转换数据格式", description = "将数据从一种格式转换为另一种格式，支持CRS转换")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "转换成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/data/convert")
    public ResponseEntity<Map<String, Object>> convertData(
            @RequestBody DataConvertRequest request) {
        Map<String, Object> result = dataService.convertData(request);
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "上传数据文件", description = "上传GeoJSON/Shapefile/KML等格式的GIS数据文件")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式错误或大小超限")
    })
    @PostMapping("/data/upload")
    public ResponseEntity<DataUploadResponse> uploadData(
            @Parameter(description = "待上传的文件") @RequestParam("file") MultipartFile file) {
        DataUploadResponse response = dataService.uploadData(file);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "获取数据详情", description = "根据ID获取GIS数据，可指定返回格式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "数据不存在")
    })
    @GetMapping("/data/{dataId}")
    public ResponseEntity<?> getData(
            @Parameter(description = "数据ID") @PathVariable String dataId,
            @Parameter(description = "返回格式") @RequestParam(required = false) String format) {
        return ResponseEntity.ok(dataService.getData(dataId, format));
    }
    
    @Operation(summary = "获取GeoJSON格式", description = "以GeoJSON格式返回GIS数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "数据不存在")
    })
    @GetMapping("/data/{dataId}/geojson")
    public ResponseEntity<?> getDataAsGeoJSON(
            @Parameter(description = "数据ID") @PathVariable String dataId) {
        return ResponseEntity.ok(dataService.getDataAsGeoJSON(dataId));
    }

    /**
     * 数据脱敏处理
     * POST /api/v1/data/{id}/sanitize
     * Body: { "rules": { "phone": true, "email": true, "coordinate": true, "address": true } }
     * @return 脱敏后的数据（原数据保留，新记录返回）
     */
    @Operation(summary = "数据脱敏", description = "对GIS数据进行脱敏处理，支持手机号、邮箱、坐标、地址等敏感信息掩码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "脱敏成功"),
        @ApiResponse(responseCode = "404", description = "数据不存在")
    })
    @PostMapping("/data/{dataId}/sanitize")
    public ResponseEntity<Map<String, Object>> sanitizeData(
            @Parameter(description = "数据ID") @PathVariable String dataId,
            @RequestBody(required = false) Map<String, Boolean> rules) {

        Map<String, Object> rawData = dataService.getDataAsGeoJSON(dataId);
        if (rawData.containsKey("error")) {
            return ResponseEntity.notFound().build();
        }

        // 默认启用所有脱敏规则
        if (rules == null) {
            rules = Map.of(
                    "phone", true,
                    "email", true,
                    "coordinate", true,
                    "address", true
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> geojson = (Map<String, Object>) rawData.get("geojson");
        Map<String, Object> sanitized = sanitizationService.sanitize(geojson, rules);

        return ResponseEntity.ok(Map.of(
                "originalId", dataId,
                "sanitizedData", sanitized,
                "rulesApplied", rules,
                "message", "脱敏完成，原数据已保留"
        ));
    }
}
