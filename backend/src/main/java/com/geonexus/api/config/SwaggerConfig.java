package com.geonexus.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger API 文档配置
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI geoAgentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GeoNexus API")
                        .description("数字GIS专家系统 - REST API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GeoNexus Team")
                                .email("support@geonexus.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
