# GeoNexus 实施方案 v2.0

> 本文档是 SPEC.md 的补充，提供各功能模块的详细实现方案
> 版本：v2.0 | 更新：2026-04-13

---

## 一、架构重构方案

### 1.1 问题诊断

| # | 问题 | 严重性 | 类型 |
|---|------|--------|------|
| 1 | 命名不统一（geoagent vs geonexus） | 🔴 必须修复 | 命名 |
| 2 | 30+ 服务堆在一个模块，无法演进 | 🔴 必须重构 | 架构 |
| 3 | GISService 900+ 行，职责过重 | 🔴 必须拆分 | 架构 |
| 4 | 缺少消息队列，实时数据架构缺失 | 🔴 必须新增 | 架构 |
| 5 | 无多租户支持 | 🟡 重要 | 功能 |
| 6 | Entity 缺审计字段、软删除、版本控制 | 🟡 重要 | 数据 |
| 7 | 前端 legacy 代码未迁移 | 🟡 重要 | 前端 |
| 8 | LLM 集成无熔断/重试/路由策略 | 🟡 重要 | 可用性 |
| 9 | 缺少 OpenLayers 封装 | 🟢 次要 | 前端 |
| 10 | 路径规划缺失 | 🟢 次要 | 功能 |

### 1.2 重构目标架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Frontend (Vue 3 Monorepo)                       │
│   packages/apps/web  │  packages/components  │  packages/utils           │
│   Design System (待建立) │  vue-i18n (待建立)  │  VueUse                  │
└──────────────────────────────┬────────────────────────────────────────┘
                                │ REST (HTTP) + WebSocket
         ┌──────────────────────▼──────────────────────┐
         │            Spring Cloud Gateway              │
         │   (JWT Auth │ Rate Limit │ Routing │ Audit)  │
         └──────────────────────┬──────────────────────┘
                                │
   ┌────────────────────────────┼────────────────────────────────────┐
   │                     Backend (DDD Modules)                         │
   │                                                              │
   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
   │  │geonexus-api│  │geonexus-chat│  │geonexus-map             │  │
   │  │(Controllers)│  │(对话领域)   │  │(地图/发布/渲染)         │  │
   │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
   │  │geonexus-gis│  │geonexus-data│  │geonexus-realtime       │  │
   │  │(空间分析)   │  │(数据/ETL)   │  │(实时流/轨迹/CZML) ←NEW │  │
   │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
   │  ┌──────────────────────────────────────────────────────────┐   │
   │  │geonexus-infrastructure │ geonexus-common                 │   │
   │  │(Redis/Kafka/文件存储)    │(工具/熔断/重试)                │   │
   │  └──────────────────────────────────────────────────────────┘   │
   └─────────────────────────────────────────────────────────────────┘
          │                    │                      │
    ┌─────▼─────┐       ┌──────▼──────┐       ┌──────▼──────┐
    │ PostgreSQL│       │   Redis     │       │    Kafka    │
    │ + PostGIS │       │ (缓存/会话) │       │ (事件流)    │
    └───────────┘       └─────────────┘       └─────────────┘
          │                                        │
    ┌─────▼────────────────────────────────────────▼──────┐
    │  MinIO (文件存储)  │  GeoServer (OGC服务)           │
    └────────────────────────────────────────────────────┘
