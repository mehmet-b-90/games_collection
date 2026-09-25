import {apiFetch, responseJson} from './api.js';
import {updateDekuDealsLink} from './games.js';

let timeout;
let controller;

function selectGame(game, results) {
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
    const results = document.getElementById('searchResults');
    const scheduleSearch = () => {
        clearTimeout(timeout);
        const query = input.value.trim();
        const releaseYear = yearInput.value.trim();
        if (!query || (!/^\d+$/.test(query) && query.length < 3)) {
            results.style.display = 'none';
            return;
        }
        if (releaseYear && (!/^\d{4}$/.test(releaseYear) || Number(releaseYear) < 1900 || Number(releaseYear) > 2100)) {
            results.style.display = 'none';
            return;
        }
        timeout = setTimeout(async () => {
            controller?.abort();
            controller = new AbortController();
            try {
                const response = await apiFetch('/api/search', {
                    method: 'POST', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({query, release_year: releaseYear || null}), signal: controller.signal
                });
                renderResults(await responseJson(response, 'Die IGDB-Suche ist fehlgeschlagen.'), results);
            } catch (error) {
                if (error.name !== 'AbortError') alert(error.message);
            }
        }, 500);
    };
    input.addEventListener('input', scheduleSearch);
    yearInput.addEventListener('input', scheduleSearch);
    document.addEventListener('click', event => {
        if (!event.target.closest('.editor-fields')) results.style.display = 'none';
    });
}
