import {apiFetch, responseJson} from './api.js';

export async function openAdminModal() {
    let users;
    try {
        users = await responseJson(await apiFetch('/api/users'), 'Benutzer konnten nicht geladen werden.');
    } catch (error) { alert(error.message); return; }
    const list = document.getElementById('adminUserList');
    list.replaceChildren();
    users.forEach(user => list.appendChild(createUserRow(user)));
    new bootstrap.Modal(document.getElementById('adminModal')).show();
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
    controls.className = 'd-flex gap-2';
    const username = document.createElement('input');
    username.type = 'text'; username.className = 'form-control form-control-sm';
    username.id = `edit-user-${user.id}`; username.value = user.username; username.style.width = '120px';
    const password = document.createElement('input');
    password.type = 'password'; password.className = 'form-control form-control-sm';
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