```

### 1.3 后端模块拆分（Maven 多模块设计）

```
geonexus-backend/
├── pom.xml                    # Parent POM（版本管理、依赖管理）
│
├── geonexus-common/           # 公共模块（无外部依赖）
│   ├── pom.xml
│   └── src/main/java/com/geonexus/common/
│       ├── domain/            # 实体基类、值对象
│       │   ├── BaseEntity.java         # 审计字段基类
│       │   ├── TenantAware.java        # 多租户接口
│       │   └── GeoPoint.java           # 地理坐标值对象
│       ├── exception/         # 统一异常体系
│       │   ├── GeoNexusException.java
│       │   ├── ResourceNotFoundException.java
│       │   └── GeometryProcessException.java
│       ├── result/            # 统一返回格式
│       │   └── ApiResult.java
│       └── util/              # 工具类
│
├── geonexus-gis/              # GIS 空间分析模块
│   ├── pom.xml
│   └── src/main/java/com/geonexus/gis/
│       ├── spatial/            # 空间计算
│       │   ├── SpatialAnalysisService.java   # 缓冲区/叠加/空间连接
│       │   ├── GeometryCalcService.java      # 距离/面积/长度/质心
│       │   ├── GeometryProcessService.java   # 简化/凸包/边界
│       │   └── ProjectionService.java        # 坐标投影转换
│       ├── index/             # 空间索引
│       │   └── SpatialIndexService.java
│       ├── validate/          # 几何验证
│       │   └── GeometryValidationService.java
│       └── format/            # 格式转换
│           └── GeoJSONConverter.java
│
├── geonexus-chat/             # 对话模块
│   ├── pom.xml
│   └── src/main/java/com/geonexus/chat/
│       ├── domain/
│       │   ├── ChatSessionEntity.java
│       │   └── MessageEntity.java
│       ├── repository/
│       ├── service/
│       │   ├── ChatService.java
│       │   └── ContextManager.java
│       └── controller/
│           └── ChatController.java
│
├── geonexus-map/              # 地图模块
│   ├── pom.xml
│   └── src/main/java/com/geonexus/map/
│       ├── domain/
│       │   └── MapDocumentEntity.java
│       ├── service/
│       │   ├── MapService.java            # 地图生成/渲染
│       │   ├── MapPublishService.java     # 图层发布
│       │   └── TileCacheService.java     # 瓦片缓存
│       └── controller/
│           └── MapController.java
│
├── geonexus-data/             # 数据管理模块
│   ├── pom.xml
│   └── src/main/java/com/geonexus/data/
│       ├── service/
│       │   ├── DataService.java          # 数据上传/下载
│       │   ├── FormatConverterService.java # 格式转换
│       │   └── ETLService.java          # ETL Pipeline
│       └── controller/
│           └── DataController.java
│
├── geonexus-realtime/         # 实时数据模块（新增 Kafka）
│   ├── pom.xml
│   └── src/main/java/com/geonexus/realtime/
│       ├── kafka/
│       │   ├── GeoEventConsumer.java     # Kafka 消费者
│       │   └── TrajectoryProducer.java   # Kafka 生产者
│       ├── service/
│       │   ├── TrajectoryService.java   # 轨迹处理
│       │   ├── GeofenceService.java     # 电子围栏
│       │   └── CZMLGeneratorService.java # CZML 生成
│       ├── websocket/
│       │   └── RealtimeEndpoint.java    # WebSocket 端点
│       └── controller/
│           └── RealtimeController.java
│
├── geonexus-llm/              # LLM 模块（独立）
│   ├── pom.xml
│   └── src/main/java/com/geonexus/llm/
│       ├── provider/
│       │   ├── LLMProvider.java         # 接口
│       │   ├── OpenAIProvider.java
│       │   ├── AnthropicProvider.java
│       │   └── OllamaProvider.java
│       ├── service/
│       │   ├── LLMService.java          # 统一调度
│       │   ├── ModelRouter.java        # 路由策略
│       │   └── TokenCounter.java       # 用量统计
│       └── config/
│           └── ResilienceConfig.java    # 熔断/重试配置
│
├── geonexus-geoserver/        # GeoServer 集成模块
│   ├── pom.xml
│   └── src/main/java/com/geonexus/geoserver/
│       ├── service/
│       │   ├── GeoServerService.java
│       │   └── StyleService.java       # SLD 样式管理
│       └── controller/
│           └── GeoServerController.java
│
├── geonexus-infra/            # 基础设施模块
│   ├── pom.xml
│   └── src/main/java/com/geonexus/infra/
│       ├── config/
│       │   ├── RedisConfig.java
│       │   ├── KafkaConfig.java
│       │   └── MinioConfig.java
│       ├── security/
│       │   ├── JwtAuthFilter.java
│       │   └── RateLimitInterceptor.java
│       └── observability/
│           ├── MetricsConfig.java
│           └── TracingConfig.java
│
└── geonexus-api/              # API 网关（入口）
    ├── pom.xml
    └── src/main/java/com/geonexus/api/
        ├── GeoNexusApplication.java
        ├── config/
        │   ├── CorsConfig.java
        │   ├── SwaggerConfig.java
        │   └── WebSocketConfig.java
        └── controller/               # 聚合 Controller（如需要）
