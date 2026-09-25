let csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || '';

async function refreshCsrfToken() {
    const response = await fetch('/api/csrf-token', {
        method: 'GET',
        headers: {'Accept': 'application/json'},
        credentials: 'same-origin',
        cache: 'no-store'
    });
    if (!response.ok || response.redirected) {
        throw new Error('Deine Sitzung ist abgelaufen. Bitte lade die Seite neu und melde dich erneut an.');
    }
    const result = await response.json().catch(() => ({}));
    if (!result.csrf_token) {
        throw new Error('Das Sicherheitstoken konnte nicht erneuert werden. Bitte lade die Seite neu.');
    }
    csrfToken = result.csrf_token;
    const meta = document.querySelector('meta[name="csrf-token"]');
    if (meta) meta.content = csrfToken;
}

export async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    const method = (options.method || 'GET').toUpperCase();
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers.set('X-CSRFToken', csrfToken);
    const response = await fetch(url, {...options, headers, credentials: 'same-origin'});
    if (response.headers.get('X-CSRF-Error') !== '1') return response;

    await refreshCsrfToken();
    headers.set('X-CSRFToken', csrfToken);
    return fetch(url, {...options, headers, credentials: 'same-origin'});
}

export async function responseJson(response, fallback) {
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.error || fallback);
    return result;
}
