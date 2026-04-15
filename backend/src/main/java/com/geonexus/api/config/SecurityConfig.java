package com.geonexus.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Spring Security 6.x 安全配置
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Component  // Make it a Spring bean so it can be injected
public class SecurityConfig {

    private static final String JWT_SECRET;
    static {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret == null || envSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable must be set. " +
                "Do NOT use hardcoded secrets in production.");
        }
        JWT_SECRET = envSecret;
    }
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 生成JWT Token（简化版，实际生产应使用JJWT库）
     */
    public String generateToken(String userId, String username, Map<String, Object> claims) {
        try {
            long now = System.currentTimeMillis();
            long exp = now + 86400000; // 24小时

            String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String payload = base64UrlEncode(String.format(
                    "{\"sub\":\"%s\",\"username\":\"%s\",\"iat\":%d,\"exp\":%d,%s}",
                    userId, username, now / 1000, exp / 1000,
                    claims.entrySet().stream()
                            .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
                            .reduce((a, b) -> a + "," + b)
                            .orElse("")
            ));
            String signature = hmacSha256(header + "." + payload, JWT_SECRET);

            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            log.error("生成Token失败", e);
            throw new RuntimeException("Token生成失败", e);
        }
    }

    /**
     * 验证并解析JWT Token
     */
    public Claims validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid token format");
            }

            String signature = hmacSha256(parts[0] + "." + parts[1], JWT_SECRET);
            if (!signature.equals(parts[2])) {
                throw new RuntimeException("Invalid token signature");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            // 简化解析：提取sub和username
            return new Claims(payload);
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            throw new RuntimeException("Token验证失败", e);
        }
    }

    private String base64UrlEncode(String text) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }

    /**
     * 从Authorization header提取用户ID
     * @param authHeader Authorization header值（Bearer token格式）
     * @return userId，若token无效则返回null
     */
    public String extractUserIdFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = validateToken(token);
            return claims.getSubject();
        } catch (RuntimeException e) {
            log.warn("无法从token提取用户ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT Claims 持有类
     */
    public static class Claims {
        private final String subject;
        private final String username;

        public Claims(String payload) {
            this.subject = extract(payload, "sub");
            this.username = extract(payload, "username");
        }

        private String extract(String json, String key) {
            int keyStart = json.indexOf("\"" + key + "\"");
            if (keyStart < 0) return null;
            int colon = json.indexOf(":", keyStart);
            int valueStart = json.indexOf("\"", colon);
            int valueEnd = json.indexOf("\"", valueStart + 1);
            return valueStart >= 0 && valueEnd > valueStart ? json.substring(valueStart + 1, valueEnd) : null;
        }

        public String getSubject() { return subject; }
        public String get(String key) {
            if ("username".equals(key)) return username;
            return null;
        }
    }
    
    // 公开端点
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/health"
    };
    
    // 管理员端点
    private static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/admin/**",
            "/api/v1/system/**"
    };
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（API场景下不需要）
                .csrf(csrf -> csrf.disable())
                
                // CORS配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 无状态会话
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开端点
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // 管理员端点
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                        // 其他需要认证
                        .anyRequest().authenticated()
                )
                
                // 添加安全头
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny()) // 防止点击劫持
                        .contentTypeOptions(content -> {}) // X-Content-Type-Options
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)) // HSTS
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                
                // 禁用匿名用户
                .anonymous(anonymous -> anonymous.disable())
                
                // 异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Unauthorized access attempt: {}", request.getRequestURI());
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("Access denied: {} by user", request.getRequestURI());
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                        })
                );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的来源（生产环境应配置具体域名）
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.geonexus.ai"
        ));
        
        // 允许的方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 允许的头
        configuration.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization",
                "X-Requested-With", "X-API-Key", "X-Request-ID"
        ));
        
        // 暴露的头
        configuration.setExposedHeaders(Arrays.asList(
                "X-Request-ID", "X-Rate-Limit-Remaining", "X-Rate-Limit-Reset"
        ));
        
        // 允许凭证
        configuration.setAllowCredentials(true);
        
        // 预检请求缓存时间
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
