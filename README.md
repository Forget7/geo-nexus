# GeoNexus - 数字GIS专家系统

> 基于大模型与优秀开源项目构建的智能GIS专家系统

## 📸 项目预览

<!-- 截图待补充。如有截图，请放在 `docs/screenshots/` 目录下并取消下方注释 -->
<!-- ![Chat + Map](./docs/screenshots/chat-map.png) -->
<!-- ![3D Cesium View](./docs/screenshots/cesium-3d.png) -->
<!-- ![Data Upload](./docs/screenshots/data-upload.png) -->

**预览截图请放到 `docs/screenshots/` 目录**，推荐分辨率 1280×800，支持 PNG/JPG 格式。

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3 Monorepo)                       │
│   packages/apps/web        # 主应用（已废弃，视图移至src/views/）
│   packages/components      # 共享组件 (CesiumView/ChatPanel/...)      │
│   packages/stores          # Pinia 状态管理                          │
│   packages/hooks           # 组合式函数 (useCesium/useMap/...)       │
│   packages/types           # TypeScript 类型定义                     │
│   packages/utils           # 工具函数                               │
│   src/                     # 主应用源码 (views/api/router等)         │
│   Port: 3000                                                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ REST API + WebSocket
┌──────────────────────────────▼──────────────────────────────────────┐
│                       Backend (Java Spring Boot)                        │
│   Spring Boot 3.2 + Spring Data JPA + GeoTools + JTS                 │
│   Port: 8000                                                         │
│   ┌────────────────────────────────────────────────────────────┐    │
│   │ REST: /api/v1/{chat,map,data,tools,tasks,auth,publish}     │    │
│   │ WS:   /ws/v1/{chat,realtime}                              │    │
│   │ Swagger: /swagger-ui.html  |  Prometheus: /actuator/...   │    │
│   └────────────────────────────────────────────────────────────┘    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                        Infrastructure                                  │
│   PostgreSQL + PostGIS  |  Redis 7  |  GeoServer 2.25  |  MinIO    │
│   Docker Compose 编排  |  Kubernetes 部署配置                       │
└─────────────────────────────────────────────────────────────────────┘
```

## ✨ 核心功能

| 分类 | 功能 | 状态 |
|------|------|------|
| 🤖 AI 对话 | 自然语言理解 + GIS 查询 | ✅ 已实现 |
| 🗺️ 地图可视化 | 2D (Leaflet) / 3D (Cesium) | ✅ 已实现 |
| 📐 空间分析 | 缓冲区/叠加/空间连接/凸包/质心 | ✅ 已实现 |
| 🔄 数据处理 | 格式转换/投影转换/ETL | ✅ 已实现 |
| 🧭 路径规划 | OSRM 集成/轨迹服务 | ✅ 已实现 |
| 📡 OGC 服务 | WMS/WFS/WMTS (GeoServer) | ✅ 已实现 |
| 🤝 多Agent | Coordinator + Specialist Agent | ✅ 已实现 |
| 🔌 模型无关 | OpenAI/Claude/Gemini/Ollama | ✅ 已实现 |
| ⏰ 4D 可视化 | CZML 时空数据流 | 🔜 规划中 |
| 🏗️ 3D Tiles | 倾斜摄影/城市模型加载 | 🔜 规划中 |
| 📊 AI 分析 | 热力图聚合/异常检测 | 🔜 规划中 |
| 🧩 低代码 | SLD 配置器/模板市场 | 🔜 规划中 |

## 🛠️ 技术栈

### Frontend

| 技术 | 用途 |
|------|------|
| Vue 3 + Composition API | 前端框架 |
| TypeScript | 类型安全 |
| Pinia | 状态管理 |
| Vue Router | 页面路由 |
| pnpm workspaces | Monorepo 包管理 |
| Leaflet | 2D 地图 |
| CesiumJS | 3D 地图 / Cesium ion |
| Axios | HTTP 客户端 |

### Backend

| 技术 | 用途 |
|------|------|
| Spring Boot 3.2 | Web 框架 |
| Spring Data JPA | ORM / 持久化 |
| Spring Data Redis | 缓存 / 会话 |
| Springdoc OpenAPI | API 文档 |
| Spring AOP | 切面（权限/审计） |
| OpenTelemetry | 分布式追踪 |
| GeoTools | GIS 数据处理 |
| JTS | 几何操作 |
| Lombok | 简化代码 |

### Infrastructure

| 技术 | 用途 |
|------|------|
| PostgreSQL 15 + PostGIS 3.3 | 空间数据库 |
| Redis 7 Alpine | 缓存 / 消息队列 |
| GeoServer 2.25 | OGC 标准服务 |
| MinIO | 对象存储 |
| Docker Compose | 本地编排 |
| Kubernetes | 生产部署 |

## 📁 项目结构

```
geonexus/
├── backend/                          # Java Spring Boot 后端
│   └── src/main/java/com/geonexus/
│       ├── GeoNexusApplication.java
│       ├── api/v1/                  # REST 控制器
│       │   ├── AuthController.java
│       │   ├── ChatController.java
│       │   ├── DataController.java
│       │   ├── GeoServerController.java
│       │   ├── MapController.java
│       │   ├── PublishController.java
│       │   ├── TaskController.java
│       │   └── ToolsController.java
│       ├── config/                  # 配置类（已废弃，统一迁移至api/config/）
│       ├── api/config/              # API配置类
│       │   ├── CorsConfig.java
│       │   ├── GlobalExceptionHandler.java
│       │   ├── RateLimitInterceptor.java
│       │   ├── SecurityConfig.java
│       │   ├── SwaggerConfig.java
│       │   └── WebSocketConfig.java
│       ├── domain/                  # JPA 实体
│       │   ├── AsyncTaskEntity.java
│       │   ├── ChatSessionEntity.java
│       │   ├── GISDataEntity.java
│       │   ├── MapDocumentEntity.java
│       │   └── MessageEntity.java
│       ├── model/dto/               # 请求/响应 DTO
│       ├── repository/              # JPA 仓库
│       └── service/                 # 业务逻辑 (30+ 服务)
│           ├── LLMService.java      # 大模型统一调用
│           ├── GISService.java      # 地理空间分析
│           ├── GeoServerService.java # OGC 服务管理
│           ├── MapService.java      # 地图生成
│           ├── ChatService.java     # 对话管理
│           ├── StreamingChatService.java
│           ├── DataService.java     # 数据导入/转换
│           ├── CoordinateTransformService.java
│           ├── TrajectoryService.java
│           ├── GeofenceService.java
│           ├── AdvancedGeoProcessingService.java
│           ├── TileCacheService.java
│           ├── IntelligentMapService.java
│           ├── LabelPlacementService.java
│           ├── WorkflowService.java
│           ├── AlertService.java
│           ├── CollaborationService.java
│           ├── KnowledgeBaseService.java
│           ├── SatelliteImageryService.java
│           ├── Model3DService.java
│           └── ...
│
├── frontend/                         # Vue 3 Monorepo
│   ├── packages/
│   │   ├── apps/web/               # 主应用（预留）
│   │   ├── components/             # 共享组件库
│   │   │   └── src/
│   │   │       ├── map/
│   │   │       │   ├── CesiumView.vue
│   │   │       │   ├── IntelligentMap.vue
│   │   │       │   ├── MapView.vue
│   │   │       │   ├── MapToolbar.vue
│   │   │       │   ├── AttributeTable.vue
│   │   │       │   ├── StatisticsPanel.vue
│   │   │       │   └── MapShare.vue
│   │   │       ├── chat/
│   │   │       │   ├── ChatPanel.vue
│   │   │       │   └── ChatMessage.vue
│   │   │       └── common/
│   │   │           └── LanguageSwitcher.vue
│   │   ├── stores/                 # Pinia 状态
│   │   │   └── src/                # ⚠️ stores在src/子目录
│   │   │       ├── chat.ts         # ⚠️ 混杂EffectManager，需拆分
│   │   │       ├── map.ts          # ⚠️ 混杂useAppStore，需拆分
│   │   │       ├── app.ts
│   │   │       └── ui.ts
│   │   ├── hooks/                 # 组合式函数
│   │   │   └── src/                # ⚠️ hooks在src/子目录
│   │   │       ├── useCesium.ts
│   │   │       ├── useCesiumAdvanced.ts
│   │   │       ├── useCesiumIon.ts
│   │   │       ├── useGeoJSON.ts
│   │   │       └── useMapSync.ts
│   │   ├── types/                 # TS 类型定义
│   │   └── utils/                 # 工具函数
│   │       └── src/
│   ├── src/
│   │   ├── views/                # 页面视图
│   │   │   ├── ChatView.vue
│   │   │   ├── MapView.vue
│   │   │   ├── DataView.vue
│   │   │   ├── ToolsView.vue
│   │   │   ├── HomeView.vue
│   │   │   └── SharedMapView.vue
│   │   ├── api/                  # API 客户端
│   │   ├── router/               # 路由配置
│   │   ├── locales/              # i18n 语言包
│   │   ├── components/           # 根级组件
│   │   │   └── common/
│   │   └── App.vue
│   └── public/
│
├── config/                          # YAML 配置
│   ├── agents.yaml                  # Agent 角色定义
│   ├── gis_tools.yaml              # GIS 工具配置
│   └── models.yaml                 # 模型列表
│
├── k8s/                             # Kubernetes 部署
│   ├── geonexus.yaml
│   └── monitoring/dashboards/
│
├── scripts/                          # 运维脚本
│   └── health-check.sh
│
├── docker-compose.yml               # Docker 编排
├── .env.example                     # 环境变量模板
└── SPEC.md                          # 架构文档
```

## 🚀 快速开始

### 前置条件

- Docker & Docker Compose
- JDK 17+
- Node.js 18+
- pnpm 9+

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入你的 API Keys
```