```

### 1.4 模块依赖关系

```
各模块依赖方向（单向依赖，禁止循环）：

geonexus-api
    ↓
    ├─► geonexus-chat
    ├─► geonexus-map
    ├─► geonexus-data
    ├─► geonexus-realtime
    └─► geonexus-geoserver

geonexus-chat ──► geonexus-common
geonexus-map ──► geonexus-common
geonexus-data ──► geonexus-common
geonexus-realtime ──► geonexus-common
geonexus-llm ──► geonexus-common
geonexus-gis ──► geonexus-common
geonexus-geoserver ──► geonexus-common

geonexus-infra ──► geonexus-common

geonexus-chat ──► geonexus-llm
geonexus-chat ──► geonexus-gis

geonexus-realtime ──► geonexus-gis
geonexus-map ──► geonexus-gis
geonexus-map ──► geonexus-geoserver
```

### 1.5 GISService 拆分方案（现有 900+ 行 → 6 个专一服务）

| 原方法 | 拆分后服务 | 新方法名 |
|--------|-----------|---------|
| `calculate_distance` | GeometryCalcService | `haversineDistance()` |
| `calculate_area` | GeometryCalcService | `area()` |
| `calculate_length` | GeometryCalcService | `length()` |
| `buffer_analysis` | SpatialAnalysisService | `buffer()` |
| `spatial_join` | SpatialAnalysisService | `spatialJoin()` |
| `overlay_analysis` | SpatialAnalysisService | `union()`/`intersection()`/`difference()` |
| `reproject_coordinates` | ProjectionService | `transform()` |
| `simplify_geometry` | GeometryProcessService | `simplify()` |
| `convex_hull` | GeometryProcessService | `convexHull()` |
| `validate_geometry` | GeometryValidationService | `isValid()` |
| `load_geojson` | (移至 DataService) | - |
| `load_shapefile` | (移至 DataService) | - |

### 1.4 前端重构方案

```
现状：
  src/              ← legacy 代码，未迁移
  packages/         ← 新 monorepo 结构，但未完全使用

目标：完全迁移到 packages/ 结构

迁移清单：
  [x] packages/apps/web/src/views/      # Views（已完成）
  [ ] src/components/map/               → packages/components/src/map/
  [ ] src/components/common/           → packages/components/src/common/
  [ ] src/composables/                  → packages/hooks/src/
  [ ] src/stores/                       → packages/stores/src/

Cesium 组件合并：
  CesiumView.vue        ← 3D 地图主组件
  CesiumViewPanel.vue   ← Cesium 操作面板
  CesiumScene.vue       ← 场景配置
  UnifiedMapPanel.vue   ← 2D/3D 统一入口
  ↓ 合并为：
  CesiumView.vue        ← 主组件（含 CesiumScene 配置）
  MapPanel.vue          ← 2D/3D 统一操作面板
```

---

## 二、实时数据架构（WebSocket + Kafka）

### 2.1 整体数据流

```
[数据源: 传感器/车辆/航班]
         ↓
    [MQTT Broker / Kafka Connect]
         ↓
    [GeoEventCollector (新服务)]
         ↓ 解析 + 标准化
    [Kafka Topic: geonexus.geo-events]
         ↓
    ┌───────────────────────────────────────┐
    │          Realtime Domain               │
    │  ┌─────────────────────────────────┐  │
    │  │ TrajectoryProcessor             │  │
    │  │ 1. 轨迹补全（平滑/插值）        │  │
    │  │ 2. Geofence 越界检测             │  │
    │  │ 3. 告警触发                     │  │
    │  └─────────────────────────────────┘  │
    │           ↓                              │
    │  ┌─────────────────────────────────┐  │
    │  │ CZMLGeneratorService            │  │
    │  │  生成轨迹 CZML 文件              │  │
    │  │  写入 MinIO: /czml/{id}.czml   │  │
    │  └─────────────────────────────────┘  │
    └───────────────────────────────────────┘
         ↓                    ↓
    [WebSocket]          [PostgreSQL]
    /ws/v1/realtime       持久化轨迹
         ↓
    [前端 Cesium]
    加载 CZML + 播放
