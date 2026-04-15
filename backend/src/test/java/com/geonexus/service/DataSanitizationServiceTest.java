package com.geonexus.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataSanitizationService 数据脱敏服务测试
 */
@ExtendWith(MockitoExtension.class)
class DataSanitizationServiceTest {

    private DataSanitizationService service;

    @BeforeEach
    void setUp() {
        service = new DataSanitizationService();
    }

    @Nested
    @DisplayName("手机号脱敏测试")
    class PhoneSanitizationTests {

        @Test
        @DisplayName("标准手机号脱敏：13812345678 -> 138****5678")
        void testMaskPhone_standard() {
            String result = service.maskPhone("13812345678");
            assertEquals("138****5678", result);
        }

        @Test
        @DisplayName("其他运营商手机号脱敏")
        void testMaskPhone_variousPrefixes() {
            assertEquals("159****9876", service.maskPhone("15912349876"));
            assertEquals("186****5555", service.maskPhone("18612345555"));
            assertEquals("199****1234", service.maskPhone("19912341234"));
        }

        @Test
        @DisplayName("手机号脱敏中间四位被遮蔽")
        void testMaskPhone_middleFourMasked() {
            String result = service.maskPhone("13812345678");
            assertTrue(result.contains("****"));
            assertTrue(result.startsWith("138"));
            assertTrue(result.endsWith("5678"));
            assertFalse(result.contains("1234"));
        }

        @Test
        @DisplayName("null 手机号返回 null")
        void testMaskPhone_null() {
            assertNull(service.maskPhone(null));
        }

        @Test
        @DisplayName("空字符串手机号返回原值")
        void testMaskPhone_empty() {
            assertEquals("", service.maskPhone(""));
        }
    }

    @Nested
    @DisplayName("邮箱脱敏测试")
    class EmailSanitizationTests {

        @Test
        @DisplayName("标准邮箱脱敏")
        void testMaskEmail_standard() {
            String result = service.maskEmail("test@example.com");
            assertTrue(result.contains("***"));
            assertFalse(result.contains("test"));
            assertTrue(result.endsWith("@example.com"));
        }

        @Test
        @DisplayName("长用户名邮箱脱敏只显示首字符")
        void testMaskEmail_longUsername() {
            String result = service.maskEmail("longusername@example.com");
            assertTrue(result.startsWith("l***"));
            assertFalse(result.contains("ongusername"));
        }

        @Test
        @DisplayName("短用户名邮箱脱敏")
        void testMaskEmail_shortUsername() {
            String result = service.maskEmail("a@example.com");
            assertTrue(result.startsWith("a***"));
            assertTrue(result.endsWith("@example.com"));
        }

        @Test
        @DisplayName("邮箱格式验证")
        void testMaskEmail_preservesDomain() {
            String result = service.maskEmail("user@company.co.uk");
            assertTrue(result.contains("@company.co.uk"));
            assertFalse(result.contains("user"));
        }

        @Test
        @DisplayName("null 邮箱返回 null")
        void testMaskEmail_null() {
            assertNull(service.maskEmail(null));
        }
    }

    @Nested
    @DisplayName("坐标精度降级测试")
    class CoordinatePrecisionTests {

        @Test
        @DisplayName("坐标保留3位小数（约111米精度）")
        void testRoundCoord_precision() {
            BigDecimal result = service.roundCoord(116.39725678);
            assertEquals(new BigDecimal("116.397"), result);
        }

        @Test
        @DisplayName("经纬度坐标精度降级")
        void testRoundCoord_latLon() {
            BigDecimal lon = service.roundCoord(116.39725678);
            BigDecimal lat = service.roundCoord(39.91651234);

            assertEquals(new BigDecimal("116.397"), lon);
            assertEquals(new BigDecimal("39.917"), lat); // Rounds up
        }

        @Test
        @DisplayName("负数坐标精度降级")
        void testRoundCoord_negative() {
            BigDecimal result = service.roundCoord(-74.00612345);
            assertEquals(new BigDecimal("-74.006"), result);
        }

        @Test
        @DisplayName("大数坐标精度降级")
        void testRoundCoord_largeNumber() {
            BigDecimal result = service.roundCoord(12959327.123456);
            assertEquals(new BigDecimal("12959327.123"), result);
        }
    }

