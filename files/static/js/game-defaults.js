import {apiFetch, responseJson} from './api.js';

let defaults = JSON.parse(document.getElementById('initialGameDefaults').textContent);

export function applyGameDefaults() {
    document.getElementById('gameTypeSearch').value = defaults.game_type ?? '';
    document.getElementById('formatSelect').value = defaults.format;
    document.getElementById('statusSelect').value = defaults.status;
    Array.from(document.getElementById('ownedPlatformInput').options).forEach(option => {
        option.selected = defaults.platforms.includes(option.value);
    });
}

export function initializeGameDefaults() {
    document.getElementById('defaultGameType').value = defaults.game_type ?? '';
    document.getElementById('defaultFormat').value = defaults.format;
    document.getElementById('defaultStatus').value = defaults.status;
    const platforms = Array.from(document.querySelectorAll('input[name="defaultPlatform"]'));
    platforms.forEach(input => { input.checked = defaults.platforms.includes(input.value); });
    const button = document.getElementById('saveGameDefaults');
    const status = document.getElementById('gameDefaultsStatus');
    button.addEventListener('click', async () => {
        const type = document.getElementById('defaultGameType').value;
        const values = {game_type: type === '' ? null : Number(type),
            format: document.getElementById('defaultFormat').value,
            status: document.getElementById('defaultStatus').value,
            platforms: platforms.filter(input => input.checked).map(input => input.value)};
        button.disabled = true;
        status.textContent = 'Standardwerte werden gespeichert …';
        try {
            defaults = await responseJson(await apiFetch('/api/account/game-defaults', {
                method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(values)
            }), 'Standardwerte konnten nicht gespeichert werden.');
            status.textContent = 'Standardwerte gespeichert.';
        } catch (error) { status.textContent = error.message; }
        finally { button.disabled = false; }
    });
}
