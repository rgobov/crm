<script>
    import { createEventDispatcher } from 'svelte';
    import { scale, fade } from 'svelte/transition';
    import { timeUtils } from '$lib/utils/timeUtils.js';

    export let appt;
    export let startHour;
    export let hourHeight;
    export let timezone = 'Europe/Moscow';

    const dispatch = createEventDispatcher();

    $: apptStyle = (() => {
        const top = timeUtils.getTimeOffset(appt.startTime, startHour, hourHeight, timezone);
        const actualHeight = appt.durationInMinutes * (hourHeight / 60);
        return `top: ${top}px; height: ${actualHeight - 2}px;`;
    })();

    const statusColors = {
        'SCHEDULED': '#64748b',   // Slate Gray (нейтральный)
        'CONFIRMED': '#0891b2',   // Cyan (отличается от зеленого)
        'ARRIVED': '#7c3aed',     // Violet
        'NEEDS_CALL': '#a855f7',   // Purple (требует внимания)
        'COMPLETED': '#16a34a',   // Green (успешно завершено)
        'CANCELLED': '#dc2626'     // Red (отмена)
    };

    // ФОНОВЫЕ ЦВЕТА (очень светлые пастельные)
    const backgroundColors = {
        'SCHEDULED': '#f8fafc',   // Очень светлый серый
        'CONFIRMED': '#ecfeff',   // Очень светлый cyan
        'ARRIVED': '#f3e8ff',     // Очень светлый фиолетовый
        'NEEDS_CALL': '#faf5ff',   // Очень светлый пурпурный
        'COMPLETED': '#f0fdf4',   // Очень светлый зеленый
        'CANCELLED': '#fef2f2'     // Очень светлый красный
    };

    $: color = statusColors[appt.status] || statusColors['SCHEDULED'];
    $: bgColor = backgroundColors[appt.status] || '#fdf6e3';
    $: isShort = appt.durationInMinutes < 30;
</script>

<button class="appt-box btn-reset"
     style="{apptStyle} --status-color: {color}; --bg-color: {bgColor}"
     on:click|stopPropagation={() => dispatch('click', appt)}
     in:scale={{duration: 200, start: 0.95}}>
    <div class="appt-content" class:compact={isShort}>
        <div class="t-row">
            <span class="tm">
                {timeUtils.formatTime(appt.startTime, timezone)} — {timeUtils.getEndTime(appt.startTime, appt.durationInMinutes, timezone)}
            </span>
            <div class="indicators">
                <span class="st-dot" style="background: {color}"></span>
            </div>
        </div>

        <div class="main-info">
            <div class="cl">{appt.clientName}</div>

            <div class="sub-details-stack">
                {#if appt.referenceTag}
                    <span class="ref-tag">🚗 {appt.referenceTag}</span>
                {/if}
                <span class="sv">{appt.service}</span>
            </div>

            {#if appt.comment && !isShort}
                <div class="cmt-preview" transition:fade>
                    {appt.comment}
                </div>
            {/if}
        </div>
    </div>
</button>

<style>
    .btn-reset {
        background: none; border: none; padding: 0; margin: 0;
        text-align: left; cursor: pointer; font-family: inherit;
        display: block;
    }

    .appt-box {
        position: absolute;
        left: 2px !important;
        right: 2px !important;
        width: auto !important;

        background: var(--bg-color);
        border-radius: 10px;
        box-shadow: 0 1px 4px rgba(0,0,0,0.04);
        border: 1px solid rgba(7, 54, 66, 0.08);
        overflow: hidden;
        z-index: 150;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        box-sizing: border-box;
    }

    .appt-box:hover {
        z-index: 400 !important;
        box-shadow: 0 12px 24px rgba(0,0,0,0.12);
        transform: translateY(-2px) scale(1.01);
        border-color: var(--status-color);
    }

    .appt-content {
        height: 100%;
        border-left: 3.5px solid var(--status-color);
        padding: 5px 8px;
        display: flex;
        flex-direction: column;
        gap: 1px;
        box-sizing: border-box;
    }
    .appt-content.compact { padding: 3px 6px; gap: 0; }

    .t-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; opacity: 0.8; }
    .tm { font-size: 10px; font-weight: 800; color: #586e75; letter-spacing: 0.2px; white-space: nowrap; }

    .st-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }

    .main-info { flex: 1; min-height: 0; display: flex; flex-direction: column; justify-content: flex-start; gap: 0.5px; overflow: hidden; }
    .cl { font-size: 13.5px; font-weight: 850; color: #002b36; line-height: 1.15; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .sub-details-stack { display: flex; flex-direction: column; gap: 0.5px; overflow: hidden; margin-top: 1px; }
    .ref-tag { font-size: 9.5px; font-weight: 900; color: #2aa198; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .sv { font-size: 10px; color: #586e75; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .cmt-preview {
        margin-top: 3px;
        font-size: 10.5px;
        line-height: 1.25;
        color: #073642;
        font-weight: 500;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        word-break: break-word;
        padding-top: 3px;
        border-top: 1px solid rgba(7, 54, 66, 0.06);
    }

    /* Увеличиваем шрифт комментариев только для десктопной версии */
    @media (min-width: 1024px) {
        .cmt-preview {
            font-size: 11px;
        }
    }

    .compact .cmt-preview { display: none; }
</style>
