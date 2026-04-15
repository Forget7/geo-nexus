package com.geonexus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据源配置 - 外部GIS服务URL统一管理
 */
@Data
@Component
@ConfigurationProperties(prefix = "geonexus.datasource")
public class DataSourceProperties {
    
    private Map<String, ServiceEndpoint> services;
    
    @Data
    public static class ServiceEndpoint {
        private String name;
        private String type;
        private String url;
        private Map<String, String> credentials;
        private List<String> capabilities;
        private boolean enabled = true;
    }
}