```

### 2.2 WebSocket 协议设计

```json
// 客户端订阅
{
  "type": "subscribe",
  "channels": ["trajectory.vehicle.001", "geofence.alert"]
}

// 服务端推送
{
  "type": "entity_update",
  "channel": "trajectory.vehicle.001",
  "data": {
    "id": "vehicle.001",
    "position": [116.397, 39.908],
    "timestamp": "2026-04-13T07:55:00Z",
    "heading": 45,
    "speed": 60
  }
}

// 告警推送
{
  "type": "alert",
  "channel": "geofence.alert", 
  "data": {
    "alertType": "GEOFENCE_EXIT",
    "entityId": "vehicle.001",
    "geofenceId": "zone.A1",
    "timestamp": "2026-04-13T07:55:00Z"
  }
}
```

### 2.3 Kafka Topic 设计

| Topic | 用途 | Partition | Key |
|-------|------|-----------|-----|
| `geonexus.geo-events` | 原始事件采集 | 6 | entityId |
| `geonexus.trajectory` | 轨迹处理 | 6 | entityId |
| `geonexus.czmler` | CZML 生成任务 | 3 | sessionId |
| `geonexus.alerts` | 告警事件 | 3 | alertType |
| `geonexus.analytics` | 分析任务 | 6 | taskId |

### 2.4 待办

- [ ] 设计 Kafka Topic + Consumer Group
- [ ] 实现 GeoEventCollector（从 MQTT 桥接到 Kafka）
- [ ] 实现 TrajectoryProcessor（轨迹平滑、补全、越界检测）
- [ ] 实现 CZMLGeneratorService
- [ ] 前端 WebSocket Client Hook（`useRealtimeTrack.ts`）
- [ ] 前端 Cesium 时间轴控件（TimeSlider）

---

## 三、3D Tiles 加载

### 3.1 三种数据源

| 数据源 | 接入方式 | 场景 |
|--------|---------|------|
| Cesium ion | `Cesium3DTileset.fromIon(assetId)` | 全球精选 3D 城市 |
| 自托管（MinIO/S3） | `Cesium3DTileset.fromUrl(url)` | 私有倾斜摄影 |
| OSGB（国产格式） | 预处理转换 | 大疆/Smart3D 数据 |

### 3.2 OSGB 预处理 Pipeline

```
OSGB 文件目录
    ↓
[osgb2cesium 工具]  (推荐: cesium-unreal-converter / osgb2cesium)
    ↓
生成 3D Tileset JSON 索引 + B3DM 二进制
    ↓
上传至 MinIO: /3dtiles/{project}/{tileset.json}
    ↓
前端加载: Cesium3DTileset.fromUrl(MinIO_Presigned_URL)
```

### 3.3 3D Tiles 图层管理设计

```typescript
// 前端：3D Tiles 图层配置
interface TilesetLayer {
  id: string
  name: string
  type: 'ion' | 'self-hosted' | 'osgb'
  url: string              // ion assetId 或 tileset.json URL
  ionAssetId?: number      // ion 资产 ID
  style?: Cesium3DTilesetStyle  // 样式配置
  position?: {             // 初始位置
    lon: number
    lat: number
    height: number
  }
  visible: boolean
  maxScreenSpaceError?: number  // 渲染精度
}
```

### 3.4 待办

- [ ] 调研 osgb2cesium 工具（推荐 2-3 个备选）
- [ ] 实现 TilesetService（后端管理 tileset 元数据）
- [ ] 前端 3D Tiles 图层管理器 UI
- [ ] tileset 样式编辑面板（颜色、透明度、显隐条件）

---

## 四、AI 空间分析

### 4.1 热力图聚合 + 异常检测

```
POI 数据集
    ↓
