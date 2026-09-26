import {apiFetch, responseJson} from './api.js';

let persistedTheme;
let latestThemeRequest = 0;
let themeSaveQueue = Promise.resolve();

export function openAccountModal() {
    ['accountCurrentPassword', 'accountNewPassword', 'accountNewPasswordConfirm'].forEach(id => { document.getElementById(id).value = ''; });
    new bootstrap.Modal(document.getElementById('accountModal')).show();
}

function applyTheme(theme) {
    document.documentElement.dataset.uiTheme = theme;
    document.documentElement.dataset.bsTheme = theme === 'editorial' ? 'light' : 'dark';
}

function saveTheme(theme) {
    const status = document.getElementById('themeSaveStatus');
    const requestId = ++latestThemeRequest;
    applyTheme(theme);
    status.textContent = 'Darstellung wird gespeichert …';
    themeSaveQueue = themeSaveQueue.then(async () => {
        if (requestId !== latestThemeRequest) return;
        try {
            const response = await apiFetch('/api/account/theme', {
                method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({theme})
            });
            await responseJson(response, 'Die Darstellung konnte nicht gespeichert werden.');
            persistedTheme = theme;
            if (requestId === latestThemeRequest) status.textContent = 'Darstellung gespeichert.';
        } catch (error) {
            if (requestId !== latestThemeRequest) return;
            applyTheme(persistedTheme);
            const persistedInput = document.querySelector(`input[name="uiTheme"][value="${persistedTheme}"]`);
            if (persistedInput) persistedInput.checked = true;
            status.textContent = error.message;
        }
    });
}

export function initializeThemePicker() {
    persistedTheme = document.documentElement.dataset.uiTheme;
    document.querySelectorAll('input[name="uiTheme"]').forEach(input => {
        input.addEventListener('change', () => {
            if (input.checked) saveTheme(input.value);
        });
    });
}

export async function saveAccount() {
    const username = document.getElementById('accountUsername').value.trim();
    const currentPassword = document.getElementById('accountCurrentPassword').value;
    const newPassword = document.getElementById('accountNewPassword').value;
    const confirmation = document.getElementById('accountNewPasswordConfirm').value;
    if (!currentPassword) return alert('Bitte gib dein aktuelles Passwort ein.');
    if (newPassword !== confirmation) return alert('Die neuen Passwörter stimmen nicht überein.');
    const button = document.getElementById('accountSaveBtn');
    button.disabled = true;
    try {
        const response = await apiFetch('/api/account', {
            method: 'PUT', headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, current_password: currentPassword, new_password: newPassword, new_password_confirm: confirmation})
        });
        await responseJson(response, 'Die Kontoeinstellungen konnten nicht gespeichert werden.');
        location.reload();
    } catch (error) { alert(error.message); } finally { button.disabled = false; }
}

export async function deleteOwnAccount() {
    const currentPassword = document.getElementById('accountCurrentPassword').value;
    if (!currentPassword) return alert('Bitte gib zuerst dein aktuelles Passwort ein.');
    if (!confirm('Konto und die gesamte Spielesammlung dauerhaft löschen? Dieser Vorgang kann nicht rückgängig gemacht werden.')) return;
    try {
        const response = await apiFetch('/api/account', {
            method: 'DELETE', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({current_password: currentPassword})
        });
        await responseJson(response, 'Das Konto konnte nicht gelöscht werden.');
        window.location.href = '/register';
    } catch (error) { alert(error.message); }
}
