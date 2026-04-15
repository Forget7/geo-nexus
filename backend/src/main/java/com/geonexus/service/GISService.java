package com.geonexus.service;
import com.geonexus.common.geometry.GeoJSONUtils;
import com.geonexus.model.ToolExecuteRequest;
import com.geonexus.model.ToolInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Slf4j@Service
public class GISService {
    private final ObjectMapper objectMapper;
    private final Map<String, ToolInfo> tools = new LinkedHashMap<>();
    private final GeometryFactory gf = new GeometryFactory();
    private final GeometryCalcService gcs;
    private final SpatialAnalysisService sas;
    private final GeometryProcessService gps;
    private final ProjectionService ps;
    
    public GISService(ObjectMapper objectMapper, GeometryCalcService gcs, SpatialAnalysisService sas, GeometryProcessService gps, ProjectionService ps) {
        this.objectMapper = objectMapper;
        this.gcs = gcs;
        this.sas = sas;
        this.gps = gps;
        this.ps = ps;
    }
    
    @PostConstruct
    public void initTools() {
        tools.put("calculate_distance", ToolInfo.builder().name("calculate_distance").description("计算两点间距离").parameters(Map.of("point1", "", "point2", "", "unit", "km")).build());
        tools.put("calculate_area", ToolInfo.builder().name("calculate_area").description("计算几何面积").parameters(Map.of("geometry", "", "unit", "sqkm")).build());
        tools.put("calculate_length", ToolInfo.builder().name("calculate_length").description("计算几何长度").parameters(Map.of("geometry", "", "unit", "km")).build());
        tools.put("buffer_analysis", ToolInfo.builder().name("buffer_analysis").description("缓冲区分析").parameters(Map.of("geometry", "", "distanceKm", "")).build());
        tools.put("overlay_analysis", ToolInfo.builder().name("overlay_analysis").description("叠加分析").parameters(Map.of("layer1", "", "layer2", "", "operation", "")).build());
        tools.put("spatial_filter", ToolInfo.builder().name("spatial_filter").description("空间过滤").parameters(Map.of("geojson", "", "bounds", "")).build());
        tools.put("simplify_geometry", ToolInfo.builder().name("simplify_geometry").description("几何简化").parameters(Map.of("geometry", "", "tolerance", "")).build());
        tools.put("convex_hull", ToolInfo.builder().name("convex_hull").description("凸包计算").parameters(Map.of("geometry", "")).build());
        tools.put("reproject", ToolInfo.builder().name("reproject").description("坐标投影转换").parameters(Map.of("geometry", "", "fromCrs", "", "toCrs", "")).build());
        tools.put("centroid", ToolInfo.builder().name("centroid").description("计算质心").parameters(Map.of("geometry", "")).build());
        tools.put("bounding_box", ToolInfo.builder().name("bounding_box").description("计算边界框").parameters(Map.of("geometry", "")).build());
        tools.put("intersects", ToolInfo.builder().name("intersects").description("判断相交").parameters(Map.of("geometry1", "", "geometry2", "")).build());
        tools.put("contains", ToolInfo.builder().name("contains").description("判断包含").parameters(Map.of("geometry1", "", "geometry2", "")).build());
        tools.put("distance", ToolInfo.builder().name("distance").description("计算距离").parameters(Map.of("geometry1", "", "geometry2", "", "unit", "km")).build());
        tools.put("nearest_points", ToolInfo.builder().name("nearest_points").description("最近点对").parameters(Map.of("geometry1", "", "geometry2", "")).build());
        tools.put("geocode", ToolInfo.builder().name("geocode").description("地址转坐标").parameters(Map.of("address", "")).build());
        tools.put("reverse_geocode", ToolInfo.builder().name("reverse_geocode").description("坐标转地址").parameters(Map.of("lat", "", "lon", "")).build());
        tools.put("load_shapefile", ToolInfo.builder().name("load_shapefile").description("加载Shapefile").parameters(Map.of("path", "")).build());
        tools.put("load_geojson", ToolInfo.builder().name("load_geojson").description("加载GeoJSON").parameters(Map.of("path", "")).build());
    }
    
    public List<ToolInfo> listTools() { return new ArrayList<>(tools.values()); }
    