### 2. 启动所有服务（Docker Compose 一键启动）

```bash
docker-compose up -d
```

### 3. 本地开发模式（非 Docker）

```bash
# 终端 1：启动基础设施
docker-compose up -d postgres cache kafka geoserver minio

# 终端 2：启动后端
cd backend && ./mvnw spring-boot:run

# 终端 3：启动前端
cd frontend && pnpm dev
```

### 4. 访问

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| API | http://localhost:8000 |
| Swagger | http://localhost:8000/swagger-ui.html |
| GeoServer | http://localhost:8080/geoserver |
| MinIO Console | http://localhost:9001 |

## 🌐 API 概览

> 完整 API 文档请访问 [Swagger UI](http://localhost:8000/swagger-ui.html)

### 对话 (Chat)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/chat` | 发送消息 |
| GET | `/api/v1/chat/sessions` | 获取会话列表 |
| GET | `/api/v1/chat/sessions/{sessionId}` | 获取指定会话 |
| GET | `/api/v1/chat/sessions/{sessionId}/messages` | 获取会话消息历史 |
| DELETE | `/api/v1/chat/sessions/{sessionId}` | 删除会话 |
| GET | `/api/v1/chat/models` | 获取可用模型列表 |
| GET | `/api/v1/chat/models/usage` | 获取 Token 使用统计 |

### 地图 (Map)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/map/generate` | 生成地图 |
| GET | `/api/v1/map/{mapId}` | 获取地图 |
| GET | `/api/v1/map/{mapId}/2d` | 获取 2D 地图 |
| GET | `/api/v1/map/{mapId}/3d` | 获取 3D 地图 |
| GET | `/api/v1/map/{mapId}/html` | 获取嵌入式 HTML |

### 数据 (Data)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/data/upload` | 上传数据 |
| GET | `/api/v1/data/{dataId}` | 获取数据详情 |
| GET | `/api/v1/data/{dataId}/geojson` | 获取 GeoJSON |
| POST | `/api/v1/data/convert` | 格式转换 |
| POST | `/api/v1/data/{dataId}/sanitize` | 数据脱敏 |

### 工具 (Tools)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/tools` | 获取工具列表 |
| POST | `/api/v1/tools/execute` | 执行工具 |
| GET | `/api/v1/tools/distance` | 距离计算 |
| POST | `/api/v1/tools/buffer` | 缓冲区分析 |
| POST | `/api/v1/tools/intersect` | 交集分析 |
| POST | `/api/v1/tools/union` | 合并分析 |
| POST | `/api/v1/tools/difference` | 差集分析 |
| POST | `/api/v1/tools/convexHull` | 凸包计算 |
| POST | `/api/v1/tools/simplify` | 几何简化 |
| POST | `/api/v1/tools/within` | 空间包含判断 |
| POST | `/api/v1/tools/geocode` | 地址编码 |
| GET | `/api/v1/tools/geocode/reverse` | 逆地理编码 |
| POST | `/api/v1/tools/labels/generate` | 智能标注生成 |

### 任务 (Tasks)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/tasks/{type}` | 创建异步任务 |
| GET | `/api/v1/tasks/{taskId}` | 查询任务状态 |

### GeoServer

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/geoserver/health` | GeoServer 健康检查 |
| GET | `/api/v1/geoserver/workspaces` | 获取工作空间列表 |
| POST | `/api/v1/geoserver/workspaces` | 创建工作空间 |
| GET | `/api/v1/geoserver/layers/{layerName}` | 获取图层 |
| POST | `/api/v1/geoserver/layers` | 发布图层 |
| GET | `/api/v1/geoserver/layers/{layerName}/wms-url` | 获取 WMS URL |
| GET | `/api/v1/geoserver/wms/capabilities` | WMS 能力描述 |
| GET | `/api/v1/geoserver/wfs/capabilities` | WFS 能力描述 |
| GET | `/api/v1/geoserver/styles/templates` | 获取样式模板 |
| POST | `/api/v1/geoserver/styles` | 创建样式 |
| PUT | `/api/v1/geoserver/styles/{styleName}` | 更新样式 |
| GET | `/api/v1/geoserver/styles/{styleName}` | 获取样式 |

### 分享发布 (Publish)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/publish` | 发布地图分享 |
| GET | `/api/v1/publish/list` | 获取分享列表 |
| GET | `/api/v1/publish/{shareId}` | 获取分享详情 |
| PUT | `/api/v1/publish/{shareId}` | 更新分享 |
| DELETE | `/api/v1/publish/{shareId}` | 删除分享 |
| GET | `/api/v1/publish/{shareId}/embed` | 获取嵌入代码 |
| GET | `/api/v1/publish/{shareId}/ogc` | 获取 OGC 服务地址 |

### 地图模板 (Templates)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/templates` | 获取模板列表 |
| GET | `/api/v1/templates/categories` | 获取模板分类 |
| GET | `/api/v1/templates/{id}` | 获取模板详情 |
| POST | `/api/v1/templates/{id}/apply` | 应用模板 |

### 瓦片集 (Tilesets)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/tilesets` | 获取瓦片集列表 |
| POST | `/api/v1/tilesets` | 上传瓦片集 |
| GET | `/api/v1/tilesets/{id}` | 获取瓦片集详情 |
| DELETE | `/api/v1/tilesets/{id}` | 删除瓦片集 |
| GET | `/api/v1/tilesets/{id}/config` | 获取配置 |
| PATCH | `/api/v1/tilesets/{id}/status` | 更新状态 |

### 三维场景 (Scenes)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/scenes/templates` | 获取场景模板 |
| POST | `/api/v1/scenes/templates` | 创建场景模板 |
| GET | `/api/v1/scenes/templates/{id}` | 获取场景模板详情 |
| DELETE | `/api/v1/scenes/templates/{id}` | 删除场景模板 |
| POST | `/api/v1/scenes/templates/{id}/apply` | 应用场景模板 |

### 路径规划 (Routes)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/routes/plan` | 路径规划 |

### 离线包 (Offline)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/offline/package` | 生成离线包 |
| GET | `/api/v1/offline/package/{jobId}` | 查询离线包状态 |
| GET | `/api/v1/offline/estimate` | 估算离线包大小 |

### 点云 (PointCloud)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/pointcloud/load` | 加载点云 |
| POST | `/api/v1/pointcloud/batch-load` | 批量加载点云 |
| GET | `/api/v1/pointcloud/formats` | 获取支持格式 |

### 权限 (Permission)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/permissions/user/me` | 获取当前用户权限 |
| GET | `/api/v1/permissions/{type}/{id}` | 获取资源权限 |
| POST | `/api/v1/permissions` | 创建权限规则 |
| DELETE | `/api/v1/permissions/{id}` | 删除权限规则 |

### 审计 (Audit)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/audit/logs` | 获取审计日志 |
| GET | `/api/v1/audit/logs/{id}` | 获取审计日志详情 |
| GET | `/api/v1/audit/stats` | 获取审计统计 |

### 认证 (Auth)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/auth/token` | 获取 JWT Token |
| POST | `/api/v1/auth/refresh` | 刷新 Token |
| GET | `/api/v1/auth/validate` | 验证 Token |

### WebSocket

| 路径 | 描述 |
|------|------|
| `/ws/v1/chat` | WebSocket 对话（流式响应） |
| `/ws/v1/realtime` | 实时轨迹数据流 |

### Actuator

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/actuator/health` | 完整健康检查 |
| GET | `/actuator/health/liveness` | 存活检查 |
| GET | `/actuator/health/readiness` | 就绪检查 |
| GET | `/actuator/prometheus` | Prometheus 指标 |

## 🛠️ GIS 工具集

### 基础计算
- `calculate_distance` — Haversine 距离计算
- `calculate_area` — 面积计算
- `calculate_length` — 长度/周长计算
- `reproject_coordinates` — 坐标投影转换

### 空间分析
- `buffer_analysis` — 缓冲区分析
- `spatial_join` — 空间连接
- `overlay_analysis` — 叠加分析
- `convex_hull` — 凸包计算
- `centroid` — 质心计算
- `simplify_geometry` — 几何简化

### 数据管理
- `geocode` — 地址编码
- `reverse_geocode` — 逆地理编码
- `fetch_osm_data` — OSM 数据获取

## 📡 支持的坐标系

| EPSG | 名称 | 类型 |
|------|------|------|
| 4326 | WGS 84 | Geographic |
| 3857 | Web Mercator | Projected |
| 4490 | CGCS 2000 | Geographic |
| 32650 | UTM Zone 50N | Projected |

## 🤖 支持的模型

通过 `LLMService` 统一调用：

| 提供商 | 模型 |
|--------|------|
| OpenAI | gpt-4o, gpt-4-turbo, gpt-3.5-turbo |
| Anthropic | claude-3-5-sonnet, claude-3-opus |
| Google | gemini-1.5-pro, gemini-1.5-flash |
| Ollama | llama3, mistral, qwen2 |
| Meta | llama-3-70b-instruct |

## 🔧 环境变量

> 完整环境变量列表见 `.env.example`。生产环境必须设置以下变量。

### 数据库

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `DB_HOST` | PostgreSQL 主机 | localhost |
| `DB_PORT` | PostgreSQL 端口 | 5432 |
| `DB_NAME` | 数据库名 | geonexus |
| `DB_USER` | 数据库用户名 | geonexus |
| `DB_PASSWORD` | 数据库密码 | -（必须设置） |

### Redis

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `REDIS_HOST` | Redis 主机 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | - |

### Kafka

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 服务器 | localhost:9092 |

### MinIO

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `MINIO_ENDPOINT` | MinIO 地址 | localhost:9000 |
| `MINIO_ACCESS_KEY` | MinIO Access Key | - |
| `MINIO_SECRET_KEY` | MinIO Secret Key | - |

### GeoServer

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `GEOSERVER_URL` | GeoServer 地址 | http://localhost:8080/geoserver |
| `GEOSERVER_USERNAME` | GeoServer 用户名 | admin |
| `GEOSERVER_PASSWORD` | GeoServer 密码 | -（必须设置） |
| `GEOSERVER_WORKSPACE` | GeoServer 工作空间 | geonexus |

### LLM API Keys

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `OPENAI_API_KEY` | OpenAI API 密钥 | - |
| `ANTHROPIC_API_KEY` | Anthropic API 密钥 | - |
| `GOOGLE_API_KEY` | Google API 密钥 | - |
| `LLM_API_KEY` | 通用 LLM API 密钥（fallback） | - |

### Cesium

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `CESIUM_ION_TOKEN` | Cesium Ion Token | - |

### JWT

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `JWT_SECRET` | JWT 签名密钥 | -（生产必须设置） |
| `JWT_EXPIRATION` | Token 过期时间(ms) | 3600000 |

### OpenTelemetry（可选）

| 变量 | 描述 | 默认值 |
|------|------|--------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP 接收端点 | http://localhost:4317 |
| `OTEL_SERVICE_NAME` | 服务名 | geonexus-api |

## ❓ 常见问题

**Q: 启动后端报数据库连接失败？**
A: 确保 PostgreSQL + PostGIS 已启动：`docker-compose up -d postgres`。检查 `.env` 中 `DB_PASSWORD` 是否与 docker-compose 中 `DB_PASSWORD` 一致。

**Q: Swagger UI 访问 404？**
A: 确认后端已完全启动，访问 http://localhost:8000/swagger-ui.html（注意是 `.html` 后缀）。

**Q: 地图无法加载 Cesium ion 影像？**
A: 需要设置 `CESIUM_ION_TOKEN` 环境变量，请在 [cesium.com/ion](https://cesium.com/ion/) 免费注册获取。

**Q: GeoServer 图层发布失败？**
A: 确认 GeoServer 已启动（`docker-compose up -d geoserver`），并检查 `GEOSERVER_URL` / `GEOSERVER_PASSWORD` 配置正确。

**Q: LLM 对话返回 401？**
A: 检查 `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` / `GOOGLE_API_KEY` 环境变量是否正确设置，且额度充足。

**Q: WebSocket 连接失败？**
A: 确认前端 `VITE_API_URL` 配置的后端地址可访问，前端默认配置为 `http://localhost:8000`。

## 🤝 贡献

欢迎贡献代码！请阅读 [CONTRIBUTING.md](./CONTRIBUTING.md) 了解开发环境搭建、代码规范和分支策略。

## 📚 文档

- [SPEC.md](./SPEC.md) — 完整架构文档（系统架构、API、数据模型、Roadmap）
- [IMPLEMENTATION.md](./IMPLEMENTATION.md) — 功能模块详细实施方案
- [API 文档](http://localhost:8000/swagger-ui.html) — Swagger UI

## License

MIT
