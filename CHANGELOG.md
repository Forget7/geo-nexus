# Changelog

## [v1.0.0] - 2026-04-14

> 完整功能版本发布。核心 GIS 专家系统功能全部可用。

### Added

- **完整 GIS 专家系统功能**（见 [SPEC.md](./SPEC.md)）
- Vue 3 + Spring Boot 微服务架构
- Cesium 3D 地图 + Cesium ion 集成
- Leaflet 2D 地图支持
- ChatGPT/LLM 自然语言对话（OpenAI / Anthropic / Google / Ollama）
- GeoServer OGC 服务（WMS / WFS / WMTS）
- WebSocket 实时数据流（对话 + 轨迹）
- 完整国际化和 UI 多语言（vue-i18n）
- OpenTelemetry 分布式追踪
- 细粒度资源权限 + 数据脱敏
- 地图模板市场
- 异步任务队列（缓冲区分析等耗时操作）
- 多 Agent 协作（Coordinator + Specialist）
- JWT 认证与 RBAC 权限控制
- 矢量数据格式转换（GeoJSON / SHP / KML / GML / GPX）
- 坐标投影转换（EPSG:4326 / EPSG:3857 / EPSG:4490 等）
- 空间分析（缓冲区 / 叠加 / 空间连接 / 凸包 / 质心）
- Docker Compose 一键部署
- Kubernetes 生产部署配置
- Prometheus 监控指标
- 分层健康检查（liveness / readiness）

---

## [v2.0] - 2026-04-13

All notable changes to the GeoNexus project will be documented in this file.

## [v2.0] - 2026-04-13

> Major architecture upgrade. See [IMPLEMENTATION.md](./IMPLEMENTATION.md) for full details.

### � Architecture

- **命名统一为 `geonexus`**：旧名 `geoagent` 已废弃
  - Docker 容器名：`geoagent-api` → `geonexus-api`
  - 数据库名：`geoagent` → `geonexus`
  - application.yml name：`geoagent` → `geonexus`
  - Redis key 前缀：统一为 `geonexus:*`
  - Docker network：`geoagent-network` → `geonexus-network`

- **后端 DDD 多模块化**（进行中）：
  - `geonexus-api` — API 入口
  - `geonexus-chat` — 对话领域
  - `geonexus-map` — 地图领域
  - `geonexus-gis` — GIS 分析领域
  - `geonexus-data` — 数据管理领域
  - `geonexus-realtime` — 实时数据（新增 Kafka 架构）
  - `geonexus-infrastructure` — 基础设施
  - `geonexus-common` — 公共模块

- **GISService 拆分**：900+ 行单服务拆分为多个专一服务
  - `SpatialAnalysisService` / `GeometryCalculationService`
  - `ProjectionService` / `SpatialIndexService`

- **新增 Kafka**：实时数据流基础设施
  - Topic：`geonexus.geo-events`、`geonexus.trajectory`、`geonexus.alerts`
  - 实时轨迹处理：GeoEventCollector → TrajectoryProcessor → CZMLGenerator

### 📡 新增功能

| 功能 | 描述 | 优先级 |
|------|------|--------|
| WebSocket 实时数据 | 传感器/车辆/航班实时轨迹接入 | P0 |
| Kafka 事件流 | 事件驱动架构基础 | P0 |
| CZML 时空数据流 | 4D 轨迹可视化 | P1 |
| 3D Tiles 加载 | OSGB / 3D Tiles 倾斜摄影 | P0 |
| AI 空间分析 | 热力图聚合 + 异常检测 | P1 |
| SLD 样式配置器 | 低代码地图样式编辑 | P1 |
| 国际化 i18n | vue-i18n 多语言支持 | P1 |
| Cesium ion 影像 | 高分辨率商业卫星影像 | P1 |
| 离线瓦片缓存 | .geonexus 离线包格式 | P2 |
| 路径规划 | OSRM / GraphHopper 集成 | P2 |
| OpenLayers | 备选 2D 渲染器（大数据量场景） | P2 |

### 🔧 技术栈更新

**新增：**
- Spring Kafka（事件流）
- Resilience4j（熔断/重试）
- OpenLayers（2D 备选渲染器）
- vue-i18n（国际化）
- VueUse（组合式工具）

**前端 monorepo 完成：**
- `packages/apps/web` — 主应用
- `packages/components` — 共享组件
- `packages/stores` — Pinia 状态
- `packages/hooks` — 组合式函数
- `packages/types` — TypeScript 类型
- `packages/utils` — 工具函数

### 🛡️ 安全增强

- Entity 基类增加审计字段（`createdBy`、`updatedBy`、`deletedAt`）
- 软删除支持
- 乐观锁（`@Version`）
- 多租户支持（`tenantId`）
- 行级权限过滤（Row Level Security）
- 审计日志表（`audit_logs`）
- 数据脱敏规则配置

### 📊 可观测性

- 分层健康检查（liveness / readiness / full）
- Prometheus Metrics（含 LLM Token 统计）
- Kafka Consumer Lag 监控
- HikariCP 连接池监控
- OpenTelemetry 分布式追踪（规划中）

### ⚠️ Breaking Changes

1. 数据库名从 `geoagent` 改为 `geonexus`（需数据迁移）
2. Docker 容器名从 `geoagent-*` 改为 `geonexus-*`
3. 环境变量前缀统一为 `GEONEXUS_`
4. Redis key 前缀从 `geoagent:` 改为 `geonexus:`
5. Docker network 从 `geoagent-network` 改为 `geonexus-network`

### 🗺️ Roadmap

- [ ] 后端 DDD 模块拆分（进行中）
- [ ] 前端 legacy 代码迁移完成
- [ ] Kafka 实时数据架构上线
- [ ] 3D Tiles 加载完成
- [ ] AI 热力图异常检测上线

---

## [v1.0] - 2026-04-12

Initial release with basic GIS expert system capabilities.
