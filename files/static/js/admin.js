import {apiFetch, responseJson} from './api.js';

export async function openAdminModal() {
    const modal = document.getElementById('adminModal');
    if (!modal) return;
    bootstrap.Modal.getOrCreateInstance(modal).show();
    document.getElementById('adminBackupBtn').onclick = exportDatabase;
    const status = document.getElementById('adminUsersStatus');
    status.textContent = 'Benutzer werden geladen …';
    try {
        const users = await responseJson(await apiFetch('/api/users'), 'Benutzer konnten nicht geladen werden.');
        document.getElementById('adminUserList').replaceChildren(...users.map(createUserRow));
        status.textContent = '';
    } catch (error) { status.textContent = error.message; }
}

async function exportDatabase() {
    const button = document.getElementById('adminBackupBtn');
    const status = document.getElementById('adminBackupStatus');
    button.disabled = true;
    status.textContent = 'Backup wird erstellt …';
    try {
        const response = await apiFetch('/api/admin/backup', {method: 'POST'});
        if (!response.ok) await responseJson(response, 'Das Backup konnte nicht erstellt werden.');
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = response.headers.get('Content-Disposition')?.match(/filename="?([^";]+)"?/)?.[1]
            || 'Games-Collection-Backup.db';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
        status.textContent = 'Download gestartet. Prüfe, ob die Backup-Datei gespeichert wurde.';
    } catch (error) { status.textContent = error.message; }
    finally { button.disabled = false; }
}

function createUserRow(user) {
    const item = document.createElement('div');
    item.className = 'list-group-item d-flex justify-content-between align-items-center flex-wrap gap-2';
    const identity = document.createElement('div');
    identity.className = 'd-flex align-items-center gap-2';
    const name = document.createElement('span');
    name.className = 'fw-bold'; name.textContent = user.username; identity.appendChild(name);
    if (user.is_admin) {
        const badge = document.createElement('span');
        badge.className = 'badge bg-warning text-dark'; badge.textContent = 'Admin'; identity.appendChild(badge);
    }
    const controls = document.createElement('div');
    controls.className = 'admin-user-controls d-flex flex-wrap gap-2';
    const username = document.createElement('input');
    username.type = 'text'; username.className = 'form-control form-control-sm';
    username.setAttribute('aria-label', 'Benutzername');
    username.id = `edit-user-${user.id}`; username.value = user.username; username.style.width = '120px';
    const password = document.createElement('input');
    password.type = 'password'; password.className = 'form-control form-control-sm';
    password.setAttribute('aria-label', 'Neues Passwort');
    password.autocomplete = 'new-password';
    password.id = `edit-pass-${user.id}`; password.placeholder = 'Neues Passwort'; password.style.width = '140px';
    const save = document.createElement('button');
    save.className = 'btn btn-success btn-sm'; save.textContent = 'Speichern'; save.addEventListener('click', () => updateUser(user.id));
    controls.append(username, password, save);
    if (!user.is_admin) {
        const remove = document.createElement('button');
        remove.className = 'btn btn-danger btn-sm'; remove.textContent = 'Löschen'; remove.addEventListener('click', () => deleteUser(user.id));
        controls.appendChild(remove);
    }
    item.append(identity, controls);
    return item;
}

async function updateUser(userId) {
    try {
        const response = await apiFetch(`/api/users/${userId}`, {
            method: 'PUT', headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username: document.getElementById(`edit-user-${userId}`).value, password: document.getElementById(`edit-pass-${userId}`).value})
        });
        await responseJson(response, 'Benutzer konnte nicht aktualisiert werden.');
        alert('Benutzer erfolgreich aktualisiert.');
        await openAdminModal();
    } catch (error) { alert(error.message); }
}

async function deleteUser(userId) {
    if (!confirm('Achtung: Dies löscht den Benutzer UND seine gesamte Spielesammlung. Wirklich fortfahren?')) return;
    try {
        await responseJson(await apiFetch(`/api/users/${userId}`, {method: 'DELETE'}), 'Benutzer konnte nicht gelöscht werden.');
        await openAdminModal();
    } catch (error) { alert(error.message); }
}
