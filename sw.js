/**
 * PARDOFINANZAS PRO — SERVICE WORKER (PWA)
 * Carga instantánea offline y soporte para app móvil instalable
 * Versión 28 con invalidación inmediata de caché antigua
 */

const CACHE_NAME = "pardofinanzas-pwa-v33";
const STATIC_ASSETS = [
  "./",
  "./index.html",
  "./css/styles.css",
  "./js/api.js?v=26.9.8",
  "./js/nlp_parser.js?v=26.9.8",
  "./js/app.js?v=26.9.8",
  "./manifest.json",
  "./assets/icon-192.png",
  "./assets/icon-512.png"
];

// Instalación y pre-cache de archivos estáticos
self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    })
  );
  self.skipWaiting();
});

// Activación y limpieza de caches antiguos
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            console.log("🧹 PWA: Eliminando caché antigua:", key);
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// Estrategia Network-First con fallback a Cache
self.addEventListener("fetch", (event) => {
  // Ignorar llamadas de Apps Script API o externas
  if (event.request.url.includes("script.google.com") || event.request.url.includes("chrome-extension")) {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200) {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return networkResponse;
      })
      .catch(() => {
        return caches.match(event.request).then((cachedResponse) => {
          if (cachedResponse) {
            return cachedResponse;
          }
          if (event.request.mode === "navigate") {
            return caches.match("./index.html");
          }
        });
      })
  );
});
