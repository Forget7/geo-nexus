package com.geonexus.service;

import com.geonexus.model.MapGenerateRequest;
import com.geonexus.model.MapGenerateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.geonexus.model.dto.MapTemplateDTO;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapService {
    
    @Value("${geonexus.output.dir:./data/output}")
    private String outputDir;
    
    @Value("${geonexus.cesium.token:}")
    private String cesiumToken;
    
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> mapStore = new HashMap<>();
    
    private static final Map<String, TileLayer> TILE_LAYERS = Map.of(
        "osm", new TileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", "© OpenStreetMap"),
        "satellite", new TileLayer("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}", "Esri"),
        "dark", new TileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", "© CartoDB"),
        "terrain", new TileLayer("https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png", "© OpenTopoMap")
    );
    
    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(outputDir));
    }
    
    public MapGenerateResponse generateMap(MapGenerateRequest request) {
        String mapId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            // 根据模式生成HTML
            String html;
            String mode = request.getMode() != null ? request.getMode() : "2d";
            
            if ("3d".equals(mode)) {
                html = generateCesiumHtml(request);
            } else {
                html = generateLeafletHtml(request);
            }
            
            // 保存文件
            String filename = String.format("map_%s_%s.html", mapId, mode);
            Path filepath = Paths.get(outputDir, filename);
            Files.writeString(filepath, html);
            
            // 生成2D和3D版本
            String filename2d = String.format("map_%s_2d.html", mapId);
            String filename3d = String.format("map_%s_3d.html", mapId);
            
            Files.writeString(Paths.get(outputDir, filename2d), generateLeafletHtml(request));
            Files.writeString(Paths.get(outputDir, filename3d), generateCesiumHtml(request));
            
            // 存储
            Map<String, Object> mapData = new HashMap<>();
            mapData.put("id", mapId);
            mapData.put("geojson", request.getGeojson());
            mapData.put("html", html);
            mapData.put("mode", mode);
            mapStore.put(mapId, mapData);
            
            return MapGenerateResponse.builder()
                    .id(mapId)
                    .url(String.format("/api/v1/map/%s/html", mapId))
                    .url2d(String.format("/api/v1/map/%s/2d", mapId))
                    .url3d(String.format("/api/v1/map/%s/3d", mapId))
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to generate map", e);
            throw new RuntimeException("地图生成失败: " + e.getMessage(), e);
        }
    }
    
    private String generateLeafletHtml(MapGenerateRequest request) throws IOException {
        List<Double> center = request.getCenter() != null ? request.getCenter() : List.of(35.0, 105.0);
        int zoom = request.getZoom() != null ? request.getZoom() : 10;
        String tileType = request.getTileType() != null ? request.getTileType() : "osm";
        
        TileLayer tile = TILE_LAYERS.getOrDefault(tileType, TILE_LAYERS.get("osm"));
        String geojsonStr = request.getGeojson() != null ? 
                objectMapper.writeValueAsString(request.getGeojson()) : "null";
        
        return String.format("""
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GeoNexus Map - Leaflet 2D</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        body { margin: 0; padding: 0; }
        #map { width: 100%%; height: 100vh; }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        const map = L.map('map').setView([%f, %f], %d);
        
        L.tileLayer('%s', {
            attribution: '%s',
            maxZoom: 19
        }).addTo(map);
        
        L.control.scale().addTo(map);
        L.control.mousePosition().addTo(map);
        
        const geojsonData = %s;
        if (geojsonData) {
            L.geoJSON(geojsonData, {
                style: {
                    fillColor: '#2563EB',
                    color: '#1E40AF',
                    weight: 2,
                    fillOpacity: 0.7
                },
                onEachFeature: function(feature, layer) {
                    if (feature.properties) {
                        let popup = '';
                        for (let key in feature.properties) {
                            // HTML-escape user-provided property values to prevent XSS
                            var val = feature.properties[key];
                            var encoded = String(val)
                                .replace(/&/g, '&amp;')
                                .replace(/</g, '&lt;')
                                .replace(/>/g, '&gt;')
                                .replace(/"/g, '&quot;')
                                .replace(/'/g, '&#x27;');
                            popup += '<b>' + key + ':</b> ' + encoded + '<br>';
                        }
                        layer.bindPopup(popup);
                    }
                }
            }).addTo(map);
        }
    </script>
</body>
</html>
""", center.get(0), center.get(1), zoom, tile.url, tile.attribution, geojsonStr);
    }
    
    private String generateCesiumHtml(MapGenerateRequest request) {
        List<Double> center = request.getCenter() != null ? request.getCenter() : List.of(35.0, 105.0);
        int height = request.getHeight() != null ? request.getHeight() : 10000;
        String geojsonStr;
        try {
            geojsonStr = request.getGeojson() != null ? 
                    objectMapper.writeValueAsString(request.getGeojson()) : "null";
        } catch (Exception e) {
            geojsonStr = "null";
        }
        
        return String.format("""
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GeoNexus Map - Cesium 3D</title>
    <script src="https://cesium.com/downloads/cesiumjs/releases/1.104/Build/Cesium/Cesium.js"></script>
    <link href="https://cesium.com/downloads/cesiumjs/releases/1.104/Build/Cesium/Widgets/widgets.css" rel="stylesheet">
    <style>
        body { margin: 0; padding: 0; }
        #cesiumContainer { width: 100%%; height: 100vh; }
    </style>
</head>
<body>
    <div id="cesiumContainer"></div>
    <script>
        Cesium.Ion.defaultAccessToken = '%s';
        
        const viewer = new Cesium.Viewer('cesiumContainer', {
            terrain: Cesium.Terrain.fromWorldTerrain(),
            geocoder: false,
            homeButton: true,
            sceneModePicker: true,
            navigationHelpButton: false,
            baseLayerPicker: true,
            fullscreenButton: false,
            animation: false,
            timeline: false,
            creditContainer: document.createElement('div'),
        });
        
        viewer.camera.flyTo({
            destination: Cesium.Cartesian3.fromDegrees(%f, %f, %d),
            orientation: {
                heading: Cesium.Math.toRadians(0),
                pitch: Cesium.Math.toRadians(-45),
                roll: 0
            }
        });
        
        const geojsonData = %s;
        if (geojsonData) {
            Cesium.GeoJsonDataSource.load(geojsonData, {
                stroke: Cesium.Color.fromCssColorString('#2563EB'),
                fill: Cesium.Color.fromCssColorString('#2563EB').withAlpha(0.6),
                strokeWidth: 2
            }).then(function(dataSource) {
                viewer.dataSources.add(dataSource);
            });
        }
    </script>
</body>
</html>
""", cesiumToken, center.get(1), center.get(0), height, geojsonStr);
    }
    
    public Map<String, Object> getMapInfo(String mapId) {
        return mapStore.getOrDefault(mapId, Map.of("error", "Map not found"));
    }
    
    public String getMapHtml(String mapId, String mode) {
        Map<String, Object> mapData = mapStore.get(mapId);
        if (mapData == null) {
            return null;
        }
        
        try {
            if ("3d".equals(mode)) {
                MapGenerateRequest request = new MapGenerateRequest();
                request.setGeojson((Map<String, Object>) mapData.get("geojson"));
                return generateCesiumHtml(request);
            } else {
                MapGenerateRequest request = new MapGenerateRequest();
                request.setGeojson((Map<String, Object>) mapData.get("geojson"));
                return generateLeafletHtml(request);
            }
        } catch (IOException e) {
            String escaped = e.getMessage() == null ? "Unknown error"
                : e.getMessage().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
            return "<html><body><h1>Error generating map</h1><p>" + escaped + "</p></body></html>";
        }
    }
    
    
    /**
     * 基于模板创建地图
     */
    public MapGenerateResponse createFromTemplate(MapTemplateDTO template, String userId) {
        try {
            Map<String, Object> geojson = buildGeoJSONFromTemplate(template);
            MapGenerateRequest request = new MapGenerateRequest();
            request.setGeojson(geojson);
            request.setCenter(List.of(template.getCenterLng(), template.getCenterLat()));
            request.setZoom(template.getZoom());
            request.setTileType(template.getTileType());
            request.setMode("2d");
            
            MapGenerateResponse response = generateMap(request);
            log.info("Created map from template {} for user {}", template.getId(), userId);
            return response;
        } catch (Exception e) {
            log.error("Failed to create map from template {}", template.getId(), e);
            throw new RuntimeException("从模板创建地图失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据模板配置构建 GeoJSON
     */
    private Map<String, Object> buildGeoJSONFromTemplate(MapTemplateDTO template) {
        List<Map<String, Object>> features = new ArrayList<>();
        
        if (template.getLayers() != null) {
            for (MapTemplateDTO.LayerConfigDTO layer : template.getLayers()) {
                if ("marker".equals(layer.getType()) && layer.getLat() != null && layer.getLng() != null) {
                    features.add(Map.of(
                            "type", "Feature",
                            "geometry", Map.of(
                                    "type", "Point",
                                    "coordinates", List.of(layer.getLng(), layer.getLat())
                            ),
                            "properties", Map.of(
                                    "name", layer.getName() != null ? layer.getName() : "",
                                    "icon", layer.getIcon() != null ? layer.getIcon() : "marker",
                                    "style", layer.getStyle() != null ? layer.getStyle() : ""
                            )
                    ));
                } else if ("polygon".equals(layer.getType())) {
                    features.add(createSamplePolygonFeature(template.getCenterLat(), template.getCenterLng(), layer));
                }
            }
        }
        
        return Map.of(
                "type", "FeatureCollection",
                "features", features
        );
    }
    
    private Map<String, Object> createSamplePolygonFeature(double centerLat, double centerLng, MapTemplateDTO.LayerConfigDTO layer) {
        double offset = 0.05;
        return Map.of(
                "type", "Feature",
                "geometry", Map.of(
                        "type", "Polygon",
                        "coordinates", List.of(List.of(
                                List.of(centerLng - offset, centerLat - offset),
                                List.of(centerLng + offset, centerLat - offset),
                                List.of(centerLng + offset, centerLat + offset),
                                List.of(centerLng - offset, centerLat + offset),
                                List.of(centerLng - offset, centerLat - offset)
                        ))
                ),
                "properties", Map.of(
                        "name", layer.getName() != null ? layer.getName() : "区域",
                        "style", layer.getStyle() != null ? layer.getStyle() : "",
                        "fillColor", "#2563EB"
                )
        );
    }
    private record TileLayer(String url, String attribution) {}
}
