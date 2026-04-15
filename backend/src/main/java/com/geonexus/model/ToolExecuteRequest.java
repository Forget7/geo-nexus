package com.geonexus.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Data
@Schema(description = "工具执行请求")
public class ToolExecuteRequest {
    @Schema(description = "工具名称", example = "buffer_analysis")
    private String tool;
    
    @Schema(description = "工具参数字典")
    private Map<String, Object> params;
}