    @Nested
    @DisplayName("门牌号脱敏测试")
    class AddressSanitizationTests {

        @Test
        @DisplayName("门牌号脱敏：123号 -> xx号")
        void testMaskAddressNumber_standard() {
            String result = service.maskAddressNumber("北京市朝阳区123号");
            assertTrue(result.contains("xx号"));
            assertFalse(result.contains("123"));
        }

        @Test
        @DisplayName("多门牌号全部脱敏")
        void testMaskAddressNumber_multiple() {
            String result = service.maskAddressNumber("1号2号3号");
            // All number+号 patterns replaced with xx号
            assertFalse(result.contains("1号"));
        }

        @Test
        @DisplayName("null 地址返回 null")
        void testMaskAddressNumber_null() {
            assertNull(service.maskAddressNumber(null));
        }
    }

    @Nested
    @DisplayName("GeoJSON 数据整体脱敏测试")
    class GeoJSONSanitizationTests {

        @Test
        @DisplayName("FeatureCollection 脱敏")
        void testSanitize_featureCollection() {
            Map<String, Object> geojson = new HashMap<>();
            geojson.put("type", "FeatureCollection");
            List<Map<String, Object>> features = List.of(
                    createFeatureWithPhone("13812345678"),
                    createFeatureWithEmail("test@example.com")
            );
            geojson.put("features", new java.util.ArrayList<>(features));

            Map<String, Boolean> rules = Map.of(
                    "phone", true,
                    "email", true,
                    "coordinate", false,
                    "address", false
            );

            Map<String, Object> result = service.sanitize(geojson, rules);

            assertTrue((Boolean) result.get("_sanitized"));
            assertNotNull(result.get("_sanitizedAt"));
        }

        @Test
        @DisplayName("Feature 脱敏")
        void testSanitize_singleFeature() {
            Map<String, Object> feature = createFeatureWithPhone("13812345678");
            feature.put("type", "Feature");

            Map<String, Boolean> rules = Map.of(
                    "phone", true,
                    "email", false,
                    "coordinate", false,
                    "address", false
            );

            Map<String, Object> result = service.sanitize(feature, rules);

            assertTrue((Boolean) result.get("_sanitized"));
        }

        @Test
        @DisplayName("坐标脱敏规则")
        void testSanitize_coordinateRule() {
            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");
            Map<String, Object> props = new HashMap<>();
            props.put("name", "Test");
            feature.put("properties", props);

            Map<String, Object> geom = new HashMap<>();
            geom.put("type", "Point");
            geom.put("coordinates", new double[]{116.39725678, 39.91651234});
            feature.put("geometry", geom);

            Map<String, Boolean> rules = Map.of(
                    "phone", false,
                    "email", false,
                    "coordinate", true,
                    "address", false
            );

            Map<String, Object> result = service.sanitize(feature, rules);
            assertTrue((Boolean) result.get("_sanitized"));
        }

        @Test
        @DisplayName("所有规则关闭时不做脱敏")
        void testSanitize_allRulesOff() {
            Map<String, Object> feature = createFeatureWithPhone("13812345678");
            feature.put("type", "Feature");

            Map<String, Boolean> rules = Map.of(
                    "phone", false,
                    "email", false,
                    "coordinate", false,
                    "address", false
            );

            Map<String, Object> result = service.sanitize(feature, rules);

            // Sanitized flag still set, but content unchanged
            assertTrue((Boolean) result.get("_sanitized"));
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) result.get("properties");
            assertEquals("13812345678", props.get("phone"));
        }

        private Map<String, Object> createFeatureWithPhone(String phone) {
            Map<String, Object> feature = new HashMap<>();
            Map<String, Object> props = new HashMap<>();
            props.put("phone", phone);
            feature.put("properties", props);
            return feature;
        }

        private Map<String, Object> createFeatureWithEmail(String email) {
            Map<String, Object> feature = new HashMap<>();
            Map<String, Object> props = new HashMap<>();
            props.put("email", email);
            feature.put("properties", props);
            return feature;
        }
    }
}