[AdvancedGeoProcessingService]
    ↓ DBSCAN 聚类
聚类结果 + 密度权重
    ↓ IsolationForest 异常检测
异常点标记
    ↓
GeoJSON 热力图 FeatureCollection
    ↓
[前端 Cesium]
CesiumHeatmapImageryProvider 或
DataSourceSupplerier 自定义渲染
```

**依赖服务：**
- `SpatialClusteringService`（新，用于 DBSCAN）
- `AnomalyDetectionService`（新，用于 IsolationForest）
- `HeatmapGeneratorService`（新，生成热力图 GeoJSON）

### 4.2 智能标注推荐

```
地图视野边界（viewport bounds）
    ↓
获取视野内所有要素
    ↓
[LabelPlacementService]
    1. 密度计算（格网统计）
    2. 冲突检测（JTS Buffer + Intersection）
    3. 可见性评分（重要度 + 遮挡）
    4. 贪心算法布局
    ↓
返回非冲突标注位置列表
```

### 4.3 对话式查询（NL → GIS DSL）

```
用户输入: "找出天安门10公里内三级以上医院"
    ↓
[LLMService] → Prompt Engineering
    ↓
{
  "layer": "hospital",
  "filter": "level >= 3",
  "spatialRelation": {
    "type": "buffer",
    "reference": "天安门坐标",
    "distance": 10,
    "unit": "km"
  },
  "outputFormat": "geojson"
}
    ↓
PostGIS / GeoTools 执行查询
    ↓
返回 GeoJSON + 地图链接
```

**Prompt 模板（优化后）：**
```
你是一个专业的GIS查询生成器。

输入：用户自然语言描述的GIS查询需求
输出：JSON格式的GIS查询DSL

支持的操作：
- 图层: hospital, school, road, building, water, landuse
- 空间关系: buffer, intersect, within, contains, distance
- 属性过滤: =, !=, >, <, >=, <=, like, in

示例输入: "找出天安门10公里内所有医院"
示例输出: {"layer": "hospital", "spatialRelation": {"type": "buffer", "center": [116.397, 39.908], "radius": 10, "unit": "km"}, "properties": {}}

请严格按照JSON格式返回，不要包含其他文字。
```

### 4.4 待办

- [ ] 实现 SpatialClusteringService（DBSCAN）
- [ ] 实现 AnomalyDetectionService（IsolationForest + scikit-learn）
- [ ] 实现 HeatmapGeneratorService
- [ ] 优化 LabelPlacementService（冲突算法）
- [ ] 完善 NL → GIS DSL Prompt

---

## 五、低代码地图配置

### 5.1 图层可视化搭建

```
前端组件：LayerManagerPanel.vue

功能：
  1. 图层列表（可拖拽排序）
  2. 显隐控制（眼睛图标）
  3. 透明度拖拽条
  4. 缩放至图层按钮
  5. 右键菜单（样式/删除/复制）
  6. 批量操作（全显/全隐/折叠）

拖拽排序 → 记录 layerOrder → 保存至 MapDocumentEntity.layers
```

### 5.2 SLD 样式配置器

```
设计思路：类似 QGIS 的符号化配置器

步骤：
1. 选择要素类型（点/线/面）
2. 选择符号化类型
   - 单一符号（Single Symbol）
   - 分类符号（Categorized）→ 按字段值分类
   - 分级符号（Graduated）→ 按数值范围分级
   - 热力图（Heatmap）
3. 参数配置面板
   - 点：形状、大小、颜色、描边
   - 线：线型、宽度、颜色、端点样式
   - 面：填充色、填充图案、描边
4. 实时预览（MiniMap）
5. 导出 SLD XML

