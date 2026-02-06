<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';

    const dispatch = createEventDispatcher();

    export let day = new Date();
    export let appointments = [];
    export let staff = [];

    // Размеры
    const HOUR_HEIGHT = 100;
    const SLOT_HEIGHT = HOUR_HEIGHT / 4;
    const STAFF_WIDTH = 160;
    const TIME_COL_WIDTH = 60;

    let startHour = 8;
    let endHour = 22;
    let hours = [];
    let nowLinePos = -1;
    let timer;

    let scrollHeader;
    let scrollBody;

    // Реактивный расчет шкалы при изменении данных
    $: {
        let minH = 8;
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
        for (let i = startHour; i <= endHour; i++) {
            hours.push(i);
        }
    }

    function syncScroll(e) {
        if (scrollHeader && e.target === scrollBody) {
            scrollHeader.scrollLeft = scrollBody.scrollLeft;
        }
    }

    onMount(() => {
        updateNowLine();
        timer = setInterval(updateNowLine, 60000);
        setTimeout(scrollToCurrentTime, 300);
    });

    onDestroy(() => clearInterval(timer));

    function updateNowLine() {
        const now = new Date();
        if (now.toDateString() === day.toDateString()) {
            const currentMins = now.getHours() * 60 + now.getMinutes();
            const startMins = startHour * 60;
            nowLinePos = (currentMins - startMins) * (HOUR_HEIGHT / 60);
        } else {
            nowLinePos = -1;
        }
    }

    function scrollToCurrentTime() {
        if (scrollBody && nowLinePos > 150) {
            scrollBody.scrollTo({ top: nowLinePos - 150, behavior: 'smooth' });
        }
    }

    function getApptStyle(appt) {
        const start = new Date(appt.startTime);
        const minsFromStart = (start.getHours() - startHour) * 60 + start.getMinutes();
        const top = minsFromStart * (HOUR_HEIGHT / 60);
        const height = appt.durationInMinutes * (HOUR_HEIGHT / 60);
        return `top: ${top}px; height: ${height - 2}px;`;
    }

    function getStatusColor(status) {
        const colors = {
            'SCHEDULED': '#3b82f6',
            'CONFIRMED': '#10b981',
            'NEEDS_CALL': '#f59e0b',
            'COMPLETED': '#64748b',
            'CANCELLED': '#ef4444'
        };
        return colors[status] || '#3b82f6';
    }

    function getWorkZoneStyle(member) {
        if (!member.workStartTime || !member.workEndTime) return '';
        const [sH, sM] = member.workStartTime.split(':').map(Number);
        const [eH, eM] = member.workEndTime.split(':').map(Number);
        const startPos = ((sH - startHour) * 60 + sM) * (HOUR_HEIGHT / 60);
        const endPos = ((eH - startHour) * 60 + eM) * (HOUR_HEIGHT / 60);
        return `top: ${startPos}px; height: ${endPos - startPos}px;`;
    }
</script>

<div class="timeline">
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
                <div class="hour-mark" style="height: {HOUR_HEIGHT}px">
                    <span>{h}:00</span>
                </div>
            {/each}
        </div>

        <div class="main-grid" style="width: {staff.length * STAFF_WIDTH}px; height: {hours.length * HOUR_HEIGHT}px">
            <div class="off-hours-layer">
                {#each staff as s, i}
                    <div class="column-bg" style="left: {i * STAFF_WIDTH}px; width: {STAFF_WIDTH}px">
                        {#if !s.dayOff}
                            <div class="work-zone" style={getWorkZoneStyle(s)}></div>
                        {/if}
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
                            <!-- ИСПРАВЛЕНО: Теперь используем dispatch для открытия окна создания -->
                            <button class="slot-trigger"
                                 style="height: {SLOT_HEIGHT}px"
                                 on:click={() => dispatch('emptySlotTap', { hour: hours[Math.floor(i/4)], min: (i%4)*15, staffId: s.id })}>
                            </button>
                        {/each}

                        {#each appointments.filter(a => a.staffMemberId === s.id) as appt}
                            <!-- ИСПРАВЛЕНО: Теперь используем dispatch для открытия деталей записи -->
                            <div class="appt-card"
                                 style="{getApptStyle(appt)} --status-color: {getStatusColor(appt.status)}"
                                 on:click|stopPropagation={() => dispatch('appointmentTap', appt)}>
                                <div class="appt-inner">
                                    <div class="appt-head">
                                        <span class="client">{appt.clientName}</span>
                                        {#if appt.reminderSent}<span class="bell">🔔</span>{/if}
                                    </div>
                                    <span class="service">{appt.service}</span>
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
    .header-row { display: flex; height: 74px; background: white; z-index: 100; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
    .time-label-corner { width: 60px; display: flex; align-items: center; justify-content: center; border-right: 1px solid #f1f5f9; font-size: 18px; }
    .staff-row-scroll { flex: 1; overflow: hidden; }
    .staff-inner { display: flex; height: 100%; }
    .staff-card { flex-shrink: 0; display: flex; align-items: center; padding: 0 12px; gap: 10px; border-right: 1px solid #f1f5f9; }
    .avatar-circle { width: 36px; height: 36px; background: #eff6ff; color: #3b82f6; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    .staff-meta { display: flex; flex-direction: column; min-width: 0; }
    .name { font-size: 13px; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .spec { font-size: 10px; color: #94a3b8; font-weight: 600; text-transform: uppercase; }
    .grid-body { flex: 1; display: flex; overflow: auto; position: relative; }
    .time-axis { flex-shrink: 0; background: white; border-right: 1px solid #f1f5f9; position: sticky; left: 0; z-index: 50; }
    .hour-mark { display: flex; justify-content: center; padding-top: 8px; font-size: 11px; font-weight: 700; color: #94a3b8; }
    .main-grid { position: relative; background: #f8fafc; }
    .off-hours-layer, .lines-layer, .columns-layer { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }
    .column-bg { position: absolute; top: 0; bottom: 0; border-right: 1px solid #f1f5f9; }
    .work-zone { position: absolute; left: 0; right: 0; background: white; }
    .grid-line { position: absolute; left: 0; right: 0; height: 1px; background: #f1f5f9; }
    .grid-line.bold { background: #e2e8f0; }
    .staff-column { position: absolute; top: 0; bottom: 0; }
    .slot-trigger { width: 100%; border: none; background: transparent; cursor: pointer; display: block; }
    .appt-card { position: absolute; left: 6px; right: 6px; background: white; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); z-index: 60; cursor: pointer; overflow: hidden; }
    .appt-inner { height: 100%; border-left: 4px solid var(--status-color); padding: 8px; display: flex; flex-direction: column; gap: 2px; }
    .appt-head { display: flex; justify-content: space-between; align-items: flex-start; }
    .client { font-size: 12px; font-weight: 800; color: #0f172a; line-height: 1.2; }
    .service { font-size: 10px; color: #64748b; font-weight: 500; }
    .time-now { position: absolute; left: 0; right: 0; z-index: 80; pointer-events: none; }
    .time-now .line { height: 2px; background: #ef4444; width: 100%; }
    .time-now .pulse-dot { position: absolute; left: -4px; top: -3px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; animation: pulse 2s infinite; }
</style>
