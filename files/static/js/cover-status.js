import {apiFetch, responseJson} from './api.js';

const paths = {
    'Gespielt': '<path d="m5 12 4 4L19 6"/>',
    'Nicht gespielt': '<path d="m6 6 12 12M18 6 6 18"/>',
    'Abgebrochen': '<circle cx="12" cy="12" r="9"/><path d="m6 6 12 12"/>'
};

export function initializeCoverStatus() {
    const toggle = document.getElementById('showCoverStatus');
    const message = document.getElementById('coverStatusPreferenceMessage');
    const apply = () => document.documentElement.classList.toggle('hide-cover-status', !toggle.checked);
    try { toggle.checked = localStorage.getItem('showCoverStatus') !== 'false'; } catch { /* Session-only if storage is unavailable. */ }
    apply();
    toggle.addEventListener('change', () => {
        apply();
        try {
            localStorage.setItem('showCoverStatus', String(toggle.checked));
            message.textContent = '';
        } catch {
            message.textContent = 'Die Einstellung gilt vorübergehend. Der Browser konnte sie nicht dauerhaft speichern.';
        }
    });
    document.querySelectorAll('.cover-status').forEach(control => {
        const select = control.querySelector('select');
        const icon = control.querySelector('.cover-status-icon');
        icon.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">${paths[select.value]}</svg>`;
        // Keep card activation separate from the native, keyboard-accessible picker.
        ['click', 'keydown', 'keyup'].forEach(type => control.addEventListener(type, event => event.stopPropagation()));
        select.addEventListener('change', async () => {
            const previous = control.closest('.game-card').dataset.status;
            select.disabled = true;
            try {
                const response = await apiFetch(`/api/games/${control.dataset.gameId}`, {
                    method: 'PUT', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({status: select.value, version: Number(control.dataset.version)})
                });
                await responseJson(response, 'Der Status konnte nicht gespeichert werden.');
                location.reload();
            } catch (error) {
                select.value = previous;
                alert(error.message);
                select.disabled = false;
                select.focus();
            }
        });
    });
}