后端：SLDGeneratorService → GeoServer REST API
前端：SLDEditor.vue
```

### 5.3 待办

- [ ] LayerManagerPanel.vue 组件
- [ ] SldEditor.vue（SLD 可视化编辑器）
- [ ] 地图模板导入/导出功能
- [ ] MapDocument 版本管理与回滚

---

## 六、离线 / 弱网支持

### 6.1 瓦片缓存策略

```
缓存层级（从近到远）：
L1: 内存（LruCache）
L2: IndexedDB（前端瓦片缓存）
L3: Service Worker（拦截 /tiles/ 请求）
L4: 后端 TileCacheService（预生成瓦片）

缓存更新策略：
  → 区域下载（用户选择 bounding box + 缩放级别）
  → 瓦片过期时间（7天/30天/永不过期）
  → 后台静默更新（检测到新瓦片时）
```

### 6.2 离线包设计

```
离线地图包格式：.geonexus（本质上是一个 .zip）

结构：
  /offline-map.geonexus
    /tiles/           # 瓦片文件（.pbf 或 .png）
    /tileset.json     # 瓦片集元数据
    /geojson/         # 离线矢量数据
    /config.json      # 图层配置、坐标系、中心点
    /manifest.json    # 包信息、创建时间、有效期

下载流程：
1. 用户框选区域 + 选择缩放级别
2. 后端 TileCacheService 计算瓦片数量
3. 前端显示预估大小，用户确认
4. 后端批量生成瓦片 → 打包 → MinIO
5. 前端下载 .geonexus 文件
6. Service Worker 拦截地图请求 → 优先读本地包
```

### 6.3 待办

- [ ] 离线包下载 UI（Leaflet.draw 框选区域）
- [ ] TileCacheService 批量瓦片生成
- [ ] .geonexus 包格式定义与解析器
- [ ] Service Worker 拦截逻辑（`sw.js`）
- [ ] 断网续传（分片下载 + 校验）

---

## 七、Cesium ion 影像 + 多影像源

### 7.1 影像源管理

```typescript
// 影像源配置
interface ImagerySource {
  id: string
  name: string
  type: 'ion' | 'wms' | 'tms' | 'xyz'
  default: boolean
  
  // ion 类型
  ionAssetId?: number
  ionAccessToken?: string
  
  // WMS/TMS/XYZ 类型
  url?: string
  layers?: string        // WMS 图层名
  format?: string        // image/png
  styles?: string
  crs?: string
}

// 默认影像源
const DEFAULT_IMAGERY = [
  { id: 'world-imagery', name: 'Esri World Imagery', type: 'ion', ionAssetId: 3845, default: true },
  { id: 'bing-maps', name: 'Bing Maps', type: 'ion', ionAssetId: 2 },
  { id: 'tianditu', name: '天地图影像', type: 'wms', url: 'http://t0.tianditu.gov.cn/img_w/wmts' },
]
```

### 7.2 待办

- [ ] 影像源切换 UI（设置面板）
- [ ] 多影像叠加对比功能（ swipe 控件）
- [ ] 天地图等国内影像源接入

---

## 八、安全与权限细化

### 8.1 数据模型扩展

```sql
-- 资源表
CREATE TABLE gis_resources (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL,  -- 'dataset', 'layer', 'feature', 'map'
  parent_id UUID REFERENCES gis_resources(id),
  metadata JSONB,
  created_at TIMESTAMP DEFAULT NOW(),
  deleted_at TIMESTAMP,  -- 软删除
  version INT DEFAULT 1
);

-- 权限表
CREATE TABLE resource_permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  user_id UUID NOT NULL,
  resource_id UUID REFERENCES gis_resources(id),
  permission VARCHAR(20) NOT NULL,  -- 'read', 'write', 'admin', 'delete'
  row_filter JSONB,  -- 行级过滤，如 {"district": "朝阳区"}
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(tenant_id, user_id, resource_id)
);

-- 审计日志表
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID,
  user_id UUID,
  action VARCHAR(50) NOT NULL,  -- 'CREATE', 'UPDATE', 'DELETE', 'READ'
  resource_type VARCHAR(50),
  resource_id UUID,
  details JSONB,
  ip_address INET,
  user_agent TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
