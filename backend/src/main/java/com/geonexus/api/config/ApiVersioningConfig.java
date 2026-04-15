package com.geonexus.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.concurrent.TimeUnit;

/**
 * Web MVC配置 - 统一出入口配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final ObjectMapper objectMapper;
    
    public WebMvcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * API版本管理
     */
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {{
            setOrder(0);
            setInterceptors(new Object[]{});
        }};
    }
    
    /**
     * 静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前端静态资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/", "file:./public/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .resourceChain(true)
                .addResolver(new org.springframework.web.servlet.resource.VersionResourceResolver()
                        .addContentVersionStrategy("/**"));
        
        // Swagger资源
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.x/");
        
        // 文件上传目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:${UPLOAD_DIR:./data/uploads}/")
                .setCacheControl(CacheControl.noCache());
    }
    
    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "https://*.geonexus.ai",
                        "https://*.github.io"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                        "X-Request-ID",
                        "X-Rate-Limit-Remaining",
                        "X-Rate-Limit-Reset",
                        "X-Total-Count"
                )
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    /**
     * 视图控制器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径重定向
        registry.addRedirectViewController("/", "/index.html");
        
        // 健康检查
        registry.addViewController("/health").setViewName("forward:/actuator/health");
        
        // API文档
        registry.addViewController("/docs").setViewName("redirect:/swagger-ui.html");
    }
    
    /**
     * 格式化配置
     */
    @Override
    public void configureMessageConverters(java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        converters.add(converter);
    }
    
    /**
     * JSON时间格式
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        return converter;
    }
    
    /**
     * 请求拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // API日志拦截器
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override
            public boolean preHandle(
                    HttpServletRequest request, 
                    HttpServletResponse response, 
                    Object handler) {
                
                // 设置请求ID
                String requestId = request.getHeader("X-Request-ID");
                if (requestId == null) {
                    requestId = java.util.UUID.randomUUID().toString();
                }
                response.setHeader("X-Request-ID", requestId);
                
                // 记录开始时间
                request.setAttribute("startTime", System.currentTimeMillis());
                
                return true;
            }
            
            @Override
            public void afterCompletion(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler,
                    Exception ex) {
                
                Long startTime = (Long) request.getAttribute("startTime");
                if (startTime != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    String requestId = response.getHeader("X-Request-ID");
                    
                    // 慢请求警告（超过1秒）
                    if (duration > 1000) {
                        org.slf4j.LoggerFactory.getLogger("com.geonexus.api")
                                .warn("Slow request: {} {} took {}ms", 
                                        request.getMethod(), 
                                        request.getRequestURI(), 
                                        duration);
                    }
                }
            }
        }).addPathPatterns("/api/**");
        
        // 性能监控拦截器
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            private final java.util.concurrent.ConcurrentHashMap<String, Long> concurrentUsers = 
                    new java.util.concurrent.ConcurrentHashMap<>();
            
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String sessionId = request.getSession(false) != null 
                        ? request.getSession().getId() 
                        : request.getHeader("X-Request-ID");
                
                concurrentUsers.put(sessionId, System.currentTimeMillis());
                request.setAttribute("concurrentUsers", concurrentUsers.size());
                
                return true;
            }
            
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                    Object handler, Exception ex) {
                String sessionId = request.getSession(false) != null 
                        ? request.getSession().getId() 
                        : request.getHeader("X-Request-ID");
                
                concurrentUsers.remove(sessionId);
            }
        }).addPathPatterns("/api/**");
    }
    
    /**
     * 路径匹配配置
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer
                // 忽略尾部斜杠
                .setUseTrailingSlashMatch(false)
                // URL路径解编码
                .setUrlDecode(true)
                // 路径变量自动绑定到同名的@PathVariable
                .setRegisterSuffixPatternMatch(false)
                // 内容协商
                .setContentNegotiationManager(new org.springframework.web.accept.ContentNegotiationManager(
                        new org.springframework.web.accept.PathExtensionContentNegotiationManager(
                                java.util.Map.of(
                                        "json", "application/json",
                                        "xml", "application/xml",
                                        "html", "text/html"
                                )
                        )
                ));
    }
    
    /**
     * 异步处理配置
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer
                // 异步任务调度器
                .setTaskExecutor(new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor(
                        2,  // core
                        10, // max
                        60, // keepAlive
                        java.util.concurrent.TimeUnit.SECONDS,
                        new java.util.concurrent.LinkedBlockingQueue<>(100)
                ))
                // 异步超时时间
                .setDefaultTimeout(30_000)
                // 异步调用处理拦截器
                .registerCallableInterceptors(new org.springframework.web.context.request.AsyncWebRequestInterceptor() {
                    @Override
                    public void beforeConcurrentHandling(
                            javax.servlet.ServletRequest request, 
                            javax.servlet.ServletResponse response,
                            Object deferredResult) {}
                    
                    @Override
                    public void preHandle(javax.servlet.ServletRequest request, 
                            javax.servlet.ServletResponse response) {}
                    
                    @Override
                    public void afterCompletion(javax.servlet.ServletRequest request,
                            javax.servlet.ServletResponse response,
                            Object deferredResult,
                            Exception ex) {}
                });
    }
    
    /**
     * 默认视图解析器
     */
    @Bean
    public org.springframework.web.servlet.view.InternalResourceViewResolver viewResolver() {
        return new org.springframework.web.servlet.view.InternalResourceViewResolver(
                "classpath:/templates/",
                ".html",
                "UTF-8"
        );
    }
}
