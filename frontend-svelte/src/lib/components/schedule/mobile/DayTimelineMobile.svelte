<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import { timeSyncService } from '$lib/services/timeSyncService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchService } from '$lib/services/branchService.js';
    import TimelineAppointment from '../TimelineAppointment.svelte';
    import TimelineNowIndicator from '../TimelineNowIndicator.svelte';
    import { fade } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    export let day = new Date();
    export let appointments = [];
    export let staff = [];

    let TIME_COL_WIDTH = 48;
    let HOUR_HEIGHT = 72;
    let SLOT_HEIGHT = HOUR_HEIGHT / 4;
    let STAFF_WIDTH = 0;

    let startHour = 8, endHour = 22, hours = [];
    let currentTime = new Date(), nowLinePos = -1, branchTime = "";
    let currentBranch = null;
    let mainScroll;

    $: apptsByStaff = (() => {
        const map = { 'unassigned': [] };
        staff.forEach(s => map[s.id] = []);
        appointments.forEach(a => {
            const sid = a.staffMemberId;
            if (sid && map[sid]) map[sid].push(a);
            else map['unassigned'].push(a);
        });
        return map;
    })();

    // РЕАКТИВНЫЙ РАСЧЕТ ВЕРСТКИ (Золотое сечение 1.618)
    $: {
        const width = typeof window !== 'undefined' ? window.innerWidth : 375;
        const availableWidth = width - TIME_COL_WIDTH;
        const totalCols = staff.length + (apptsByStaff['unassigned']?.length > 0 ? 1 : 0);

        if (totalCols <= 1) {
            STAFF_WIDTH = availableWidth;
            HOUR_HEIGHT = Math.floor(STAFF_WIDTH / 1.618);
            if (HOUR_HEIGHT > 120) HOUR_HEIGHT = 120;
        } else if (totalCols === 2) {
            STAFF_WIDTH = Math.floor(availableWidth / 2);
            HOUR_HEIGHT = Math.floor(STAFF_WIDTH / 1.3);
        } else {
            STAFF_WIDTH = Math.floor(availableWidth / 3);
            HOUR_HEIGHT = Math.floor(STAFF_WIDTH / 1.618);
        }
        SLOT_HEIGHT = HOUR_HEIGHT / 4;
    }

    onMount(async () => {
        await timeSyncService.sync();
        await fetchBranchData();
        const timer = setInterval(() => {
            currentTime = timeSyncService.getNow();
            updateNowPosition();
        }, 30000);
        updateNowPosition();
        setTimeout(() => {
            if (mainScroll && nowLinePos > 0) {
                mainScroll.scrollTo({ top: nowLinePos - (mainScroll.clientHeight / 3), behavior: 'smooth' });
            }
        }, 600);
        return () => {
            window.removeEventListener('resize', updateLayout);
            clearInterval(timer);
        };
    });

    async function fetchBranchData() {
        if (!$activeBranchId) return;
        try {
            const branches = await branchService.getBranches();
            currentBranch = branches.find(b => b.id === $activeBranchId);
        } catch (e) {}
    }

    function updateNowPosition() {
        if (!currentBranch) {
            nowLinePos = -1; // ✅ Явно скрываем линию если нет филиала
            branchTime = "";
            return;
        }
        const tz = currentBranch.timezone;
        const formatter = new Intl.DateTimeFormat('sv-SE', { timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit' });
        if (formatter.format(currentTime) === formatter.format(day)) {
            nowLinePos = timeUtils.getTimeOffset(currentTime.toISOString(), startHour, HOUR_HEIGHT, tz);
            branchTime = timeUtils.formatTime(currentTime.toISOString(), tz);
        } else {
            nowLinePos = -1;
            branchTime = "";
        }
    }

    $: if ($activeBranchId) fetchBranchData();
    $: if (day || startHour || currentBranch) updateNowPosition();

    $: {
        let minH = 9, maxH = 20;
        appointments.forEach(a => {
            const h = new Date(a.startTime).getHours();
            minH = Math.min(minH, h);
            maxH = Math.max(maxH, h + 1);
        });
        staff.forEach(s => {
            if (s.workStartTime) minH = Math.min(minH, parseInt(s.workStartTime.split(':')[0]));
            if (s.workEndTime) maxH = Math.max(maxH, parseInt(s.workEndTime.split(':')[0]));
        });
        startHour = Math.max(0, minH - 1);
        endHour = Math.min(24, maxH + 1);
        hours = Array.from({length: endHour - startHour + 1}, (_, i) => startHour + i);
    }

    function handleEmptySlotClick(staffId, hour, minute, status) {
        if (status === 'OFF' || status === 'BREAK') return;
        dispatch('emptySlotTap', { hour, min: minute, staffId });
    }
</script>

<div class="mobile-timeline-unified" bind:this={mainScroll}>
    <div class="scroll-canvas" style="width: {(staff.length + (apptsByStaff['unassigned']?.length > 0 ? 1 : 0)) * STAFF_WIDTH + TIME_COL_WIDTH}px">

        <div class="sticky-top-left" style="width: {TIME_COL_WIDTH}px">🕒</div>

        <header class="staff-header-sticky">
            <div class="staff-row">
                {#each staff as s (s.id)}
                    <button class="staff-cell btn-reset" class:is-off={s.dayOff} style="width: {STAFF_WIDTH}px" on:click={() => dispatch('staffTap', s)}>
                        <div class="avatar-wrap">
                            {#if s.photoData}
                                <img class="avatar" class:is-off={s.dayOff} src="data:image/jpeg;base64,{s.photoData}" alt={s.name} />
                            {:else}
                                <div class="avatar" class:is-off={s.dayOff}>{s.name ? s.name.charAt(0) : '?'}</div>
                            {/if}
                        </div>
                        <div class="meta">
                            <span class="n">{s.name}</span>
                            <span class="s">{s.specialty || 'Специалист'}</span>
                        </div>
                    </button>
                {/each}
                {#if apptsByStaff['unassigned']?.length > 0}
                    <div class="staff-cell unassigned" style="width: {STAFF_WIDTH}px">
                        <div class="avatar">?</div>
                        <div class="meta"><span class="n">...</span></div>
                    </div>
                {/if}
            </div>
        </header>

        <aside class="time-axis-sticky" style="width: {TIME_COL_WIDTH}px">
            {#each hours as h (h)}
                <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                    <span class="h-label">{h}:00</span>
                </div>
            {/each}
            <TimelineNowIndicator {nowLinePos} label={branchTime} mode="dot" />
        </aside>

        <main class="grid-body">
            <div class="cols-container">
                {#each [...staff, ...(apptsByStaff['unassigned']?.length > 0 ? [{id: null}] : [])] as s ( (s.id || 'unassigned') )}
                    <div class="staff-col" style="width: {STAFF_WIDTH}px">
                        {#each Array(hours.length * 4) as _, i}
                            {@const h = hours[Math.floor(i/4)]}
                            {@const m = (i%4)*15}
                            {@const status = timeUtils.getSlotStatus(s.id ? s : null, h, m)}
                            <button class="slot-btn"
                                    class:is-break={status === 'BREAK'}
                                    class:is-off={status === 'OFF'}
                                    class:zebra={h % 2 === 0}
                                    style="height: {SLOT_HEIGHT}px"
                                    on:click|stopPropagation={() => handleEmptySlotClick(s.id, h, m, status)}>
                            </button>
                        {/each}
                        {#each apptsByStaff[s.id || 'unassigned'] || [] as appt (appt.id)}
                            <TimelineAppointment {appt} {startHour} hourHeight={HOUR_HEIGHT} timezone={currentBranch?.timezone} on:click={(e) => dispatch('appointmentTap', e.detail)} />
                        {/each}
                    </div>
                {/each}
            </div>
            <div class="grid-lines">
                {#each Array(hours.length * 4) as _, i}
                    {@const isHour = i % 4 === 0}
                    <div class="l" class:bold={isHour} class:dashed={!isHour} style="top: {i * SLOT_HEIGHT}px"></div>
                {/each}
            </div>
            <TimelineNowIndicator {nowLinePos} mode="line" />
        </main>
    </div>
</div>

<style>
    * { box-sizing: border-box; }
    .btn-reset { background: none; border: none; padding: 0; margin: 0; text-align: left; cursor: pointer; font-family: inherit; }
    .mobile-timeline-unified { height: 100%; width: 100%; overflow-x: auto; overflow-y: hidden; -webkit-overflow-scrolling: touch; scrollbar-width: none; background: #fdf6e3; }
    .mobile-timeline-unified::-webkit-scrollbar { display: none; }
    .scroll-canvas { display: grid; grid-template-areas: "corner header" "axis grid"; position: relative; min-height: 100%; }
    .sticky-top-left { grid-area: corner; position: sticky; top: 0; left: 0; z-index: 500; background: #eee8d5; border-right: 1.5px solid #ddd6c1; border-bottom: 1.5px solid #ddd6c1; height: 60px; display: flex; align-items: center; justify-content: center; color: #93a1a1; font-size: 14px; }
    .staff-header-sticky { grid-area: header; position: sticky; top: 0; z-index: 400; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; height: 60px; }
    .time-axis-sticky { grid-area: axis; position: sticky; top: 0; left: 0; z-index: 410; background: #eee8d5; border-right: 1.5px solid #ddd6c1; }
    .grid-body { grid-area: grid; position: relative; background: #fdf6e3; overflow-y: auto; -webkit-overflow-scrolling: touch; }
    .staff-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 8px; gap: 8px; border-right: 1px solid #ddd6c1; overflow: hidden; transition: opacity 0.2s; }
    .staff-cell.is-off { opacity: 0.5; background: #eee8d5; }
    .staff-cell.is-off .n, .staff-cell.is-off .s { color: #93a1a1; }
    .avatar { width: 32px; height: 32px; background: var(--primary-gradient); color: white; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 13px; object-fit: cover; }
    img.avatar { background: #ddd; }
    .avatar.is-off { background: #93a1a1; }
    .meta { display: flex; flex-direction: column; gap: 1px; min-width: 0; overflow: hidden; }
    .n { display: block; font-size: 11px; font-weight: 850; color: #073642; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .s { display: block; font-size: 8px; color: #93a1a1; font-weight: 700; text-transform: uppercase; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .hour-cell { position: relative; }
    .h-label { position: absolute; top: 0; left: 50%; transform: translate(-50%, -50%); font-size: 9px; font-weight: 900; color: #586e75; background: #fdf6e3; padding: 1px 4px; border-radius: 4px; border: 1px solid #ddd6c1; }
    .cols-container { display: flex; height: 100%; }
    .staff-col { position: relative; height: 100%; border-right: 1.5px solid #ddd6c1; flex-shrink: 0; }
    .slot-btn { width: 100%; border: none; display: block; background: #fdf6e3; }
    .slot-btn.zebra { background: #f5efdc; }
    .slot-btn.is-off { background-color: #eee8d5 !important; background-image: repeating-linear-gradient(45deg, transparent, transparent 10px, rgba(147, 161, 161, 0.05) 10px, rgba(147, 161, 161, 0.05) 20px) !important; opacity: 0.6; }
    .slot-btn.is-break { background: #f5efdc !important; border-left: 3px solid #b58900; }
    .grid-lines { position: absolute; inset: 0; pointer-events: none; z-index: 50; }
    .l { position: absolute; left: 0; right: 0; height: 1px; }
    .l.bold { background: #ddd6c1; height: 1.5px; opacity: 0.6; }
    .l.dashed { border-top: 1px dashed #ddd6c1; height: 0; opacity: 0.3; }
</style>
