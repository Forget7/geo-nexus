import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { visualizer } from 'rollup-plugin-visualizer'
import { compression } from 'vite-plugin-compression'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  // ========== 基础配置 ==========
  root: '.',
  base: '/',
  publicDir: 'public',
  
  // ========== 构建配置 ==========
  build: {
    target: 'es2015',
    cssTarget: 'chrome80',
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: process.env.NODE_ENV === 'development' ? 'cheap-source-map' : false,
    rollupOptions: {
      output: {
        // 手动分包
        manualChunks: {
          // Vue核心
          'vue-core': ['vue', 'vue-router', 'pinia'],
          // Leaflet
          'leaflet': ['leaflet'],
          // 地图相关
          'cesium': ['cesium'],
          // UI框架
          'element-plus': ['element-plus'],
          // 工具库
          'utils': ['axios', 'lodash-es', 'dayjs'],
        },
        // 文件名哈希
        entryFileNames: 'assets/js/[name]-[hash].js',
        chunkFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
        // 分割vendor
        vendor: ['vue', 'vue-router', 'pinia'],
      },
      // 外部依赖
      external: [],
    },
    // 压缩配置
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: process.env.NODE_ENV === 'production',
        drop_debugger: true,
        pure_funcs: ['console.log', 'console.info'],
      },
      format: {
        comments: false,
      },
    },
    // 资源内联阈值
    assetsInlineLimit: 4096,
    // 分块警告阈值
    chunkSizeWarningLimit: 600,
    // 启用CSS代码分割
    cssCodeSplit: true,
    // 关闭SSR
    ssr: false,
    // 生产SourceMap
    sourcemap: false,
  },
  
  // ========== 插件配置 ==========
  plugins: [
    vue({
      template: {
        compilerOptions: {
          // 自定义元素
          isCustomElement: (tag) => tag.startsWith('ion-'),
        },
      },
    }),
    
    // 自动导入
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
      eslintrc: {
        enabled: true,
      },
      resolvers: [
        ElementPlusResolver(),
      ],
    }),
    
    // 自动组件导入
    Components({
      dirs: ['src/components'],
      dts: 'src/components.d.ts',
      resolvers: [
        ElementPlusResolver(),
      ],
      globs: ['src/components/**/*.{vue,tsx}'],
    }),
    
    // Gzip压缩
    compression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240, // 10KB以上才压缩
    }),
    
    // Brotli压缩
    compression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 10240,
    }),
    
    // 打包分析
    visualizer({
      filename: 'dist/stats.html',
      open: false,
      gzipSize: true,
      brotliSize: true,
    }),
  ],
  
  // ========== 依赖优化 ==========
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'axios',
      'leaflet',
      'dayjs',
      'lodash-es',
    ],
    exclude: [
      // 不需要预构建的依赖
    ],
    // 强制预构建
    force: process.env.NODE_ENV === 'production',
  },
  
  // ========== 开发服务器 ==========
  server: {
    port: 3000,
    host: '0.0.0.0',
    // 代理配置
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8000',
        changeOrigin: true,
        secure: false,
      },
      '/ws': {
        target: process.env.VITE_WS_URL || 'ws://localhost:8000',
        ws: true,
      },
      '/v3/api-docs': {
        target: process.env.VITE_API_URL || 'http://localhost:8000',
        rewrite: (path) => path.replace(/^\/v3/, '/v3'),
      },
    },
    // CORS
    cors: true,
    // 监听端口
    strictPort: false,
    // HTTPS
    https: false,
    // 热更新
    hmr: {
      overlay: true,
    },
    // 监听文件
    watch: {
      usePolling: false,
      ignored: ['**/node_modules/**', 'dist/**'],
    },
  },
  
  // ========== 解析配置 ==========
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '~': resolve(__dirname, 'src'),
      '@components': resolve(__dirname, 'src/components'),
      '@views': resolve(__dirname, 'src/views'),
      '@stores': resolve(__dirname, 'src/stores'),
      '@utils': resolve(__dirname, 'src/utils'),
      '@api': resolve(__dirname, 'src/api'),
      '@types': resolve(__dirname, 'src/types'),
      '@composables': resolve(__dirname, 'src/composables'),
      '@assets': resolve(__dirname, 'src/assets'),
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
    // 条件导出
    mainFields: ['module', 'jsnext:main', 'jsnext'],
  },
  
  // ========== CSS配置 ==========
  css: {
    devSourcemap: true,
    preprocessorOptions: {
      scss: {
        additionalData: `@import "@/assets/styles/variables.scss";`,
        api: 'modern-compiler',
      },
      less: {
        javascriptEnabled: true,
      },
    },
  },
  
  // ========== 预览服务器 ==========
  preview: {
    port: 4173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
  
  // ========== Eslint ==========
  eslint: {
    cache: true,
    cacheLocation: 'node_modules/.cache/eslint',
    fix: true,
  },
  
  // ========== TypeScript ==========
  typescript: {
    tsconfigPath: './tsconfig.json',
    reportStyleErrors: true,
  },
})

// 环境变量类型声明
interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_WS_URL: string
  readonly VITE_CESIUM_ION_TOKEN: string
  readonly VITE_MAP_TILE_SERVER: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
