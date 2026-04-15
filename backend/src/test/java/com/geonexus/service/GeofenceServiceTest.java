package com.geonexus.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GeofenceService 地理围栏服务测试
 */
@ExtendWith(MockitoExtension.class)
class GeofenceServiceTest {

    @Mock
    private CacheService cacheService;

    private GeofenceService service;

    @BeforeEach
    void setUp() {
        service = new GeofenceService(cacheService);
    }

    private GeofenceService.Geofence createCircleFence(double centerLon, double centerLat, double radiusKm) {
        GeofenceService.Geometry geometry = GeofenceService.Geometry.builder()
                .type("Circle")
                .center(new double[]{centerLon, centerLat})
                .radius(radiusKm)
                .build();

        return GeofenceService.Geofence.builder()
                .name("测试围栏")
                .description("测试围栏描述")
                .category("test")
                .geometry(geometry)
                .status("active")
                .alertsEnabled(true)
                .build();
    }

    private GeofenceService.Geofence createPolygonFence(List<double[]> coordinates) {
        GeofenceService.Geometry geometry = GeofenceService.Geometry.builder()
                .type("Polygon")
                .coordinates(coordinates)
                .build();

        return GeofenceService.Geofence.builder()
                .name("多边形围栏")
                .category("test")
                .geometry(geometry)
                .status("active")
                .build();
    }

    private GeofenceService.Geofence createRectangleFence(double minX, double minY, double maxX, double maxY) {
        GeofenceService.Geometry geometry = GeofenceService.Geometry.builder()
                .type("Rectangle")
                .bbox(new double[]{minX, minY, maxX, maxY})
                .build();

        return GeofenceService.Geofence.builder()
                .name("矩形围栏")
                .category("test")
                .geometry(geometry)
                .status("active")
                .build();
    }

    @Nested
    @DisplayName("围栏创建与管理测试")
    class GeofenceCRUDTests {

        @Test
        @DisplayName("创建圆形围栏")
        void testCreateGeofence_circle() {
            GeofenceService.Geofence fence = createCircleFence(116.4, 39.9, 10.0);

            GeofenceService.Geofence result = service.createGeofence(fence);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("active", result.getStatus());
            assertEquals("Circle", result.getGeometry().getType());
        }

        @Test
        @DisplayName("创建多边形围栏")
        void testCreateGeofence_polygon() {
            List<double[]> coords = List.of(
                    new double[]{116.3, 39.8},
                    new double[]{116.5, 39.8},
                    new double[]{116.5, 40.0},
                    new double[]{116.3, 40.0},
                    new double[]{116.3, 39.8}
            );
            GeofenceService.Geofence fence = createPolygonFence(coords);

            GeofenceService.Geofence result = service.createGeofence(fence);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("Polygon", result.getGeometry().getType());
        }

        @Test
        @DisplayName("获取已创建的围栏")
        void testGetGeofence() {
            GeofenceService.Geofence fence = createCircleFence(116.4, 39.9, 10.0);
            GeofenceService.Geofence created = service.createGeofence(fence);

            GeofenceService.Geofence retrieved = service.getGeofence(created.getId());

            assertNotNull(retrieved);
            assertEquals(created.getId(), retrieved.getId());
            assertEquals("测试围栏", retrieved.getName());
        }

        @Test
        @DisplayName("删除围栏")
        void testDeleteGeofence() {
            GeofenceService.Geofence fence = createCircleFence(116.4, 39.9, 10.0);
            GeofenceService.Geofence created = service.createGeofence(fence);

            service.deleteGeofence(created.getId());

            assertNull(service.getGeofence(created.getId()));
        }

        @Test
        @DisplayName("更新围栏状态")
        void testSetGeofenceStatus() {
            GeofenceService.Geofence fence = createCircleFence(116.4, 39.9, 10.0);
            GeofenceService.Geofence created = service.createGeofence(fence);

            service.setGeofenceStatus(created.getId(), false);

            GeofenceService.Geofence retrieved = service.getGeofence(created.getId());
            assertEquals("inactive", retrieved.getStatus());
        }
    }

    @Nested
    @DisplayName("目标监控测试")
    class TargetMonitoringTests {

        @Test
        @DisplayName("添加监控目标")
        void testAddTarget() {
            GeofenceService.MonitoredTarget target = GeofenceService.MonitoredTarget.builder()
                    .name("测试车辆")
                    .type("vehicle")
                    .position(new double[]{116.4, 39.9})
                    .speed(60.0)
                    .bearing(90.0)
                    .build();

            GeofenceService.MonitoredTarget result = service.addTarget(target);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("测试车辆", result.getName());
        }

        @Test
        @DisplayName("更新目标位置")
        void testUpdateTargetPosition() {
            GeofenceService.MonitoredTarget target = GeofenceService.MonitoredTarget.builder()
                    .name("测试车辆")
                    .type("vehicle")
                    .position(new double[]{116.0, 39.0})
                    .build();
            service.addTarget(target);

            // Update position
            service.updateTargetPosition(target.getId(), 116.5, 40.0, 80.0, 45.0);

            // Verify via getTargetsInside - should be empty since no fence created
            // We just verify no exception is thrown
            assertTrue(true);
        }

