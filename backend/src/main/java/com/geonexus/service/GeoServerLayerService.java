package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GeoServer Layer/FeatureType 管理服务
 * 
 * 职责：Layer 信息查询、FeatureType 发布、PostGIS Store 管理
 */
@Slf4j
@Service
public class GeoServerLayerService {

    @Value("${geonexus.geoserver.url:http://localhost:8080/geoserver}")
    private String geoServerUrl;

    @Value("${geonexus.geoserver.username:admin}")
    private String username;

    @Value("${geonexus.geoserver.password:geoserver}")
    private String password;

    @Value("${geonexus.geoserver.workspace:geonexus}")
    private String workspace;

    private final RestTemplate restTemplate;

    public GeoServerLayerService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 创建PostGIS数据存储
     */
    public boolean createPostGISStore(String storeName, String database, String host,
                                       int port, String user, String password) {
        String url = String.format("%s/rest/workspaces/%s/datastores", geoServerUrl, workspace);

        String body = String.format("""
            <dataStore>
                <name>%s</name>
                <connectionParameters>
                    <entry key="dbtype">postgis</entry>
                    <entry key="host">%s</entry>
                    <entry key="port">%d</entry>
                    <entry key="database">%s</entry>
                    <entry key="user">%s</entry>
                    <entry key="passwd">%s</entry>
                    <entry key="schema">public</entry>
                    <entry key="sslmode">disable</entry>
                </connectionParameters>
            </dataStore>
            """, storeName, host, port, database, user, password);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            log.info("创建PostGIS Store {}: {}", storeName, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("创建PostGIS Store失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建空的DataStore（用于后续上传数据）
     */
    public boolean createEmptyDataStore(String storeName) {
        String url = String.format("%s/rest/workspaces/%s/datastores",
                geoServerUrl, workspace);

        String body = String.format("""
            <dataStore>
                <name>%s</name>
            </dataStore>
            """, storeName);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception e) {
            log.debug("创建DataStore (可能已存在): {}", e.getMessage());
            return true;
        }
    }

    /**
     * 发布要素类为WFS图层
     */
    public boolean publishFeatureType(String storeName, String featureName,
                                       String srs, String... bbox) {
        String url = String.format("%s/rest/workspaces/%s/datastores/%s/featuretypes",
                geoServerUrl, workspace, storeName);

        StringBuilder bboxStr = new StringBuilder();
        if (bbox != null && bbox.length == 4) {
            bboxStr.append(String.format("""
                <nativeBBox>
                    <minx>%s</minx>
                    <miny>%s</miny>
                    <maxx>%s</maxx>
                    <maxy>%s</maxy>
                    <crs>%s</crs>
                </nativeBBox>
                """, bbox[0], bbox[1], bbox[2], bbox[3], srs));
        }

        String body = String.format("""
            <featureType>
                <name>%s</name>
                <nativeName>%s</nativeName>
                <srs>%s</srs>
                %s
            </featureType>
            """, featureName, featureName, srs, bboxStr);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            log.info("发布FeatureType {}: {}", featureName, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("发布FeatureType失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取图层信息
     */
    public Map<String, Object> getLayerInfo(String layerName) {
        String url = String.format("%s/rest/layers/%s:%s.json",
                geoServerUrl, workspace, layerName);

        try {
            HttpHeaders headers = createHeaders();

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return parseLayerInfoFromJson(response.getBody());
            }
        } catch (Exception e) {
            log.error("获取Layer信息失败: {}", e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * 解析Layer信息JSON响应
     */
    private Map<String, Object> parseLayerInfoFromJson(String json) {
        Map<String, Object> info = new HashMap<>();
        if (json == null) return info;

        // 简化实现 - 实际应使用Jackson解析
        info.put("raw", json);
        return info;
    }

    /**
     * 创建HTTP Basic认证头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
        headers.set("Authorization", authHeader);
        return headers;
    }
}
