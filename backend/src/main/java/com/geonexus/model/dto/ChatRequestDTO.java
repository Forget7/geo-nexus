package com.geonexus.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 聊天请求DTO - 带验证
 */
@Data
@Schema(description = "聊天请求")
public class ChatRequestDTO {
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 10000, message = "消息内容过长")
    @Schema(description = "消息内容", example = "找出天安门10公里内的医院")
    private String message;
    
    @Schema(description = "会话ID", example = "sess_abc123")
    private String sessionId;
    
    @Size(max = 50, message = "模型名称过长")
    @Schema(description = "模型名称", example = "gpt-4o")
    private String model;
    
    @DecimalMin(value = "0.0", message = "温度必须大于等于0")
    @DecimalMax(value = "2.0", message = "温度必须小于等于2")
    @Schema(description = "温度参数", example = "0.7")
    private Double temperature;
    
    @Min(value = 1, message = "最大Token必须大于0")
    @Max(value = 128000, message = "最大Token超出范围")
    @Schema(description = "最大Token数", example = "4000")
    private Integer maxTokens;
    
    @Schema(description = "上下文附件数据")
    private Map<String, Object> context;
    
    @Schema(description = "地图模式", example = "2d", allowableValues = {"2d", "3d", "none"})
    private String mapMode; // "2d", "3d", "none"
    
    @Schema(description = "启用的工具列表", example = "[\"buffer_analysis\", \"geocode\"]")
    private List<String> tools; // 启用的工具列表
}