```

### 8.2 行级过滤实现

```java
// GeoTools Filter 加入租户 + 行级权限条件
public Filter applyRowLevelSecurity(Filter original, UserDetails user) {
    // 1. 租户条件
    Filter tenantFilter = FF.equals(
        FF.property("tenantId"), 
        FF.literal(user.getTenantId())
    );
    
    // 2. 行级权限（从 permission.row_filter 读取）
    List<Filter> rowFilters = permissionService.getRowFilters(user, resourceId);
    
    // 3. 组合
    return FF.and(Arrays.asList(
        original, tenantFilter, FF.or(rowFilters)
    ));
}
```

### 8.3 待办

- [ ] 审计日志 AOP 切面
- [ ] 行级权限 Filter 中间件
- [ ] 数据脱敏规则配置（正则替换）
- [ ] API 权限校验注解（`@RequiresPermission("resource:read")`）

---

## 九、国际化（i18n）

### 9.1 方案

```
前端：vue-i18n
  locales/
    zh-CN.json
    en-US.json

使用方式：
$t('map.toolbar.zoomIn')
$t('gis.buffer.radius')
```

### 9.2 待办

- [ ] 建立 `locales/` 目录结构
- [ ] 提取所有 UI 文本到语言包
- [ ] 实现语言切换（AppHeader 设置面板）
- [ ] 地图标注的多语言（从语言包读取图例翻译）

---

## 十、其他待补全功能

### 10.1 路径规划

```
方案：
  → 集成 OSRM（开源路由引擎，Docker 部署）
  → 或 Mapbox Directions API
  → 或 GraphHopper

API 设计：
POST /api/v1/routing/route
{
  "points": [[lon1, lat1], [lon2, lat2], ...],
  "profile": "car",  // car/bike/pedestrian
  "options": { "avoid": ["highways", "tolls"] }
}

响应：
{
  "distance": 12345,  // 米
  "duration": 600,     // 秒
  "geometry": "Encoded Polyline",
  "steps": [...]
}
```

### 10.2 OpenLayers 封装

```
packages/components/src/map/
  LeafletMap.vue      # Leaflet 2D
  OpenLayersMap.vue   # OpenLayers 2D（大数据量场景） ← 新增
  CesiumMap.vue       # Cesium 3D

使用场景：
  - 小数据量、快速原型 → Leaflet
  - 大数据量、复杂渲染 → OpenLayers
  - 3D 可视化 → Cesium
```

---

## 十一、测试策略

### 11.1 分层测试

```
单元测试：
  - Service 层：每个方法独立测试（Mockito）
  - GIS 几何计算：JTS TestBuilder 对比验证
  - LLM Prompt：快照测试

集成测试：
  - Controller 层：MockMvc
  - Kafka Consumer：EmbeddedKafka
  - PostgreSQL/Redis：Testcontainers

E2E 测试：
  - Playwright：前端交互测试
  - REST Assured：API 端到端测试
```

---

## 十二、实施优先级 v2.0

| 优先级 | 模块 | 关联 | 工时估 |
|--------|------|------|--------|
| P0 | 1.架构重构（命名统一 + 模块拆分） | 所有 | 高 |
| P0 | 2.实时数据架构（Kafka + WebSocket） | 实时轨迹/CZML | 高 |
| P0 | 3.3D Tiles 加载 | Cesium 3D | 高 |
| P1 | 4.LLM 增强（熔断/路由/Token 统计） | 对话质量 | 中 |
| P1 | 5.AI 空间分析（热力图+异常检测） | AI+GIS | 中 |
| P1 | 6.SLD 样式配置器 | 低代码 | 中 |
| P1 | 7.国际化 i18n | 面向更多用户 | 低 |
| P2 | 8.CZML 时空流 | 依赖 P0 | 中 |
| P2 | 9.离线瓦片缓存 | 弱网场景 | 高 |
| P2 | 10.细粒度权限 + 审计 | 企业场景 | 中 |
| P2 | 11.路径规划 | OSRM 集成 | 低 |
| P2 | 12.OpenLayers 封装 | 备选渲染器 | 低 |

---

*Last updated: 2026-04-13*
