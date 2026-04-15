# 贡献指南

感谢您对 GeoNexus 的关注！我们欢迎任何形式的贡献，无论是新功能、问题修复还是文档改进。

## 开发环境搭建

### 1. 克隆项目

```bash
git clone <repository-url>
cd geonexus
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入必要的 API Keys 和配置
```

**必须设置的环境变量：**
- `DB_PASSWORD` — PostgreSQL 数据库密码
- `JWT_SECRET` — JWT 签名密钥（生产环境必须替换）
- `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` / `GOOGLE_API_KEY` — 至少配置一个 LLM API Key

### 3. 启动基础设施（Docker）

```bash
docker-compose up -d postgres cache kafka geoserver minio
```

### 4. 启动后端

```bash
cd backend
./mvnw spring-boot:run
# 后端启动在 http://localhost:8000
```

### 5. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
# 前端启动在 http://localhost:3000
```

## 代码规范

### Java (后端)

- 遵循 Spring Boot 官方规范
- 使用 Lombok 减少样板代码
- 所有 public 方法必须有 Javadoc 或注释
- 异常应通过 `GlobalExceptionHandler` 统一处理

### TypeScript (前端)

- 使用 Vue 3 Composition API + `<script setup>`
- 启用 TypeScript strict 模式
- 组件文件使用 `PascalCase.vue` 命名
- 组合式函数使用 `use` 前缀（如 `useCesium`）

### 提交信息格式

```
<type>: <description>

feat:  添加路径规划新功能
fix:   修复地图缩放卡顿问题
docs:  更新 README
refactor: 重构 GISService 命名
test:  添加缓冲区分析单元测试
perf:  优化 GeoJSON 解析性能
chore: 更新依赖版本
```

**type 类型：**
- `feat` — 新功能
- `fix` — Bug 修复
- `docs` — 文档更新
- `style` — 代码格式（不影响功能）
- `refactor` — 重构
- `test` — 测试相关
- `perf` — 性能优化
- `chore` — 构建/工具相关

## 分支策略

| 分支 | 用途 |
|------|------|
| `master` | 生产稳定版本，只接受 PR 合并 |
| `develop` | 开发主分支，所有功能汇入 |
| `feat/<name>` | 新功能开发分支 |
| `fix/<name>` | Bug 修复分支 |
| `docs/<name>` | 文档更新分支 |

**工作流程：**
1. 从 `develop` 创建功能分支：`git checkout -b feat/my-feature`
2. 开发完成后提交 PR 到 `develop`
3. 代码 review 通过后合并
4. `develop` 稳定后合并到 `master` 发布

## 测试

### 后端测试

```bash
cd backend
./mvnw test                    # 运行所有测试
./mvnw test -Dtest=LLMServiceTest  # 运行指定测试类
./mvnw verify                  # 完整验证（含集成测试）
```

### 前端测试

```bash
cd frontend
pnpm test                      # 运行单元测试
pnpm test -- --watch          # 监听模式
pnpm test:e2e                 # E2E 测试（如已配置）
```

### 代码质量

```bash
cd backend
./mvnw spotless:apply         # 格式化代码
./mvnw checkstyle:check       # 检查代码风格

cd frontend
pnpm lint                     # ESLint 检查
pnpm lint:fix                 # 自动修复
```

## 项目结构说明

### 后端模块

```
backend/src/main/java/com/geonexus/
├── api/v1/                   # REST API 控制器
├── api/config/               # 配置类（Swagger/CORS/Security 等）
├── domain/                   # JPA 实体类
├── model/dto/                # 请求/响应 DTO
├── repository/               # JPA Repository
└── service/                  # 业务逻辑服务
```

### 前端模块

```
frontend/
├── packages/components/       # 共享 Vue 组件
├── packages/stores/           # Pinia 状态管理
├── packages/hooks/            # 组合式函数
├── packages/types/             # TypeScript 类型定义
├── packages/utils/             # 工具函数
└── src/views/                 # 页面视图
```

## 添加新功能

### 后端新增 API

1. 在 `api/v1/` 下创建或找到对应的 Controller
2. 定义 DTO（`model/dto/` 目录）
3. 在 Service 层实现业务逻辑（`service/` 目录）
4. 更新 Swagger 注解（`@Operation`, `@Schema`）
5. 添加单元测试

### 前端新增页面

1. 在 `src/views/` 下创建 Vue 组件
2. 在 `src/router/` 添加路由
3. 如需 API 调用，在 `src/api/` 下添加对应的 API 函数
4. 如需全局状态，在 Pinia stores 中添加

### GIS 工具注册

新工具需在 `config/gis_tools.yaml` 中注册，并实现对应的 Service 方法。

## 报告问题

请通过 GitHub Issues 报告问题，包含以下信息：
- 问题描述
- 复现步骤
- 环境信息（操作系统、Docker 版本、浏览器版本等）
- 相关日志

## 许可证

贡献代码即表示您同意您的代码遵循本项目的 [MIT 许可证](./LICENSE)。
