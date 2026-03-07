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
        // ФИКС: Используем left/right 4px чтобы был зазор между колонками
        return `top: ${top}px; height: ${actualHeight - 2}px; left: 4px; right: 4px; z-index: 200;`;
    })();

    const statusColors = {
        'SCHEDULED': '#268bd2',
        'CONFIRMED': '#859900',
        'NEEDS_CALL': '#b58900',
        'COMPLETED': '#93a1a1',
        'CANCELLED': '#dc322f'
    };

    $: color = statusColors[appt.status] || statusColors['SCHEDULED'];
    $: isShort = appt.durationInMinutes < 40;
</script>

<button class="appt-box btn-reset"
     style="{apptStyle} --status-color: {color}"
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

            <div class="sub-details">
                {#if appt.referenceTag}
                    <span class="ref-tag">🚗 {appt.referenceTag}</span>
                {/if}
                <span class="sv">{appt.service}</span>
            </div>

            {#if appt.comment && !isShort}
                <div class="cmt-preview" transition:fade>
                    <span class="cmt-icon">💬</span> {appt.comment}
                </div>
            {/if}
        </div>
    </div>
</button>

<style>
    .btn-reset {
        background: none; border: none; padding: 0; margin: 0;
        text-align: left; cursor: pointer;
        font-family: inherit;
        display: block; /* Важно для корректного позиционирования */
    }

    .appt-box {
        position: absolute;
        /* Удалили жесткие left/right здесь, так как они в apptStyle для реактивности */
        background: #fdf6e3;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.08);
        border: 1.5px solid #eee8d5;
        overflow: hidden;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .appt-box:hover { z-index: 300 !important; box-shadow: 0 12px 24px rgba(0,0,0,0.12); transform: translateY(-1px); }

    .appt-content { height: 100%; border-left: 4px solid var(--status-color); padding: 6px 10px; display: flex; flex-direction: column; gap: 1px; }
    .appt-content.compact { padding: 4px 8px; }

    .t-row { display: flex; justify-content: space-between; align-items: center; }
    .tm { font-size: 10px; font-weight: 900; color: #93a1a1; letter-spacing: 0.2px; }

    .st-dot { width: 7px; height: 7px; border-radius: 50%; }

    .main-info { flex: 1; min-height: 0; display: flex; flex-direction: column; justify-content: flex-start; gap: 1px; overflow: hidden; }
    .cl { font-size: 13px; font-weight: 850; color: #073642; line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .sub-details { display: flex; align-items: center; gap: 8px; flex-wrap: nowrap; overflow: hidden; }
    .ref-tag { font-size: 10px; font-weight: 900; color: #2aa198; white-space: nowrap; flex-shrink: 0; }
    .sv { font-size: 10px; color: #657b83; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .cmt-preview {
        margin-top: 4px;
        font-size: 11px;
        line-height: 1.3;
        color: #586e75;
        font-weight: 500;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        word-break: break-word;
        padding-top: 4px;
        border-top: 1px solid rgba(147, 161, 161, 0.1);
    }
    .cmt-icon { font-size: 9px; opacity: 0.6; margin-right: 2px; }
    .compact .cmt-preview { display: none; }
</style>
