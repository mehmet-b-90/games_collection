import {gameCopies, setCopies, getCopies, hasCopyDraft, initializeCopies} from './copies.js';
import {applyGameDefaults} from './game-defaults.js';
import {apiFetch, responseJson} from './api.js';

const set = (id, value = '') => { document.getElementById(id).value = value; };

let coverUrl = '';
let storedCoverUrl = '';
let customCover; // undefined keeps the stored cover, null restores the standard cover.
let coverSelection = 0;
let readingCover = false;
let saving = false;
let playtimeRequest;
let storedPlaytime = null;

const igdbDurations = [
    ['hastily', 'Hauptgeschichte'], ['normally', 'Mit Nebeninhalten'], ['completely', '100 % Abschluss']
];

function formatIgdbPlaytime(seconds) {
    if (seconds < 60) return '< 1 min';
    const minutes = Math.round(seconds / 60);
    const hours = Math.floor(minutes / 60);
    return [hours && `${hours} h`, minutes % 60 && `${minutes % 60} min`].filter(Boolean).join(' ');
}

export async function refreshIgdbPlaytime(id, savedDurations) {
    playtimeRequest?.abort();
    const panel = document.getElementById('igdbPlaytime');
    const values = document.getElementById('igdbPlaytimeValues');
    const message = document.getElementById('igdbPlaytimeMessage');
    panel.hidden = values.hidden = message.hidden = true;
    values.replaceChildren();
    if (!Number.isSafeInteger(id) || id <= 0) return;

    function showMessage(text) {
        message.textContent = text;
        panel.hidden = message.hidden = false;
    }

    let durations = savedDurations;
    if (durations == null) {
        const controller = new AbortController();
        playtimeRequest = controller;
        showMessage('IGDB-Spielzeiten werden geladen …');
        try {
            const response = await apiFetch(`/api/igdb/playtime/${id}`, {signal: controller.signal});
            durations = await responseJson(response, 'IGDB-Spielzeiten sind derzeit nicht verfügbar.');
        } catch {
            if (!controller.signal.aborted) showMessage('IGDB-Spielzeiten sind derzeit nicht verfügbar.');
            return;
        } finally {
            if (playtimeRequest === controller) playtimeRequest = undefined;
        }
        if (controller.signal.aborted) return;
    }

    for (const [field, label] of igdbDurations) {
        const seconds = durations[field];
        if (!Number.isSafeInteger(seconds) || seconds <= 0) continue;
        const entry = document.createElement('div');
        const term = document.createElement('dt');
        const value = document.createElement('dd');
        term.textContent = label;
        value.textContent = formatIgdbPlaytime(seconds);
        entry.append(term, value);
        values.append(entry);
    }
    if (values.childElementCount) {
        panel.hidden = values.hidden = false;
        message.hidden = true;
    } else {
        showMessage('Für diesen Titel sind bei IGDB keine Spielzeiten hinterlegt.');
    }
}

function updatePlaytimeVisibility() {
    const field = document.getElementById('personalPlaytime');
    field.hidden = field.disabled = document.getElementById('statusSelect').value !== 'Gespielt';
}

function resetPlaytime(minutes = null) {
    storedPlaytime = minutes;
    set('playtimeHours', minutes === null ? '' : Math.floor(minutes / 60));
    set('playtimeMinutes', minutes === null ? '' : minutes % 60);
    updatePlaytimeVisibility();
}

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

function clearWishAction() {
    const slot = document.getElementById('gameWishAction');
    slot.hidden = true;
    slot.replaceChildren();
}

export function resetForm() {
    clearWishAction();
    document.getElementById('gameSaveBtn').textContent = 'Zur Sammlung hinzufügen';
    document.getElementById('gameForm').reset();
    applyGameDefaults();
    resetPlaytime();
    refreshIgdbPlaytime();
    resetCover();
    ['gameId', 'gameVersion', 'igdbIdInput', 'igdbUrlInput', 'releaseInput', 'devInput', 'pubInput', 'engineInput', 'platformsInput', 'ageRatingInput']
        .forEach(id => set(id));
    document.getElementById('deleteBtn').classList.add('d-none');
    document.getElementById('igdbLinkBtn').classList.add('d-none');
    updateDekuDealsLink();
    document.getElementById('modalTitle').innerText = 'Neues Spiel hinzufügen';
}

export function openGameDetails(game) {
    clearWishAction();
    document.getElementById('gameSaveBtn').textContent = 'Speichern';
    set('gameId', game.id); set('gameVersion', game.version); set('titleInput', game.title);
    set('releaseYearSearch', game.release_date || '');
    set('statusSelect', game.status || 'Nicht gespielt');
    set('ratingInput', game.rating ?? '');
    resetPlaytime(game.playtime_minutes ?? null);
    refreshIgdbPlaytime(game.igdb_id, game.igdb_playtime);
    resetCover(game);
    set('releaseInput', game.release_date || ''); set('devInput', game.developer || '');
    set('pubInput', game.publisher || ''); set('engineInput', game.game_engine || '');
    set('platformsInput', game.available_platforms || ''); set('igdbIdInput', game.igdb_id || '');
    set('igdbUrlInput', game.igdb_url || ''); set('ageRatingInput', game.age_rating || '');
    setCopies(gameCopies(game));
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
    if (hasCopyDraft()) return alert('Bitte übernimm das bearbeitete Exemplar oder brich die Exemplarbearbeitung ab.');
    const id = document.getElementById('gameId').value;
    const data = {
        title, cover_url: coverUrl,
        copies: getCopies(),
        status: document.getElementById('statusSelect').value, rating,
        release_date: document.getElementById('releaseInput').value,
        developer: document.getElementById('devInput').value, publisher: document.getElementById('pubInput').value,
        game_engine: document.getElementById('engineInput').value, available_platforms: document.getElementById('platformsInput').value,
        igdb_id: document.getElementById('igdbIdInput').value, igdb_url: document.getElementById('igdbUrlInput').value,
        age_rating: document.getElementById('ageRatingInput').value
    };
    if (data.status === 'Gespielt') {
        const hours = document.getElementById('playtimeHours');
        const minutes = document.getElementById('playtimeMinutes');
        if (!hours.reportValidity() || !minutes.reportValidity()) return;
        const total = hours.value || minutes.value ? Number(hours.value) * 60 + Number(minutes.value) : null;
        if (total !== storedPlaytime) data.playtime_minutes = total;
    }
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
    initializeCopies();
    document.getElementById('statusSelect').addEventListener('change', updatePlaytimeVisibility);
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
    document.getElementById('gameModal').addEventListener('hidden.bs.modal', () => {
        resetCover();
        refreshIgdbPlaytime();
    });
    document.getElementById('titleInput').addEventListener('input', event => {
        clearWishAction();
        updateDekuDealsLink(event.target.value);
    });
    document.getElementById('gameModal').addEventListener('shown.bs.modal', () => {
        if (!document.getElementById('gameId').value) document.getElementById('titleInput').focus();
    });
}
