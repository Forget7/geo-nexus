package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * GeoServer服务 - OGC标准GIS服务 (Facade)
 * 
 * 支持:
 * - WMS (Web Map Service) - 地图服务
 * - WFS (Web Feature Service) - 要素服务
 * - WMTS (Web Map Tile Service) - 切片服务
 * - REST API - 数据管理
 * 
 * 职责已拆分至：
 * - GeoServerWorkspaceService: Workspace 管理
 * - GeoServerStyleService: SLD 样式管理
 * - GeoServerLayerService: Layer / FeatureType / PostGIS Store 管理
 * - GeoServerTileService: TileCache / GeoJSON 上传
 * - GeoServerAdminService: 健康检查、WFS 查询
 */
@Slf4j
@Service
public class GeoServerService {

    @Value("${geonexus.geoserver.url:http://localhost:8080/geoserver}")
    private String geoServerUrl;

    @Value("${geonexus.geoserver.workspace:geonexus}")
    private String workspace;

    private final GeoServerWorkspaceService workspaceService;
    private final GeoServerStyleService styleService;
    private final GeoServerLayerService layerService;
    private final GeoServerTileService tileService;
    private final GeoServerAdminService adminService;

    public GeoServerService(
            GeoServerWorkspaceService workspaceService,
            GeoServerStyleService styleService,
            GeoServerLayerService layerService,
            GeoServerTileService tileService,
            GeoServerAdminService adminService) {
        this.workspaceService = workspaceService;
        this.styleService = styleService;
        this.layerService = layerService;
        this.tileService = tileService;
        this.adminService = adminService;
    }

    // ==================== Workspace 委托 ====================

    public boolean createWorkspace(String name) {
        return workspaceService.createWorkspace(name);
    }

    public List<String> getWorkspaces() {
        return workspaceService.getWorkspaces();
    }

    // ==================== Style 委托 ====================

    public boolean createStyle(String styleName, String sldContent) {
        return styleService.createStyle(styleName, sldContent);
    }

    public boolean setLayerStyle(String layerName, String styleName) {
        return styleService.setLayerStyle(layerName, styleName);
    }

    public String getStyleContent(String styleName) {
        return styleService.getStyleContent(styleName);
    }

    public List<Map<String, String>> getStyleTemplates() {
        return styleService.getTemplatesAsMaps();
    }

    // ==================== Layer 委托 ====================

    public boolean createPostGISStore(String storeName, String database, String host,
                                       int port, String user, String password) {
        return layerService.createPostGISStore(storeName, database, host, port, user, password);
    }

    public boolean publishFeatureType(String storeName, String featureName,
                                       String srs, String... bbox) {
        return layerService.publishFeatureType(storeName, featureName, srs, bbox);
    }

    public Map<String, Object> getLayerInfo(String layerName) {
        return layerService.getLayerInfo(layerName);
    }

    // ==================== Tile 委托 ====================

    public boolean createTileCache(String layerName, String gridSetId) {
        return tileService.createTileCache(layerName, gridSetId);
    }

    public boolean uploadGeoJSON(String layerName, String geoJson) {
        return tileService.uploadGeoJSON(layerName, geoJson);
    }

    // ==================== Admin 委托 ====================

    public String executeWFSQuery(String typeName, String filter, int maxFeatures) {
        return adminService.executeWFSQuery(typeName, filter, maxFeatures);
    }

    public boolean isHealthy() {
        return adminService.isHealthy();
    }

    // ==================== URL 工具方法（无委托） ====================

    /**
     * 获取WMS GetCapabilities URL
     */
    public String getWMSCapabilitiesUrl() {
        return String.format("%s/%s/wms?service=WMS&version=1.3.0&request=GetCapabilities",
                geoServerUrl, workspace);
    }

    /**
     * 生成WMS GetMap URL
     */
    public String getWMSGetMapUrl(String layerName, String bbox,
                                   int width, int height, String format) {
        return String.format(
                "%s/%s/wms?service=WMS&version=1.1.1&request=GetMap" +
                "&layers=%s:%s" +
                "&bbox=%s" +
                "&width=%d&height=%d" +
                "&format=%s" +
                "&crs=EPSG:4326",
                geoServerUrl, workspace, workspace, layerName,
                bbox, width, height, format
        );
    }

    /**
     * 获取WFS GetCapabilities URL
     */
    public String getWFSCapabilitiesUrl() {
        return String.format("%s/%s/wfs?service=WFS&version=2.0.0&request=GetCapabilities",
                geoServerUrl, workspace);
    }
}
