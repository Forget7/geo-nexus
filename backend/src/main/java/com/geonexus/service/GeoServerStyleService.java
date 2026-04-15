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
 * GeoServer 样式(SLD)管理服务
 * 
 * 职责：SLD 样式创建、更新、图层样式关联
 */
@Slf4j
@Service
public class GeoServerStyleService {

    @Value("${geonexus.geoserver.url:http://localhost:8080/geoserver}")
    private String geoServerUrl;

    @Value("${geonexus.geoserver.username:admin}")
    private String username;

    @Value("${geonexus.geoserver.password:geoserver}")
    private String password;

    @Value("${geonexus.geoserver.workspace:geonexus}")
    private String workspace;

    private final RestTemplate restTemplate;

    public GeoServerStyleService() {
        this.restTemplate = new RestTemplate();
    }

    // ─── API bridge methods ─────────────────────────────────────────────────

    /**
     * Bridge: listStyles() → returns List<Map<String, Object>>
     * Returns empty list (actual style enumeration would need REST list endpoint)
     */
    public List<Map<String, Object>> listStyles() {
        return new ArrayList<>();
    }

    /**
     * Bridge: getStyle(workspace, styleName) → delegates to getStyleContent(styleName)
     */
    public String getStyle(String workspace, String styleName) {
        return getStyleContent(styleName);
    }

    /**
     * Bridge: createStyle(workspace, name, sld) → delegates to createStyle(name, sld)
     */
    public void createStyle(String workspace, String name, String sld) {
        createStyle(name, sld);
    }

    /**
     * Update an existing style's SLD content
     */
    public void updateStyle(String workspace, String styleName, String sld) {
        createStyle(styleName, sld); // GeoServer PUT semantics = create-or-update
    }

    /**
     * Delete a style from GeoServer
     */
    public void deleteStyle(String workspace, String styleName) {
        String url = String.format("%s/rest/styles/%s", geoServerUrl, styleName);
        try {
            HttpHeaders headers = createHeaders();
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            log.info("删除Style {}: OK", styleName);
        } catch (Exception e) {
            log.warn("删除Style {} 失败: {}", styleName, e.getMessage());
        }
    }

    /**
     * Bridge: getTemplates() → returns List<String> (template names only)
     */
    public List<String> getTemplates() {
        return getTemplatesAsMaps().stream()
                .map(m -> (String) m.get("id"))
                .toList();
    }

    // ─── Original methods ────────────────────────────────────────────────────

