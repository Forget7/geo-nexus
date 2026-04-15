package com.geonexus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geonexus.domain.ProcessDefinitionEntity;
import com.geonexus.repository.ProcessDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OGC API - Processes 服务实现
 * 提供标准化空间处理接口，支持同步/异步执行
 */
@Slf4j
@Service
public class OGCProcessService {

    private final ProcessDefinitionRepository repo;
    private final AdvancedGeoProcessingService advancedGeo;
    private final GeometryProcessService geometryProcessService;
    private final GeometryFactory geometryFactory;
    private final ObjectMapper objectMapper;

    // Built-in process implementations
    private final Map<String, ProcessHandler> handlers = new ConcurrentHashMap<>();

    public OGCProcessService(ProcessDefinitionRepository repo,
                             AdvancedGeoProcessingService advancedGeo,
                             GeometryProcessService geometryProcessService,
                             ObjectMapper objectMapper) {
        this.repo = repo;
        this.advancedGeo = advancedGeo;
        this.geometryProcessService = geometryProcessService;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.objectMapper = objectMapper;
        registerBuiltInProcesses();
        initializeProcessDefs();
    }

    private void registerBuiltInProcesses() {
        // buffer: input GeoJSON geometry + distance → buffered geometry
        handlers.put("buffer", (inputs) -> {
            Geometry geo = parseGeometry(inputs.get("geometry"));
            Double distance = ((Number) inputs.getOrDefault("distance", 0.001)).doubleValue();
            Geometry buffered = bufferGeo(geo, distance);
            return Map.of("output", toGeoJSON(buffered));
        });

        // simplify: input GeoJSON + tolerance → simplified geometry
        handlers.put("simplify", (inputs) -> {
            Geometry geo = parseGeometry(inputs.get("geometry"));
            Double tolerance = ((Number) inputs.getOrDefault("tolerance", 0.001)).doubleValue();
            Geometry simplified = simplifyGeo(geo, tolerance);
            return Map.of("output", toGeoJSON(simplified));
        });

        // convexhull: input GeoJSON FeatureCollection → hull polygon
        handlers.put("convexhull", (inputs) -> {
            Geometry input = parseGeoJSON(inputs.get("features"));
            Geometry hull = convexHullGeo(input);
            return Map.of("hull", toGeoJSON(hull));
        });

        // bufferGeoJSON: full GeoJSON with distance param
        handlers.put("bufferGeoJSON", (inputs) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> fc = (Map<String, Object>) inputs.get("geojson");
            Double distance = ((Number) inputs.getOrDefault("distance", 0.001)).doubleValue();
            List<Geometry> geoms = parseFeatureCollection(fc);
            List<Map<String, Object>> results = new ArrayList<>();
            for (Geometry g : geoms) {
                Geometry buffered = bufferGeo(g, distance);
                results.add(Map.of(
                        "type", "Feature",
                        "properties", Map.of(),
                        "geometry", toGeoJSON(buffered)
                ));
            }
            return Map.of("output", buildFeatureCollection(results));
        });

