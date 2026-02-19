<script>
    import { createEventDispatcher } from 'svelte';
    import { scale } from 'svelte/transition';
    import { timeUtils } from '$lib/utils/timeUtils.js';

    export let appt;
    export let startHour;
    export let hourHeight;
    export let timezone = 'Europe/Moscow'; // Принимаем TZ филиала

    const dispatch = createEventDispatcher();

    // РЕАКТИВНЫЙ СТИЛЬ С УЧЕТОМ ТАЙМЗОНЫ ФИЛИАЛА
    $: apptStyle = (() => {
        // Получаем отступ сверху, игнорируя локальный часовой пояс администратора
        const top = timeUtils.getTimeOffset(appt.startTime, startHour, hourHeight, timezone);
        const actualHeight = appt.durationInMinutes * (hourHeight / 60);
        return `top: ${top}px; height: ${actualHeight - 2}px; z-index: 60;`;
    })();

    const statusColors = {
        'SCHEDULED': '#3b82f6',
        'CONFIRMED': '#10b981',
        'NEEDS_CALL': '#f59e0b',
        'COMPLETED': '#64748b',
        'CANCELLED': '#ef4444'
    };

    $: color = statusColors[appt.status] || statusColors['SCHEDULED'];
</script>

<div class="appt-box"
     style="{apptStyle} --status-color: {color}"
     on:click|stopPropagation={() => dispatch('click', appt)}
     in:scale={{duration: 200, start: 0.95}}>
    <div class="appt-content">
        <div class="t-row">
            <!-- Время тоже отображаем по поясу филиала -->
            <span class="tm">{timeUtils.formatTime(appt.startTime, timezone)}</span>
            <div class="indicators">
                {#if appt.comment}
                    <span class="note-icon" title={appt.comment}>📝</span>
                {/if}
                <span class="st-dot" style="background: {color}"></span>
            </div>
        </div>
        <div class="cl">{appt.clientName}</div>
        <div class="sv">{appt.service}</div>
    </div>
</div>

<style>
    .appt-box {
        position: absolute; left: 6px; right: 6px; background: white; border-radius: 18px;
        box-shadow: 0 10px 25px -5px rgba(0,0,0,0.08), 0 8px 10px -6px rgba(0,0,0,0.05);
        cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; border: 1px solid rgba(0,0,0,0.03);
        overflow: hidden;
    }
    .appt-box:hover { transform: translateY(-2px) scale(1.02); z-index: 100 !important; box-shadow: 0 20px 35px -10px rgba(0,0,0,0.15); }
    .appt-box:active { transform: scale(0.98); }

    .appt-content { height: 100%; border-left: 6px solid var(--status-color); padding: 10px 12px; display: flex; flex-direction: column; gap: 4px; }

    .t-row { display: flex; justify-content: space-between; align-items: center; }
    .tm { font-size: 11px; font-weight: 900; color: #1e293b; }

    .indicators { display: flex; align-items: center; gap: 6px; }
    .st-dot { width: 8px; height: 8px; border-radius: 50%; transition: background 0.3s; }
    .note-icon { font-size: 10px; opacity: 0.8; }

    .cl { font-size: 13px; font-weight: 850; color: #0f172a; line-height: 1.2; word-break: break-word; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
    .sv { font-size: 10px; color: #64748b; font-weight: 750; text-transform: uppercase; letter-spacing: 0.3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
