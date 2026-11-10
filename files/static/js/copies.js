// One title owns an ordered list of copies; identical editions are allowed.
export function gameCopies(game) {
    return game.copies ?? (game.owned_platform || '').split(',').map(p => p.trim()).filter(Boolean)
        .map(platform => ({platform, format: game.format || 'Digital', edition: ''}));
}

let copies = [];
let editing = null;
let defaultFormat = 'Digital';
const byId = id => document.getElementById(id);

function formatIcon(format) {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('class', 'copy-format-icon');
    svg.setAttribute('aria-hidden', 'true');
    svg.setAttribute('focusable', 'false');
    const path = document.createElementNS(svg.namespaceURI, 'path');
    path.setAttribute('d', format === 'Digital'
        ? 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3'
        : 'M22 12a10 10 0 1 1-20 0 10 10 0 1 1 20 0M14 12a2 2 0 1 1-4 0 2 2 0 1 1 4 0M7 12a5 5 0 0 1 5-5M12 17a5 5 0 0 0 5-5');
    svg.append(path);
    return svg;
}
export const getCopies = () => copies.map(copy => ({...copy}));
export const hasCopyDraft = () => editing !== null;

export function setCopies(values, format = 'Digital') {
    copies = values.map(copy => ({...copy}));
    defaultFormat = format;
    editing = null;
    render();
}

function render() {
    const list = byId('copiesList');
    list.replaceChildren();
    byId('copyEditor').hidden = editing === null;
    byId('addCopyBtn').disabled = copies.length >= 100 || editing !== null;
    byId('copiesCount').textContent = `${copies.length} ${copies.length === 1 ? 'Exemplar' : 'Exemplare'}`;
    if (!copies.length) list.textContent = 'Noch keine Exemplare erfasst.';
    copies.forEach((copy, index) => {
        const row = document.createElement('div'); row.className = 'copy-row';
        const info = document.createElement('div'); info.className = 'copy-description';
        const title = document.createElement('strong'); title.textContent = copy.platform;
        const description = document.createElement('span');
        description.textContent = `${copy.format} · ${copy.edition || 'Ohne Ausgabenangabe'}`;
        info.append(title, description);
        const actions = document.createElement('div'); actions.className = 'copy-actions';
        [['Bearbeiten', () => open(index)], ['Entfernen', () => {
            if (confirm('Dieses Exemplar entfernen? Der Spieleintrag mit Bewertung und Status bleibt erhalten.')) {
                copies.splice(index, 1); render();
            }
        }]].forEach(([label, action]) => {
            const button = document.createElement('button'); button.type = 'button';
            button.className = 'secondary-action'; button.textContent = label;
            button.setAttribute('aria-label', `${copy.platform} · ${copy.format}: ${label}`);
            button.disabled = editing !== null; button.addEventListener('click', action); actions.append(button);
        });
        const identity = document.createElement('div'); identity.className = 'copy-identity';
        identity.append(formatIcon(copy.format), info);
        row.append(identity, actions); list.append(row);
    });
}

function open(index) {
    editing = index;
    const copy = copies[index] || {platform: byId('copyPlatform').options[0]?.value || '', format: defaultFormat, edition: ''};
    byId('copyPlatform').value = copy.platform;
    byId('copyFormat').value = copy.format;
    byId('copyEdition').value = copy.edition;
    byId('copyEditorTitle').textContent = index < 0 ? 'Exemplar hinzufügen' : 'Exemplar bearbeiten';
    byId('copyError').textContent = '';
    render(); byId('copyPlatform').focus();
}

export function initializeCopies() {
    byId('addCopyBtn').addEventListener('click', () => open(-1));
    byId('cancelCopyBtn').addEventListener('click', () => { editing = null; render(); byId('addCopyBtn').focus(); });
    byId('applyCopyBtn').addEventListener('click', () => {
        const copy = {platform: byId('copyPlatform').value, format: byId('copyFormat').value, edition: byId('copyEdition').value.trim()};
        if (!copy.platform || copy.edition.length > 80) {
            byId('copyError').textContent = 'Wähle eine Plattform und eine Ausgabe mit höchstens 80 Zeichen.'; return;
        }
        if (editing < 0) copies.push(copy); else copies[editing] = copy;
        editing = null; render(); byId('addCopyBtn').focus();
    });
    render();
}
