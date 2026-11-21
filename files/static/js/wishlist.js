import {apiFetch, responseJson} from './api.js';

let entries = [];
let sequence = 0;
let renderList;
const known = id => entries.some(entry => entry.game.id === id);
const el = (tag, text) => Object.assign(document.createElement(tag), {textContent: text});
const button = (text, click) => {
    const element = el('button', text);
    element.type = 'button';
    element.className = 'secondary-action';
    element.onclick = async () => {
        element.disabled = true;
        try { await click(); } catch (error) { alert(error.message); }
        finally { element.disabled = false; }
    };
    return element;
};
async function call(path = '', method = 'GET', data) {
    return responseJson(await apiFetch('/api/wishlist' + path, {method, cache: 'no-store',
        headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)}), 'Wunschliste nicht erreichbar.');
}
const describe = o => [o.name, o.region, o.label].filter(Boolean).join(' · ');

export function wishButton(game, onSaved) {
    const action = button('', async () => {
        if (known(game.id)) return;
        const {options} = await call('/' + game.id + '/options');
        if (!action.isConnected || action.closest('.modal:not(.show)')) return;
        const dialog = document.createElement('dialog');
        dialog.className = 'wish-dialog';
        dialog.addEventListener('click', event => event.stopPropagation());
        const select = document.createElement('select');
        select.setAttribute('aria-label', 'Plattform und Region');
        options.forEach((o, i) => select.add(new Option(describe(o), i)));
        dialog.append(el('h3', game.name), select, button('Auf die Wunschliste', async () => {
            const {platform, region} = options[select.selectedIndex];
            await call('/' + game.id, 'PUT', {platform, region});
            dialog.close();
            onSaved?.();
            await refresh();
        }), button('Abbrechen', () => dialog.close()));
        dialog.onclose = () => dialog.remove();
        (action.closest('.modal') || document.body).append(dialog);
        dialog.showModal();
    });
    action.dataset.wish = game.id;
    action.textContent = known(game.id) ? '✓ Vorgemerkt' : 'Auf die Wunschliste';
    return action;
}

async function refresh() {
    const request = ++sequence;
    const {items} = await call();
    if (request !== sequence) return;
    entries = items;
    document.querySelectorAll('[data-wish]').forEach(b => {
        b.textContent = known(Number(b.dataset.wish)) ? '✓ Vorgemerkt' : 'Auf die Wunschliste';
    });
    renderList?.();
    const notices = document.getElementById('wishNotices');
    notices.replaceChildren();
    for (const {game, release, notify} of entries) {
        if (!notify) continue;
        const row = el('div', game.name + ' ist erschienen · ' + describe(release));
        row.append(button('Gelesen', async () => {
            await call('/' + game.id + '/seen', 'POST', {client: 'web'});
            row.remove();
        }));
        notices.append(row);
    }
}

export function initializeWishlist(openEditor) {
    const grid = document.getElementById('wishGrid');
    const status = document.getElementById('wishStatus');
    renderList = () => {
        grid.replaceChildren();
        status.textContent = entries.some(e => e.stale) ? 'IGDB ist derzeit nicht aktuell. Gespeicherte Termine werden angezeigt.'
            : entries.length ? '' : 'Deine Wunschliste ist leer.';
        for (const {game, release, released} of entries) {
            const card = el('article', '');
            card.className = 'discovery-card wish-card';
            const imageId = game.cover?.image_id;
            if (imageId && /^[a-zA-Z0-9_-]+$/.test(imageId)) {
                const image = document.createElement('img');
                image.src = 'https://images.igdb.com/igdb/image/upload/t_cover_small/' + imageId + '.jpg';
                image.alt = '';
                card.append(image);
            }
            const info = el('div', '');
            info.append(el('h2', game.name), el('p', describe(release)));
            if (released) info.append(el('p', '✓ Erschienen'));
            if (!document.querySelector('[data-igdb-id="' + game.id + '"]')) {
                info.append(button('Zur Sammlung hinzufügen', () => openEditor(game)));
            }
            info.append(button('Entfernen', async () => {
                await call('/' + game.id, 'DELETE');
                await refresh();
            }));
            card.append(info);
            grid.append(card);
        }
    };
    const update = () => refresh().catch(error => { status.textContent = error.message; });
    for (const b of document.querySelectorAll('[data-discovery-tab]')) {
        b.onclick = () => {
            const wish = b.dataset.discoveryTab === 'wish';
            document.getElementById('wishPanel').hidden = !wish;
            document.getElementById('popularPanel').hidden = wish;
            document.querySelectorAll('[data-discovery-tab]').forEach(t => t.setAttribute('aria-pressed', String(t === b)));
            if (wish) update();
        };
    }
    document.getElementById('wishRetry').onclick = update;
    window.addEventListener('pageshow', event => { if (event.persisted) update(); });
    document.addEventListener('visibilitychange', () => { if (!document.hidden) update(); });
    update();
}
