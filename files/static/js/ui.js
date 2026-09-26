const iconBaseUrl = 'https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/';
const platformIconMap = {
    'Amazon Games': iconBaseUrl + 'amazon.png', 'Battle.net': '/static/logos/battlenet.png',
    'EA app': '/static/logos/ea.png', 'Epic Games Store': iconBaseUrl + 'epic-games.png',
    'GOG.com': '/static/logos/gog.png', 'Nintendo 3DS': '/static/logos/nintendo-3ds.png',
    'Nintendo DS': '/static/logos/nintendo-ds.png',
    'PlayStation 4': iconBaseUrl + 'playstation.png',
    'PlayStation 5': iconBaseUrl + 'playstation.png', 'Rockstar Games Launcher': '/static/logos/rockstar-games.png',
    Steam: iconBaseUrl + 'steam.png', Switch: iconBaseUrl + 'nintendo-switch.png',
    'Switch 2': iconBaseUrl + 'nintendo-switch.png', 'Ubisoft Connect': '/static/logos/ubisoft.png',
    'XBOX One': iconBaseUrl + 'xbox.png', 'XBOX PC': iconBaseUrl + 'xbox.png',
    'XBOX Series': iconBaseUrl + 'xbox.png'
};

const platformLabelMap = {
    'Amazon Games': 'AG', 'Battle.net': 'Bnet', 'EA app': 'EA', 'Epic Games Store': 'EGS',
    'GOG.com': 'GOG', 'Nintendo 3DS': '3DS', 'Nintendo DS': 'DS',
    'Nintendo Entertainment System (NES)': 'NES',
    'Super Nintendo Entertainment System (SNES)': 'SNES',
    'Nintendo 64 (N64)': 'N64', GameCube: 'GC', 'Game Boy': 'GB',
    'Game Boy Color': 'GBC', 'Game Boy Advance': 'GBA', Wii: 'Wii', 'Wii U': 'Wii U',
    'Sega Game Gear': 'GG', 'Sega Mega Drive': 'MD', 'Sega Dreamcast': 'DC',
    'Sega Mega CD': 'MCD', 'Sega Master System': 'SMS', 'PlayStation 4': 'PS4',
    'PlayStation 5': 'PS5', 'Rockstar Games Launcher': 'RGL', Steam: 'Steam',
    Switch: 'NS', 'Switch 2': 'NS2', 'Ubisoft Connect': 'Ubi',
    'XBOX One': 'XB1', 'XBOX PC': 'XPC', 'XBOX Series': 'XBS'
};

export function sortGames() {
    const sortSelect = document.getElementById('sortSelect');
    localStorage.setItem('gameSort', sortSelect.value);
    const cards = Array.from(document.querySelectorAll('.game-card'));
    cards.sort((a, b) => {
        if (sortSelect.value === 'title_asc') return a.dataset.title.localeCompare(b.dataset.title);
        if (sortSelect.value === 'release_desc') return (parseInt(b.dataset.release) || 0) - (parseInt(a.dataset.release) || 0);
        if (sortSelect.value === 'release_asc') return (parseInt(a.dataset.release) || 0) - (parseInt(b.dataset.release) || 0);
        return (parseInt(b.dataset.id) || 0) - (parseInt(a.dataset.id) || 0);
    });
    cards.forEach(card => document.getElementById('gameGrid').appendChild(card));
}

export function applyFilters() {
    const value = id => document.getElementById(id).value;
    const text = value('filterText').toLowerCase();
    const engine = value('filterEngine').toLowerCase();
    const platform = value('filterPlatform');
    const status = value('filterStatus');
    const format = value('filterFormat');
    const ageRating = value('filterAgeRating');
    const minimumRating = Number.parseFloat(value('filterMinRating'));
    document.querySelectorAll('.game-card').forEach(card => {
        const rating = Number.parseFloat(card.dataset.rating);
        const ownedPlatforms = (card.dataset.platform || '').split(',').map(item => item.trim()).filter(Boolean);
        const matches = (card.dataset.title.includes(text) || card.dataset.dev.includes(text) || card.dataset.pub.includes(text))
            && (!engine || card.dataset.engine.includes(engine))
            && (!platform || ownedPlatforms.includes(platform))
            && (!status || card.dataset.status === status)
            && (!format || card.dataset.format === format)
            && (!ageRating || (card.dataset.ageRating || '').includes(ageRating))
            && (Number.isNaN(minimumRating) || (!Number.isNaN(rating) && rating >= minimumRating));
        card.style.display = matches ? 'flex' : 'none';
    });
}

