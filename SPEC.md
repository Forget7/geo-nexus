# GeoNexus - 数字GIS专家系统架构文档

> 基于大模型与优秀开源项目构建的智能GIS专家系统

> **⚠️ 重要变更（v2.0）：** 命名已统一为 `geonexus`，旧名 `geoagent` 废弃。
> 架构已升级为 DDD 多模块，新增 Kafka 实时数据流，详见 [IMPLEMENTATION.md](./IMPLEMENTATION.md)。

## 1. System Architecture

> **命名约定：** 所有项目资源统一使用 `geonexus` 前缀。
> 旧名 `geoagent`（容器名、数据库名、application name）已废弃，逐步迁移中。

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Frontend (Vue 3 Monorepo)                            │
│  packages/apps/web    ┌ packages/components ┌ packages/stores            │
│  (Views: Chat/Map/   │ (CesiumView/Chat/   │ (chat/map/ui stores)       │
│   Data/Tools/Home)   │  MapToolbar/...)    │                            │
│  packages/hooks ─────► useCesium / useMapSync / useGeoJSON              │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │ HTTP / WebSocket
         ┌────────────────────▼────────────────────┐
         │             API Gateway                  │
         │  JWT Auth  │  RateLimit (60 req/min)   │
         │  CORS      │  GlobalExceptionHandler    │
         └────────────────────┬────────────────────┘
                              │
    ┌─────────────────────────┼─────────────────────────┐
    │                         │                         │
┌───▼────────┐  ┌────────────▼────────┐  ┌────────────▼────────┐
│ Chat API    │  │   Map / Publish     │  │  Tools / Tasks /    │
│ Service     │  │   API Service       │  │  Data API Service   │
└────┬────────┘  └────────────┬────────┘  └────────────┬────────┘
     │                        │                         │
┌────▼────────────────────────▼────────────────────────▼────────────┐
│                       Core Services (30+)                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐     │
│  │ LLMService   │  │ GISService   │  │ MapService           │     │
│  │ 统一模型调用  │  │ 空间分析     │  │ 地图生成渲染         │     │
│  ├──────────────┤  ├──────────────┤  ├──────────────────────┤     │
│  │ ChatService  │  │ GeoServer    │  │ CoordinateTransform  │     │
│  │ 对话管理     │  │ Service      │  │ 坐标投影转换         │     │
│  ├──────────────┤  ├──────────────┤  ├──────────────────────┤     │
│  │ Trajectory   │  │ Geofence     │  │ AdvancedGeo         │     │
│  │ Service      │  │ Service      │  │ ProcessingService   │     │
│  ├──────────────┤  ├──────────────┤  ├──────────────────────┤     │
│  │ Intelligent  │  │ LabelPlace   │  │ TileCache           │     │
│  │ MapService   │  │ mentService  │  │ Service             │     │
│  ├──────────────┤  ├──────────────┤  ├──────────────────────┤     │
│  │ Workflow     │  │ Knowledge    │  │ SatelliteImagery    │     │
│  │ Service      │  │ BaseService  │  │ Service             │     │
│  ├──────────────┤  ├──────────────┤  ├──────────────────────┤     │
│  │ Streaming    │  │ Collaboration│  │ Model3D             │     │
│  │ ChatService  │  │ Service      │  │ Service             │     │
│  └──────────────┘  └──────────────┘  └──────────────────────┘     │
└───────────────────────────────────────────────────────────────────────┘
    │                         │                         │
