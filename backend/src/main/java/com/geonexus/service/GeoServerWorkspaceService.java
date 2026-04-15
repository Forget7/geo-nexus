package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GeoServer Workspace 管理服务
 * 
 * 职责：Workspace CRUD
 */
@Slf4j
@Service
public class GeoServerWorkspaceService {

    @Value("${geonexus.geoserver.url:http://localhost:8080/geoserver}")
    private String geoServerUrl;

    @Value("${geonexus.geoserver.username:admin}")
    private String username;

    @Value("${geonexus.geoserver.password:geoserver}")
    private String password;

    private final RestTemplate restTemplate;

    public GeoServerWorkspaceService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 创建Workspace
     */
    public boolean createWorkspace(String name) {
        String url = String.format("%s/rest/workspaces", geoServerUrl);

        String body = String.format("""
            <workspace>
                <name>%s</name>
            </workspace>
            """, name);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            log.info("创建Workspace {}: {}", name, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("创建Workspace失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有Workspace
     */
    public List<String> getWorkspaces() {
        String url = String.format("%s/rest/workspaces.json", geoServerUrl);

        try {
            HttpHeaders headers = createHeaders();

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return parseWorkspacesFromJson(response.getBody());
            }
        } catch (Exception e) {
            log.error("获取Workspaces失败: {}", e.getMessage());
        }

        return Collections.emptyList();
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

    /**
     * 解析Workspaces JSON响应
     */
    private List<String> parseWorkspacesFromJson(String json) {
        List<String> workspaces = new ArrayList<>();
        if (json == null) return workspaces;

        try {
            int start = json.indexOf("[\"");
            int end = json.lastIndexOf("\"]");
            if (start > 0 && end > start) {
                String content = json.substring(start + 2, end);
                for (String ws : content.split("\",\"")) {
                    workspaces.add(ws);
                }
            }
        } catch (Exception e) {
            log.error("解析Workspaces JSON失败: {}", e.getMessage());
        }

        return workspaces;
    }
}
