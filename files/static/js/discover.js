import {apiFetch, responseJson} from './api.js';
import {resetForm} from './games.js';
import {selectGame} from './igdb-search.js';

export function initializeDiscovery() {
    const panel = document.getElementById('discover');
    const grid = document.getElementById('discoverGrid');
    const status = document.getElementById('discoverStatus');
    const retry = document.getElementById('discoverRetry');
    const owned = new Set(Array.from(document.querySelectorAll('[data-igdb-id]'), el => Number(el.dataset.igdbId)));
    let loadedAt = 0;
    let loading = false;
    let collectionScroll = 0;
    let discovering = false;
    const node = (tag, text, className) => {
        const el = document.createElement(tag);
        if (text != null) el.textContent = text;
        if (className) el.className = className;
        return el;
    };
    function render(games) {
        grid.replaceChildren();
        games.forEach(game => {
            const card = node('article', null, 'discovery-card');
            const cover = node('div', null, 'discovery-cover');
            const placeholder = node('span', 'Kein Cover', 'discovery-placeholder');
            cover.append(placeholder);
            const imageId = game.cover?.image_id;
            if (typeof imageId === 'string' && /^[a-zA-Z0-9_-]+$/.test(imageId)) {
                const image = node('img');
                image.src = `https://images.igdb.com/igdb/image/upload/t_cover_big/${imageId}.jpg`;
                image.alt = `Cover von ${game.name}`;
                image.loading = 'lazy';
                image.addEventListener('load', () => { placeholder.hidden = true; });
                image.addEventListener('error', () => { image.remove(); placeholder.hidden = false; });
                cover.append(image);
            }
            cover.append(node('span', `#${game.popularity_rank}`, 'discovery-rank'));
            card.append(cover, node('h2', game.name));
            card.append(node('p', (game.genres || []).map(g => g.name).filter(Boolean).join(' · ') || 'Genre nicht angegeben', 'discovery-genre'));
            card.append(node('p', typeof game.rating === 'number'
                ? `IGDB ★ ${(game.rating / 10).toLocaleString('de-DE', {maximumFractionDigits: 1})} / 10`
                : 'Noch keine IGDB-Bewertung', 'discovery-rating'));
            if (owned.has(game.id)) card.append(node('span', '✓ In Sammlung', 'discovery-owned'));
            else {
                const button = node('button', '+ Hinzufügen', 'secondary-action');
                button.type = 'button';
                button.setAttribute('aria-label', `${game.name} zur Sammlung hinzufügen`);
                button.addEventListener('click', () => {
                    resetForm();
                    selectGame(game);
                    bootstrap.Modal.getOrCreateInstance(document.getElementById('gameModal')).show();
                });
                card.append(button);
            }
            grid.append(card);
        });
    }
    async function load() {
        if (loading) return;
        loading = true;
        retry.disabled = true;
        grid.setAttribute('aria-busy', 'true');
        status.textContent = 'Aktuell beliebte Spiele werden geladen …';
        try {
            const response = await apiFetch('/api/discover', {cache: 'no-store'});
            if (response.redirected) throw new Error('Bitte melde dich erneut an.');
            const data = await responseJson(response, 'Aktuell beliebte Spiele konnten nicht geladen werden.');
            if (!Array.isArray(data.games)) throw new Error('Die Serverantwort konnte nicht verarbeitet werden.');
            render(data.games);
            loadedAt = Date.now();
            const date = new Date(data.updated_at);
            const stamp = Number.isNaN(date.getTime()) ? '' : ` Stand: ${date.toLocaleString('de-DE')}.`;
            status.textContent = data.games.length ? `${data.stale ? 'Aktualisierung derzeit nicht möglich. Gespeicherte Rangliste.' : `${data.games.length} beliebte Titel.`}${stamp}` : 'Zurzeit sind keine beliebten Titel verfügbar.';
        } catch (error) {
            status.textContent = `${error.message}${grid.children.length ? ' Die zuletzt geladene Liste bleibt sichtbar.' : ''}`;
        } finally {
            loading = false;
            retry.disabled = false;
            grid.setAttribute('aria-busy', 'false');
        }
    }
    function showView() {
        const next = location.hash === '#discover';
        if (next && !discovering) collectionScroll = window.scrollY;
        const changed = next !== discovering;
        discovering = next;
        panel.hidden = !next;
        document.getElementById('collectionTop').hidden = next;
        document.body.classList.toggle('discovering', next);
        document.querySelectorAll('[data-view]').forEach(link => {
            const active = link.dataset.view === (next ? 'discover' : 'collection');
            link.classList.toggle('active', active);
            if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
        });
        if (changed) window.scrollTo({top: next ? 0 : collectionScroll, behavior: 'instant'});
        if (next && Date.now() - loadedAt > 86400000) load();
    }
    retry.addEventListener('click', load);
    document.querySelector('[data-collection-filter]').addEventListener('click', () => {
        if (discovering) { location.hash = 'collectionTop'; showView(); }
    });
    window.addEventListener('hashchange', showView);
    showView();
}
