<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { fade, slide } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    export let day = new Date();
    export let appointments = [];
    export let staff = [];

    const HOUR_HEIGHT = 120;
    const SLOT_HEIGHT = HOUR_HEIGHT / 4;
    const STAFF_WIDTH = 180;
    const TIME_COL_WIDTH = 60;

    let startHour = 8;
    let endHour = 22;
    let hours = [];
    let nowLinePos = -1;
    let timer;

    let scrollHeader;
    let scrollBody;
    let selectedApptId = null; // Для разворачивания карточки

    $: {
        let minH = 9;
        let maxH = 20;
        if (staff.length > 0 || appointments.length > 0) {
            staff.forEach(s => {
                if (s.workStartTime) minH = Math.min(minH, parseInt(s.workStartTime.split(':')[0]));
                if (s.workEndTime) maxH = Math.max(maxH, parseInt(s.workEndTime.split(':')[0]));
            });
            appointments.forEach(a => {
                const start = new Date(a.startTime).getHours();
                const end = Math.ceil((new Date(a.startTime).getTime() + a.durationInMinutes * 60000) / (3600000)) % 24;
                minH = Math.min(minH, start);
                maxH = Math.max(maxH, end);
            });
        }
        startHour = Math.max(0, minH - 1);
        endHour = Math.min(24, maxH + 1);
        hours = [];
        for (let i = startHour; i <= endHour; i++) hours.push(i);
    }

    function syncScroll(e) {
        if (scrollHeader && e.target === scrollBody) scrollHeader.scrollLeft = scrollBody.scrollLeft;
    }

    onMount(() => {
        updateNowLine();
        timer = setInterval(updateNowLine, 60000);
        setTimeout(scrollToCurrentTime, 500);
    });

    onDestroy(() => clearInterval(timer));

    function updateNowLine() {
        const now = new Date();
        if (now.toDateString() === day.toDateString()) {
            const currentMins = now.getHours() * 60 + now.getMinutes();
            nowLinePos = (currentMins - startHour * 60) * (HOUR_HEIGHT / 60);
        } else {
            nowLinePos = -1;
        }
    }

    function scrollToCurrentTime() {
        if (scrollBody && nowLinePos > 200) scrollBody.scrollTo({ top: nowLinePos - 200, behavior: 'smooth' });
    }

    function getApptStyle(appt) {
        const start = new Date(appt.startTime);
        const top = ((start.getHours() - startHour) * 60 + start.getMinutes()) * (HOUR_HEIGHT / 60);
        const isSelected = selectedApptId === appt.id;
        const actualHeight = appt.durationInMinutes * (HOUR_HEIGHT / 60);
        const displayHeight = isSelected ? Math.max(actualHeight, 110) : actualHeight;
        return `top: ${top}px; height: ${displayHeight - 2}px; z-index: ${isSelected ? 100 : 60};`;
    }

    function getStatusData(status) {
        const config = {
            'SCHEDULED': { color: '#3b82f6', label: 'Ожидается', icon: '🕒' },
            'CONFIRMED': { color: '#26a69a', label: 'Подтвержден', icon: '✓' },
            'NEEDS_CALL': { color: '#ffa726', label: 'Нужен звонок', icon: '📞' },
            'COMPLETED': { color: '#94a3b8', label: 'Завершен', icon: '🏁' },
            'CANCELLED': { color: '#ef5350', label: 'Отменен', icon: '✕' }
        };
        return config[status] || config['SCHEDULED'];
    }

    async function updateStatus(appt, newStatus) {
        try {
            await adminService.updateAppointment(appt.id, { ...appt, status: newStatus });
            selectedApptId = null;
            // Список обновится через WebSocket или через родителя
        } catch (e) { alert('Ошибка обновления статуса'); }
    }

    function toggleSelection(id) {
        selectedApptId = selectedApptId === id ? null : id;
    }
</script>

