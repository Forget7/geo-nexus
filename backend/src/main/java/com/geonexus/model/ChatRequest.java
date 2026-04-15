package com.geonexus.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ChatRequest {
    @NotBlank(message = "消息不能为空")
    private String message;
    
    private String sessionId;
    private String model;
    private String mapMode = "2d";
}
