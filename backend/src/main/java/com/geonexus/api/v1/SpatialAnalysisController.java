package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.SpatialAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 空间分析控制器 - 空间分析 REST API
 */
@RestController
@RequestMapping("/api/v1/spatial")
@Tag(name = "空间分析", description = "空间分析（缓冲区/叠加/空间连接）")
@RequiredArgsConstructor
public class SpatialAnalysisController {

    private final SpatialAnalysisService spatialAnalysisService;

    @PostMapping("/buffer")
    @Operation(summary = "缓冲区分析")
    public ResponseEntity<ApiResponse<Map<String, Object>>> buffer(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> geometry = (Map<String, Object>) request.get("geometry");
        double distance = ((Number) request.get("distanceKm")).doubleValue();

        Geometry geom = spatialAnalysisService.parseGeometry(geometry);
        Geometry buffer = spatialAnalysisService.buffer(geom, distance);

        Map<String, Object> result = new HashMap<>();
        result.put("geometry", toGeoJSON(buffer));
        result.put("areaKm2", buffer.getArea());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/union")
    @Operation(summary = "叠加分析 - Union")
    public ResponseEntity<ApiResponse<Map<String, Object>>> union(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> geom1Map = (Map<String, Object>) request.get("geometry1");
        @SuppressWarnings("unchecked")
        Map<String, Object> geom2Map = (Map<String, Object>) request.get("geometry2");

        Geometry geom1 = spatialAnalysisService.parseGeometry(geom1Map);
        Geometry geom2 = spatialAnalysisService.parseGeometry(geom2Map);
        Geometry result = spatialAnalysisService.union(geom1, geom2);

        Map<String, Object> response = new HashMap<>();
        response.put("geometry", toGeoJSON(result));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/intersection")
    @Operation(summary = "叠加分析 - Intersection")
    public ResponseEntity<ApiResponse<Map<String, Object>>> intersection(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> geom1Map = (Map<String, Object>) request.get("geometry1");
        @SuppressWarnings("unchecked")
        Map<String, Object> geom2Map = (Map<String, Object>) request.get("geometry2");

        Geometry geom1 = spatialAnalysisService.parseGeometry(geom1Map);
        Geometry geom2 = spatialAnalysisService.parseGeometry(geom2Map);
        Geometry result = spatialAnalysisService.intersection(geom1, geom2);

        Map<String, Object> response = new HashMap<>();
        response.put("geometry", toGeoJSON(result));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/difference")
    @Operation(summary = "叠加分析 - Difference")
    public ResponseEntity<ApiResponse<Map<String, Object>>> difference(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> geom1Map = (Map<String, Object>) request.get("geometry1");
        @SuppressWarnings("unchecked")
        Map<String, Object> geom2Map = (Map<String, Object>) request.get("geometry2");

        Geometry geom1 = spatialAnalysisService.parseGeometry(geom1Map);
        Geometry geom2 = spatialAnalysisService.parseGeometry(geom2Map);
        Geometry result = spatialAnalysisService.difference(geom1, geom2);

        Map<String, Object> response = new HashMap<>();
        response.put("geometry", toGeoJSON(result));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/sym-difference")
    @Operation(summary = "叠加分析 - Symmetric Difference")
    public ResponseEntity<ApiResponse<Map<String, Object>>> symDifference(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> geom1Map = (Map<String, Object>) request.get("geometry1");
        @SuppressWarnings("unchecked")
        Map<String, Object> geom2Map = (Map<String, Object>) request.get("geometry2");

        Geometry geom1 = spatialAnalysisService.parseGeometry(geom1Map);
        Geometry geom2 = spatialAnalysisService.parseGeometry(geom2Map);
        Geometry result = spatialAnalysisService.symDifference(geom1, geom2);

        Map<String, Object> response = new HashMap<>();
        response.put("geometry", toGeoJSON(result));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter/bounds")
    @Operation(summary = "按边界框过滤要素")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> filterByBounds(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features = (List<Map<String, Object>>) request.get("features");
        double south = ((Number) request.get("south")).doubleValue();
        double west = ((Number) request.get("west")).doubleValue();
        double north = ((Number) request.get("north")).doubleValue();
        double east = ((Number) request.get("east")).doubleValue();

        List<Map<String, Object>> filtered = spatialAnalysisService.filterByBounds(features, south, west, north, east);
        return ResponseEntity.ok(ApiResponse.success(filtered));
    }

    @PostMapping("/join")
    @Operation(summary = "空间连接")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> spatialJoin(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features1 = (List<Map<String, Object>>) request.get("features1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features2 = (List<Map<String, Object>>) request.get("features2");
        String predicate = (String) request.getOrDefault("predicate", "intersects");

        List<Map<String, Object>> joined = spatialAnalysisService.spatialJoin(features1, features2, predicate);
        return ResponseEntity.ok(ApiResponse.success(joined));
    }

    @PostMapping("/parse")
    @Operation(summary = "解析GeoJSON为Geometry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> parseGeometry(@RequestBody Map<String, Object> geojson) {
        Geometry geom = spatialAnalysisService.parseGeometry(geojson);
        Map<String, Object> result = new HashMap<>();
        result.put("geometry", toGeoJSON(geom));
        result.put("type", geom != null ? geom.getGeometryType() : null);
        result.put("isValid", geom != null && geom.isValid());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Map<String, Object> toGeoJSON(Geometry geom) {
        Map<String, Object> geojson = new HashMap<>();
        if (geom == null) {
            return geojson;
        }
        geojson.put("type", geom.getGeometryType());
        geojson.put("coordinates", toCoordinates(geom));
        return geojson;
    }

    private Object toCoordinates(Geometry geom) {
        if (geom instanceof Point) {
            Point p = (Point) geom;
            return List.of(p.getX(), p.getY());
        } else if (geom instanceof LineString) {
            LineString ls = (LineString) geom;
            List<List<Double>> coords = new java.util.ArrayList<>();
            for (Coordinate c : ls.getCoordinates()) {
                coords.add(List.of(c.x, c.y));
            }
            return coords;
        } else if (geom instanceof Polygon) {
            Polygon poly = (Polygon) geom;
            List<List<List<Double>>> rings = new java.util.ArrayList<>();
            for (int i = 0; i < poly.getNumInteriorRing() + 1; i++) {
                LineString ring = i == 0 ? poly.getExteriorRing() : poly.getInteriorRingN(i - 1);
                List<List<Double>> coords = new java.util.ArrayList<>();
                for (Coordinate c : ring.getCoordinates()) {
                    coords.add(List.of(c.x, c.y));
                }
                rings.add(coords);
            }
            return rings;
        }
        return null;
    }
}
