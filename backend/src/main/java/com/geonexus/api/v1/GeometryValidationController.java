package com.geonexus.api.v1;

import com.geonexus.service.GeometryValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 几何验证控制器
 */
@Tag(name = "Geometry Validation", description = "几何有效性验证与修复")
@RestController
@RequestMapping("/api/v1/geometry")
@RequiredArgsConstructor
public class GeometryValidationController {

    private final GeometryValidationService validationService;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final WKTReader wktReader = new WKTReader();

    private Geometry parseGeometry(Object geoObj) {
        if (geoObj == null) return null;
        if (geoObj instanceof String) {
            try {
                return wktReader.read((String) geoObj);
            } catch (Exception e) {
                return null;
            }
        }
        if (geoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> geoMap = (Map<String, Object>) geoObj;
            return parseGeoJSON(geoMap);
        }
        return null;
    }

    private Geometry parseGeoJSON(Map<String, Object> geoMap) {
        String type = (String) geoMap.get("type");
        if (type == null) return null;

        Object coordsObj = geoMap.get("coordinates");
        if (coordsObj == null) return null;

        try {
            switch (type) {
                case "Point":
                    return parsePoint(coordsObj);
                case "LineString":
                    return parseLineString(coordsObj);
                case "Polygon":
                    return parsePolygon(coordsObj);
                case "MultiPoint":
                    return parseMultiPoint(coordsObj);
                case "MultiLineString":
                    return parseMultiLineString(coordsObj);
                case "MultiPolygon":
                    return parseMultiPolygon(coordsObj);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Coordinate[] parseCoordinateArray(Object coordsObj, int expectedDim) {
        if (coordsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) coordsObj;
            Coordinate[] coords = new Coordinate[list.size()];
            for (int i = 0; i < list.size(); i++) {
                coords[i] = parseCoordinate(list.get(i), expectedDim);
            }
            return coords;
        }
        return new Coordinate[0];
    }

    private Coordinate parseCoordinate(Object obj, int expectedDim) {
        if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Number> xy = (List<Number>) obj;
            if (expectedDim == 2 || xy.size() < 3) {
                return new Coordinate(xy.get(0).doubleValue(), xy.get(1).doubleValue());
            } else {
                return new Coordinate(xy.get(0).doubleValue(), xy.get(1).doubleValue(), xy.get(2).doubleValue());
            }
        }
        return new Coordinate(0, 0);
    }

    private Point parsePoint(Object coordsObj) {
        return geometryFactory.createPoint(parseCoordinate(coordsObj, 2));
    }

    private LineString parseLineString(Object coordsObj) {
        Coordinate[] coords = parseCoordinateArray(coordsObj, 2);
        return geometryFactory.createLineString(coords);
    }

    private Polygon parsePolygon(Object coordsObj) {
        if (coordsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> rings = (List<Object>) coordsObj;
            if (rings.isEmpty()) return geometryFactory.createPolygon();
            LinearRing shell = geometryFactory.createLinearRing(parseCoordinateArray(rings.get(0), 2));
            if (rings.size() == 1) {
                return geometryFactory.createPolygon(shell);
            }
            LinearRing[] holes = new LinearRing[rings.size() - 1];
            for (int i = 1; i < rings.size(); i++) {
                holes[i - 1] = geometryFactory.createLinearRing(parseCoordinateArray(rings.get(i), 2));
            }
            return geometryFactory.createPolygon(shell, holes);
        }
        return geometryFactory.createPolygon();
    }

    private MultiPoint parseMultiPoint(Object coordsObj) {
        Coordinate[] coords = parseCoordinateArray(coordsObj, 2);
        return geometryFactory.createMultiPoint(coords);
    }

    private MultiLineString parseMultiLineString(Object coordsObj) {
        if (coordsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> lines = (List<Object>) coordsObj;
            LineString[] lineStrings = new LineString[lines.size()];
            for (int i = 0; i < lines.size(); i++) {
                lineStrings[i] = parseLineString(lines.get(i));
            }
            return geometryFactory.createMultiLineString(lineStrings);
        }
        return geometryFactory.createMultiLineString();
    }

    private MultiPolygon parseMultiPolygon(Object coordsObj) {
        if (coordsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> polys = (List<Object>) coordsObj;
            Polygon[] polygons = new Polygon[polys.size()];
            for (int i = 0; i < polys.size(); i++) {
                polygons[i] = parsePolygon(polys.get(i));
            }
            return geometryFactory.createMultiPolygon(polygons);
        }
        return geometryFactory.createMultiPolygon();
    }

    @PostMapping("/validate")
    @Operation(summary = "验证几何有效性（详细）", description = "检查几何是否有效，并返回详细错误信息")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> request) {
        Geometry geom = parseGeometry(request.get("geometry"));
        if (geom == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing geometry"));
        }
        GeometryValidationService.ValidationResult result = validationService.validate(geom);
        return ResponseEntity.ok(Map.of(
                "valid", result.isValid(),
                "message", result.getMessage()
        ));
    }

