/**
 * GeoNexus TypeScript 类型定义
 */

// ========== 几何类型 ==========

export interface Point {
  type: 'Point'
  coordinates: [number, number] // [lng, lat]
}

export interface LineString {
  type: 'LineString'
  coordinates: [number, number][]
}

export interface Polygon {
  type: 'Polygon'
  coordinates: [number, number][][]
}

export interface MultiPoint {
  type: 'MultiPoint'
  coordinates: [number, number][]
}

export interface MultiLineString {
  type: 'MultiLineString'
  coordinates: [number, number][][]
}

export interface MultiPolygon {
  type: 'MultiPolygon'
  coordinates: [number, number][][][]
}

export interface GeometryCollection {
  type: 'GeometryCollection'
  geometries: Geometry[]
}

export type Geometry = Point | LineString | Polygon | MultiPoint | MultiLineString | MultiPolygon | GeometryCollection

// ========== GeoJSON ==========

export interface GeoJSONFeature<G extends Geometry = Geometry, P = Record<string, unknown>> {
  type: 'Feature'
  geometry: G
  properties: P
  id?: string | number
}

export interface GeoJSONFeatureCollection<G extends Geometry = Geometry, P = Record<string, unknown>> {
  type: 'FeatureCollection'
  features: GeoJSONFeature<G, P>[]
  totalFeatures?: number
  numberMatched?: number
  numberReturned?: number
  bbox?: [number, number, number, number]
  crs?: CRS
}

export interface CRS {
  type: string
  properties: Record<string, unknown>
}

// ========== API类型 ==========

// 通用
export interface APIResponse<T = unknown> {
  success: boolean
  data?: T
  error?: APIError
  timestamp: number
}

export interface APIError {
  code: string
  message: string
  details?: Record<string, unknown>
  stack?: string
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// 聊天
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  timestamp: number
  attachments?: Attachment[]
  metadata?: ChatMetadata
}

export interface ChatMetadata {
  sessionId?: string
  model?: string
  mapUrl?: string
  tokens?: TokenUsage
  tools?: string[]
  attachments?: string[]
}

export interface ChatSession {
  id: string
  title: string
  userId: string
  createdAt: number
  updatedAt: number
  messageCount: number
  lastMessage?: string
  tags?: string[]
  starred?: boolean
  archived?: boolean
}

export interface TokenUsage {
  inputTokens: number
  outputTokens: number
  totalTokens: number
}

// 地图
export interface MapConfig {
  id: string
  title: string
  center: [number, number]
  zoom: number
  mode: '2d' | '3d'
  tileType: TileType
  layers: MapLayer[]
  style?: MapStyle
  crs?: string
}

export type TileType = 'osm' | 'satellite' | 'dark' | 'terrain' | 'custom'

export interface MapLayer {
  id: string
  name: string
  type: 'geojson' | 'wms' | 'wmts' | 'tile' | 'marker' | 'heatmap'
  visible: boolean
  opacity: number
  data?: Geometry | Geometry[] | string
  url?: string
  style?: LayerStyle
  zIndex?: number
}

export interface LayerStyle {
  color?: string
  fillColor?: string
  fillOpacity?: number
  weight?: number
  opacity?: number
  radius?: number
  dashArray?: string
}

export interface MapStyle {
  background?: string
  labels?: boolean
  scale?: boolean
  compass?: boolean
  minimap?: boolean
}

// GIS数据
export interface GISData {
  id: string
  name: string
  type: DataType
  format: DataFormat
  size: number
  crs?: string
  bounds?: BoundingBox
  geometryType?: string
  featureCount?: number
  properties?: DataProperties
  tags?: string[]
  createdAt: number
  updatedAt: number
  uploadedBy?: string
  url?: string
  thumbnail?: string
}

export type DataType = 'vector' | 'raster' | 'tabular'
export type DataFormat = 'GeoJSON' | 'Shapefile' | 'KML' | 'GML' | 'GPX' | 'CSV' | 'GeoTIFF' | 'MBTiles'

export interface BoundingBox {
  minLng: number
  minLat: number
  maxLng: number
  maxLat: number
}

export interface DataProperties {
  encoding?: string
  hasZ?: boolean
  hasM?: boolean
  is3D?: boolean
  simplifyTolerance?: number
}

// 附件
export interface Attachment {
  id: string
  name: string
  type: string
  size: number
  url: string
  mimeType: string
  thumbnailUrl?: string
}

// ========== GIS工具 ==========

export interface GISTool {
  name: string
  description: string
  category: ToolCategory
  parameters: ToolParameter[]
  returns: ToolReturn
  examples?: ToolExample[]
  tags?: string[]
}

