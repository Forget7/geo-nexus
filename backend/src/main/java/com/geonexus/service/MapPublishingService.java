package com.geonexus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * 地图发布服务 - 将GIS专家系统创建的地图发布为可分享服务
 */
@Slf4j
@Service
public class MapPublishingService {
    
    private final MapService mapService;
    private final GeoServerService geoServerService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    
    // 分享链接基础URL
    private static final String SHARE_BASE_URL = "https://geonexus.ai/map/";
    private static final String EMBED_BASE_URL = "https://geonexus.ai/embed/";
    
    // 链接有效期（默认7天）
    private static final long DEFAULT_EXPIRY_DAYS = 7;
    
    // GeoServer URL from configuration (避免硬编码)
    private final String geoserverUrl;
    
    public MapPublishingService(
            MapService mapService,
            GeoServerService geoServerService,
            CacheService cacheService,
            ObjectMapper objectMapper,
            @Value("${geoserver.url:http://localhost:8080/geoserver}") String geoserverUrl) {
        this.mapService = mapService;
        this.geoServerService = geoServerService;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.geoserverUrl = geoserverUrl;
    }
    
    /**
     * 发布地图为分享服务
     */
    public MapPublishResult publish(MapPublishRequest request) {
        log.info("发布地图: name={}, type={}", request.getName(), request.getPublishType());
        
        // 生成唯一分享ID
        String shareId = generateShareId();
        
        // 构建分享配置
        MapShareConfig config = new MapShareConfig();
        config.setShareId(shareId);
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setOwnerId(request.getOwnerId());
        config.setPublishType(request.getPublishType());
        config.setCreatedAt(System.currentTimeMillis());
        config.setExpiryDays(request.getExpiryDays() != null ? request.getExpiryDays() : DEFAULT_EXPIRY_DAYS);
        config.setExpiresAt(config.getCreatedAt() + config.getExpiryDays() * 24 * 60 * 60 * 1000L);
        
        // 根据发布类型处理
        switch (request.getPublishType()) {
            case "public":
                return publishAsPublic(config, request);
            case "link":
                return publishAsLink(config, request);
            case "embed":
                return publishAsEmbed(config, request);
            case "ogc":
                return publishAsOGC(config, request);
            default:
                throw new IllegalArgumentException("不支持的发布类型: " + request.getPublishType());
        }
    }
    
    /**
     * 公开发布 - 任何人都可以查看
     */
    private MapPublishResult publishAsPublic(MapShareConfig config, MapPublishRequest request) {
        // 保存到数据库/缓存
        saveShareConfig(config, request);
        
        // 生成分享链接
        String shareUrl = SHARE_BASE_URL + config.getShareId();
        String embedCode = generateEmbedCode(config.getShareId(), request.getEmbedOptions());
        
        return MapPublishResult.builder()
                .shareId(config.getShareId())
                .shareUrl(shareUrl)
                .embedCode(embedCode)
                .embedIframe(getEmbedIframe(config.getShareId(), request.getEmbedOptions()))
                .embedScript(getEmbedScript(config.getShareId(), request.getEmbedOptions()))
                .qrCodeUrl(generateQRCode(shareUrl))
                .expiresAt(config.getExpiresAt())
                .accessCount(0)
                .build();
    }
    
    /**
     * 链接发布 - 通过链接访问
     */
    private MapPublishResult publishAsLink(MapShareConfig config, MapPublishRequest request) {
        // 生成带token的链接
        String accessToken = generateAccessToken();
        config.setAccessToken(accessToken);
        
        saveShareConfig(config, request);
        
        String shareUrl = SHARE_BASE_URL + config.getShareId() + "?token=" + accessToken;
        
        return MapPublishResult.builder()
                .shareId(config.getShareId())
                .shareUrl(shareUrl)
                .accessToken(accessToken)
                .embedCode(null)
                .embedIframe(null)
                .qrCodeUrl(generateQRCode(shareUrl))
                .expiresAt(config.getExpiresAt())
                .accessCount(0)
                .build();
    }
    
