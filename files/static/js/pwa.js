let installPrompt;

function isInstalled() {
    return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
}

export function initializePwa() {
    const button = document.getElementById('installAppBtn');
    const status = document.getElementById('installAppStatus');

    if ('serviceWorker' in navigator) {
        window.addEventListener('load', async () => {
            try {
                await navigator.serviceWorker.register('/service-worker.js', {scope: '/'});
            } catch (_) {
                status.textContent = 'Die Installation benötigt eine HTTPS-Verbindung.';
            }
        });
    } else {
        status.textContent = 'Dieser Browser unterstützt keine App-Installation.';
    }

    if (isInstalled()) {
        status.textContent = 'Games Collection ist als App installiert.';
        return;
    }

    window.addEventListener('beforeinstallprompt', event => {
        event.preventDefault();
        installPrompt = event;
        button.classList.remove('d-none');
        status.textContent = 'Die App kann auf diesem Gerät installiert werden.';
    });

    button.addEventListener('click', async () => {
        if (!installPrompt) return;
        await installPrompt.prompt();
        const choice = await installPrompt.userChoice;
        status.textContent = choice.outcome === 'accepted'
            ? 'Installation wurde gestartet.'
            : 'Installation wurde abgebrochen.';
        installPrompt = undefined;
        button.classList.add('d-none');
    });

    window.addEventListener('appinstalled', () => {
        installPrompt = undefined;
        button.classList.add('d-none');
        status.textContent = 'Games Collection wurde erfolgreich installiert.';
    });
}
