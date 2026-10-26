const CACHE_PREFIX = 'games-collection-';
const CACHE_NAME = `${CACHE_PREFIX}v3`;
const STATIC_ASSETS = [
    '/static/offline.html',
    '/static/style.css',
    '/static/js/main.js',
    '/static/js/discover.js',
    '/static/js/api.js',
    '/static/js/account.js',
    '/static/js/admin.js',
    '/static/js/charts.js',
    '/static/js/games.js',
    '/static/js/igdb-search.js',
    '/static/js/pwa.js',
    '/static/js/ui.js',
    '/static/manifest.webmanifest',
    '/static/icons/app-icon-32.png',
    '/static/icons/apple-touch-icon-180.png',
    '/static/icons/app-icon-192.png',
    '/static/icons/app-icon-512.png',
    '/static/icons/app-icon-maskable-512.png'
];

self.addEventListener('install', event => {
    event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(STATIC_ASSETS)));
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(names => Promise.all(
            names.filter(name => name.startsWith(CACHE_PREFIX) && name !== CACHE_NAME)
                .map(name => caches.delete(name))
        )).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', event => {
    const request = event.request;
    if (request.method !== 'GET') return;

    const url = new URL(request.url);
    if (url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return;

    if (request.mode === 'navigate') {
        event.respondWith(fetch(request).catch(() => caches.match('/static/offline.html')));
        return;
    }

    if (url.pathname.startsWith('/static/')) {
        event.respondWith(
            fetch(request).then(response => {
                if (response.ok) {
                    const copy = response.clone();
                    caches.open(CACHE_NAME).then(cache => cache.put(request, copy));
                }
                return response;
            }).catch(() => caches.match(request))
        );
    }
});
