import {apiFetch, responseJson} from './api.js';
import {updateDekuDealsLink} from './games.js';

let timeout;
let controller;
let requestSequence = 0;

export function selectGame(game, results = document.getElementById('searchResults')) {
    clearTimeout(timeout);
    controller?.abort();
    controller = undefined;
    requestSequence += 1;
    const set = (id, value = '') => { document.getElementById(id).value = value; };
    const releaseYear = game.first_release_date ? new Date(game.first_release_date * 1000).getFullYear() : '';
    set('titleInput', game.name);
    set('coverUrlInput', game.cover?.image_id ? `https://images.igdb.com/igdb/image/upload/t_cover_big/${game.cover.image_id}.jpg` : '');
    set('releaseYearSearch', releaseYear);
    set('releaseInput', releaseYear);
    set('igdbIdInput', game.id || '');
    set('igdbUrlInput', game.url || '');
    set('engineInput', (game.game_engines || []).map(engine => engine.name).join(', '));
    set('platformsInput', (game.platforms || []).map(platform => platform.name).join(', '));
    const companies = game.involved_companies || [];
    set('devInput', companies.filter(item => item.developer && item.company).map(item => item.company.name).join(', '));
    set('pubInput', companies.filter(item => item.publisher && item.company).map(item => item.company.name).join(', '));
    set('ageRatingInput', game.mapped_age_rating || '');
    updateDekuDealsLink(game.name || '');
    results.style.display = 'none';
}

function renderResults(games, results) {
    results.replaceChildren();
    games.forEach(game => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'list-group-item list-group-item-action d-flex align-items-center gap-3 text-bg-dark border-secondary';
        const image = document.createElement('img');
        image.src = game.cover?.image_id ? `https://images.igdb.com/igdb/image/upload/t_cover_small/${game.cover.image_id}.jpg` : 'https://via.placeholder.com/40x56?text=?';
        image.alt = 'Cover';
        image.style.cssText = 'width:40px;height:56px;object-fit:cover;border-radius:4px';
        const title = document.createElement('span');
        title.className = 'fw-bold text-white';
        const year = game.first_release_date ? ` (${new Date(game.first_release_date * 1000).getFullYear()})` : '';
        title.textContent = `${game.name || ''}${year}`;
        button.append(image, title);
        button.addEventListener('click', () => selectGame(game, results));
        results.appendChild(button);
    });
    results.style.display = games.length ? 'block' : 'none';
}

export function initializeIgdbSearch() {
    const input = document.getElementById('titleInput');
    const yearInput = document.getElementById('releaseYearSearch');
    const typeInput = document.getElementById('gameTypeSearch');
    const results = document.getElementById('searchResults');
    const scheduleSearch = () => {
        clearTimeout(timeout);
        controller?.abort();
        controller = undefined;
        const sequence = ++requestSequence;
        const query = input.value.trim();
        const releaseYear = yearInput.value.trim();
        const gameType = typeInput.value === '' ? null : Number(typeInput.value);
        if (!query || (!/^\d+$/.test(query) && query.length < 3)) {
            results.style.display = 'none';
            return;
        }
        if (releaseYear && (!/^\d{4}$/.test(releaseYear) || Number(releaseYear) < 1900 || Number(releaseYear) > 2100)) {
            results.style.display = 'none';
            return;
        }
        timeout = setTimeout(async () => {
            const activeController = new AbortController();
            controller = activeController;
            try {
                const response = await apiFetch('/api/search', {
                    method: 'POST', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({query, release_year: releaseYear || null, game_type: gameType}), signal: activeController.signal
                });
                const games = await responseJson(response, 'Die IGDB-Suche ist fehlgeschlagen.');
                if (sequence === requestSequence && !activeController.signal.aborted) renderResults(games, results);
            } catch (error) {
                if (error.name !== 'AbortError' && sequence === requestSequence) alert(error.message);
            } finally {
                if (controller === activeController) controller = undefined;
            }
        }, 500);
    };
    input.addEventListener('input', scheduleSearch);
    yearInput.addEventListener('input', scheduleSearch);
    typeInput.addEventListener('change', scheduleSearch);
    document.addEventListener('click', event => {
        if (!event.target.closest('.editor-fields')) results.style.display = 'none';
    });
}
