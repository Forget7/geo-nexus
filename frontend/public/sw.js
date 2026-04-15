/**
 * GeoNexus Service Worker - 离线瓦片缓存 + 推送通知
 *
 * 策略：
 * - 瓦片请求：Cache First（优先缓存）
 * - API 请求：Network First（优先网络）
 */

const CACHE_NAME = 'geonexus-tiles-v1';
const OFFLINE_CACHE = 'geonexus-offline-v1';
const TILE_URL_PATTERN = /\/tiles\/|\/tile\/|\/maptile\//;
const API_PATTERN = /\/api\//;

const PRECACHE_URLS = [
  '/',
  '/index.html',
];

// ==================== 安装阶段 ====================
self.addEventListener('install', (event) => {
  console.log('[SW] Installing...');
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  );
});

// ==================== 激活阶段 ====================
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating...');
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(
        keys.filter(key => key !== CACHE_NAME && key !== OFFLINE_CACHE)
            .map(key => caches.delete(key))
      ))
      .then(() => self.clients.claim())
  );
});

// ==================== 请求拦截 ====================
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // 瓦片请求：Cache First
  if (TILE_URL_PATTERN.test(url.pathname)) {
    event.respondWith(cacheFirst(request, CACHE_NAME));
    return;
  }

  // API 请求：Network First，失败时返回缓存
  if (API_PATTERN.test(url.pathname)) {
    event.respondWith(networkFirst(request));
    return;
  }

  // 其他请求：Network First
  event.respondWith(networkFirst(request));
});

// ==================== 缓存策略 ====================

/**
 * Cache First：先查缓存，没有再请求网络
 */
async function cacheFirst(request, cacheName) {
  const cached = await caches.match(request);
  if (cached) {
    // 后台更新缓存（stale-while-revalidate）
    fetchAndCache(request, cacheName).catch(() => {});
    return cached;
  }

  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(cacheName);
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    // 返回离线瓦片占位图
    return createOfflinePlaceholder();
  }
}

/**
 * Network First：先请求网络，失败时返回缓存
 */
async function networkFirst(request) {
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    const cached = await caches.match(request);
    if (cached) {
      return cached;
    }
    // 离线页面
    if (request.mode === 'navigate') {
      return caches.match('/index.html');
    }
    throw error;
  }
}

async function fetchAndCache(request, cacheName) {
  const response = await fetch(request);
  if (response.ok) {
    const cache = await caches.open(cacheName);
    cache.put(request, response.clone());
  }
}

function createOfflinePlaceholder() {
  return new Response(
    `<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256">
      <rect fill="#e0e0e0" width="256" height="256"/>
      <text x="50%" y="50%" text-anchor="middle" fill="#999" font-size="14">
        离线瓦片
      </text>
    </svg>`,
    { headers: { 'Content-Type': 'image/svg+xml' } }
  );
}

// ==================== 离线包管理 ====================

/**
 * 缓存瓦片包
 */
async function cacheTilesPackage(url) {
  console.log('[SW] Caching tiles package from:', url);
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch package');

  const cache = await caches.open(OFFLINE_CACHE);
  // 瓦片包以流式处理（实际实现需要解析 .geonexus 包）
  await cache.add(url);
  console.log('[SW] Tiles package cached successfully');
}

/**
 * 清除离线缓存
 */
async function clearOfflineCache() {
  await caches.delete(OFFLINE_CACHE);
  console.log('[SW] Offline cache cleared');
}

// ==================== 消息处理 ====================
self.addEventListener('message', (event) => {
  const { type, url } = event.data;

  switch (type) {
    case 'CACHE_TILES':
      cacheTilesPackage(url);
      break;
    case 'CLEAR_OFFLINE':
      clearOfflineCache();
      break;
    case 'SKIP_WAITING':
      self.skipWaiting();
      break;
  }
});

// ==================== 定期同步 ====================
self.addEventListener('periodicsync', (event) => {
  if (event.tag === 'sync-tiles') {
    event.waitUntil(syncTiles());
  }
});

async function syncTiles() {
  // 后台同步瓦片
  console.log('[SW] Periodic tile sync');
}

// ==================== 推送通知 ====================
self.addEventListener('push', (event) => {
  if (!event.data) return;

  const data = event.data.json();
  const options = {
    body: data.body || '',
    icon: '/icon-192.png',
    badge: '/favicon.ico',
    vibrate: [100, 50, 100],
    data: {
      url: data.url || '/'
    },
    actions: [
      { action: 'open', title: '打开' },
      { action: 'dismiss', title: '忽略' }
    ]
  };

  event.waitUntil(
    self.registration.showNotification(data.title || 'GeoNexus', options)
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  if (event.action === 'dismiss') return;

  const url = event.notification.data?.url || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window' }).then((clientList) => {
      for (const client of clientList) {
        if (client.url === url && 'focus' in client) {
          return client.focus();
        }
      }
      return clients.openWindow(url);
    })
  );
});

// ==================== 背景同步 ====================
self.addEventListener('sync', (event) => {
  if (event.tag === 'retry-upload') {
    event.waitUntil(retryFailedUploads());
  }
});

async function retryFailedUploads() {
  const failed = await getFromOfflineStore('failed-uploads');
  for (const upload of failed) {
    try {
      await fetch(upload.url, upload.options);
      await removeFromOfflineStore(upload.id);
    } catch (e) {
      console.error('[SW] Retry failed:', e);
    }
  }
}

// ==================== 离线存储辅助 ====================
async function getFromOfflineStore(key) {
  // 简单实现，实际应使用 IndexedDB
  try {
    const store = await caches.open(OFFLINE_CACHE);
    const response = await store.match(`/offline-store/${key}`);
    if (response) {
      return response.json();
    }
  } catch (e) {
    console.error('[SW] Failed to get from offline store:', e);
  }
  return [];
}

async function removeFromOfflineStore(id) {
  // 简单实现，实际应使用 IndexedDB
  console.log('[SW] Removing from offline store:', id);
}
