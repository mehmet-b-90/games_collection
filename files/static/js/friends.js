import {apiFetch, responseJson} from './api.js';

export function initializeFriends() {
    const list = document.getElementById('friendsList');
    const status = document.getElementById('friendsStatus');
    const library = document.getElementById('friendLibrary');
    const grid = document.getElementById('friendLibraryGrid');
    const search = document.getElementById('friendLibrarySearch');
    let busy = false;
    let games = [];
    let generation = 0;
    const node = (tag, text, cls) => {
        const el = document.createElement(tag);
        if (text != null) el.textContent = text;
        if (cls) el.className = cls;
        return el;
    };
    function closeLibrary() {
        games = [];
        library.hidden = true;
        list.hidden = false;
        document.getElementById('friendRequestForm').hidden = false;
        grid.replaceChildren();
        search.value = '';
    }
    function renderGames() {
        grid.replaceChildren();
        const filtered = games.filter(game => game.title.toLocaleLowerCase('de').includes(search.value.trim().toLocaleLowerCase('de')));
        document.getElementById('friendLibraryCount').textContent = `${filtered.length} von ${games.length} Spielen · Nur ansehen`;
        filtered.forEach(game => {
            const card = node('article', null, 'discovery-card');
            const cover = node('div', null, 'discovery-cover');
            cover.append(node('span', 'Kein Cover', 'discovery-placeholder'));
            const coverUrl = game.custom_cover_url || game.cover_url;
            if (coverUrl) {
                const img = node('img');
                img.src = coverUrl;
                img.alt = `Cover von ${game.title}`;
                img.loading = 'lazy';
                img.addEventListener('error', () => img.remove());
                cover.append(img);
            }
            card.append(cover, node('h3', game.title),
                node('p', `${game.release_date || 'Jahr unbekannt'} · ${game.age_rating || 'Keine Altersfreigabe'}`),
                node('p', game.owned_platform || 'Keine Besitzplattform'),
                node('p', `${game.format} · ${game.status} · ★ ${game.rating ?? '–'}/10`));
            const details = node('details');
            details.append(node('summary', 'Weitere Spieldaten'));
            [['Entwickler', game.developer], ['Publisher', game.publisher], ['Engine', game.game_engine],
                ['Verfügbare Plattformen', game.available_platforms]].forEach(([label, value]) => {
                if (value) details.append(node('p', `${label}: ${value}`));
            });
            card.append(details);
            grid.append(card);
        });
    }
    async function request(url, method = 'GET', body) {
        const response = await apiFetch(url, {method, cache: 'no-store',
            ...(body === undefined ? {} : {headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)})});
        if (response.redirected) throw new Error('Bitte melde dich erneut an.');
        return responseJson(response, 'Die Aktion konnte nicht ausgeführt werden. Bitte aktualisiere die Liste.');
    }
    function button(label, fn) {
        const el = node('button', label, 'secondary-action');
        el.type = 'button';
        el.disabled = busy;
        el.addEventListener('click', fn);
        return el;
    }
    async function loadLibrary(id) {
        closeLibrary();
        const data = await request(`/api/friends/${id}/games`);
        games = data.games;
        document.getElementById('friendLibraryTitle').textContent = `Sammlung von ${data.user.username}`;
        library.hidden = false;
        list.hidden = true;
        document.getElementById('friendRequestForm').hidden = true;
        renderGames();
    }
    async function load() {
        closeLibrary();
        const data = await request('/api/friends');
        list.replaceChildren();
        for (const [heading, match] of [
            ['Freunde', f => f.status === 'accepted'],
            ['Erhaltene Anfragen', f => f.status === 'pending' && f.incoming],
            ['Gesendete Anfragen', f => f.status === 'pending' && !f.incoming]]) {
            const section = node('section');
            section.append(node('h2', heading));
            const entries = data.friends.filter(match);
            if (!entries.length) section.append(node('p', 'Noch keine Einträge.'));
            entries.forEach(friend => {
                const row = node('div', null, 'friend-row');
                row.append(node('strong', friend.username));
                if (friend.status === 'accepted') row.append(button('Sammlung ansehen', () => run(() => loadLibrary(friend.id))));
                if (friend.status === 'pending' && friend.incoming) row.append(button('Annehmen', () => run(async () => {
                    await request(`/api/friends/${friend.id}`, 'PUT', {}); await load();
                }, 'Freundschaft bestätigt. Eure Sammlungen sind jetzt gegenseitig sichtbar.')));
                const label = friend.status === 'accepted' ? 'Freundschaft beenden' : friend.incoming ? 'Ablehnen' : 'Zurückziehen';
                row.append(button(label, () => {
                    if (friend.status === 'accepted' && !window.confirm(`Freundschaft mit ${friend.username} beenden? Die gegenseitige Freigabe endet damit.`)) return;
                    run(async () => { closeLibrary(); await request(`/api/friends/${friend.id}`, 'DELETE', {}); await load(); }, 'Freundschaft oder Anfrage entfernt.');
                }));
                section.append(row);
            });
            list.append(section);
        }
    }
    async function run(action, success = '') {
        if (busy) return;
        const current = generation;
        busy = true;
        status.textContent = 'Wird geladen …';
        document.querySelectorAll('#friends button').forEach(b => { b.disabled = true; });
        try {
            await action();
            if (current === generation) status.textContent = success;
        } catch (error) {
            if (current === generation) { closeLibrary(); status.textContent = error.message; }
        } finally {
            busy = false;
            document.querySelectorAll('#friends button').forEach(b => { b.disabled = false; });
            if (location.hash !== '#friends') closeLibrary();
            else if (current !== generation) run(load);
        }
    }
    document.getElementById('friendRequestForm').addEventListener('submit', event => {
        event.preventDefault();
        const input = document.getElementById('friendUsername');
        const username = input.value;
        run(async () => { await request('/api/friends', 'POST', {username}); input.value = ''; await load(); }, 'Anfrage gesendet. Die Sammlung wird erst nach Annahme sichtbar.');
    });
    document.getElementById('friendsRefresh').addEventListener('click', () => run(load));
    document.getElementById('friendLibraryClose').addEventListener('click', closeLibrary);
    document.getElementById('openFriends').addEventListener('click', () => {
        if (location.hash === '#friends') run(load);
        else location.hash = 'friends';
    });
    search.addEventListener('input', renderGames);
    function show() {
        generation++;
        closeLibrary();
        if (location.hash === '#friends') run(load);
    }
    window.addEventListener('hashchange', show);
    show();
}
