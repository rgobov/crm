<script>
    import { createEventDispatcher } from 'svelte';
    import { scale } from 'svelte/transition';
    import { timeUtils } from '$lib/utils/timeUtils.js';

    export let appt;
    export let startHour;
    export let hourHeight;
    export let timezone = 'Europe/Moscow';

    const dispatch = createEventDispatcher();

    $: apptStyle = (() => {
        const top = timeUtils.getTimeOffset(appt.startTime, startHour, hourHeight, timezone);
        const actualHeight = appt.durationInMinutes * (hourHeight / 60);
        // ПОВЫШАЕМ Z-INDEX ДО 200, чтобы быть выше сетки (50)
        return `top: ${top}px; height: ${actualHeight - 2}px; z-index: 200;`;
    })();

    const statusColors = {
        'SCHEDULED': '#268bd2',
        'CONFIRMED': '#859900',
        'NEEDS_CALL': '#b58900',
        'COMPLETED': '#93a1a1',
        'CANCELLED': '#dc322f'
    };

    $: color = statusColors[appt.status] || statusColors['SCHEDULED'];
</script>

<div class="appt-box"
     style="{apptStyle} --status-color: {color}"
     on:click|stopPropagation={() => dispatch('click', appt)}
     in:scale={{duration: 200, start: 0.95}}>
    <div class="appt-content">
        <div class="t-row">
            <span class="tm">{timeUtils.formatTime(appt.startTime, timezone)}</span>
            <div class="indicators">
                {#if appt.comment}
                    <span class="note-icon" title={appt.comment}>📝</span>
                {/if}
                <span class="st-dot" style="background: {color}"></span>
            </div>
        </div>

        <div class="main-info">
            <div class="cl">{appt.clientName}</div>
            {#if appt.referenceTag}
                <div class="ref-tag">🚗 {appt.referenceTag}</div>
            {/if}
        </div>

        <div class="sv">{appt.service}</div>
    </div>
</div>

<style>
    .appt-box {
        position: absolute; left: 6px; right: 6px;
        background: #fdf6e3;
        border-radius: 18px;
        box-shadow: 0 8px 20px -5px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05);
        cursor: pointer; transition: transform 0.2s, box-shadow 0.2s;
        border: 1.5px solid #eee8d5;
        overflow: hidden;
    }
    .appt-box:hover { transform: translateY(-2px) scale(1.01); z-index: 300 !important; box-shadow: 0 15px 30px -10px rgba(0,0,0,0.15); border-color: var(--status-color); }
    .appt-box:active { transform: scale(0.99); }

    .appt-content { height: 100%; border-left: 5px solid var(--status-color); padding: 8px 12px; display: flex; flex-direction: column; gap: 2px; }

    .t-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
    .tm { font-size: 11px; font-weight: 900; color: #586e75; }

    .indicators { display: flex; align-items: center; gap: 6px; }
    .st-dot { width: 8px; height: 8px; border-radius: 50%; transition: background 0.3s; }
    .note-icon { font-size: 10px; opacity: 0.8; }

    .main-info { flex: 1; min-height: 0; display: flex; flex-direction: column; justify-content: center; }

    .cl { font-size: 13px; font-weight: 850; color: #073642; line-height: 1.1; word-break: break-word; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; }

    .ref-tag { font-size: 11px; font-weight: 900; color: #2aa198; margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .sv { font-size: 10px; color: #657b83; font-weight: 750; text-transform: uppercase; letter-spacing: 0.3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
