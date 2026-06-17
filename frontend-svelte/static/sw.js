// Service Worker для 999 CRM PWA
const CACHE = "pwabuilder-offline-v1";

importScripts('https://storage.googleapis.com/workbox-cdn/releases/5.1.2/workbox-sw.js');

const offlineFallbackPage = "offline.html";

self.addEventListener("message", (event) => {
  if (event.data && event.data.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});

self.addEventListener('install', async (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.add(offlineFallbackPage))
  );
});

if (workbox.navigationPreload.isSupported()) {
  workbox.navigationPreload.enable();
}

// Кэшируем только статические файлы
workbox.routing.registerRoute(
  new RegExp('/\\.(css|js|png|jpg|jpeg|gif|svg|woff|woff2)$'),
  new workbox.strategies.CacheFirst({
    cacheName: CACHE
  })
);

// API запросы всегда из сети
workbox.routing.registerRoute(
  new RegExp('/api/'),
  new workbox.strategies.NetworkOnly()
);

// Остальные запросы - NetworkFirst с fallback на offline.html
workbox.routing.registerRoute(
  new RegExp('/*'),
  new workbox.strategies.NetworkFirst({
    cacheName: CACHE,
    plugins: [
      new workbox.expiration.ExpirationPlugin({
        maxEntries: 50,
        maxAgeSeconds: 24 * 60 * 60 // 24 часа
      }),
      {
        handlerDidError: async ({ request }) => {
          if (request.mode === 'navigate') {
            return caches.match(offlineFallbackPage);
          }
          return Response.error();
        }
      }
    ]
  })
);
