package com.geonexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA 审计配置
 * 启用 @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy 自动填充
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * AuditorAware 实现 - 返回当前用户名
     * 
     * 注意：当前应用未启用 Spring Security，
     * 因此通过 ThreadLocal 上下文机制获取。
     * 使用 ChatContextHolder.setCurrentUser(userId) 设置当前用户。
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // 优先从 ThreadLocal 获取（通过 ChatContextHolder）
            String userId = ChatContextHolder.getCurrentUser();
            if (userId != null) {
                return Optional.of(userId);
            }
            return Optional.of("system");
        };
    }
}
