// Issue #450: the service worker of the installed editor (a PWA). It caches one page only: the one that says to start
// Minecraft, shown when the editor is opened from its icon while the game (and so the editor's server) is not running.
// Everything else goes to the network as before; nothing of the editor is cached, so an update of the mod is seen at once.
const CACHE = "halisoncode-offline-1";
const OFFLINE = "/halisoncode-offline.html";

self.addEventListener("install", event => {
    event.waitUntil(caches.open(CACHE).then(cache => cache.add(OFFLINE)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", event => {
    event.waitUntil(caches.keys()
        .then(keys => Promise.all(keys.filter(key => key !== CACHE).map(key => caches.delete(key))))
        .then(() => self.clients.claim()));
});

self.addEventListener("fetch", event => {
    if (event.request.mode !== "navigate") return;
    event.respondWith(fetch(event.request).catch(() => caches.match(OFFLINE)));
});
