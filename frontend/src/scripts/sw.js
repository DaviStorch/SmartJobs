const CACHE_NAME = "smartjobs-shell-v3";
const APP_SHELL = [
    "/offline.html",
    "/css/global.css",
    "/css/layout.css",
    "/css/components.css",
    "/css/pages.css",
    "/css/responsive.css",
    "/js/app.js",
    "/images/icon.svg",
    "/manifest.webmanifest"
];

self.addEventListener("install", (event) => {
    event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)));
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil(caches.keys().then((keys) => Promise.all(
        keys.filter((key) => key.startsWith("smartjobs-") && key !== CACHE_NAME).map((key) => caches.delete(key))
    )));
    self.clients.claim();
});

self.addEventListener("fetch", (event) => {
    const request = event.request;
    if (request.method !== "GET" || new URL(request.url).origin !== self.location.origin) return;
    if (request.mode === "navigate") {
        event.respondWith(fetch(request).catch(() => caches.match("/offline.html")));
        return;
    }
    const url = new URL(request.url);
    const isPublicAsset = url.pathname.startsWith("/css/") || url.pathname.startsWith("/js/")
        || url.pathname.startsWith("/images/") || url.pathname === "/manifest.webmanifest"
        || url.pathname === "/offline.html";
    if (!isPublicAsset) return;
    event.respondWith(fetch(request).then((response) => {
        if (response.ok && response.type === "basic") {
            const copy = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, copy));
        }
        return response;
    }).catch(() => caches.match(request)));
});