export type ToolCategory = 
  | 'measurement' 
  | 'analysis' 
  | 'transformation' 
  | 'conversion' 
  | 'validation'

export interface ToolParameter {
  name: string
  type: ParameterType
  description: string
  required: boolean
  default?: unknown
  options?: unknown[]
  validation?: ValidationRule[]
}

export type ParameterType = 'string' | 'number' | 'boolean' | 'geometry' | 'array' | 'object' | 'enum'

export interface ValidationRule {
  type: 'min' | 'max' | 'minLength' | 'maxLength' | 'pattern' | 'custom'
  value?: unknown
  message?: string
}

export interface ToolReturn {
  type: ParameterType
  description: string
  format?: string
}

export interface ToolExample {
  name: string
  parameters: Record<string, unknown>
  description?: string
}

export interface ToolExecution {
  id: string
  tool: string
  parameters: Record<string, unknown>
  startedAt: number
  completedAt?: number
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'
  result?: unknown
  error?: string
  progress?: number
  logs?: ExecutionLog[]
}

export interface ExecutionLog {
  timestamp: number
  level: 'debug' | 'info' | 'warn' | 'error'
  message: string
}

// ========== GeoServer ==========

export interface GeoServerLayer {
  name: string
  workspace: string
  title: string
  abstract?: string
  keywords?: string[]
  srs?: string
  bbox?: BoundingBox
  style?: string
  url?: string
}

export interface GeoServerStyle {
  name: string
  workspace?: string
  filename: string
  format: 'sld' | 'css' | 'xml'
}

export interface WMSCapabilities {
  version: string
  name: string
  title: string
  abstract?: string
  layers: WMSLayer[]
  srs: string[]
  bbox: BoundingBox
  imageFormats: string[]
}

export interface WMSLayer {
  name: string
  title: string
  abstract?: string
  srs: string[]
  bbox: BoundingBox
  styles: WMSStyle[]
  childLayers?: WMSLayer[]
}

// ========== 用户与认证 ==========

export interface User {
  id: string
  username: string
  email?: string
  avatar?: string
  roles: UserRole[]
  preferences?: UserPreferences
  createdAt: number
  lastLoginAt?: number
}

export type UserRole = 'user' | 'admin' | 'operator'

export interface UserPreferences {
  theme?: 'light' | 'dark' | 'auto'
  language?: string
  defaultMapCenter?: [number, number]
  defaultMapZoom?: number
  units?: 'metric' | 'imperial'
}

export interface AuthToken {
  accessToken: string
  refreshToken?: string
  expiresIn: number
  tokenType: 'Bearer'
}

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

// ========== WebSocket ==========

export interface WSMessage<T = unknown> {
  type: WSMessageType
  payload?: T
  timestamp: number
  requestId?: string
}

export type WSMessageType = 
  | 'chat.message'
  | 'chat.typing'
  | 'chat.read'
  | 'map.update'
  | 'tool.progress'
  | 'tool.complete'
  | 'tool.error'
  | 'notification'
  | 'ping'
  | 'pong'

// ========== 事件 ==========

export interface AppEvent<T = unknown> {
  id: string
  type: string
  source: string
  timestamp: number
  data: T
}

// ========== 配置 ==========

export interface AppConfig {
  apiUrl: string
  wsUrl?: string
  tileServers: Record<TileType, string>
  defaultMapCenter: [number, number]
  defaultMapZoom: number
  maxUploadSize: number
  allowedFileTypes: string[]
  features: FeatureFlags
}

export interface FeatureFlags {
  enable3D: boolean
  enableOffline: boolean
  enableRealTimeCollaboration: boolean
  enableAdvancedGIS: boolean
}

// ========== 状态 ==========

export interface AppState {
  initialized: boolean
  version: string
  user?: User
  config?: AppConfig
}

// ========== 工具类型 ==========

export interface Coordinates {
  lng: number
  lat: number
}

export interface Distance {
  value: number
  unit: 'km' | 'm' | 'miles' | 'feet'
}

export interface Area {
  value: number
  unit: 'km²' | 'm²' | 'ha' | 'acres'
}

// ========== Vue组件类型 ==========

export interface SelectOption {
  label: string
  value: string | number
  disabled?: boolean
  icon?: string
  group?: string
}

export interface TableColumn {
  key: string
  title: string
  width?: string
  minWidth?: string
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  fixed?: 'left' | 'right'
  ellipsis?: boolean
}

export interface TreeNode {
  id: string
  label: string
  icon?: string
  children?: TreeNode[]
  isLeaf?: boolean
  disabled?: boolean
  selected?: boolean
  expanded?: boolean
  data?: unknown
}
