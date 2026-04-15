package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.DataSanitizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 数据脱敏API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sanitization")
@RequiredArgsConstructor
@Tag(name = "数据脱敏", description = "GIS数据脱敏与隐私保护")
public class DataSanitizationController {

    private final DataSanitizationService dataSanitizationService;

    @PostMapping("/sanitize")
    @Operation(summary = "批量脱敏GIS数据", description = "按规则对GeoJSON格式的GIS数据进行脱敏处理")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sanitize(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> gisData = (Map<String, Object>) request.get("gisData");
        @SuppressWarnings("unchecked")
        Map<String, Boolean> rules = (Map<String, Boolean>) request.get("rules");
        if (gisData == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("gisData is required"));
        }
        if (rules == null) {
            rules = Map.of(
                    "phone", true,
                    "email", true,
                    "coordinate", true,
                    "address", true
            );
        }
        Map<String, Object> result = dataSanitizationService.sanitize(gisData, rules);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/mask-phone")
    @Operation(summary = "脱敏手机号", description = "138****5678 格式")
    public ResponseEntity<ApiResponse<String>> maskPhone(@RequestBody Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String masked = dataSanitizationService.maskPhone(phone);
        return ResponseEntity.ok(ApiResponse.success(masked));
    }

    @PostMapping("/mask-email")
    @Operation(summary = "脱敏邮箱", description = "a***@domain.com 格式")
    public ResponseEntity<ApiResponse<String>> maskEmail(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String masked = dataSanitizationService.maskEmail(email);
        return ResponseEntity.ok(ApiResponse.success(masked));
    }

    @PostMapping("/round-coord")
    @Operation(summary = "坐标精度降级", description = "保留3位小数（约111米精度）")
    public ResponseEntity<ApiResponse<BigDecimal>> roundCoord(@RequestBody Map<String, Object> request) {
        double value = ((Number) request.get("value")).doubleValue();
        BigDecimal rounded = dataSanitizationService.roundCoord(value);
        return ResponseEntity.ok(ApiResponse.success(rounded));
    }

    @PostMapping("/mask-address")
    @Operation(summary = "脱敏门牌号", description = "xx号 格式")
    public ResponseEntity<ApiResponse<String>> maskAddress(@RequestBody Map<String, Object> request) {
        String address = (String) request.get("address");
        String masked = dataSanitizationService.maskAddressNumber(address);
        return ResponseEntity.ok(ApiResponse.success(masked));
    }
}
