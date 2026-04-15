package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * GeoServer 管理员服务
 * 
 * 职责：健康检查、WFS 查询（需要直接 HTTP 调用）
 */
@Slf4j
@Service
public class GeoServerAdminService {

    @Value("${geonexus.geoserver.url:http://localhost:8080/geoserver}")
    private String geoServerUrl;

    @Value("${geonexus.geoserver.username:admin}")
    private String username;

    @Value("${geonexus.geoserver.password:geoserver}")
    private String password;

    @Value("${geonexus.geoserver.workspace:geonexus}")
    private String workspace;

    private final RestTemplate restTemplate;

    public GeoServerAdminService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 执行WFS查询
     */
    public String executeWFSQuery(String typeName, String filter, int maxFeatures) {
        String url = String.format("%s/%s/wfs", geoServerUrl, workspace);
        String body = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <wfs:GetFeature service="WFS" version="2.0.0"
                xmlns:wfs="http://www.opengis.net/wfs/2.0"
                xmlns:fes="http://www.opengis.net/fes/2.0">
                <wfs:Query typeNames="%s:%s" resultType="results" count="%d">%s</wfs:Query>
            </wfs:GetFeature>
            """, workspace, typeName, maxFeatures, filter);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("执行WFS查询失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            String url = String.format("%s/rest/about/version.json", geoServerUrl);
            HttpHeaders headers = createHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("GeoServer健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth, StandardCharsets.UTF_8));
        return headers;
    }
}
