package com.geonexus.api.v1;

import com.geonexus.model.dto.TilesetDTO;
import com.geonexus.service.TilesetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 3D Tileset 管理 API
 *
 * 提供 tileset 的注册、查询、删除功能
 */
@RestController
@RequestMapping("/api/v1/tilesets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "3D瓦片管理", description = "3D Tileset注册、查询、更新与删除接口")
public class TilesetController {

    private final TilesetService tilesetService;

    /**
     * GET /api/v1/tilesets - 列出所有 tileset
     */
    @Operation(summary = "列出Tileset", description = "获取所有已注册的3D Tileset，支持按类型过滤")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<List<TilesetDTO>> listTilesets(
            @Parameter(description = "Tileset类型：CESIUM_ION/SELF_HOSTED/MINIO") @RequestParam(required = false) String type
    ) {
        List<TilesetDTO> tilesets;
        if (type != null) {
            tilesets = tilesetService.listTilesetsByType(TilesetDTO.TilesetType.valueOf(type.toUpperCase()));
        } else {
            tilesets = tilesetService.listTilesets();
        }
        return ResponseEntity.ok(tilesets);
    }

    /**
     * GET /api/v1/tilesets/{id} - 获取 tileset 信息
     */
    @Operation(summary = "获取Tileset信息", description = "根据ID获取Tileset详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "Tileset不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TilesetDTO> getTileset(
            @Parameter(description = "Tileset ID") @PathVariable String id) {
        return tilesetService.getTileset(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/tilesets - 注册新 tileset
     */
    @Operation(summary = "注册Tileset", description = "注册新的3D Tileset到系统中")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping
    public ResponseEntity<TilesetDTO> registerTileset(@RequestBody TilesetDTO request) {
        TilesetDTO tileset = tilesetService.register(request);
        return ResponseEntity.ok(tileset);
    }

    /**
     * DELETE /api/v1/tilesets/{id} - 删除 tileset
     */
    @Operation(summary = "删除Tileset", description = "根据ID删除指定的3D Tileset")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "Tileset不存在")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTileset(
            @Parameter(description = "Tileset ID") @PathVariable String id) {
        if (tilesetService.deleteTileset(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * PATCH /api/v1/tilesets/{id}/status - 更新 tileset 状态
     */
    @Operation(summary = "更新Tileset状态", description = "更新Tileset的加载状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @Parameter(description = "Tileset ID") @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        tilesetService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/v1/tilesets/{id}/config - 获取前端加载配置
     */
    @Operation(summary = "获取Tileset加载配置", description = "获取前端Cesium加载Tileset所需的完整配置参数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "Tileset不存在")
    })
    @GetMapping("/{id}/config")
    public ResponseEntity<?> getLoadConfig(
            @Parameter(description = "Tileset ID") @PathVariable String id) {
        return tilesetService.getTileset(id)
                .map(t -> ResponseEntity.ok(Map.of(
                        "id", t.getId(),
                        "name", t.getName(),
                        "type", t.getType(),
                        "url", t.getUrl() != null ? t.getUrl() : "",
                        "ionAssetId", t.getIonAssetId() != null ? t.getIonAssetId() : 0,
                        "position", Map.of(
                                "longitude", t.getLongitude() != null ? t.getLongitude() : 0,
                                "latitude", t.getLatitude() != null ? t.getLatitude() : 0,
                                "height", t.getHeight() != null ? t.getHeight() : 0
                        ),
                        "orientation", Map.of(
                                "heading", t.getHeading() != null ? t.getHeading() : 0,
                                "pitch", t.getPitch() != null ? t.getPitch() : 0,
                                "roll", t.getRoll() != null ? t.getRoll() : 0
                        ),
                        "maxScreenSpaceError", t.getMaxScreenSpaceError() != null ? t.getMaxScreenSpaceError() : 16
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
