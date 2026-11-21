function sortGames() {
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

function applyFilters() {
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
        const copies = JSON.parse(card.dataset.copies || '[]');
        const matches = (card.dataset.title.includes(text) || card.dataset.dev.includes(text) || card.dataset.pub.includes(text))
            && (!engine || card.dataset.engine.includes(engine))
            && ((!platform && !format) || copies.some(copy => (!platform || copy.platform === platform) && (!format || copy.format === format)))
            && (!status || card.dataset.status === status)
            && (!ageRating || (card.dataset.ageRating || '').includes(ageRating))
            && (Number.isNaN(minimumRating) || (!Number.isNaN(rating) && rating >= minimumRating));
        card.hidden = !matches;
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
}