┌───▼───────────┐  ┌─────────▼──────────┐  ┌───────────▼────────────────┐
│ PostgreSQL    │  │   Redis            │  │  MinIO / GeoServer         │
│ + PostGIS     │  │   (缓存/会话/队列) │  │  (文件存储/OGC服务)       │
└───────────────┘  └────────────────────┘  └─────────────────────────────┘
```

### 1.2 技术栈

#### Frontend
| 技术 | 用途 |
|------|------|
| Vue 3 + Composition API | 前端框架 |
| TypeScript | 类型安全 |
| Pinia | 状态管理 |
| Vue Router | 页面路由 |
| Vue-i18n | 国际化（✅ 已实现） |
| VueUse | 组合式工具函数 |
| Axios | HTTP客户端 |
| Leaflet | 2D地图（轻量场景） |
| OpenLayers | 2D地图（大数据量场景） |
| CesiumJS | 3D地图 / Cesium ion |

#### Backend
| 技术 | 用途 |
|------|------|
| Spring Boot 3.2 | Java Web框架 |
| Spring Data JPA | ORM持久化 |
| Spring Data Redis | 缓存/会话 |
| Spring Kafka | 事件流/实时数据 |
| GeoTools | GIS数据处理 |
| JTS | 几何操作 |
| Springdoc OpenAPI | API文档 |
| Spring AOP | 切面编程（权限/审计） |
| Resilience4j | 熔断/限流/重试 |

#### Infrastructure
| 技术 | 用途 |
|------|------|
| PostgreSQL 15 + PostGIS 3.3 | 空间数据库 |
| Redis 7 | 缓存/会话/消息队列 |
| Apache Kafka | 事件流/实时数据 |
| MinIO | 对象存储 |
| GeoServer 2.25 | OGC标准服务(WMS/WFS/WMTS) |
| Docker Compose | 本地编排 |
| Kubernetes | 生产部署 |

## 2. API Specification

### 2.1 REST API Endpoints

```
Base URL: /api/v1

┌─────────────────────────────────────────────────────────────────┐
│ Chat API                                                         │
├─────────────────────────────────────────────────────────────────┤
│ POST   /chat              # 发送消息                             │
│ GET    /chat/history/{id} # 获取历史                             │
│ DELETE /chat/{id}         # 删除会话                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Map API                                                          │
├─────────────────────────────────────────────────────────────────┤
│ POST   /map/generate      # 生成地图                             │
│ GET    /map/{id}          # 获取地图信息                          │
│ GET    /map/{id}/html     # 获取HTML                            │
│ GET    /map/{id}/2d       # 2D地图                              │
│ GET    /map/{id}/3d       # 3D地图                              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Data API                                                         │
├─────────────────────────────────────────────────────────────────┤
│ POST   /data/convert       # 格式转换                           │
│ POST   /data/upload        # 上传数据                            │
│ GET    /data/{id}          # 获取数据                            │
│ GET    /data/{id}/geojson  # GeoJSON格式                        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Task API (异步任务)                                               │
├─────────────────────────────────────────────────────────────────┤
│ POST   /tasks/{type}       # 创建异步任务                         │
│ GET    /tasks/{taskId}     # 获取任务状态                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Tools API                                                        │
├─────────────────────────────────────────────────────────────────┤
│ GET    /tools              # 工具列表                            │
│ POST   /tools/execute      # 执行工具                            │
│ GET    /tools/distance     # 距离计算                            │
│ POST   /tools/buffer       # 缓冲区分析                          │
│ POST   /tools/geocode      # 地理编码                            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ GeoServer API (OGC标准服务)                                     │
├─────────────────────────────────────────────────────────────────┤
│ GET    /geoserver/health           # 健康检查                     │
│ GET    /geoserver/wms/capabilities # WMS GetCapabilities        │
│ GET    /geoserver/wfs/capabilities # WFS GetCapabilities        │
│ POST   /geoserver/layers          # 发布图层                      │
│ GET    /geoserver/layers/{name}  # 获取图层信息                  │
│ POST   /geoserver/styles          # 创建SLD样式                  │
│ GET    /geoserver/workspaces      # 获取所有Workspace             │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Request/Response Examples

**Chat:**
```json
// POST /api/v1/chat
// Request
{
  "message": "找出天安门10公里内的医院",
  "sessionId": "sess_xxx",
  "mapMode": "2d"
}

// Response
{
  "sessionId": "sess_xxx",
  "type": "message",
  "content": "已找到15所医院，地图生成中...",
  "mapUrl": "/api/v1/map/map_abc123/html"
}
```