        // intersect: two GeoJSON layers → intersection
        handlers.put("intersect", (inputs) -> {
            Geometry a = parseGeometry(inputs.get("a"));
            Geometry b = parseGeometry(inputs.get("b"));
            Geometry intersection = intersectGeo(a, b);
            return Map.of("intersection", toGeoJSON(intersection));
        });
    }

    private void initializeProcessDefs() {
        if (repo.count() > 0) return;
        List<ProcessDefinitionEntity> defs = List.of(
            makeDef("buffer", "空间缓冲", "给定几何周围生成指定距离的缓冲区域", "geometry",
                List.of(new ProcessDef("geometry","几何对象","输入几何","GeoJSON",true,null),
                         new ProcessDef("distance","缓冲距离","缓冲半径（度或米）","number",true,0.001)),
                List.of(new ProcessDef("output","缓冲结果","输出缓冲后几何","GeoJSON",false,null)),
                true),
            makeDef("simplify", "几何简化", "使用 Douglas-Peucker 算法简化几何对象", "geometry",
                List.of(new ProcessDef("geometry","几何对象","待简化几何","GeoJSON",true,null),
                         new ProcessDef("tolerance","容差","简化容差值","number",true,0.001)),
                List.of(new ProcessDef("output","简化结果","输出简化后几何","GeoJSON",false,null)),
                true),
            makeDef("convexhull", "凸包计算", "计算给定点集的凸包多边形", "geometry",
                List.of(new ProcessDef("features","要素集合","输入点集","GeoJSON",true,null)),
                List.of(new ProcessDef("hull","凸包","输出凸包多边形","GeoJSON",false,null)),
                false),
            makeDef("bufferGeoJSON", "批量缓冲", "对整个 GeoJSON 图层进行缓冲操作", "transformation",
                List.of(new ProcessDef("geojson","GeoJSON","输入图层","GeoJSON",true,null),
                         new ProcessDef("distance","缓冲距离","缓冲半径","number",true,0.001)),
                List.of(new ProcessDef("output","结果图层","输出缓冲后图层","GeoJSON",false,null)),
                true),
            makeDef("intersect", "空间交集", "计算两个图层的空间交集", "analysis",
                List.of(new ProcessDef("a","图层A","第一个图层","GeoJSON",true,null),
                         new ProcessDef("b","图层B","第二个图层","GeoJSON",true,null)),
                List.of(new ProcessDef("intersection","交集结果","输出交集要素","GeoJSON",false,null)),
                true)
        );
        for (var def : defs) {
            repo.save(def);
        }
    }

    private ProcessDefinitionEntity makeDef(String pid, String title, String desc,
            String cat, List<ProcessDef> ins, List<ProcessDef> outs, boolean async) {
        ProcessDefinitionEntity.ProcessInput[] inputs = ins.stream()
            .map(p -> ProcessDefinitionEntity.ProcessInput.builder()
                .id(p.id).title(p.title).description(p.desc)
                .type(p.type).required(p.required).defaultValue(
                    p.defaultValue != null ? p.defaultValue.toString() : null)
                .build())
            .toArray(ProcessDefinitionEntity.ProcessInput[]::new);

        ProcessDefinitionEntity.ProcessOutput[] outputs = outs.stream()
            .map(p -> ProcessDefinitionEntity.ProcessOutput.builder()
                .id(p.id).title(p.title).description(p.desc).type(p.type)
                .build())
            .toArray(ProcessDefinitionEntity.ProcessOutput[]::new);

        return ProcessDefinitionEntity.builder()
            .processId(pid)
            .title(title)
            .description(desc)
            .version("1.0.0")
            .category(cat)
            .inputs(Arrays.asList(inputs))
            .outputs(Arrays.asList(outputs))
            .supportsAsync(async)
            .build();
    }

    // raw record for builder
    private record ProcessDef(String id, String title, String desc, String type,
                              boolean required, Object defaultValue) {}

    // ===== API methods =====

    public List<ProcessDefinitionEntity> getAllProcesses() {
        return repo.findAllByOrderByCategoryAscProcessIdAsc();
    }

    public ProcessDefinitionEntity getProcess(String processId) {
        return repo.findByProcessId(processId).orElse(null);
    }

    /**
     * Execute a process synchronously.
     * inputs: Map of inputId -> value (GeoJSON objects or primitives)
     */
    public Map<String, Object> executeProcess(String processId, Map<String, Object> inputs) {
        ProcessHandler handler = handlers.get(processId);
        if (handler == null) throw new RuntimeException("Process not found: " + processId);
        return handler.execute(inputs);
    }

    /**
     * Execute a process asynchronously (returns job ID).
     */
    public String submitJob(String processId, Map<String, Object> inputs) {
        String jobId = UUID.randomUUID().toString();
        CompletableFuture.runAsync(() -> {
            try {
                executeProcess(processId, inputs);
            } catch (Exception e) {
                log.error("Async job {} failed: {}", jobId, e.getMessage());
            }
        });
        return jobId;
    }

    // ===== GeoJSON → JTS geometry =====

    /**
     * Parse a GeoJSON geometry object to JTS Geometry.
     * Handles Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon.
     */
    private Geometry parseGeometry(Object geoObj) {
        if (geoObj == null) return geometryFactory.createPoint(new Coordinate(0, 0));
        try {
            Map<String, Object> geo = (Map<String, Object>) geoObj;
            String type = (String) geo.getOrDefault("type", "Point");
            Object coords = geo.get("coordinates");

            return switch (type) {
                case "Point" -> parsePoint(coords);
                case "LineString" -> parseLineString(coords);
                case "Polygon" -> parsePolygon(coords);
                case "MultiPoint" -> parseMultiPoint(coords);
                case "MultiLineString" -> parseMultiLineString(coords);
                case "MultiPolygon" -> parseMultiPolygon(coords);
                case "GeometryCollection" -> parseGeometryCollection(geo);
                default -> geometryFactory.createPoint(new Coordinate(0, 0));
            };
        } catch (Exception e) {
            log.warn("Failed to parse geometry: {}", e.getMessage());
            return geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }

    @SuppressWarnings("unchecked")
    private Point parsePoint(Object coords) {
        List<Double> c = (List<Double>) coords;
        return geometryFactory.createPoint(new Coordinate(c.get(0), c.get(1)));
    }

    @SuppressWarnings("unchecked")
    private LineString parseLineString(Object coords) {
        List<List<Double>> c = (List<List<Double>>) coords;
        Coordinate[] jtsCoords = c.stream()
            .map(p -> new Coordinate(p.get(0), p.get(1)))
            .toArray(Coordinate[]::new);
        return geometryFactory.createLineString(jtsCoords);
    }

    @SuppressWarnings("unchecked")
    private Polygon parsePolygon(Object coords) {
        List<List<List<Double>>> rings = (List<List<List<Double>>>) coords;
        LinearRing shell = createLinearRing(rings.get(0));
        LinearRing[] holes = new LinearRing[rings.size() - 1];
        for (int i = 1; i < rings.size(); i++) {
            holes[i - 1] = createLinearRing(rings.get(i));
        }
        return geometryFactory.createPolygon(shell, holes);
    }

    private LinearRing createLinearRing(List<List<Double>> coords) {
        Coordinate[] jtsCoords = coords.stream()
            .map(p -> new Coordinate(p.get(0), p.get(1)))
            .toArray(Coordinate[]::new);
        return geometryFactory.createLinearRing(jtsCoords);
    }

    @SuppressWarnings("unchecked")
    private MultiPoint parseMultiPoint(Object coords) {
        List<List<Double>> c = (List<List<Double>>) coords;
        Point[] points = c.stream()
            .map(p -> geometryFactory.createPoint(new Coordinate(p.get(0), p.get(1))))
            .toArray(Point[]::new);
        return geometryFactory.createMultiPoint(points);
    }

    @SuppressWarnings("unchecked")
    private MultiLineString parseMultiLineString(Object coords) {
        List<List<List<Double>>> lines = (List<List<List<Double>>>) coords;
        LineString[] lineStrings = lines.stream()
            .map(this::parseLineString)
            .toArray(LineString[]::new);
        return geometryFactory.createMultiLineString(lineStrings);
    }

    @SuppressWarnings("unchecked")
    private MultiPolygon parseMultiPolygon(Object coords) {
        List<List<List<List<Double>>>> polys = (List<List<List<List<Double>>>>) coords;
        Polygon[] polygons = polys.stream()
            .map(this::parsePolygon)
            .toArray(Polygon[]::new);
        return geometryFactory.createMultiPolygon(polygons);
    }

    @SuppressWarnings("unchecked")
    private Geometry parseGeometryCollection(Map<String, Object> geo) {
        List<Map<String, Object>> geoms = (List<Map<String, Object>>) geo.get("geometries");
        Geometry[] allGeoms = geoms.stream()
            .map(this::parseGeometry)
            .toArray(Geometry[]::new);
        return geometryFactory.createGeometryCollection(allGeoms);
    }

    /**
     * Parse a GeoJSON FeatureCollection or array of features into a GeometryCollection.
     */
    @SuppressWarnings("unchecked")
    private Geometry parseGeoJSON(Object fcObj) {
        if (fcObj == null) return geometryFactory.createPoint(new Coordinate(0, 0));
        try {
            Map<String, Object> fc = (Map<String, Object>) fcObj;
            String type = (String) fc.getOrDefault("type", "");
            List<Map<String, Object>> features;

            if ("FeatureCollection".equals(type)) {
                features = (List<Map<String, Object>>) fc.get("features");
            } else if ("Feature".equals(type)) {
                features = List.of(fc);
            } else if (fcObj instanceof List) {
                features = (List<Map<String, Object>>) fcObj;
            } else {
                return parseGeometry(fcObj);
            }

            List<Geometry> geoms = new ArrayList<>();
            for (Map<String, Object> feature : features) {
                Object geomObj = feature.get("geometry");
                if (geomObj != null) {
                    geoms.add(parseGeometry(geomObj));
                }
            }
            if (geoms.isEmpty()) {
                return geometryFactory.createPoint(new Coordinate(0, 0));
            }
            return geometryFactory.createGeometryCollection(geoms.toArray(new Geometry[0]));
        } catch (Exception e) {
            log.warn("Failed to parse GeoJSON: {}", e.getMessage());
            return geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }

    /**
     * Parse a GeoJSON FeatureCollection into a list of individual geometries.
     */
    @SuppressWarnings("unchecked")
    private List<Geometry> parseFeatureCollection(Map<String, Object> fc) {
        List<Map<String, Object>> features = (List<Map<String, Object>>) fc.get("features");
        List<Geometry> geoms = new ArrayList<>();
        if (features != null) {
            for (Map<String, Object> feature : features) {
                Object geomObj = feature.get("geometry");
                if (geomObj != null) {
                    geoms.add(parseGeometry(geomObj));
                }
            }
        }
        return geoms;
    }

    // ===== Geometry operations =====

    private Geometry bufferGeo(Geometry geo, double distance) {
        try {
            return geo.buffer(distance);
        } catch (Exception e) {
            log.error("Buffer failed: {}", e.getMessage());
            return geo;
        }
    }

    private Geometry simplifyGeo(Geometry geo, double tolerance) {
        try {
            return org.locationtech.jts.simplify.TopologyPreservingSimplifier.simplify(geo, tolerance);
        } catch (Exception e) {
            log.error("Simplify failed: {}", e.getMessage());
            return geo;
        }
    }

    private Geometry convexHullGeo(Geometry geo) {
        try {
            return geo.convexHull();
        } catch (Exception e) {
            log.error("ConvexHull failed: {}", e.getMessage());
            return geo;
        }
    }

    private Geometry intersectGeo(Geometry a, Geometry b) {
        try {
            return a.intersection(b);
        } catch (Exception e) {
            log.error("Intersection failed: {}", e.getMessage());
            return geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }

    // ===== JTS geometry → GeoJSON =====

    private Map<String, Object> toGeoJSON(Geometry geometry) {
        Map<String, Object> geojson = new HashMap<>();
        geojson.put("type", geometry.getGeometryType());
        geojson.put("coordinates", toGeoJSONCoordinates(geometry));
        return geojson;
    }

    private Object toGeoJSONCoordinates(Geometry geometry) {
        if (geometry instanceof Point p) {
            return List.of(p.getX(), p.getY());
        } else if (geometry instanceof LineString ls) {
            List<List<Double>> coords = new ArrayList<>();
            for (Coordinate c : ls.getCoordinates()) {
                coords.add(List.of(c.x, c.y));
            }
            return coords;
        } else if (geometry instanceof Polygon poly) {
            List<List<List<Double>>> rings = new ArrayList<>();
            List<List<Double>> exterior = new ArrayList<>();
            for (Coordinate c : poly.getExteriorRing().getCoordinates()) {
                exterior.add(List.of(c.x, c.y));
            }
            rings.add(exterior);
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                List<List<Double>> interior = new ArrayList<>();
                for (Coordinate c : poly.getInteriorRingN(i).getCoordinates()) {
                    interior.add(List.of(c.x, c.y));
                }
                rings.add(interior);
            }
            return rings;
        } else if (geometry instanceof GeometryCollection gc) {
            List<Map<String, Object>> geoms = new ArrayList<>();
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                geoms.add(toGeoJSON(gc.getGeometryN(i)));
            }
            return geoms;
        }
        return geometry.toText();
    }

    private Map<String, Object> buildFeatureCollection(List<Map<String, Object>> features) {
        return Map.of(
            "type", "FeatureCollection",
            "features", features
        );
    }

    @FunctionalInterface
    interface ProcessHandler {
        Map<String, Object> execute(Map<String, Object> inputs);
    }
}
