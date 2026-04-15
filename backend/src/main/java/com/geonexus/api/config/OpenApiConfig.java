package com.geonexus.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.*;
import io.swagger.v3.oas.models.tags.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * OpenAPI 3.0 文档配置
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI geoAgentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GeoNexus Digital GeoNexus API")
                        .description("基于大模型与GIS工具的智能地理信息系统 - 完整的REST API文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GeoNexus Team")
                                .email("support@geonexus.ai")
                                .url("https://geonexus.ai"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                        .termsOfService("https://geonexus.ai/terms"))
                
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8000")
                                .description("本地开发环境"),
                        new Server()
                                .url("https://api.geonexus.ai")
                                .description("生产环境")
                ))
                
                .tags(List.of(
                        new Tag()
                                .name("Chat")
                                .description("对话接口 - 与GIS专家系统自然语言对话"),
                        new Tag()
                                .name("Map")
                                .description("地图接口 - 生成和渲染地图"),
                        new Tag()
                                .name("Data")
                                .description("数据接口 - 上传、转换、管理地理数据"),
                        new Tag()
                                .name("Tools")
                                .description("工具接口 - 执行GIS空间分析工具"),
                        new Tag()
                                .name("GeoServer")
                                .description("GeoServer接口 - OGC标准空间服务"),
                        new Tag()
                                .name("Auth")
                                .description("认证接口 - JWT令牌管理")
                ))
                
                .components(new io.swagger.v3.oas.models.Components()
                        .securitySchemes(List.of(
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT令牌认证")
                                        .name("bearerAuth"),
                                
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(In.HEADER)
                                        .name("X-API-Key")
                                        .description("API密钥认证")
                        ))
                        
                        .schemas(Map.of(
                                "ChatRequest", new io.swagger.v3.oas.models.media.Schema()
                                        .type("object")
                                        .required(List.of("message"))
                                        .property("message", new io.swagger.v3.oas.models.media.Schema().type("string").description("消息内容"))
                                        .property("sessionId", new io.swagger.v3.oas.models.media.Schema().type("string").description("会话ID"))
                                        .property("model", new io.swagger.v3.oas.models.media.Schema().type("string").description("模型名称")),
                                
                                "ChatResponse", new io.swagger.v3.oas.models.media.Schema()
                                        .type("object")
                                        .property("message", new io.swagger.v3.oas.models.media.Schema().type("string").description("回复内容"))
                                        .property("sessionId", new io.swagger.v3.oas.models.media.Schema().type("string").description("会话ID"))
                                        .property("mapUrl", new io.swagger.v3.oas.models.media.Schema().type("string").description("生成的地图URL")),
                                
                                "Error", new io.swagger.v3.oas.models.media.Schema()
                                        .type("object")
                                        .property("code", new io.swagger.v3.oas.models.media.Schema().type("integer").description("错误码"))
                                        .property("message", new io.swagger.v3.oas.models.media.Schema().type("string").description("错误信息"))
                                        .property("details", new io.swagger.v3.oas.models.media.Schema().type("object").description("详细信息"))
                        ))
                )
                
                .security(List.of(
                        new SecurityRequirement().addList("bearerAuth"),
                        new SecurityRequirement().addList("X-API-Key")
                ));
    }
}
