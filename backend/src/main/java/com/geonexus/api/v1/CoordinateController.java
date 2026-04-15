package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.CoordinateTransformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/coordinates")
@Tag(name = "坐标转换", description = "坐标系查询与坐标变换")
@RequiredArgsConstructor
public class CoordinateController {

    private final CoordinateTransformService coordService;

    // ===== CRS =====

    @GetMapping("/crs/{epsg}")
    @Operation(summary = "获取坐标系信息")
    public ResponseEntity<ApiResponse<CoordinateTransformService.CoordinateSystem>> getCRS(@PathVariable String epsg) {
        return ResponseEntity.ok(ApiResponse.success(coordService.getCRS(epsg)));
    }

    @GetMapping("/crs")
    @Operation(summary = "搜索坐标系")
    public ResponseEntity<ApiResponse<List<CoordinateTransformService.CoordinateSystem>>> searchCRS(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(coordService.searchCRS(keyword)));
    }

    @GetMapping("/crs/all")
    @Operation(summary = "列出所有坐标系")
    public ResponseEntity<ApiResponse<List<CoordinateTransformService.CoordinateSystem>>> getAllCRS() {
        return ResponseEntity.ok(ApiResponse.success(coordService.getAllCRS()));
    }

    @GetMapping("/crs/type/{type}")
    @Operation(summary = "按类型获取坐标系")
    public ResponseEntity<ApiResponse<List<CoordinateTransformService.CoordinateSystem>>> getCRSByType(@PathVariable String type) {
        return ResponseEntity.ok(ApiResponse.success(coordService.getCRSByType(type)));
    }

    // ===== Transform =====

    @PostMapping("/transform")
    @Operation(summary = "坐标转换")
    public ResponseEntity<ApiResponse<double[]>> transform(@RequestBody Map<String, Object> body) {
        double[] point = ((List<Number>) body.get("point")).stream()
            .mapToDouble(Number::doubleValue).toArray();
        String from = (String) body.get("fromEpsg");
        String to = (String) body.get("toEpsg");
        return ResponseEntity.ok(ApiResponse.success(coordService.transform(point, from, to)));
    }

    @PostMapping("/transform/batch")
    @Operation(summary = "批量坐标转换")
    public ResponseEntity<ApiResponse<List<double[]>>> transformBatch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<List<Number>> pointsRaw = (List<List<Number>>) body.get("points");
        List<double[]> points = pointsRaw.stream()
            .map(p -> p.stream().mapToDouble(Number::doubleValue).toArray())
            .toList();
        String from = (String) body.get("fromEpsg");
        String to = (String) body.get("toEpsg");
        return ResponseEntity.ok(ApiResponse.success(coordService.transformBatch(points, from, to)));
    }

    @PostMapping("/transform/geojson")
    @Operation(summary = "GeoJSON整图转换")
    public ResponseEntity<ApiResponse<String>> transformGeoJSON(@RequestBody Map<String, Object> body) {
        String geojson = (String) body.get("geojson");
        String from = (String) body.get("fromEpsg");
        String to = (String) body.get("toEpsg");
        return ResponseEntity.ok(ApiResponse.success(coordService.transformGeoJSON(geojson, from, to)));
    }

    @PostMapping("/lonlat-to-xy")
    @Operation(summary = "经纬度转平面坐标")
    public ResponseEntity<ApiResponse<double[]>> lonLatToXY(@RequestBody Map<String, Object> body) {
        double lon = ((Number) body.get("lon")).doubleValue();
        double lat = ((Number) body.get("lat")).doubleValue();
        String target = (String) body.get("targetEpsg");
        return ResponseEntity.ok(ApiResponse.success(coordService.lonLatToXY(lon, lat, target)));
    }

    @PostMapping("/xy-to-lonlat")
    @Operation(summary = "平面坐标转经纬度")
    public ResponseEntity<ApiResponse<double[]>> xyToLonLat(@RequestBody Map<String, Object> body) {
        double x = ((Number) body.get("x")).doubleValue();
        double y = ((Number) body.get("y")).doubleValue();
        String source = (String) body.get("sourceEpsg");
        return ResponseEntity.ok(ApiResponse.success(coordService.xyToLonLat(x, y, source)));
    }

    // ===== Geodetic =====

    @GetMapping("/utm/zone")
    @Operation(summary = "获取UTM分带号")
    public ResponseEntity<ApiResponse<Integer>> getUTMZone(@RequestParam double longitude) {
        return ResponseEntity.ok(ApiResponse.success(coordService.getUTMZone(longitude)));
    }

    @GetMapping("/central-meridian")
    @Operation(summary = "计算中央经线")
    public ResponseEntity<ApiResponse<Double>> calculateCentralMeridian(@RequestParam double longitude) {
        return ResponseEntity.ok(ApiResponse.success(coordService.calculateCentralMeridian(longitude)));
    }

    @GetMapping("/hemisphere")
    @Operation(summary = "判断南北半球")
    public ResponseEntity<ApiResponse<Boolean>> isNorthern(@RequestParam double latitude) {
        return ResponseEntity.ok(ApiResponse.success(coordService.isNorthern(latitude)));
    }

    @PostMapping("/transform/seven-params")
    @Operation(summary = "七参数转换")
    public ResponseEntity<ApiResponse<double[]>> transformWithSevenParams(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> pt = (List<Number>) body.get("point");
        double[] point = pt.stream().mapToDouble(Number::doubleValue).toArray();
        boolean reverse = Boolean.TRUE.equals(body.get("reverse"));
        CoordinateTransformService.SevenParams params = new CoordinateTransformService.SevenParams();
        params.setDx(((Number) body.getOrDefault("dx", 0)).doubleValue());
        params.setDy(((Number) body.getOrDefault("dy", 0)).doubleValue());
        params.setDz(((Number) body.getOrDefault("dz", 0)).doubleValue());
        params.setRx(((Number) body.getOrDefault("rx", 0)).doubleValue());
        params.setRy(((Number) body.getOrDefault("ry", 0)).doubleValue());
        params.setRz(((Number) body.getOrDefault("rz", 0)).doubleValue());
        params.setScale(((Number) body.getOrDefault("scale", 0)).doubleValue());
        return ResponseEntity.ok(ApiResponse.success(
            coordService.transformWithSevenParams(point, params, reverse)));
    }

    @PostMapping("/geodetic-to-cartesian")
    @Operation(summary = "大地坐标转地心坐标")
    public ResponseEntity<ApiResponse<double[]>> geodeticToCartesian(@RequestBody Map<String, Object> body) {
        double lon = ((Number) body.get("lon")).doubleValue();
        double lat = ((Number) body.get("lat")).doubleValue();
        double height = ((Number) body.getOrDefault("height", 0)).doubleValue();
        String epsg = (String) body.get("epsg");
        return ResponseEntity.ok(ApiResponse.success(
            coordService.geodeticToCartesian(lon, lat, height, epsg)));
    }

    @PostMapping("/cartesian-to-geodetic")
    @Operation(summary = "地心坐标转大地坐标")
    public ResponseEntity<ApiResponse<double[]>> cartesianToGeodetic(@RequestBody Map<String, Object> body) {
        double x = ((Number) body.get("x")).doubleValue();
        double y = ((Number) body.get("y")).doubleValue();
        double z = ((Number) body.get("z")).doubleValue();
        String epsg = (String) body.get("epsg");
        return ResponseEntity.ok(ApiResponse.success(
            coordService.cartesianToGeodetic(x, y, z, epsg)));
    }

    @GetMapping("/height-anomaly")
    @Operation(summary = "计算高程异常")
    public ResponseEntity<ApiResponse<Double>> calculateHeightAnomaly(
            @RequestParam double lat, @RequestParam double lon) {
        return ResponseEntity.ok(ApiResponse.success(coordService.calculateHeightAnomaly(lat, lon)));
    }

    @GetMapping("/distance")
    @Operation(summary = "计算两点距离")
    public ResponseEntity<ApiResponse<Double>> calculateDistance(
            @RequestParam double lon1, @RequestParam double lat1,
            @RequestParam double lon2, @RequestParam double lat2) {
        return ResponseEntity.ok(ApiResponse.success(
            coordService.calculateDistance(lon1, lat1, lon2, lat2)));
    }

    @GetMapping("/bearing")
    @Operation(summary = "计算方位角")
    public ResponseEntity<ApiResponse<Double>> calculateBearing(
            @RequestParam double lon1, @RequestParam double lat1,
            @RequestParam double lon2, @RequestParam double lat2) {
        return ResponseEntity.ok(ApiResponse.success(
            coordService.calculateBearing(lon1, lat1, lon2, lat2)));
    }
}
