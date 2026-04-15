package com.geonexus.common.geometry;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GeoJSON 工具类
 * 提供 Geometry 与 GeoJSON 之间的转换
 */
public class GeoJSONUtils {
    
    private static final GeometryFactory GEOMETRY_FACTORY = 
        new GeometryFactory(new PackedCoordinateSequenceFactory());
    
    private GeoJSONUtils() {
        // 工具类
    }
    
    /**
     * 从GeoJSON解析Geometry
     */
    @SuppressWarnings("unchecked")
    public static Geometry parseGeometry(Map<String, Object> geojson) {
        if (geojson == null) {
            throw new IllegalArgumentException("Geometry is null");
        }
        
        String type = (String) geojson.get("type");
        Object coords = geojson.get("coordinates");
        
        if (type == null) {
            throw new IllegalArgumentException("Geometry type is null");
        }
        
        return switch (type.toLowerCase()) {
            case "point" -> createPoint((List<Number>) coords);
            case "linestring" -> createLineString((List<List<Number>>) coords);
            case "polygon" -> createPolygon((List<List<List<Number>>>) coords);
            case "multipoint" -> createMultiPoint((List<List<Number>>) coords);
            case "multilinestring" -> createMultiLineString((List<List<List<Number>>>) coords);
            case "multipolygon" -> createMultiPolygon((List<List<List<List<Number>>>>) coords);
            case "geometrycollection" -> createGeometryCollection((List<Map<String, Object>>) geojson.get("geometries"));
            default -> throw new IllegalArgumentException("Unknown geometry type: " + type);
        };
    }
    
    private static Point createPoint(List<Number> coords) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(coords.get(0).doubleValue(), coords.get(1).doubleValue()));
    }
    
    private static LineString createLineString(List<List<Number>> coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            List<Number> c = coordinates.get(i);
            coords[i] = new Coordinate(c.get(0).doubleValue(), c.get(1).doubleValue());
        }
        return GEOMETRY_FACTORY.createLineString(coords);
    }
    
    private static Polygon createPolygon(List<List<List<Number>>> rings) {
        List<LineString> shells = new ArrayList<>();
        List<LineString> holes = new ArrayList<>();
        
        for (int i = 0; i < rings.size(); i++) {
            List<List<Number>> ring = rings.get(i);
            Coordinate[] coords = new Coordinate[ring.size()];
            for (int j = 0; j < ring.size(); j++) {
                List<Number> c = ring.get(j);
                coords[j] = new Coordinate(c.get(0).doubleValue(), c.get(1).doubleValue());
            }
            LineString ls = GEOMETRY_FACTORY.createLineString(coords);
            if (i == 0) {
                shells.add(ls);
            } else {
                holes.add(ls);
            }
        }
        
        if (shells.isEmpty()) {
            return GEOMETRY_FACTORY.createPolygon();
        }
        
        return GEOMETRY_FACTORY.createPolygon(shells.get(0), holes.toArray(new LineString[0]));
    }
    
    private static MultiPoint createMultiPoint(List<List<Number>> coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            List<Number> c = coordinates.get(i);
            coords[i] = new Coordinate(c.get(0).doubleValue(), c.get(1).doubleValue());
        }
        return GEOMETRY_FACTORY.createMultiPoint(coords);
    }
    
    private static MultiLineString createMultiLineString(List<List<List<Number>>> lines) {
        LineString[] lineStrings = new LineString[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            lineStrings[i] = createLineString(lines.get(i));
        }
        return GEOMETRY_FACTORY.createMultiLineString(lineStrings);
    }
    
    @SuppressWarnings("unchecked")
    private static MultiPolygon createMultiPolygon(List<List<List<List<Number>>>> polys) {
        Polygon[] polygons = new Polygon[polys.size()];
        for (int i = 0; i < polys.size(); i++) {
            polygons[i] = createPolygon(polys.get(i));
        }
        return GEOMETRY_FACTORY.createMultiPolygon(polygons);
    }
    
    @SuppressWarnings("unchecked")
    private static GeometryCollection createGeometryCollection(List<Map<String, Object>> geoms) {
        Geometry[] geometries = new Geometry[geoms.size()];
        for (int i = 0; i < geoms.size(); i++) {
            geometries[i] = parseGeometry(geoms.get(i));
        }
        return GEOMETRY_FACTORY.createGeometryCollection(geometries);
    }
    
    /**
     * Geometry转GeoJSON
     */
    public static Map<String, Object> toGeoJSON(Geometry geom) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", geom.getGeometryType());
        result.put("coordinates", geometryToCoords(geom));
        return result;
    }
    
    private static Object geometryToCoords(Geometry geom) {
        if (geom instanceof Point p) {
            return List.of(p.getX(), p.getY());
        } else if (geom instanceof LineString ls) {
            List<List<Double>> coords = new ArrayList<>();
            for (Coordinate c : ls.getCoordinates()) {
                coords.add(List.of(c.x, c.y));
            }
            return coords;
        } else if (geom instanceof Polygon poly) {
            List<Object> rings = new ArrayList<>();
            rings.add(geometryToCoords(poly.getExteriorRing()));
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                rings.add(geometryToCoords(poly.getInteriorRingN(i)));
            }
            return rings;
        } else if (geom instanceof MultiPoint mp) {
            List<Object> coords = new ArrayList<>();
            for (Coordinate c : mp.getCoordinates()) {
                coords.add(List.of(c.x, c.y));
            }
            return coords;
        } else if (geom instanceof MultiLineString mls) {
            List<Object> lines = new ArrayList<>();
            for (int i = 0; i < mls.getNumGeometries(); i++) {
                lines.add(geometryToCoords(mls.getGeometryN(i)));
            }
            return lines;
        } else if (geom instanceof MultiPolygon mpoly) {
            List<Object> polys = new ArrayList<>();
            for (int i = 0; i < mpoly.getNumGeometries(); i++) {
                polys.add(geometryToCoords(mpoly.getGeometryN(i)));
            }
            return polys;
        } else if (geom instanceof GeometryCollection gc) {
            List<Object> geoms = new ArrayList<>();
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                geoms.add(Map.of(
                    "type", gc.getGeometryN(i).getGeometryType(),
                    "coordinates", geometryToCoords(gc.getGeometryN(i))
                ));
            }
            return geoms;
        }
        return null;
    }
    
    /**
     * 从GeoJSON提取单个几何体（支持FeatureCollection）
     */
    @SuppressWarnings("unchecked")
    public static Geometry extractGeometryFromGeoJSON(Map<String, Object> geojson) {
        if (geojson.containsKey("features")) {
            List<Map<String, Object>> features = (List<Map<String, Object>>) geojson.get("features");
            List<Geometry> geoms = new ArrayList<>();
            for (Map<String, Object> feature : features) {
                Map<String, Object> geom = (Map<String, Object>) feature.get("geometry");
                if (geom != null) {
                    geoms.add(parseGeometry(geom));
                }
            }
            if (geoms.isEmpty()) {
                return GEOMETRY_FACTORY.createPoint(new Coordinate(0, 0));
            }
            if (geoms.size() == 1) {
                return geoms.get(0);
            }
            return geoms.get(0); // 简化处理，实际应该用 UnaryUnionOp 合并
        } else if (geojson.containsKey("geometry")) {
            return parseGeometry((Map<String, Object>) geojson.get("geometry"));
        } else {
            return parseGeometry(geojson);
        }
    }
}