    /**
     * 嵌入发布 - 可嵌入到其他网站
     */
    private MapPublishResult publishAsEmbed(MapShareConfig config, MapPublishRequest request) {
        config.setEmbedAllowed(true);
        config.setEmbedDomains(request.getAllowedDomains());
        
        saveShareConfig(config, request);
        
        String shareUrl = SHARE_BASE_URL + config.getShareId();
        String embedCode = generateEmbedCode(config.getShareId(), request.getEmbedOptions());
        
        return MapPublishResult.builder()
                .shareId(config.getShareId())
                .shareUrl(shareUrl)
                .embedCode(embedCode)
                .embedIframe(getEmbedIframe(config.getShareId(), request.getEmbedOptions()))
                .embedScript(getEmbedScript(config.getShareId(), request.getEmbedOptions()))
                .qrCodeUrl(generateQRCode(shareUrl))
                .expiresAt(config.getExpiresAt())
                .accessCount(0)
                .build();
    }
    
    /**
     * OGC标准服务发布 - WMS/WMTS
     */
    private MapPublishResult publishAsOGC(MapShareConfig config, MapPublishRequest request) {
        // 发布到GeoServer
        try {
            Map<String, Object> geoResult = geoServerService.publishLayer(
                    config.getShareId(),
                    request.getGeoJson(),
                    request.getStyle()
            );
            
            config.setLayerName(config.getShareId());
            config.setWmsUrl(geoResult.get("wmsUrl") != null 
                    ? geoResult.get("wmsUrl").toString() 
                    : geoserverUrl + "/" + config.getShareId() + "/wms");
            config.setWmtsUrl(geoResult.get("wmtsUrl") != null 
                    ? geoResult.get("wmtsUrl").toString()
                    : geoserverUrl + "/" + config.getShareId() + "/wmts");
            
            saveShareConfig(config, request);
            
            return MapPublishResult.builder()
                    .shareId(config.getShareId())
                    .shareUrl(SHARE_BASE_URL + config.getShareId())
                    .ogcService(new OGCServiceInfo(
                            config.getWmsUrl(),
                            config.getWmtsUrl(),
                            config.getShareId(),
                            "1.3.0"
                    ))
                    .wmsEndpoint(config.getWmsUrl())
                    .wmtsEndpoint(config.getWmtsUrl())
                    .expiresAt(config.getExpiresAt())
                    .accessCount(0)
                    .build();
                    
        } catch (Exception e) {
            log.error("OGC服务发布失败", e);
            throw new RuntimeException("OGC服务发布失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取分享地图
     */
    public MapShareConfig getSharedMap(String shareId, String accessToken) {
        String cacheKey = "map:share:" + shareId;
        MapShareConfig config = (MapShareConfig) cacheService.get(cacheKey);
        
        if (config == null) {
            throw new MapNotFoundException("分享不存在或已过期: " + shareId);
        }
        
        // 检查过期
        if (System.currentTimeMillis() > config.getExpiresAt()) {
            cacheService.delete(cacheKey);
            throw new MapExpiredException("分享已过期");
        }
        
        // 验证访问令牌
        if (config.getAccessToken() != null && 
                !config.getAccessToken().equals(accessToken)) {
            throw new AccessDeniedException("访问令牌无效");
        }
        
        // 增加访问计数
        config.incrementAccessCount();
        cacheService.set(cacheKey, config);
        
        return config;
    }
    
    /**
     * 更新分享配置
     */
    public MapPublishResult updateShare(String shareId, MapPublishRequest request) {
        MapShareConfig config = getSharedMap(shareId, request.getAccessToken());
        
        // 更新配置
        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getExpiryDays() != null) {
            config.setExpiryDays(request.getExpiryDays());
            config.setExpiresAt(config.getCreatedAt() + request.getExpiryDays() * 24 * 60 * 60 * 1000L);
        }
        
        // 保存更新
        String cacheKey = "map:share:" + shareId;
        cacheService.set(cacheKey, config);
        
        return MapPublishResult.builder()
                .shareId(shareId)
                .shareUrl(SHARE_BASE_URL + shareId)
                .expiresAt(config.getExpiresAt())
                .build();
    }
    
    /**
     * 取消分享
     */
    public void unpublish(String shareId, String accessToken) {
        MapShareConfig config = getSharedMap(shareId, accessToken);
        
        // 验证所有权
        if (!config.getOwnerId().equals(accessToken)) {
            throw new AccessDeniedException("无权取消此分享");
        }
        
        // 如果是OGC服务，删除GeoServer图层
        if ("ogc".equals(config.getPublishType()) && config.getLayerName() != null) {
            try {
                geoServerService.deleteLayer(config.getLayerName());
            } catch (Exception e) {
                log.warn("删除GeoServer图层失败: {}", config.getLayerName());
            }
        }
        
        // 删除缓存
        String cacheKey = "map:share:" + shareId;
        cacheService.delete(cacheKey);
        
        log.info("取消分享: shareId={}", shareId);
    }
    
    /**
     * 获取用户的分享列表
     */
    public List<MapShareInfo> listShares(String ownerId) {
        // 从缓存/数据库查询用户的分享
        List<MapShareInfo> shares = new ArrayList<>();
        // 实现实际查询逻辑
        return shares;
    }
    
    // ==================== 私有方法 ====================
    
    private String generateShareId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    private String generateAccessToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private void saveShareConfig(MapShareConfig config, MapPublishRequest request) {
        String cacheKey = "map:share:" + config.getShareId();
        cacheService.set(cacheKey, config, 
                java.time.Duration.ofDays(config.getExpiryDays() + 1));
    }
    
    private String generateEmbedCode(String shareId, EmbedOptions options) {
        if (options == null) {
            options = new EmbedOptions();
        }
        
        return getEmbedIframe(shareId, options);
    }
    
    private String getEmbedIframe(String shareId, EmbedOptions options) {
        int width = options.getWidth() != null ? options.getWidth() : 800;
        int height = options.getHeight() != null ? options.getHeight() : 600;
        String title = options.getTitle() != null ? options.getTitle() : "GeoNexus Map";
        
        return String.format(
                "<iframe src=\"%s%s\" width=\"%d\" height=\"%d\" title=\"%s\" frameborder=\"0\"></iframe>",
                EMBED_BASE_URL, shareId, width, height, title
        );
    }
    
    private String getEmbedScript(String shareId, EmbedOptions options) {
        int width = options != null && options.getWidth() != null ? options.getWidth() : 800;
        int height = options != null && options.getHeight() != null ? options.getHeight() : 600;
        
        return String.format(
                "<script src=\"%sembed.js?id=%s&width=%d&height=%d\"></script>",
                EMBED_BASE_URL, shareId, width, height
        );
    }
    
    private String generateQRCode(String url) {
        // 使用Google Chart API生成二维码
        return "https://chart.googleapis.com/chart?cht=qr&chs=200x200&chl=" + 
                java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8) + "&choe=UTF-8";
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    public static class MapPublishRequest {
        private String name;
        private String description;
        private String ownerId;
        private String publishType; // public, link, embed, ogc
        private Integer expiryDays;
        private Object geoJson;
        private Map<String, Object> style;
        private String accessToken;
        private EmbedOptions embedOptions;
        private List<String> allowedDomains;
    }
    
    @lombok.Data
    public static class EmbedOptions {
        private Integer width;
        private Integer height;
        private String title;
        private Boolean responsive;
        private Boolean showControls;
        private Boolean showSidebar;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MapPublishResult {
        private String shareId;
        private String shareUrl;
        private String accessToken;
        private String embedCode;
        private String embedIframe;
        private String embedScript;
        private String qrCodeUrl;
        private Long expiresAt;
        private Long accessCount;
        private OGCServiceInfo ogcService;
        private String wmsEndpoint;
        private String wmtsEndpoint;
    }
    
    @lombok.Data
    public static class OGCServiceInfo {
        private String wmsUrl;
        private String wmtsUrl;
        private String layerName;
        private String version;
        
        public OGCServiceInfo(String wmsUrl, String wmtsUrl, String layerName, String version) {
            this.wmsUrl = wmsUrl;
            this.wmtsUrl = wmtsUrl;
            this.layerName = layerName;
            this.version = version;
        }
    }
    
    @lombok.Data
    public static class MapShareConfig {
        private String shareId;
        private String name;
        private String description;
        private String ownerId;
        private String publishType;
        private Long createdAt;
        private Long expiresAt;
        private int expiryDays;
        private String accessToken;
        private boolean embedAllowed;
        private List<String> embedDomains;
        private String layerName;
        private String wmsUrl;
        private String wmtsUrl;
        private volatile long accessCount = 0;
        
        public void incrementAccessCount() {
            this.accessCount++;
        }
    }
    
    @lombok.Data
    public static class MapShareInfo {
        private String shareId;
        private String name;
        private String publishType;
        private Long createdAt;
        private Long expiresAt;
        private Long accessCount;
    }
    
    // 异常类
    public static class MapNotFoundException extends RuntimeException {
        public MapNotFoundException(String message) { super(message); }
    }
    
    public static class MapExpiredException extends RuntimeException {
        public MapExpiredException(String message) { super(message); }
    }
    
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) { super(message); }
    }
}
