package com.geonexus.api.v1;

import com.geonexus.service.MapLayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业地图出图布局控制器
 * 支持地图布局 HTML 生成、PNG/PDF 导出、布局模板查询
 */
@RestController
@RequestMapping("/api/v1/layout")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "地图出图", description = "专业地图布局设计与导出")
public class MapLayoutController {

    private final MapLayoutService layoutService;

    @PostMapping("/generate")
    @Operation(summary = "生成布局HTML", description = "根据布局配置生成地图布局HTML")
    @ApiResponse(responseCode = "200", description = "生成成功")
    public ResponseEntity<String> generateLayout(@RequestBody MapLayoutService.MapLayoutRequest request) {
        String html = layoutService.generateLayoutHtml(request);
        return ResponseEntity.ok(html);
    }

    @PostMapping("/export/png")
    @Operation(summary = "导出PNG", description = "导出为高分辨率PNG图片")
    @ApiResponse(responseCode = "200", description = "导出成功")
    public ResponseEntity<byte[]> exportPng(@RequestBody MapLayoutService.MapLayoutRequest request) {
        byte[] png = layoutService.exportToPng(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", "map.png");
        return new ResponseEntity<>(png, headers, HttpHeaders.OK);
    }

    @PostMapping("/export/pdf")
    @Operation(summary = "导出PDF", description = "导出为PDF文档")
    @ApiResponse(responseCode = "200", description = "导出成功")
    public ResponseEntity<byte[]> exportPdf(@RequestBody MapLayoutService.MapLayoutRequest request) {
        byte[] pdf = layoutService.exportToPdf(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "map.pdf");
        return new ResponseEntity<>(pdf, headers, HttpHeaders.OK);
    }

    @GetMapping("/templates")
    @Operation(summary = "获取布局模板", description = "获取预定义的布局模板列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<List<LayoutTemplate>> getTemplates() {
        List<LayoutTemplate> templates = List.of(
            LayoutTemplate.builder().id("a4-landscape").name("A4 横版").width(297).height(210).build(),
            LayoutTemplate.builder().id("a4-portrait").name("A4 竖版").width(210).height(297).build(),
            LayoutTemplate.builder().id("a3-landscape").name("A3 横版").width(420).height(297).build(),
            LayoutTemplate.builder().id("a3-portrait").name("A3 竖版").width(297).height(420).build(),
            LayoutTemplate.builder().id("poster").name("海报").width(600).height(400).build(),
            LayoutTemplate.builder().id("custom").name("自定义").width(0).height(0).build()
        );
        return ResponseEntity.ok(templates);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LayoutTemplate {
        private String id;
        private String name;
        private int width;
        private int height;
    }
}