    /**
     * 创建SLD样式
     */
    public boolean createStyle(String styleName, String sldContent) {
        String url = String.format("%s/rest/styles", geoServerUrl);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.add("Accept", "application/xml");

            HttpEntity<String> request = new HttpEntity<>(sldContent, headers);

            // 首先尝试创建
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            // 然后设置SLD内容
            String sldUrl = String.format("%s/rest/styles/%s", geoServerUrl, styleName);
            HttpHeaders sldHeaders = createHeaders();
            sldHeaders.setContentType(MediaType.APPLICATION_XML);

            HttpEntity<String> sldRequest = new HttpEntity<>(sldContent, sldHeaders);
            ResponseEntity<String> sldResponse = restTemplate.exchange(
                    sldUrl, HttpMethod.PUT, sldRequest, String.class
            );

            log.info("创建Style {}: {}", styleName, sldResponse.getStatusCode());
            return sldResponse.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("创建Style失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 为图层设置样式
     */
    public boolean setLayerStyle(String layerName, String styleName) {
        String url = String.format("%s/rest/layers/%s:%s/styles",
                geoServerUrl, workspace, layerName);

        String body = String.format("""
            <layer>
                <defaultStyle>
                    <name>%s</name>
                </defaultStyle>
            </layer>
            """, styleName);

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            log.info("设置Layer {} Style {}: {}", layerName, styleName, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("设置Layer Style失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取已有样式的SLD内容
     */
    public String getStyleContent(String styleName) {
        String url = String.format("%s/rest/styles/%s", geoServerUrl, styleName);
        try {
            HttpHeaders headers = createHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("获取Style {} 失败: {}", styleName, e.getMessage());
        }
        return null;
    }

    /**
     * 获取预设SLD模板列表
     */
    public List<Map<String, String>> getTemplatesAsMaps() {
        List<Map<String, String>> templates = new ArrayList<>();
        templates.add(Map.of(
                "id", "point-default",
                "name", "点-默认圆形",
                "description", "蓝色圆形点符号",
                "sld", createDefaultPointStyle("#2563EB", 6)
        ));
        templates.add(Map.of(
                "id", "line-default",
                "name", "线-默认",
                "description", "蓝色实线",
                "sld", createDefaultLineStyle("#2563EB", 2)
        ));
        templates.add(Map.of(
                "id", "polygon-default",
                "name", "面-默认填充",
                "description", "蓝色半透明填充+边框",
                "sld", createDefaultPolygonStyle("#2563EB", "#1D4ED8", 0.7)
        ));
        templates.add(Map.of(
                "id", "point-gradient",
                "name", "点-渐变色",
                "description", "渐变色热力点",
                "sld", createGradientPointSLD()
        ));
        templates.add(Map.of(
                "id", "line-dashed",
                "name", "线-虚线",
                "description", "蓝色虚线",
                "sld", createDashedLineSLD()
        ));
        templates.add(Map.of(
                "id", "polygon-pattern",
                "name", "面-纹理填充",
                "description", "斜线纹理填充",
                "sld", createPatternPolygonSLD()
        ));
        return templates;
    }

    // ─── 预设 SLD 模板 ────────────────────────────────────────────────────

    private static String createGradientPointSLD() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <StyledLayerDescriptor version="1.0.0"
                xmlns="http://www.opengis.net/sld"
                xmlns:ogc="http://www.opengis.net/ogc">
              <NamedLayer>
                <Name>Gradient Point</Name>
                <UserStyle>
                  <Title>Heat-style Point</Title>
                  <FeatureTypeStyle>
                    <Rule>
                      <PointSymbolizer>
                        <Graphic>
                          <Mark><WellKnownName>circle</WellKnownName>
                            <Fill>
                              <CssParameter name="fill">#EF4444</CssParameter>
                            </Fill>
                          </Mark>
                          <Size>12</Size>
                        </Graphic>
                      </PointSymbolizer>
                    </Rule>
                  </FeatureTypeStyle>
                </UserStyle>
              </NamedLayer>
            </StyledLayerDescriptor>""";
    }

    private static String createDashedLineSLD() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <StyledLayerDescriptor version="1.0.0"
                xmlns="http://www.opengis.net/sld"
                xmlns:ogc="http://www.opengis.net/ogc">
              <NamedLayer>
                <Name>Dashed Line</Name>
                <UserStyle>
                  <Title>Dashed Line</Title>
                  <FeatureTypeStyle>
                    <Rule>
                      <LineSymbolizer>
                        <Stroke>
                          <CssParameter name="stroke">#3B82F6</CssParameter>
                          <CssParameter name="stroke-width">2</CssParameter>
                          <CssParameter name="stroke-dasharray">5 3</CssParameter>
                        </Stroke>
                      </LineSymbolizer>
                    </Rule>
                  </FeatureTypeStyle>
                </UserStyle>
              </NamedLayer>
            </StyledLayerDescriptor>""";
    }

    private static String createPatternPolygonSLD() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <StyledLayerDescriptor version="1.0.0"
                xmlns="http://www.opengis.net/sld"
                xmlns:ogc="http://www.opengis.net/ogc">
              <NamedLayer>
                <Name>Pattern Polygon</Name>
                <UserStyle>
                  <Title>Pattern Fill Polygon</Title>
                  <FeatureTypeStyle>
                    <Rule>
                      <PolygonSymbolizer>
                        <Fill>
                          <GraphicFill>
                            <Graphic>
                              <Mark><WellKnownName>circle</WellKnownName>
                                <Fill><CssParameter name="fill">#10B981</CssParameter></Fill>
                                <Size>4</Size>
                              </Mark>
                            </Graphic>
                          </GraphicFill>
                        </Fill>
                        <Stroke>
                          <CssParameter name="stroke">#047857</CssParameter>
                          <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                      </PolygonSymbolizer>
                    </Rule>
                  </FeatureTypeStyle>
                </UserStyle>
              </NamedLayer>
            </StyledLayerDescriptor>""";
    }

    /**
     * 生成默认点样式
     */
    public static String createDefaultPointStyle(String color, double size) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <StyledLayerDescriptor version="1.0.0"
                xmlns="http://www.opengis.net/sld"
                xmlns:ogc="http://www.opengis.net/ogc">
                <NamedLayer>
                    <Name>Default Point</Name>
                    <UserStyle>
                        <Title>Default Point Style</Title>
                        <FeatureTypeStyle>
                            <Rule>
                                <PointSymbolizer>
                                    <Graphic>
                                        <Mark>
                                            <WellKnownName>circle</WellKnownName>
                                            <Fill>
                                                <CssParameter name="fill">%s</CssParameter>
                                            </Fill>
                                        </Mark>
                                        <Size>%f</Size>
                                    </Graphic>
                                </PointSymbolizer>
                            </Rule>
                        </FeatureTypeStyle>
                    </UserStyle>
                </NamedLayer>
            </StyledLayerDescriptor>
            """, color, size);
    }

    /**
     * 生成默认线样式
     */
    public static String createDefaultLineStyle(String color, double width) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <StyledLayerDescriptor version="1.0.0"
                xmlns="http://www.opengis.net/sld"
                xmlns:ogc="http://www.opengis.net/ogc">
                <NamedLayer>
                    <Name>Default Line</Name>
                    <UserStyle>
                        <Title>Default Line Style</Title>
                        <FeatureTypeStyle>
                            <Rule>
                                <LineSymbolizer>
                                    <Stroke>
                                        <CssParameter name="stroke">%s</CssParameter>
                                        <CssParameter name="stroke-width">%f</CssParameter>
                                    </Stroke>
                                </LineSymbolizer>
                            </Rule>
                        </FeatureTypeStyle>
                    </UserStyle>
                </NamedLayer>
            </StyledLayerDescriptor>
            """, color, width);
    }

    /**
     * 生成默认面样式
     */
    public static String createDefaultPolygonStyle(String fillColor,
                                                     String strokeColor,
                                                     double opacity) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <StyledLayerDescriptor version="1.0.0"
                xmlns="http://www.opengis.net/sld"
                xmlns:ogc="http://www.opengis.net/ogc">
                <NamedLayer>
                    <Name>Default Polygon</Name>
                    <UserStyle>
                        <Title>Default Polygon Style</Title>
                        <FeatureTypeStyle>
                            <Rule>
                                <PolygonSymbolizer>
                                    <Fill>
                                        <CssParameter name="fill">%s</CssParameter>
                                        <CssParameter name="fill-opacity">%f</CssParameter>
                                    </Fill>
                                    <Stroke>
                                        <CssParameter name="stroke">%s</CssParameter>
                                        <CssParameter name="stroke-width">1</CssParameter>
                                    </Stroke>
                                </PolygonSymbolizer>
                            </Rule>
                        </FeatureTypeStyle>
                    </UserStyle>
                </NamedLayer>
            </StyledLayerDescriptor>
            """, fillColor, opacity, strokeColor);
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