**Task (Async):**
```json
// POST /api/v1/tasks/buffer
// Request
{
  "geometry": {...},
  "distanceKm": 10
}

// Response (202 Accepted)
{
  "taskId": "task_xxx",
  "statusUrl": "/api/v1/tasks/task_xxx"
}

// GET /api/v1/tasks/task_xxx
// Response
{
  "taskId": "task_xxx",
  "status": "COMPLETED",
  "result": {...},
  "createdAt": "2026-04-12T10:00:00",
  "completedAt": "2026-04-12T10:00:30"
}
```

## 3. Data Models

### 3.1 Domain Entities

```
ChatSession
├── id: String (UUID)
├── userId: String
├── messages: List<Message>
├── context: Map<String, Object>
├── createdAt: LocalDateTime
└── updatedAt: LocalDateTime

Message
├── id: String (UUID)
├── role: Enum (USER, ASSISTANT, SYSTEM)
├── content: String
├── metadata: Map<String, Object>
└── timestamp: LocalDateTime

MapDocument
├── id: String (UUID)
├── name: String
├── geojson: JSON
├── mode: Enum (2D, 3D)
├── renderer: String
├── thumbnail: String (URL)
├── createdBy: String
└── createdAt: LocalDateTime

GISData
├── id: String (UUID)
├── filename: String
├── format: Enum (GEOJSON, SHP, KML, GML, GPX)
├── crs: String
├── size: Long
├── storageUrl: String
└── uploadedAt: LocalDateTime

AsyncTask
├── id: String (UUID)
├── type: Enum (BUFFER, DISTANCE, OVERLAY, etc.)
├── status: Enum (PENDING, PROCESSING, COMPLETED, FAILED)
├── params: JSON
├── result: JSON
├── error: String
├── createdAt: LocalDateTime
└── completedAt: LocalDateTime
```

## 4. Frontend Architecture

### 4.1 Monorepo 结构（pnpm workspaces）

```
frontend/
├── packages/
│   ├── apps/web/                      # 主应用
│   │   ├── src/views/
│   │   │   ├── HomeView.vue
│   │   │   ├── ChatView.vue
│   │   │   ├── MapView.vue
│   │   │   ├── DataView.vue
│   │   │   └── ToolsView.vue
│   │   └── vite.config.ts
│   │
│   ├── components/                    # 共享组件库
│   │   └── src/
│   │       ├── map/
│   │       │   ├── CesiumView.vue      # Cesium 3D 地图主组件
│   │       │   ├── IntelligentMap.vue   # 智能地图（AI 增强）
│   │       │   ├── MapView.vue         # 通用地图视图
│   │       │   ├── MapToolbar.vue      # 地图工具栏
│   │       │   ├── AttributeTable.vue  # 属性表
│   │       │   ├── StatisticsPanel.vue # 统计分析面板
│   │       │   └── MapShare.vue        # 地图分享
│   │       └── chat/
│   │           ├── ChatPanel.vue       # 聊天面板
│   │           └── ChatMessage.vue     # 消息气泡
│   │
│   ├── stores/                       # Pinia 状态管理
│   │   ├── app.ts                   # 全局应用状态
│   │   ├── chat.ts                  # 聊天状态
│   │   └── map.ts                   # 地图状态
│   │
│   ├── hooks/                        # 组合式函数
│   │   ├── useCesium.ts             # Cesium 基础集成
│   │   ├── useCesiumAdvanced.ts     # Cesium 高级用法
│   │   ├── useCesiumIon.ts          # Cesium ion 集成
│   │   ├── useGeoJSON.ts            # GeoJSON 处理
│   │   ├── useMap.ts                # 通用地图 hook
│   │   ├── useMapSync.ts            # 地图同步
│   │   └── useApi.ts                # API 调用
│   │
│   ├── types/                        # TypeScript 类型定义
│   │   ├── api.ts                   # API 请求/响应类型
│   │   ├── geo.ts                   # GIS 类型（Position/BoundingBox/GeoJSON/MapLayer）
│   │   └── store.ts
│   │
│   └── utils/                        # 工具函数
│       ├── format.ts                # 格式化
│       ├── geo.ts                   # 地理计算
│       ├── cache.ts                 # 缓存
│       ├── error.ts                 # 错误处理
│       └── request.ts              # 请求封装
│
├── src/                              # 根目录旧代码（迁移中）
│   ├── components/map/
│   │   ├── CesiumScene.vue
│   │   ├── CesiumViewPanel.vue
│   │   ├── UnifiedMapPanel.vue
│   │   └── OpenLayersMap.ts
│   ├── views/
│   │   └── SharedMapView.vue
│   └── composables/
│
└── public/
    ├── embed.html                   # 嵌入式地图 HTML
    └── sw.js                        # Service Worker（离线支持）
```
```

### 4.2 State Management (Pinia)

```typescript
// chat store
useChatStore:
  - state: messages, sessionId, isLoading, error
  - actions: sendMessage(), clearMessages(), deleteSession()

