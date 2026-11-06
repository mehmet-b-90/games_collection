import {applyGameDefaults} from './game-defaults.js';
import {apiFetch, responseJson} from './api.js';

const set = (id, value = '') => { document.getElementById(id).value = value; };

let coverUrl = '';
let storedCoverUrl = '';
let customCover; // undefined keeps the stored cover, null restores the standard cover.
let coverSelection = 0;
let readingCover = false;
let saving = false;

function renderCover() {
    const url = customCover === undefined ? storedCoverUrl || coverUrl : customCover || coverUrl;
    const preview = document.getElementById('coverPreview');
    if (url) preview.src = url;
    else preview.removeAttribute('src');
    preview.classList.toggle('d-none', !url);
    document.getElementById('coverPlaceholder').classList.toggle('d-none', Boolean(url));
    document.getElementById('resetCoverBtn').classList.toggle('d-none',
        !(customCover || (customCover === undefined && storedCoverUrl)));
}

export function setCoverUrl(url) {
    coverUrl = url;
    renderCover();
}

function updateSaveButton() {
    document.getElementById('gameSaveBtn').disabled = readingCover || saving;
}

function resetCover(game = {}) {
    coverSelection += 1;
    readingCover = false;
    updateSaveButton();
    customCover = undefined;
    storedCoverUrl = game.custom_cover_url || '';
    setCoverUrl(game.cover_url || '');
}

async function prepareCover(file) {
    if (file.size > 20 * 1024 * 1024) throw new Error('Die Coverdatei darf höchstens 20 MiB groß sein.');
    const bitmap = await createImageBitmap(file);
    try {
        const canvas = document.createElement('canvas');
        const scale = Math.min(1, 800 / Math.max(bitmap.width, bitmap.height));
        canvas.width = Math.max(1, Math.round(bitmap.width * scale));
        canvas.height = Math.max(1, Math.round(bitmap.height * scale));
        const context = canvas.getContext('2d', {alpha: false});
        context.fillStyle = '#fff';
        context.fillRect(0, 0, canvas.width, canvas.height);
        context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
        const jpeg = canvas.toDataURL('image/jpeg', 0.85);
        if (jpeg.length > 512 * 1024 || !jpeg.startsWith('data:image/jpeg;base64,')) {
            throw new Error('Die Coverdatei konnte nicht ausreichend verkleinert werden.');
        }
        return jpeg;
    } finally {
        bitmap.close();
    }
}

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
    applyGameDefaults();
    resetCover();
    ['gameId', 'gameVersion', 'igdbIdInput', 'igdbUrlInput', 'releaseInput', 'devInput', 'pubInput', 'engineInput', 'platformsInput', 'ageRatingInput']
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
    set('ratingInput', game.rating ?? '');
    resetCover(game);
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
    if (readingCover || saving) return;
    const title = document.getElementById('titleInput').value.trim();
    if (!title) return alert('Bitte gib einen Spieletitel ein, bevor du speicherst.');
    const rating = document.getElementById('ratingInput').value.trim();
    if (rating && (!Number.isInteger(Number(rating)) || Number(rating) < 0 || Number(rating) > 10)) return alert('Bitte gib eine ganze Bewertung zwischen 0 und 10 ein.');
    const platforms = Array.from(document.getElementById('ownedPlatformInput').selectedOptions).map(option => option.value).join(', ');
    if (!platforms) return alert('Bitte wähle mindestens eine Plattform aus, auf der du das Spiel besitzt.');
    const id = document.getElementById('gameId').value;
    const data = {
        title, cover_url: coverUrl,
        format: document.getElementById('formatSelect').value, owned_platform: platforms,
        status: document.getElementById('statusSelect').value, rating,
        release_date: document.getElementById('releaseInput').value,
        developer: document.getElementById('devInput').value, publisher: document.getElementById('pubInput').value,
        game_engine: document.getElementById('engineInput').value, available_platforms: document.getElementById('platformsInput').value,
        igdb_id: document.getElementById('igdbIdInput').value, igdb_url: document.getElementById('igdbUrlInput').value,
        age_rating: document.getElementById('ageRatingInput').value
    };
    if (customCover !== undefined) data.custom_cover = customCover && customCover.split(',')[1];
    if (id) data.version = document.getElementById('gameVersion').value;
    saving = true;
    updateSaveButton();
    try {
        const response = await apiFetch(id ? `/api/games/${id}` : '/api/games', {
            method: id ? 'PUT' : 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)
        });
        await responseJson(response, 'Das Spiel konnte nicht gespeichert werden.');
        location.reload();
    } catch (error) {
        alert(error.message);
    } finally {
        saving = false;
        updateSaveButton();
    }
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
    const input = document.getElementById('coverFileInput');
    document.getElementById('chooseCoverBtn').addEventListener('click', () => {
        if (!saving) input.click();
    });
    document.getElementById('resetCoverBtn').addEventListener('click', () => {
        if (saving) return;
        coverSelection += 1;
        readingCover = false;
        customCover = storedCoverUrl ? null : undefined;
        updateSaveButton();
        renderCover();
    });
    input.addEventListener('change', async () => {
        const file = input.files[0];
        input.value = '';
        if (!file || saving) return;
        const selection = ++coverSelection;
        readingCover = true;
        updateSaveButton();
        try {
            const preview = await prepareCover(file);
            if (selection === coverSelection) {
                customCover = preview;
                renderCover();
            }
        } catch (error) {
            if (selection === coverSelection) {
                alert(error instanceof DOMException ? 'Dieses Bild konnte nicht gelesen werden. Wähle ein JPEG-, PNG- oder WebP-Bild.' : error.message);
            }
        } finally {
            if (selection === coverSelection) {
                readingCover = false;
                updateSaveButton();
            }
        }
    });
    document.getElementById('gameModal').addEventListener('hidden.bs.modal', () => resetCover());
    document.getElementById('titleInput').addEventListener('input', event => {
        updateDekuDealsLink(event.target.value);
    });
    document.getElementById('gameModal').addEventListener('shown.bs.modal', () => {
        if (!document.getElementById('gameId').value) document.getElementById('titleInput').focus();
    });
}
