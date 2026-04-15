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
@Schema(description = "地图生成响应")
public class MapGenerateResponse {
    @Schema(description = "地图ID")
    private String id;
    
    @Schema(description = "地图URL")
    private String url;
    
    @Schema(description = "2D地图URL")
    private String url2d;
    
    @Schema(description = "3D地图URL")
    private String url3d;
    
    @Schema(description = "缩略图URL")
    private String thumbnail;
    
    @Schema(description = "错误信息")
    private String error;
}
