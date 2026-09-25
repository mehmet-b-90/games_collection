import {apiFetch, responseJson} from './api.js';

const set = (id, value = '') => { document.getElementById(id).value = value; };

export function updateDekuDealsLink(title = '') {
    const link = document.getElementById('dekuDealsBtn');
    const normalizedTitle = title.trim();
    link.classList.toggle('d-none', !normalizedTitle);
    link.href = normalizedTitle
        ? `https://www.dekudeals.com/search?q=${encodeURIComponent(normalizedTitle)}`
        : '#';
}

export function resetForm() {
    document.getElementById('gameForm').reset();
    ['gameId', 'gameVersion', 'coverUrlInput', 'igdbIdInput', 'igdbUrlInput', 'releaseInput', 'devInput', 'pubInput', 'engineInput', 'platformsInput', 'ageRatingInput']
        .forEach(id => set(id));
    document.getElementById('deleteBtn').classList.add('d-none');
    document.getElementById('igdbLinkBtn').classList.add('d-none');
    updateDekuDealsLink();
    document.getElementById('modalTitle').innerText = 'Neues Spiel hinzufügen';
}

export function openGameDetails(game) {
    set('gameId', game.id); set('gameVersion', game.version); set('titleInput', game.title);
    set('releaseYearSearch', game.release_date || '');
    set('formatSelect', game.format || 'Digital'); set('statusSelect', game.status || 'Nicht gespielt');
    set('ratingInput', game.rating ?? ''); set('coverUrlInput', game.cover_url || '');
    set('releaseInput', game.release_date || ''); set('devInput', game.developer || '');
    set('pubInput', game.publisher || ''); set('engineInput', game.game_engine || '');
    set('platformsInput', game.available_platforms || ''); set('igdbIdInput', game.igdb_id || '');
    set('igdbUrlInput', game.igdb_url || ''); set('ageRatingInput', game.age_rating || '');
    const owned = (game.owned_platform || '').split(',').map(item => item.trim());
    Array.from(document.getElementById('ownedPlatformInput').options).forEach(option => { option.selected = owned.includes(option.value); });
    const link = document.getElementById('igdbLinkBtn');
    link.classList.toggle('d-none', !game.igdb_url);
    if (game.igdb_url) link.href = game.igdb_url;
    updateDekuDealsLink(game.title || '');
    document.getElementById('deleteBtn').classList.remove('d-none');
    document.getElementById('modalTitle').innerText = 'Spiel bearbeiten';
    new bootstrap.Modal(document.getElementById('gameModal')).show();
}

export async function saveGame() {
    const title = document.getElementById('titleInput').value.trim();
    if (!title) return alert('Bitte gib einen Spieletitel ein, bevor du speicherst.');
    const rating = document.getElementById('ratingInput').value.trim();
    if (rating && (!Number.isInteger(Number(rating)) || Number(rating) < 0 || Number(rating) > 10)) return alert('Bitte gib eine ganze Bewertung zwischen 0 und 10 ein.');
    const platforms = Array.from(document.getElementById('ownedPlatformInput').selectedOptions).map(option => option.value).join(', ');
    if (!platforms) return alert('Bitte wähle mindestens eine Plattform aus, auf der du das Spiel besitzt.');
    const id = document.getElementById('gameId').value;
    const data = {
        title, cover_url: document.getElementById('coverUrlInput').value,
        format: document.getElementById('formatSelect').value, owned_platform: platforms,
        status: document.getElementById('statusSelect').value, rating,
        release_date: document.getElementById('releaseInput').value,
        developer: document.getElementById('devInput').value, publisher: document.getElementById('pubInput').value,
        game_engine: document.getElementById('engineInput').value, available_platforms: document.getElementById('platformsInput').value,
        igdb_id: document.getElementById('igdbIdInput').value, igdb_url: document.getElementById('igdbUrlInput').value,
        version: document.getElementById('gameVersion').value, age_rating: document.getElementById('ageRatingInput').value
    };
    const button = document.getElementById('gameSaveBtn');
    button.disabled = true;
    try {
        const response = await apiFetch(id ? `/api/games/${id}` : '/api/games', {
            method: id ? 'PUT' : 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)
        });
        await responseJson(response, 'Das Spiel konnte nicht gespeichert werden.');
        location.reload();
    } catch (error) { alert(error.message); } finally { button.disabled = false; }
}

export async function deleteGame() {
    if (!confirm('Wirklich löschen?')) return;
    try {
        const response = await apiFetch(`/api/games/${document.getElementById('gameId').value}`, {method: 'DELETE'});
        await responseJson(response, 'Das Spiel konnte nicht gelöscht werden.');
        location.reload();
    } catch (error) { alert(error.message); }
}

export function initializeGameModal() {
    document.getElementById('titleInput').addEventListener('input', event => {
        updateDekuDealsLink(event.target.value);
    });
    document.getElementById('gameModal').addEventListener('shown.bs.modal', () => {
        if (!document.getElementById('gameId').value) document.getElementById('titleInput').focus();
    });
}
