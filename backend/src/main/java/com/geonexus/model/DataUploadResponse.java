package com.geonexus.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据上传响应")
public class DataUploadResponse {
    @Schema(description = "上传后数据ID")
    private String id;
    
    @Schema(description = "文件名")
    private String filename;
    
    @Schema(description = "数据格式", example = "GeoJSON")
    private String format;
    
    @Schema(description = "文件大小（字节）")
    private Long size;
    
    @Schema(description = "访问URL")
    private String url;
}