    @PostMapping("/is-valid")
    @Operation(summary = "检查几何是否有效", description = "返回布尔值表示几何是否有效")
    public ResponseEntity<Map<String, Object>> isValid(@RequestBody Map<String, Object> request) {
        Geometry geom = parseGeometry(request.get("geometry"));
        if (geom == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing geometry"));
        }
        boolean valid = validationService.isValid(geom);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping("/make-valid")
    @Operation(summary = "修复几何", description = "将无效几何修复为有效几何，使用buffer(0)技术")
    public ResponseEntity<Map<String, Object>> makeValid(@RequestBody Map<String, Object> request) {
        Geometry geom = parseGeometry(request.get("geometry"));
        if (geom == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing geometry"));
        }
        Geometry fixed = validationService.makeValid(geom);
        return ResponseEntity.ok(Map.of(
                "originalValid", geom.isValid(),
                "fixedValid", fixed.isValid(),
                "geometry", toGeoJSON(fixed)
        ));
    }

    @PostMapping("/is-simple")
    @Operation(summary = "检查几何是否简单", description = "检查几何是否无自相交等简单性问题")
    public ResponseEntity<Map<String, Object>> isSimple(@RequestBody Map<String, Object> request) {
        Geometry geom = parseGeometry(request.get("geometry"));
        if (geom == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing geometry"));
        }
        boolean simple = validationService.isSimple(geom);
        return ResponseEntity.ok(Map.of("simple", simple));
    }

    private Map<String, Object> toGeoJSON(Geometry geom) {
        return Map.of(
                "type", geom.getGeometryType(),
                "coordinates", toGeoJSONCoords(geom)
        );
    }

    @SuppressWarnings("unchecked")
    private Object toGeoJSONCoords(Geometry geom) {
        if (geom instanceof Point) {
            Point p = (Point) geom;
            return java.util.List.of(p.getX(), p.getY());
        } else if (geom instanceof LineString) {
            CoordinateSequence cs = geom.getCoordinateSequence();
            java.util.List<java.util.List<Double>> coords = new java.util.ArrayList<>();
            for (int i = 0; i < cs.size(); i++) {
                coords.add(java.util.List.of(cs.getX(i), cs.getY(i)));
            }
            return coords;
        } else if (geom instanceof Polygon) {
            java.util.List<java.util.List<java.util.List<Double>>> rings = new java.util.ArrayList<>();
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                java.util.List<java.util.List<Double>> ring = new java.util.ArrayList<>();
                CoordinateSequence cs = geom.getGeometryN(i).getCoordinateSequence();
                for (int j = 0; j < cs.size(); j++) {
                    ring.add(java.util.List.of(cs.getX(j), cs.getY(j)));
                }
                rings.add(ring);
            }
            return rings;
        }
        return geom.toText();
    }
}
