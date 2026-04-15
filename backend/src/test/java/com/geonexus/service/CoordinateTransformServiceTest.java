package com.geonexus.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CoordinateTransformService 坐标转换服务测试
 */
class CoordinateTransformServiceTest {

    private CoordinateTransformService service;

    @BeforeEach
    void setUp() {
        service = new CoordinateTransformService();
    }

    @Nested
    @DisplayName("WGS84 -> Web Mercator 转换测试")
    class Wgs84ToWebMercatorTests {

        @Test
        @DisplayName("北京天安门坐标 WGS84 转 Web Mercator")
        void testWgs84ToWebMercator_beijing() {
            double[] point = new double[]{116.3972, 39.9165};
            double[] result = service.transform(point, "EPSG:4326", "EPSG:3857");

            assertNotNull(result);
            assertEquals(2, result.length);
            // Web Mercator X (经度) should be positive for Beijing
            assertTrue(result[0] > 0, "Web Mercator X should be positive");
            // Web Mercator Y (纬度) should be positive for Beijing
            assertTrue(result[1] > 0, "Web Mercator Y should be positive");
            // Beijing in Web Mercator is approximately (12959327.0, 4854470.0)
            assertTrue(result[0] > 1e7, "Web Mercator X should be ~12.9M");
            assertTrue(result[1] > 4e6, "Web Mercator Y should be ~4.8M");
        }

        @Test
        @DisplayName("上海坐标 WGS84 转 Web Mercator")
        void testWgs84ToWebMercator_shanghai() {
            double[] point = new double[]{121.4737, 31.2304};
            double[] result = service.transform(point, "EPSG:4326", "EPSG:3857");

            assertNotNull(result);
            assertTrue(result[0] > 1e7);
            assertTrue(result[1] > 3e6);
        }

        @Test
        @DisplayName("负经纬度坐标转换")
        void testWgs84ToWebMercator_negativeCoords() {
            double[] point = new double[]{-74.006, 40.7128}; // New York
            double[] result = service.transform(point, "EPSG:4326", "EPSG:3857");

            assertNotNull(result);
            assertTrue(result[0] < 0, "West longitude should have negative X");
        }
    }

    @Nested
    @DisplayName("同坐标系转换测试")
    class SameCRSTests {

        @Test
        @DisplayName("EPSG:4326 -> EPSG:4326 应返回原值")
        void testTransformSameCRS_wgs84() {
            double[] point = new double[]{116.3972, 39.9165};
            double[] result = service.transform(point, "EPSG:4326", "EPSG:4326");

            assertNotNull(result);
            assertEquals(116.3972, result[0], 0.0001);
            assertEquals(39.9165, result[1], 0.0001);
        }

        @Test
        @DisplayName("EPSG:3857 -> EPSG:3857 应返回原值")
        void testTransformSameCRS_webMercator() {
            double[] point = new double[]{12959327.0, 4854470.0};
            double[] result = service.transform(point, "EPSG:3857", "EPSG:3857");

            assertNotNull(result);
            assertEquals(12959327.0, result[0], 0.001);
            assertEquals(4854470.0, result[1], 0.001);
        }
    }

    @Nested
    @DisplayName("批量转换测试")
    class BatchTransformTests {

        @Test
        @DisplayName("批量转换多个坐标点")
        void testTransformBatch() {
            var points = java.util.List.of(
                    new double[]{116.3972, 39.9165},
                    new double[]{121.4737, 31.2304},
                    new double[]{-74.006, 40.7128}
            );

            var results = service.transformBatch(points, "EPSG:4326", "EPSG:3857");

            assertNotNull(results);
            assertEquals(3, results.size());
            // All results should have 2 coordinates
            for (double[] r : results) {
                assertEquals(2, r.length);
            }
            // Beijing and Shanghai should have positive X,Y
            assertTrue(results.get(0)[0] > 0 && results.get(0)[1] > 0);
            assertTrue(results.get(1)[0] > 0 && results.get(1)[1] > 0);
            // New York should have negative X
            assertTrue(results.get(2)[0] < 0);
        }

        @Test
        @DisplayName("空列表批量转换")
        void testTransformBatch_empty() {
            var results = service.transformBatch(java.util.List.of(), "EPSG:4326", "EPSG:3857");
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("CRS 注册表测试")
    class CRSRegistryTests {

        @Test
        @DisplayName("获取已注册的 CRS")
        void testGetCRS_wgs84() {
            var crs = service.getCRS("EPSG:4326");
            assertNotNull(crs);
            assertEquals("WGS 84", crs.getName());
            assertEquals("geographic", crs.getType());
        }

        @Test
        @DisplayName("获取 Web Mercator CRS")
        void testGetCRS_webMercator() {
            var crs = service.getCRS("EPSG:3857");
            assertNotNull(crs);
            assertEquals("Web Mercator", crs.getName());
            assertEquals("projected", crs.getType());
        }

        @Test
        @DisplayName("获取中国坐标系 CRS")
        void testGetCRS_china2000() {
            var crs = service.getCRS("EPSG:4490");
            assertNotNull(crs);
            assertEquals("China Geodetic Coordinate System 2000", crs.getName());
        }
    }

    @Nested
    @DisplayName("坐标边界测试")
    class CoordinateBoundsTests {

        @Test
        @DisplayName("Web Mercator 边界内坐标")
        void testTransformWithinBounds() {
            // 联合国总部附近
            double[] point = new double[]{-73.1, 40.75};
            double[] result = service.transform(point, "EPSG:4326", "EPSG:3857");

            assertNotNull(result);
            // Should be within Web Mercator bounds
            double[] bounds = service.getCRS("EPSG:3857").getBounds();
            assertTrue(result[0] >= bounds[0] && result[0] <= bounds[2],
                    "X should be within Web Mercator bounds");
            assertTrue(result[1] >= bounds[1] && result[1] <= bounds[3],
                    "Y should be within Web Mercator bounds");
        }
    }
}