// map store  
useMapStore:
  - state: currentMapId, mapMode, center, zoom, geojson
  - actions: generateMap(), setMode(), setTileType()
```

## 5. Backend Architecture

### 5.1 Package Structure

```
com.geonexus/           # 注意：实际包名为 com.geonexus（非 geoagent）
├── GeoNexusApplication.java
├── api/
│   ├── v1/
│   │   ├── AuthController.java       # JWT 认证
│   │   ├── ChatController.java        # 对话管理
│   │   ├── DataController.java        # 数据上传/转换
│   │   ├── GeoServerController.java   # GeoServer OGC 管理
│   │   ├── MapController.java         # 地图生成
│   │   ├── PublishController.java     # 图层发布
│   │   ├── TaskController.java        # 异步任务
│   │   └── ToolsController.java       # GIS 工具执行
│   └── config/
│       ├── ApiVersioningConfig.java   # API 版本控制
│       ├── CorsConfig.java
│       ├── GlobalExceptionHandler.java
│       ├── MetricsConfig.java
│       ├── OpenApiConfig.java
│       ├── RateLimitInterceptor.java   # 限流
│       ├── SecurityConfig.java
│       ├── SwaggerConfig.java
│       ├── WebMvcConfig.java
│       └── WebSocketConfig.java
├── domain/                               # JPA 实体
│   ├── AsyncTaskEntity.java
│   ├── ChatSessionEntity.java
│   ├── GISDataEntity.java
│   ├── MapDocumentEntity.java
│   └── MessageEntity.java
├── model/dto/                            # 请求/响应 DTO
│   ├── ChatRequestDTO.java
│   ├── DataRequestDTO.java
│   └── MapRequestDTO.java
├── repository/                           # JPA 仓库
│   ├── AsyncTaskRepository.java
│   ├── ChatSessionRepository.java
│   ├── MapDocumentRepository.java
│   └── MessageRepository.java
└── service/                              # 业务逻辑（30+ 服务）
    ├── LLMService.java                   # 大模型统一调用
    ├── ChatService.java                  # 对话管理
    ├── StreamingChatService.java         # 流式对话
    ├── GISService.java                   # 地理空间分析（GeoTools+JTS）
    ├── AdvancedGeoProcessingService.java # 高级空间处理
    ├── MapService.java                   # 地图生成渲染
    ├── GeoServerService.java             # GeoServer OGC 操作
    ├── CoordinateTransformService.java    # 坐标投影转换
    ├── DataService.java                  # 数据导入/ETL
    ├── GeoETLService.java                # ETL Pipeline
    ├── TrajectoryService.java            # 轨迹分析
    ├── GeofenceService.java             # 电子围栏
    ├── TileCacheService.java            # 瓦片缓存管理
    ├── IntelligentMapService.java       # 智能地图
    ├── LabelPlacementService.java       # 标注冲突解决
    ├── WorkflowService.java            # 工作流引擎
    ├── AlertService.java               # 告警服务
    ├── CollaborationService.java       # 协作服务
    ├── KnowledgeBaseService.java       # 知识库
    ├── SatelliteImageryService.java    # 卫星影像
    ├── Model3DService.java             # 3D 模型
    ├── SpatialIndexService.java        # 空间索引
    ├── DataQualityService.java         # 数据质量
    ├── StreamingAnalyticsService.java   # 流式分析
    ├── EnterpriseService.java          # 企业级功能
    ├── UserManagementService.java      # 用户管理
    ├── ModelConfigService.java        # 模型配置
    ├── SkillManagementService.java    # 工具管理
    ├── ToolManagementService.java     # 工具注册
    ├── OSMService.java                # OSM 数据获取
    ├── MilitarySymbolService.java     # 军事标号
    ├── CacheService.java             # 缓存服务
    ├── CircuitBreaker.java           # 熔断器
    ├── RetryUtil.java                # 重试工具
    ├── AsyncConfig.java              # 异步配置
    └── AsyncTaskExecutor.java        # 异步任务执行器
