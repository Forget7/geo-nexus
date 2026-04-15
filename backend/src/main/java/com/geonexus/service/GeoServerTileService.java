package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * GeoServer TileCache / GeoJSON 上传服务
 * 
 * 职责：TileCache 创建、GeoJSON 上传、切片服务
 */
@Slf4j
@Service
public class GeoServerTileService {

    @Value("${geonexus.geoserver.url:http://localhost:8080/geoserver}")
    private String geoServerUrl;

    @Value("${geonexus.geoserver.username:admin}")
    private String username;

    @Value("${geonexus.geoserver.password:geoserver}")
    private String password;

    @Value("${geonexus.geoserver.workspace:geonexus}")
    private String workspace;

    private final RestTemplate restTemplate;
    private final GeoServerLayerService layerService;

    public GeoServerTileService(GeoServerLayerService layerService) {
        this.restTemplate = new RestTemplate();
        this.layerService = layerService;
    }

    /**
     * 创建TileCache
     */
    public boolean createTileCache(String layerName, String gridSetId) {
        String url = String.format("%s/rest/layers/%s:%s/resource",
                geoServerUrl, workspace, layerName);

        String body = String.format("""
            <coverage>
                <enabled>true</enabled>
                <advertised>true</advertised>
            </coverage>
            """);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            log.info("创建TileCache for {}: {}", layerName, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("创建TileCache失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通过REST API上传GeoJSON
     */
    public boolean uploadGeoJSON(String layerName, String geoJson) {
        String url = String.format("%s/rest/workspaces/%s/datastores/%s/file.json",
                geoServerUrl, workspace, layerName);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 创建临时数据存储
            layerService.createEmptyDataStore(layerName);

            // 上传GeoJSON
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT,
                    new HttpEntity<>(geoJson, headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // 发布为图层
                return layerService.publishFeatureType(layerName, layerName, "EPSG:4326");
            }

            log.info("上传GeoJSON {}: {}", layerName, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("上传GeoJSON失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建HTTP Basic认证头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth, java.nio.charset.StandardCharsets.UTF_8);
        headers.set("Authorization", authHeader);
        return headers;
    }
}