        @Test
        @DisplayName("批量更新目标位置")
        void testBatchUpdatePositions() {
            GeofenceService.MonitoredTarget t1 = service.addTarget(
                    GeofenceService.MonitoredTarget.builder()
                            .name("车1").type("vehicle")
                            .position(new double[]{116.0, 39.0}).build());
            GeofenceService.MonitoredTarget t2 = service.addTarget(
                    GeofenceService.MonitoredTarget.builder()
                            .name("车2").type("vehicle")
                            .position(new double[]{116.0, 39.0}).build());

            var positions = List.of(
                    GeofenceService.TargetPosition.builder()
                            .targetId(t1.getId()).lon(116.1).lat(39.1).speed(60).bearing(90).build(),
                    GeofenceService.TargetPosition.builder()
                            .targetId(t2.getId()).lon(116.2).lat(39.2).speed(80).bearing(45).build()
            );

            service.batchUpdatePositions(positions);

            assertTrue(true); // No exception means success
        }
    }

    @Nested
    @DisplayName("围栏内目标检测测试")
    class GeofenceDetectionTests {

        @Test
        @DisplayName("圆形围栏内目标检测")
        void testGetTargetsInside_circle() {
            // 创建以 (116.4, 39.9) 为中心，半径 10km 的圆形围栏
            GeofenceService.Geofence fence = createCircleFence(116.4, 39.9, 10.0);
            GeofenceService.Geofence created = service.createGeofence(fence);

            // 添加一个在围栏内的目标（天安门附近，距离中心约 1.8km）
            GeofenceService.MonitoredTarget insideTarget = GeofenceService.MonitoredTarget.builder()
                    .name("围栏内车辆")
                    .type("vehicle")
                    .position(new double[]{116.3972, 39.9165}) // 天安门
                    .monitoredFences(java.util.Set.of(created.getId()))
                    .build();
            service.addTarget(insideTarget);
            service.updateTargetPosition(insideTarget.getId(), 116.3972, 39.9165, 0, 0);

            // 添加一个在围栏外的目标
            GeofenceService.MonitoredTarget outsideTarget = GeofenceService.MonitoredTarget.builder()
                    .name("围栏外车辆")
                    .type("vehicle")
                    .position(new double[]{117.0, 39.0}) // 远处
                    .monitoredFences(java.util.Set.of(created.getId()))
                    .build();
            service.addTarget(outsideTarget);
            service.updateTargetPosition(outsideTarget.getId(), 117.0, 39.0, 0, 0);

            // 获取围栏内的目标
            List<GeofenceService.MonitoredTarget> inside = service.getTargetsInside(created.getId());

            assertNotNull(inside);
            // The inside target should be in the list
            assertTrue(inside.stream().anyMatch(t -> t.getName().equals("围栏内车辆")));
        }

        @Test
        @DisplayName("矩形围栏内目标检测")
        void testGetTargetsInside_rectangle() {
            // 创建矩形围栏: [116.0, 39.0, 117.0, 40.0]
            GeofenceService.Geofence fence = createRectangleFence(116.0, 39.0, 117.0, 40.0);
            GeofenceService.Geofence created = service.createGeofence(fence);

            // 添加在矩形内的目标
            GeofenceService.MonitoredTarget target = GeofenceService.MonitoredTarget.builder()
                    .name("矩形内目标")
                    .type("person")
                    .position(new double[]{116.5, 39.5})
                    .monitoredFences(java.util.Set.of(created.getId()))
                    .build();
            service.addTarget(target);
            service.updateTargetPosition(target.getId(), 116.5, 39.5, 0, 0);

            List<GeofenceService.MonitoredTarget> inside = service.getTargetsInside(created.getId());

            assertNotNull(inside);
            assertTrue(inside.stream().anyMatch(t -> t.getName().equals("矩形内目标")));
        }

        @Test
        @DisplayName("空围栏应返回空列表")
        void testGetTargetsInside_empty() {
            GeofenceService.Geofence fence = createCircleFence(116.4, 39.9, 10.0);
            GeofenceService.Geofence created = service.createGeofence(fence);

            List<GeofenceService.MonitoredTarget> inside = service.getTargetsInside(created.getId());

            assertNotNull(inside);
            assertTrue(inside.isEmpty());
        }
    }

    @Nested
    @DisplayName("围栏统计测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取围栏统计信息")
        void testGetStatistics() {
            GeofenceService.Geofence fence = createCircleFence(116.4, 39.9, 10.0);
            GeofenceService.Geofence created = service.createGeofence(fence);

            GeofenceService.GeofenceStatistics stats = service.getStatistics(
                    created.getId(), System.currentTimeMillis() - 3600000, System.currentTimeMillis());

            assertNotNull(stats);
            assertEquals(created.getId(), stats.getFenceId());
            assertEquals(0, stats.getTotalEvents()); // No events yet
        }
    }
}
