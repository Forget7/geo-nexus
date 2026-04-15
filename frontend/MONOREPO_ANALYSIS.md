# GeoNexus Monorepo 结构分析

## 目录结构

```
frontend/
├── src/                        # 直接使用的源码
│   ├── api/index.ts            # Axios 封装
│   ├── components/             # 业务组件
│   ├── locales/
│   ├── router/
│   ├── service/
│   ├── types/index.ts          # 完整类型定义（~480行）
│   ├── utils/
│   └── views/
└── packages/                   # 可发布包（但未实际 publish）
    ├── types/src/              # 重复类型定义
    │   ├── index.ts            # 仅3行：export * from './geo'; export * from './api'; export * from './store'
    │   ├── api.ts              # 部分 API 类型
    │   ├── geo.ts              # 地理类型
    │   └── store.ts            # 状态类型
    ├── stores/src/             # Pinia store
    ├── hooks/src/              # Vue hooks
    ├── components/src/         # 共享组件
    └── utils/src/              # 工具函数
```

## 问题：类型重复

| 文件 | 行数 | 内容 |
|------|------|------|
| `src/types/index.ts` | ~480 | 完整类型定义（几何、GeoJSON、API、地图、用户等） |
| `packages/types/src/index.ts` | 3 | 仅做 re-export |
| `packages/types/src/api.ts` | ~20 | 部分 API 类型（`ApiResponse`、`PageQuery` 等，与 src/types 部分重复） |
| `packages/types/src/geo.ts` | ? | 与 src/types 几何类型重复 |
| `packages/types/src/store.ts` | ~30 | 状态类型（`User`、`AppState`、`MapState`） |

**`src/types/index.ts` 是完整类型定义，是实际被使用的源头。`packages/types/` 是残缺的冗余副本。**

## 其他 packages 内容

| 包 | 状态 |
|----|------|
| `packages/stores/` | Pinia stores（`app.ts`、`chat.ts`、`map.ts`、`ui.ts`），`src/` 下无 stores → 无重复 |
| `packages/hooks/` | Vue composables，与 `src/` 下无重复 |
| `packages/components/` | 共享组件，`src/components/` 是业务组装层 → 无重复 |
| `packages/utils/` | 工具函数，与 `src/utils/` 部分功能重叠但用途不同 |
| `packages/apps/web/` | Web 应用入口，`src/main.ts` 是另一入口 → 两个入口并存 |

## 建议处理方式

1. **删除 `packages/types/`**（冗余/残缺）—— `src/types/index.ts` 已完整覆盖
2. **合并两个入口**（`src/main.ts` vs `packages/apps/web/src/main.ts`）—— 建议保留 `packages/apps/web/` 作为主入口，删除 `src/main.ts`
3. **统一包管理**：`packages/` 下的包有 `package.json` 但未实际 publish，建议在 `package.json` 中通过 workspace 引用，或将 `src/` 下的内容迁移到对应 `packages/` 子包
4. **`packages/utils/` vs `src/utils/`**：功能有重叠，但职责不同（packages 是底层工具，src 是业务工具），保持现状即可

## 结论

- **立即可做**：删除 `packages/types/`（冗余）
- **需讨论**：是否统一为单一入口（`packages/apps/web/`）
- **保持现状**：`stores`、`hooks`、`components`、`utils` 功能不重复，无需合并
