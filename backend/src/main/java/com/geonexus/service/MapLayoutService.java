package com.geonexus.service;

import com.geonexus.service.GeoServerService;
import com.geonexus.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 专业地图出图布局服务
 * 支持地图布局设计、指北针、比例尺、图例、标题框及 PDF/PNG 导出
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MapLayoutService {

    private final GeoServerService geoServerService;
    private final MapService mapService;

    /**
     * 生成地图布局 HTML（用于后续 PDF 转换）
     */
    public String generateLayoutHtml(MapLayoutRequest request) {
        String fontFamily = request.getFontFamily() != null ? request.getFontFamily() : "Arial";

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                .layout { position: relative; width: %(width)spx; height: %(height)spx; background: white; font-family: %(font)s; }
                .map-area { position: absolute; %(mapLeft)spx %(mapTop)spx %(mapWidth)spx %(mapHeight)spx; border: 1px solid #333; }
                .map-area img { width: 100%%; height: 100%%; object-fit: fill; }
            """.formatted(
                request.getWidth(), request.getHeight(), fontFamily,
                request.getMapLeft() != null ? request.getMapLeft() : 40,
                request.getMapTop() != null ? request.getMapTop() : 60,
                request.getMapWidth() != null ? request.getMapWidth() : request.getWidth() - 80,
                request.getMapHeight() != null ? request.getMapHeight() : request.getHeight() - 120
        ));

        // 指北针
        if (request.getNorthArrow() != null) {
            sb.append("""
                .north-arrow { position: absolute; %(style)s font-size: %(size)spx; text-align: center; }
                """.formatted(
                    request.getNorthArrow().getPosition() != null ? request.getNorthArrow().getPosition() : "top: 70px; right: 20px;",
                    request.getNorthArrow().getSize() > 0 ? request.getNorthArrow().getSize() : 40
            ));
        }

        // 比例尺
        if (request.getScaleBar() != null) {
            int barWidth = request.getScaleBar().getBarWidth() > 0 ? request.getScaleBar().getBarWidth() : 100;
            sb.append("""
                .scale-bar { position: absolute; %(style)s font-size: 12px; }
                .scale-bar .bar { width: %(barWidth)spx; height: 4px; background: #000; border: 1px solid #000; }
                .scale-bar .label { margin-top: 2px; text-align: center; }
                """.formatted(
                    request.getScaleBar().getPosition() != null ? request.getScaleBar().getPosition() : "bottom: 50px; left: 40px;",
                    barWidth
            ));
        }

        // 图例
        sb.append("""
                .legend { position: absolute; %(style)s background: rgba(255,255,255,0.95);
                    border: 1px solid #999; padding: 12px; font-size: 11px; max-width: 200px; }
                .legend h4 { margin-bottom: 8px; border-bottom: 1px solid #ccc; padding-bottom: 4px; }
                .legend-item { display: flex; align-items: center; margin: 4px 0; }
                .legend-symbol { width: 20px; height: 14px; margin-right: 6px; border: 1px solid #ccc; }
                .title-box { position: absolute; %(titleStyle)s background: white; padding: 8px 16px;
                    border-bottom: 3px solid %(accentColor)s; }
                .title-box h1 { font-size: %(titleSize)spx; color: #1e293b; }
                .title-box h2 { font-size: %(subtitleSize)spx; color: #64748b; margin-top: 4px; }
                .metadata-box { position: absolute; %(metaStyle)s background: rgba(255,255,255,0.9);
                    padding: 8px; font-size: 10px; color: #666; border: 1px solid #ccc; }
            """.formatted(
                request.getLegendStyle() != null ? request.getLegendStyle() : "bottom: 50px; right: 20px;",
                request.getTitleStyle() != null ? request.getTitleStyle() : "top: 10px; left: 10px;",
                request.getAccentColor() != null ? request.getAccentColor() : "#2563eb",
                request.getTitleSize() != null ? request.getTitleSize() : 18,
                request.getSubtitleSize() != null ? request.getSubtitleSize() : 12,
                request.getMetaStyle() != null ? request.getMetaStyle() : "bottom: 10px; left: 10px;"
        ));

        sb.append("""
                </style>
            </head>
            <body>
            <div class="layout">
                <div class="map-area">
                    <img src="%s" alt="Map" />
                </div>
            """.formatted(request.getMapImageUrl() != null ? request.getMapImageUrl() : ""));
        sb.append("\n");

        // 标题框
        if (request.isShowTitle()) {
            sb.append(String.format("""
                <div class="title-box">
                    <h1>%s</h1>
                    %s
                </div>
                """,
                request.getTitle() != null ? request.getTitle() : "",
                request.getSubtitle() != null ? "<h2>" + request.getSubtitle() + "</h2>" : ""
            ));
        }

        // 图例
        if (request.isShowLegend() && request.getLegendLayers() != null && !request.getLegendLayers().isEmpty()) {
            sb.append("""
                <div class="legend">
                    <h4>图例</h4>
                """);
            for (var layer : request.getLegendLayers()) {
                String symbol = renderLegendSymbol(layer);
                sb.append(String.format("""
                    <div class="legend-item">
                        <div class="legend-symbol" style="%s"></div>
                        <span>%s</span>
                    </div>
                    """, symbol, layer.getName() != null ? layer.getName() : ""));
            }
            sb.append("</div>\n");
        }

        // 指北针
        if (request.getNorthArrow() != null) {
            String arrowChar = switch (request.getNorthArrow().getStyle().toLowerCase()) {
                case "arrow" -> "↑";
                case "compass" -> "🧭";
                case "star" -> "★";
                default -> "N";
            };
            String position = request.getNorthArrow().getPosition() != null
                ? request.getNorthArrow().getPosition()
                : "top: 70px; right: 20px;";
            sb.append(String.format("""
                <div class="north-arrow" style="%s">%s</div>
                """, position, arrowChar));
        }

        // 比例尺
        if (request.getScaleBar() != null) {
            ScaleBarConfig sbConfig = request.getScaleBar();
            int barWidth = sbConfig.getBarWidth() > 0 ? sbConfig.getBarWidth() : 100;
            String position = sbConfig.getPosition() != null ? sbConfig.getPosition() : "bottom: 50px; left: 40px;";
            sb.append(String.format("""
                <div class="scale-bar" style="%s">
                    <div class="bar"></div>
                    <div class="label">0 %s | %s | %s</div>
                </div>
                """,
                position,
                formatScaleUnit(sbConfig.getUnit(), 0),
                formatScaleUnit(sbConfig.getUnit(), barWidth / 2.0),
                formatScaleUnit(sbConfig.getUnit(), (double) barWidth)
            ));
        }

        // 元数据
        if (request.isShowMetadata()) {
            String crs = request.getCrs() != null ? request.getCrs() : "EPSG:3857";
            String dataSource = request.getDataSource() != null ? request.getDataSource() : "GeoNexus";
            String meta = String.format("投影: %s | 生成时间: %s | 数据源: %s",
                crs, LocalDate.now(), dataSource);
            String metaStyle = request.getMetaStyle() != null ? request.getMetaStyle() : "bottom: 10px; left: 10px;";
            sb.append(String.format("<div class='metadata-box' style='%s'>%s</div>", metaStyle, meta));
        }

        sb.append("""
            </div>
            <style>
            @media print {
                body { width: 100%%; margin: 0; }
                .layout { page-break-inside: avoid; }
            }
            </style>
            </body>
            </html>
            """);

        return sb.toString();
    }

    /**
     * 生成高分辨率地图图片（PNG）
     */
    public byte[] exportToPng(MapLayoutRequest request) {
        String html = generateLayoutHtml(request);
        // TODO: 使用 GeoServer SLD 渲染或调用 html2canvas / puppeteer 进行截图
        // 实现方案：
        // 1. 调用 GeoServer GetMap 接口获取地图瓦片/图片
        // 2. 使用 Selenium + html2canvas 将 HTML 布局转为图片
        // 3. 或使用 Apache PDFBox 配合预渲染图片生成 PNG
        log.info("PNG export requested, HTML length: {}", html.length());
        return new byte[0];
    }

    /**
     * 生成 PDF
     */
    public byte[] exportToPdf(MapLayoutRequest request) {
        // 使用 ReportService 生成 HTML，然后用 Flying Saucer 或 iText 转 PDF
        String html = generateLayoutHtml(request);
        // 方法1：Flying Saucer (XHTMLRenderer)
        // ITextRenderer renderer = new ITextRenderer();
        // renderer.setDocumentFromString(html);
        // renderer.layout();
        // ByteArrayOutputStream out = new ByteArrayOutputStream();
        // renderer.createPDF(out);
        // return out.toByteArray();

        // 方法2（简单）：直接返回 HTML，让前端用浏览器打印
        return html.getBytes(StandardCharsets.UTF_8);
    }

    // ===== 工具方法 =====

    private String renderLegendSymbol(LegendLayer layer) {
        StringBuilder style = new StringBuilder();
        style.append("background: ");
        String type = layer.getType() != null ? layer.getType().toLowerCase() : "polygon";
        switch (type) {
            case "polygon" -> {
                String fill = layer.getFillColor() != null ? layer.getFillColor() : "#ccc";
                String stroke = layer.getStrokeColor() != null ? layer.getStrokeColor() : "#666";
                style.append(fill).append("; border: 1px solid ").append(stroke);
            }
            case "line" -> {
                String stroke = layer.getStrokeColor() != null ? layer.getStrokeColor() : "#333";
                style.append("linear-gradient(to right, ").append(stroke)
                     .append(" 0%, transparent 100%)");
            }
            case "point" -> {
                String color = layer.getColor() != null ? layer.getColor() : "#e53";
                style.append("radial-gradient(circle, ").append(color)
                             .append(" 40%, transparent 40%)");
            }
            default -> style.append("#ccc");
        }
        style.append(";");
        return style.toString();
    }

    private String formatScaleUnit(String unit, double px) {
        if (px == 0) {
            return "km".equals(unit) ? "0" : "0m";
        }
        // 粗略换算：px -> 米（假设 1px = 1km 在默认比例下）
        double meters = px * 1000 / 2;
        if ("km".equals(unit)) {
            return String.format("%.1fkm", meters / 1000);
        } else {
            return String.format("%.0fm", meters);
        }
    }

    // ===== 配置类 =====

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MapLayoutRequest {
        private int width;
        private int height;
        private String mapImageUrl;
        private String fontFamily;
        private String crs;

        // 地图区域配置
        private Integer mapLeft;
        private Integer mapTop;
        private Integer mapWidth;
        private Integer mapHeight;

        // 标题
        private boolean showTitle;
        private String title;
        private String subtitle;
        private String titleStyle;
        private Integer titleSize;
        private Integer subtitleSize;
        private String accentColor;

        // 图例
        private boolean showLegend;
        private List<LegendLayer> legendLayers;
        private String legendStyle;

        // 指北针
        private NorthArrowConfig northArrow;

        // 比例尺
        private ScaleBarConfig scaleBar;

        // 元数据
        private boolean showMetadata;
        private String metaStyle;
        private String dataSource;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LegendLayer {
        private String name;
        private String type; // polygon/line/point
        private String fillColor;
        private String strokeColor;
        private String color;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NorthArrowConfig {
        private String style; // arrow/compass/star
        private String position;
        private int size;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScaleBarConfig {
        private String position;
        private String unit; // km/m
        private int barWidth;
    }
}