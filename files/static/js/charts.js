let charts = {};

const chartColors = ['#2474f5', '#238b57', '#df3848', '#ffbd1a', '#22bddd', '#d63384', '#7147bd', '#f07c27', '#20a987', '#6c7582'];

function count(values) {
    return values.reduce((result, value) => ({...result, [value]: (result[value] || 0) + 1}), {});
}

function sliceShare(chartInstance, dataIndex) {
    const values = chartInstance.data.datasets[0].data;
    const total = values.reduce((sum, value) => sum + Number(value), 0);
    return total ? Number(values[dataIndex]) / total : 0;
}

function hasInlineLabel(chartInstance, dataIndex) {
    const label = String(chartInstance.data.labels[dataIndex] || '');
    const minimumShare = label.length > 18 ? 0.2 : 0.12;
    return sliceShare(chartInstance, dataIndex) >= minimumShare;
}

function readableTextColor(color) {
    const hex = color.replace('#', '');
    const red = Number.parseInt(hex.slice(0, 2), 16);
    const green = Number.parseInt(hex.slice(2, 4), 16);
    const blue = Number.parseInt(hex.slice(4, 6), 16);
    const luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
    return luminance > 0.62 ? '#17202b' : '#ffffff';
}

function wrapLabel(context, label, maxWidth) {
    const words = label.split(/\s+/);
    const lines = [];
    let current = '';

    words.forEach(word => {
        const candidate = current ? `${current} ${word}` : word;
        if (current && context.measureText(candidate).width > maxWidth) {
            lines.push(current);
            current = word;
        } else {
            current = candidate;
        }
    });
    if (current) lines.push(current);

    if (lines.length <= 2) return lines;
    const shortened = `${lines.slice(1).join(' ').slice(0, 13).trim()}…`;
    return [lines[0], shortened];
}

const inlinePieLabels = {
    id: 'inlinePieLabels',
    afterDatasetsDraw(chartInstance) {
        const dataset = chartInstance.data.datasets[0];
        const meta = chartInstance.getDatasetMeta(0);
        const total = dataset.data.reduce((sum, value) => sum + Number(value), 0);
        if (!total) return;

        const context = chartInstance.ctx;
        meta.data.forEach((arc, dataIndex) => {
            if (!hasInlineLabel(chartInstance, dataIndex) || arc.hidden) return;

            const {x, y, startAngle, endAngle, outerRadius} = arc.getProps(
                ['x', 'y', 'startAngle', 'endAngle', 'outerRadius'],
                true
            );
            const angle = (startAngle + endAngle) / 2;
            const radius = outerRadius * 0.62;
            const centerX = x + Math.cos(angle) * radius;
            const centerY = y + Math.sin(angle) * radius;
            const share = Number(dataset.data[dataIndex]) / total;
            const maxWidth = Math.max(58, outerRadius * Math.sin((endAngle - startAngle) / 2) * 1.55);
            const backgroundColor = Array.isArray(dataset.backgroundColor)
                ? dataset.backgroundColor[dataIndex]
                : dataset.backgroundColor;

            context.save();
            context.fillStyle = readableTextColor(backgroundColor);
            context.textAlign = 'center';
            context.textBaseline = 'middle';
            context.font = '600 11px Inter, ui-sans-serif, system-ui, sans-serif';
            const lines = wrapLabel(context, String(chartInstance.data.labels[dataIndex]), maxWidth);
            context.shadowColor = 'rgba(0, 0, 0, .28)';
            context.shadowBlur = 3;
            const lineHeight = 14;
            const firstY = centerY - ((lines.length + 1) * lineHeight) / 2 + lineHeight / 2;
            lines.forEach((line, lineIndex) => context.fillText(line, centerX, firstY + lineIndex * lineHeight, maxWidth));
            context.font = '700 10px Inter, ui-sans-serif, system-ui, sans-serif';
            context.fillText(
                `${dataset.data[dataIndex]} (${Math.round(share * 100)} %)`,
                centerX,
                firstY + lines.length * lineHeight,
                maxWidth
            );
            context.restore();
        });
    }
};

function chart(canvasId, values) {
    charts[canvasId]?.destroy();
    const canvas = document.getElementById(canvasId);
    const labels = Object.keys(values);
    const totals = Object.values(values);
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--text').trim();
    const borderColor = getComputedStyle(document.documentElement).getPropertyValue('--surface').trim();
    canvas.setAttribute('role', 'img');
    canvas.setAttribute('aria-label', labels.length
        ? labels.map((label, index) => `${label}: ${totals[index]}`).join(', ')
        : 'Keine Daten verfügbar');

    charts[canvasId] = new Chart(canvas, {
        type: 'pie',
        plugins: [inlinePieLabels],
        data: {labels, datasets: [{
            data: totals,
            backgroundColor: chartColors,
            borderWidth: 2,
            borderColor
        }]},
        options: {
            responsive: true,
            layout: {padding: 8},
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        color: textColor,
                        padding: 14,
                        usePointStyle: true,
                        pointStyle: 'circle',
                        boxWidth: 8,
                        generateLabels(chartInstance) {
                            const generate = Chart.overrides.pie.plugins.legend.labels.generateLabels;
                            return generate(chartInstance).filter(item => !hasInlineLabel(chartInstance, item.index));
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label(context) {
                            const total = context.dataset.data.reduce((sum, value) => sum + Number(value), 0);
                            const percentage = total ? Math.round((Number(context.raw) / total) * 100) : 0;
                            return ` ${context.label}: ${context.raw} (${percentage} %)`;
                        }
                    }
                }
            }
        }
    });
}

export function initializeCharts() {
    document.getElementById('statsModal').addEventListener('show.bs.modal', () => {
        const cards = Array.from(document.querySelectorAll('.game-card'));
        const split = (card, key) => (card.dataset[key] || 'Keine').split(',').map(value => value.trim()).filter(Boolean);
        chart('platformChart', count(cards.flatMap(card => split(card, 'platform'))));
        chart('formatChart', count(cards.map(card => card.dataset.format || 'Keine')));
        chart('statusChart', count(cards.map(card => card.dataset.status || 'Keine')));
        const engines = Object.entries(count(cards.flatMap(card => split(card, 'engine')))).sort((a, b) => b[1] - a[1]);
        const top = Object.fromEntries(engines.slice(0, 5).map(([name, total]) => [name === 'Keine' ? 'Unbekannt' : name.replace(/\b\w/g, letter => letter.toUpperCase()), total]));
        const other = engines.slice(5).reduce((sum, [, total]) => sum + total, 0);
        if (other) top.Sonstige = other;
        chart('engineChart', top);
    });
}