export function resetFilters() {
    ['filterText', 'filterEngine', 'filterPlatform', 'filterStatus', 'filterFormat', 'filterAgeRating', 'filterMinRating']
        .forEach(id => { document.getElementById(id).value = ''; });
    applyFilters();
    updateQuickFilterState('all');
}

function updateQuickFilterState(activeValue) {
    document.querySelectorAll('[data-quick-filter]').forEach(button => {
        button.classList.toggle('active', button.dataset.quickFilter === activeValue);
    });
}

export function setQuickFilter(type, value = '') {
    if (type === 'all') {
        resetFilters();
        return;
    }
    document.getElementById('filterStatus').value = '';
    document.getElementById('filterFormat').value = '';
    const target = type === 'status' ? 'filterStatus' : 'filterFormat';
    document.getElementById(target).value = value;
    applyFilters();
    updateQuickFilterState(value);
}

function installDisplayFieldValues() {
    ['releaseInput', 'devInput', 'pubInput', 'engineInput', 'platformsInput', 'ageRatingInput'].forEach(id => {
        const element = document.getElementById(id);
        Object.defineProperty(element, 'value', {
            get() { return this.innerText === '-' ? '' : this.innerText; },
            set(value) { this.innerText = value || '-'; }
        });
    });
    const coverInput = document.getElementById('coverUrlInput');
    const nativeValue = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
    Object.defineProperty(coverInput, 'value', {
        set(value) {
            nativeValue.set.call(this, value);
            const preview = document.getElementById('coverPreview');
            preview.src = value || '';
            preview.classList.toggle('d-none', !value);
            document.getElementById('coverPlaceholder').classList.toggle('d-none', Boolean(value));
        },
        get() { return nativeValue.get.call(this); }
    });
}

function addPlatformIcons() {
    document.querySelectorAll('.game-card').forEach(card => {
        const platforms = (card.dataset.platform || '').split(',').map(item => item.trim()).filter(Boolean);
        const container = document.createElement('div');
        container.className = 'platform-icons';
        platforms.forEach(platform => {
            const source = platformIconMap[platform];
            const badge = document.createElement('span');
            badge.className = 'platform-icon-badge';
            badge.title = platform;
            badge.setAttribute('aria-label', platform);

            const label = document.createElement('span');
            label.className = 'platform-icon-label';
            label.textContent = platformLabelMap[platform] || platform.slice(0, 4).toUpperCase();
            badge.appendChild(label);

            if (source) {
                const image = document.createElement('img');
                image.src = source;
                image.className = 'platform-icon';
                image.alt = '';
                image.onload = () => badge.classList.add('has-image');
                image.onerror = () => image.remove();
                badge.prepend(image);
            }
            container.appendChild(badge);
        });
        card.appendChild(container);
    });
}

export function initializeUi() {
    installDisplayFieldValues();
    const slider = document.getElementById('coverSizeSlider');
    const savedSize = localStorage.getItem('coverSize');
    if (savedSize) {
        slider.value = savedSize;
        document.documentElement.style.setProperty('--cover-size', `${savedSize}px`);
    }
    slider.addEventListener('input', event => {
        document.documentElement.style.setProperty('--cover-size', `${event.target.value}px`);
        localStorage.setItem('coverSize', event.target.value);
    });
    const sortSelect = document.getElementById('sortSelect');
    sortSelect.value = localStorage.getItem('gameSort') || sortSelect.value;
    sortSelect.addEventListener('change', sortGames);
    ['filterText', 'filterEngine', 'filterPlatform', 'filterStatus', 'filterFormat', 'filterAgeRating', 'filterMinRating']
        .forEach(id => document.getElementById(id).addEventListener(id === 'filterText' ? 'input' : 'change', applyFilters));
    sortGames();
    addPlatformIcons();
}