```

### 5.2 Agent 配置（config/agents.yaml）

```yaml
agents:
  coordinator:     # GIS 协调专家 - 理解意图，分发任务
  gis_analyst:    # GIS 分析师 - 空间分析工具
  map_generator:  # 地图生成专家 - 可视化
  data_manager:   # 数据管理专家 - 数据导入转换
```

### 5.3 GIS 工具注册（config/gis_tools.yaml）

```yaml
coordinate_systems:   # 支持坐标系（EPSG:4326/3857/4490/32650）
map_styles:          # 地图样式预设
supported_formats:    # 支持数据格式
osm_tags:            # OSM 数据标签过滤
```

### 5.4 Exception Handling

```java
// 自定义异常
ResourceNotFoundException  → 404
BusinessException          → 400
ValidationException        → 422

// 全局处理
GlobalExceptionHandler     → 统一响应格式
RateLimitInterceptor       → 429 Too Many Requests
```

## 6. Security

### 6.1 Rate Limiting

```
- 默认: 60 requests/minute per IP
- 可配置: geoagent.rate-limit.requests-per-minute
- 排除: /health, /swagger-ui/**
```

### 6.2 CORS

```
- 允许来源: 可配置 (默认 *)
- 允许方法: GET, POST, PUT, DELETE
- 允许头部: Content-Type, Authorization
```

## 7. Map Renderers

| Mode | Library | Tile Sources | Use Case |
|------|---------|--------------|----------|
| 2D | Leaflet | OSM, Esri, CartoDB, OpenTopoMap | 快速原型 |
| 3D | Cesium | WorldTerrain, Ion, 3D Tiles | 专业GIS |

### 7.1 3D Tiles 支持

Cesium 支持以下 3D Tiles 格式数据的加载与可视化：

| 格式 | 用途 | 数据源 |
|------|------|--------|
| Batched 3D Model (B3DM) | 建筑物、城市模型 | 倾斜摄影测量 |
| Point Cloud (PNTS) | 点云数据 | LiDAR、SLAM |
| Instanced 3D Model (I3S) | 大规模模型 | Esri Scene Service |
| Composite (CMTR) | 混合多类型 | 多源数据融合 |

## 8. 规划功能 (Roadmap)

### 8.1 已实现功能

- 自然语言对话 + GIS 智能理解
- 空间分析（缓冲区、距离、叠加、凸包、质心等）
- 2D (Leaflet) / 3D (Cesium) 地图可视化
- 数据格式转换（GeoJSON/SHP/KML/GML/GPX）
- 坐标投影转换（EPSG:4326 / EPSG:3857 等）
- 多 Agent 协作（Coordinator + Specialist）
- 模型无关接入（OpenAI / Anthropic / Google / Ollama）
- GeoServer OGC 标准服务（WMS / WFS / WMTS）
- JWT 认证与 RBAC 权限控制
- 异步任务队列（缓冲区分析等耗时操作）

### 8.2 待优化 / 新增功能

#### 3D 能力深化
| 功能 | 描述 | 优先级 |
|------|------|--------|
| 3D Tiles 加载 | 支持 OSGB、3D Tiles 格式倾斜摄影 | P0 |
| CZML 时空数据流 | 支持时间动态数据（轨迹、气象） | P1 |
| 粒子系统 | 支持扩散、流动等特效（污染扩散、风场） | P2 |
| 3D 空间分析 | 3D 缓冲区、可视域分析 | P2 |

#### 实时数据流 / 4D 可视化
| 功能 | 描述 | 优先级 |
|------|------|--------|
| WebSocket 实时接入 | 传感器、车辆、航班等实时轨迹 | P0 |
| 时间轴控件 | 时间轴回放控制（TimeSlider） | P1 |
| 4D 场景保存 | 保存时间维度场景为模板 | P2 |

#### AI 空间分析增强
| 功能 | 描述 | 优先级 |
|------|------|--------|
| 热力图聚合 + 异常检测 | AI 驱动的热力图聚合异常点检测 | P1 |
| 智能标注推荐 | 基于上下文的标注建议 | P1 |
| 对话式查询优化 | 自然语言生成更精确的 GIS 查询 | P1 |
| 路径规划优化 | 考虑实时交通的路径规划 | P2 |

#### 低代码地图配置
| 功能 | 描述 | 优先级 |
|------|------|--------|
| 图层可视化搭建 | 拖拽式图层管理 | P1 |
| SLD 样式配置器 | 图形化样式编辑 | P1 |
| 地图模板市场 | 行业模板快速创建 | P2 |

#### 离线 / 弱网支持
| 功能 | 描述 | 优先级 |
|------|------|--------|
| 矢量瓦片缓存 | 本地缓存矢量切片 | P1 |
| 离线地图包导出 | 下载离线地图包 | P2 |
| 断网续传 | 网络恢复后自动同步 | P2 |

#### 安全与权限细化
| 功能 | 描述 | 优先级 |
|------|------|--------|
| 细粒度资源权限 | 图层、数据集级权限控制 | P1 |
| 数据脱敏 | 敏感信息脱敏处理 | P2 |
| 审计日志 | 操作行为全记录 | P2 |

#### 国际化（i18n）
| 功能 | 描述 | 优先级 |
|------|------|--------|
| UI 多语言 | 中英文切换 | P1 |
| 地图标注多语言 | 图例、标注动态切换 | P2 |

#### 高分辨率影像集成
| 功能 | 描述 | 优先级 |
|------|------|--------|
| Cesium ion 影像 | 接入高分辨率商业卫星影像 | P1 |
| Maxar / Airbus 影像 | 接入更多商业影像源 | P2 |
| 点云数据服务 | LiDAR 点云展示与分析 | P2 |

## 9. Deployment

### 9.1 Docker Compose

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f api

# 停止服务
docker-compose down
```

### 9.2 Environment Variables

> **⚠️ 命名约定：** 环境变量名与 Spring Boot `application.yml` 中的 `${VAR}` 引用保持一致，见 `.env.example` 完整示例。代码中未统一加 `GEONEXUS_` 前缀。

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL 主机 | localhost |
| `DB_PORT` | PostgreSQL 端口 | 5432 |
| `DB_NAME` | 数据库名 | geonexus |
| `DB_USER` | 数据库用户名 | geonexus |
| `DB_PASSWORD` | PostgreSQL 密码 | - |
| `REDIS_HOST` | Redis 主机 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | - |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 服务器 | localhost:9092 |
| `KAFKA_CONSUMER_GROUP_ID` | Kafka Consumer Group | geonexus-group |
| `MINIO_ENDPOINT` | MinIO 地址 | localhost:9000 |
| `MINIO_ACCESS_KEY` | MinIO Access Key | geonexus |
| `MINIO_SECRET_KEY` | MinIO Secret Key | - |
| `GEOSERVER_URL` | GeoServer 地址 | http://localhost:8080/geoserver |
| `GEOSERVER_USERNAME` | GeoServer 用户名 | admin |
| `GEOSERVER_PASSWORD` | GeoServer 密码 | - |
| `GEOSERVER_WORKSPACE` | GeoServer 工作空间 | geonexus |
| `OPENAI_API_KEY` | OpenAI API 密钥 | - |
| `ANTHROPIC_API_KEY` | Anthropic API 密钥 | - |
| `GOOGLE_API_KEY` | Google API 密钥 | - |
| `LLM_API_KEY` | 通用 LLM API Key（fallback） | - |
| `CESIUM_ION_TOKEN` | Cesium Ion Token | - |
| `JWT_SECRET` | JWT 签名密钥 | -（生产必须替换） |
| `JWT_EXPIRATION` | Token 过期时间(ms) | 3600000 |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP 端点 | http://localhost:4317 |
| `OTEL_SERVICE_NAME` | 服务名 | geonexus-api |

## 10. Observability & Monitoring

### 10.1 Health Check（分层健康检查）

| 检查点 | URL | 描述 |
|--------|-----|------|
| Liveness | `GET /actuator/health/liveness` | API 进程存活 |
| Readiness | `GET /actuator/health/readiness` | 所有依赖就绪（DB/Redis/Kafka） |
| Full | `GET /actuator/health` | 完整健康报告 |
| GeoServer | `GET /api/v1/geoserver/health` | GeoServer 存活 |

### 10.2 Metrics（Prometheus 格式）

```
GET /actuator/prometheus
```

关键指标：
- `http_server_requests_seconds` — HTTP 请求延迟（P50/P90/P99）
- `hikaricp_connections_active` — 数据库连接活跃数
- `kafka_consumer_lag` — Kafka 消费延迟
- `geonexus_llm_tokens_total` — Token 消耗统计（自定义）
- `geonexus_gis_analysis_duration_seconds` — GIS 分析耗时

### 10.3 Distributed Tracing（✅ 已实现：TracingConfig + TracingAspect AOP）

```
- 全链路追踪：Spring Cloud Sleuth → Jaeger / Tempo
- 日志聚合：ELK Stack / Loki + Promtail
- 告警：AlertManager + PagerDuty
```

### 10.4 Alerting Rules（⏸ 部署配置，infra 层面处理）

| 告警 | 条件 | 严重性 |
|------|------|--------|
| API 响应超时 | P99 > 5s，持续 5min | 🔴 Critical |
| Kafka Consumer Lag | lag > 10000 | 🟡 Warning |
| 数据库连接池耗尽 | active = max，5min | 🔴 Critical |
| LLM 调用失败率 | failure rate > 10% | 🟡 Warning |
| 磁盘空间不足 | < 10% | 🔴 Critical |

---

## 11. 参考案例与技术对标

### 11.1 行业标杆对标

| 平台 | 核心能力 | 对标 GeoNexus 启示 |
|------|---------|-------------------|
| **ArcGIS Enterprise** | 企业级 GIS、Spatial ML、即时应用 | AI 空间分析、低代码模板 |
| **超图 iClient** | 多框架支持（Leaflet/OpenLayers/MapboxGL/Cesium）、全端 GIS | 多端适配、3D Tiles |
| **Mapbox** | 路径优化、搜索 geocoding、全球数据集 | 实时交通、路径规划 |
| **Cesium** | 3D Tiles、CZML、开放生态 | 4D 时空可视化、粒子系统 |

### 11.2 技术趋势

- **云原生 GIS**：容器化、K8s 部署、服务网格
- **AI + GIS**：Spatial AI、自动化制图、智能解译
- **数字孪生**：实时数据融合、4D 时空城市模型
- **开放标准**：OGC API、新型 3D Tiles 规范

---

*Last updated: 2026-04-13*
