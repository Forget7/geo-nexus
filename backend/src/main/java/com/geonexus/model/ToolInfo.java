package com.geonexus.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GIS工具信息")
public class ToolInfo {
    @Schema(description = "工具名称")
    private String name;
    
    @Schema(description = "工具描述")
    private String description;
    
    @Schema(description = "工具参数定义")
    private Map<String, Object> parameters;
}