<div class="timeline" on:click={() => selectedApptId = null}>
    <div class="header-row">
        <div class="time-label-corner">🕒</div>
        <div class="staff-row-scroll" bind:this={scrollHeader}>
            <div class="staff-inner" style="width: {staff.length * STAFF_WIDTH}px">
                {#each staff as s}
                    <div class="staff-card" style="width: {STAFF_WIDTH}px">
                        <div class="avatar-circle">{s.name.charAt(0)}</div>
                        <div class="staff-meta">
                            <span class="name">{s.name}</span>
                            <span class="spec">{s.specialty}</span>
                        </div>
                    </div>
                {/each}
            </div>
        </div>
    </div>

    <div class="grid-body" on:scroll={syncScroll} bind:this={scrollBody}>
        <div class="time-axis" style="width: {TIME_COL_WIDTH}px">
            {#each hours as h}
                <div class="hour-mark" style="height: {HOUR_HEIGHT}px"><span>{h}:00</span></div>
            {/each}
        </div>

        <div class="main-grid" style="width: {staff.length * STAFF_WIDTH}px; height: {hours.length * HOUR_HEIGHT}px">
            <div class="off-hours-layer">
                {#each staff as s, i}
                    <div class="column-bg" style="left: {i * STAFF_WIDTH}px; width: {STAFF_WIDTH}px">
                        {#if s.dayOff}<div class="day-off-mask"></div>{/if}
                    </div>
                {/each}
            </div>

            <div class="lines-layer">
                {#each Array(hours.length * 4) as _, i}
                    <div class="grid-line" class:bold={i % 4 === 0} style="top: {i * SLOT_HEIGHT}px"></div>
                {/each}
            </div>

            <div class="columns-layer">
                {#each staff as s, sIdx}
                    <div class="staff-column" style="left: {sIdx * STAFF_WIDTH}px; width: {STAFF_WIDTH}px">
                        {#each Array(hours.length * 4) as _, i}
                            <button class="slot-trigger" style="height: {SLOT_HEIGHT}px" on:click={() => dispatch('emptySlotTap', { hour: hours[Math.floor(i/4)], min: (i%4)*15, staffId: s.id })}></button>
                        {/each}

                        {#each appointments.filter(a => a.staffMemberId === s.id) as appt (appt.id)}
                            {@const status = getStatusData(appt.status)}
                            {@const isSelected = selectedApptId === appt.id}
                            <div class="appt-card"
                                 class:selected={isSelected}
                                 style="{getApptStyle(appt)} --status-color: {status.color}"
                                 on:click|stopPropagation={() => toggleSelection(appt.id)}>
                                <div class="appt-inner">
                                    <div class="appt-top">
                                        <span class="time">{new Date(appt.startTime).toLocaleTimeString('ru',{hour:'2-digit',minute:'2-digit'})}</span>
                                        <span class="status-tag">{status.label}</span>
                                    </div>
                                    <div class="appt-client">{appt.clientName}</div>
                                    <div class="appt-service">{appt.service}</div>

                                    {#if isSelected}
                                        <div class="appt-actions" transition:slide>
                                            <button class="act-btn check" on:click|stopPropagation={() => updateStatus(appt, 'CONFIRMED')}>✓</button>
                                            <button class="act-btn call" on:click|stopPropagation={() => updateStatus(appt, 'NEEDS_CALL')}>📞</button>
                                            <button class="act-btn done" on:click|stopPropagation={() => updateStatus(appt, 'COMPLETED')}>🏁</button>
                                            <button class="act-btn cancel" on:click|stopPropagation={() => updateStatus(appt, 'CANCELLED')}>✕</button>
                                            <button class="act-btn info" on:click|stopPropagation={() => dispatch('appointmentTap', appt)}>ℹ</button>
                                        </div>
                                    {/if}
                                </div>
                            </div>
                        {/each}
                    </div>
                {/each}
            </div>

            {#if nowLinePos >= 0}
                <div class="time-now" style="top: {nowLinePos}px">
                    <div class="pulse-dot"></div>
                    <div class="line"></div>
                </div>
            {/if}
        </div>
    </div>
</div>

<style>
    .timeline { height: 100%; display: flex; flex-direction: column; background: #f1f5f9; overflow: hidden; }
    .header-row { display: flex; height: 74px; background: white; z-index: 110; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
    .time-label-corner { width: 60px; display: flex; align-items: center; justify-content: center; border-right: 1px solid #f1f5f9; font-size: 18px; }
    .staff-row-scroll { flex: 1; overflow: hidden; }
    .staff-inner { display: flex; height: 100%; }
    .staff-card { flex-shrink: 0; display: flex; align-items: center; padding: 0 12px; gap: 10px; border-right: 1px solid #f1f5f9; }
    .avatar-circle { width: 36px; height: 36px; background: #eff6ff; color: #3b82f6; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    .name { font-size: 13px; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .spec { font-size: 10px; color: #94a3b8; font-weight: 600; text-transform: uppercase; }

    .grid-body { flex: 1; display: flex; overflow: auto; position: relative; scroll-behavior: smooth; }
    .time-axis { flex-shrink: 0; background: white; border-right: 1px solid #f1f5f9; position: sticky; left: 0; z-index: 105; }
    .hour-mark { display: flex; justify-content: center; padding-top: 8px; font-size: 11px; font-weight: 700; color: #94a3b8; }
    .main-grid { position: relative; background: #f8fafc; }
    .column-bg { position: absolute; top: 0; bottom: 0; border-right: 1px solid #f1f5f9; }
    .day-off-mask { position: absolute; inset: 0; background: repeating-linear-gradient(45deg, #f1f5f9, #f1f5f9 10px, #f8fafc 10px, #f8fafc 20px); opacity: 0.5; }
    .grid-line { position: absolute; left: 0; right: 0; height: 1px; background: #f1f5f9; }
    .grid-line.bold { background: #e2e8f0; }

    .staff-column { position: absolute; top: 0; bottom: 0; }
    .slot-trigger { width: 100%; border: none; background: transparent; cursor: pointer; display: block; }

    .appt-card { position: absolute; left: 4px; right: 4px; background: white; border-radius: 14px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); cursor: pointer; transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); border: 1px solid #f1f5f9; }
    .appt-card.selected { left: -2px; right: -2px; box-shadow: 0 12px 30px rgba(0,0,0,0.2); border-color: var(--status-color); }

    .appt-inner { height: 100%; border-left: 5px solid var(--status-color); padding: 8px; display: flex; flex-direction: column; overflow: hidden; }
    .appt-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
    .time { font-size: 10px; font-weight: 800; color: var(--status-color); }
    .status-tag { font-size: 8px; font-weight: 900; text-transform: uppercase; color: white; background: var(--status-color); padding: 1px 5px; border-radius: 4px; }

    .appt-client { font-size: 12px; font-weight: 800; color: #0f172a; line-height: 1.2; word-break: break-word; }
    .appt-service { font-size: 10px; color: #64748b; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }

    .appt-actions { margin-top: auto; padding-top: 8px; display: flex; justify-content: space-between; gap: 4px; }
    .act-btn { flex: 1; height: 32px; border-radius: 8px; border: none; color: white; font-weight: bold; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 14px; }
    .act-btn.check { background: #26a69a; }
    .act-btn.call { background: #ffa726; }
    .act-btn.done { background: #64748b; }
    .act-btn.cancel { background: #ef5350; }
    .act-btn.info { background: #3b82f6; }

    .time-now { position: absolute; left: 0; right: 0; z-index: 120; pointer-events: none; }
    .time-now .line { height: 2px; background: #ef4444; width: 100%; box-shadow: 0 0 8px rgba(239, 68, 68, 0.4); }
    .time-now .pulse-dot { position: absolute; left: -4px; top: -3px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; box-shadow: 0 0 10px #ef4444; animation: pulse 2s infinite; }
    @keyframes pulse { 0% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.5); opacity: 0.5; } 100% { transform: scale(1); opacity: 1; } }
</style>