    public Map<String, Object> calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2, String unit) { return gcs.haversineDistance(lat1, lon1, lat2, lon2, unit); }

    /**
     * 缓冲区分析便捷端点
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> bufferAnalysis(Map<String, Object> request) {
        Map<String, Object> geojson = (Map<String, Object>) request.get("geometry");
        double distanceKm = ((Number) request.getOrDefault("distanceKm", 1.0)).doubleValue();
        Geometry g = parseGeoJSON(geojson);
        return Map.of("buffer", GeoJSONUtils.toGeoJSON(sas.buffer(g, distanceKm)),
                "distanceKm", distanceKm, "inputGeometry", geojson);
    }

    /**
     * 地址编码便捷端点
     */
    public Map<String, Object> geocode(String address) {
        log.info("地理编码: {}", address);
        return Map.of("input", address, "message", "需要配置Nominatim服务");
    }

    /**
     * 坐标转地址便捷端点
     */
    public Map<String, Object> reverseGeocode(Double lat, Double lon) {
        return Map.of("lat", lat, "lon", lon, "message", "需要配置Nominatim服务");
    }

    /**
     * 点面包含判断（within）：判断geometry1是否被geometry2包含
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> within(Map<String, Object> request) {
        try {
            Map<String, Object> pointGeojson = (Map<String, Object>) request.get("point");
            Map<String, Object> polygonGeojson = (Map<String, Object>) request.get("polygon");
            Geometry point = parseGeoJSON(pointGeojson);
            Geometry polygon = parseGeoJSON(polygonGeojson);
            boolean isWithin = polygon.contains(point);
            return Map.of("within", isWithin,
                    "point", pointGeojson,
                    "polygon", polygonGeojson);
        } catch (Exception e) {
            log.error("within工具失败", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeTool(ToolExecuteRequest request) {
        String t = request.getTool();
        Map<String, Object> p = request.getParams();
        try {
            return switch (t) {
                case "calculate_distance" -> { List<Double> p1=(List<Double>)p.get("point1"), p2=(List<Double>)p.get("point2"); yield calculateDistance(p1.get(0),p1.get(1),p2.get(0),p2.get(1),(String)p.getOrDefault("unit","km")); }
                case "calculate_area" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); yield Map.of("area",gcs.calculateArea(g,(String)p.getOrDefault("unit","sqkm")),"unit",p.getOrDefault("unit","sqkm"),"geometry",p.get("geometry")); }
                case "calculate_length" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); yield Map.of("length",gcs.calculateLength(g,(String)p.getOrDefault("unit","km")),"unit",p.getOrDefault("unit","km"),"geometry",p.get("geometry")); }
                case "buffer_analysis" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); double d=((Number)p.get("distanceKm")).doubleValue(); yield Map.of("buffer",GeoJSONUtils.toGeoJSON(sas.buffer(g,d)),"distanceKm",d,"inputGeometry",p.get("geometry")); }
                case "overlay_analysis" -> { Geometry g1=GeoJSONUtils.extractGeometryFromGeoJSON((Map<String,Object>)p.get("layer1")); Geometry g2=GeoJSONUtils.extractGeometryFromGeoJSON((Map<String,Object>)p.get("layer2")); String op=(String)p.getOrDefault("operation","union"); Geometry r=switch(op.toLowerCase()){case"intersection"->sas.intersection(g1,g2);case"difference"->sas.difference(g1,g2);case"symdifference"->sas.symDifference(g1,g2);default->sas.union(g1,g2);}; yield Map.of("result",GeoJSONUtils.toGeoJSON(r),"operation",op,"layer1",p.get("layer1"),"layer2",p.get("layer2")); }
                case "spatial_filter" -> { List<Double> b=(List<Double>)p.get("bounds"); List<Map<String,Object>> feats=(List<Map<String,Object>>)((Map<String,Object>)p.get("geojson")).get("features"); List<Map<String,Object>> filtered=sas.filterByBounds(feats,b.get(0),b.get(1),b.get(2),b.get(3)); yield Map.of("geojson",Map.of("type","FeatureCollection","features",filtered),"totalCount",feats.size(),"filteredCount",filtered.size(),"bounds",b); }
                case "simplify_geometry" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); double tol=((Number)p.get("tolerance")).doubleValue(); yield Map.of("simplified",GeoJSONUtils.toGeoJSON(gps.simplify(g,tol)),"original",p.get("geometry"),"tolerance",tol); }
                case "convex_hull" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); yield Map.of("convexHull",GeoJSONUtils.toGeoJSON(gps.convexHull(g)),"inputGeometry",p.get("geometry")); }
                case "reproject" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); String from=(String)p.get("fromCrs"),to=(String)p.get("toCrs"); yield Map.of("reprojected",GeoJSONUtils.toGeoJSON(ps.transform(g,from,to)),"from",from,"to",to); }
                case "centroid" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); yield Map.of("centroid",GeoJSONUtils.toGeoJSON(gcs.calculateCentroid(g)),"inputGeometry",p.get("geometry")); }
                case "bounding_box" -> { Geometry g=parseGeoJSON((Map<String,Object>)p.get("geometry")); yield Map.of("bounds",gps.getBoundingBoxInfo(g),"inputGeometry",p.get("geometry")); }
                case "intersects" -> { Geometry g1=parseGeoJSON((Map<String,Object>)p.get("geometry1")); Geometry g2=parseGeoJSON((Map<String,Object>)p.get("geometry2")); yield Map.of("intersects",g1.intersects(g2),"geometry1",p.get("geometry1"),"geometry2",p.get("geometry2")); }
                case "contains" -> { Geometry g1=parseGeoJSON((Map<String,Object>)p.get("geometry1")); Geometry g2=parseGeoJSON((Map<String,Object>)p.get("geometry2")); yield Map.of("contains",g1.contains(g2),"geometry1",p.get("geometry1"),"geometry2",p.get("geometry2")); }
                case "distance" -> { Geometry g1=parseGeoJSON((Map<String,Object>)p.get("geometry1")); Geometry g2=parseGeoJSON((Map<String,Object>)p.get("geometry2")); String unit=(String)p.getOrDefault("unit","km"); yield Map.of("distance",gcs.geometryDistance(g1,g2,unit),"unit",unit,"geometry1",p.get("geometry1"),"geometry2",p.get("geometry2")); }
                case "nearest_points" -> { Geometry g1=parseGeoJSON((Map<String,Object>)p.get("geometry1")); Geometry g2=parseGeoJSON((Map<String,Object>)p.get("geometry2")); Coordinate[] np=org.locationtech.jts.operation.distance.DistanceOp.nearestPoints(g1,g2); yield Map.of("point1",GeoJSONUtils.toGeoJSON(gf.createPoint(np[0])),"point2",GeoJSONUtils.toGeoJSON(gf.createPoint(np[1])),"geometry1",p.get("geometry1"),"geometry2",p.get("geometry2")); }
                case "geocode" -> { log.info("地理编码: {}",p.get("address")); yield Map.of("input",p.get("address"),"message","需要配置Nominatim服务"); }
                case "reverse_geocode" -> yield Map.of("lat",p.get("lat"),"lon",p.get("lon"),"message","需要配置Nominatim服务");
                case "load_shapefile" -> execLoadShp(p);
                case "load_geojson" -> execLoadGeoJSON(p);
                default -> Map.of("error", "Unknown tool: " + t);
            };
        } catch (Exception e) { log.error("Tool {} failed", t, e); return Map.of("error", e.getMessage()); }
    }
    
    private Geometry parseGeoJSON(Map<String, Object> geojson) { return GeoJSONUtils.parseGeometry(geojson); }
    
    private Map<String, Object> execLoadShp(Map<String, Object> p) throws Exception {
        File file = new File((String)p.get("path")); if (!file.exists()) return Map.of("error","File not found");
        org.geotools.data.shapefile.ShapefileDataStore store = new org.geotools.data.shapefile.ShapefileDataStore(file.toURI().toURL());
        store.setCharset(StandardCharsets.UTF_8);
        String typeName = store.getTypeNames()[0];
        var source = store.getFeatureSource(typeName); var collection = source.getFeatures(); List<Map<String,Object>> features = new ArrayList<>();
        try (var it = collection.features()) { while (it.hasNext()) { features.add(simpleFeatureToMap((org.opengis.feature.simple.SimpleFeature)it.next())); } }
        store.dispose(); return Map.of("typeName",typeName,"featureCount",features.size(),"features",features);
    }
    
    private Map<String, Object> execLoadGeoJSON(Map<String, Object> p) throws Exception {
        File file = new File((String)p.get("path")); if (!file.exists()) return Map.of("error","File not found");
        String content = Files.readString(file.toPath()); Map<String,Object> geojson = objectMapper.readValue(content, Map.class);
        return Map.of("geojson",geojson,"path",p.get("path"));
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> simpleFeatureToMap(org.opengis.feature.simple.SimpleFeature f) {
        Map<String, Object> result = new HashMap<>(); result.put("id",f.getID());
        result.put("geometry", GeoJSONUtils.toGeoJSON((Geometry)f.getDefaultGeometry()));
        Map<String, Object> props = new HashMap<>();
        f.getProperties().forEach(pp -> { if (!"Geometry".equals(pp.getName().getLocalPart())) props.put(pp.getName().getLocalPart(),pp.getValue()); });
        result.put("properties", props); return result;
    }
}
