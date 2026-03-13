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

    // ГАРМОНИЧНЫЕ МОБИЛЬНЫЕ РАЗМЕРЫ
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

    function updateLayout() {
        STAFF_WIDTH = Math.floor((window.innerWidth - TIME_COL_WIDTH) / 3);
    }

    onMount(async () => {
        updateLayout();
        window.addEventListener('resize', updateLayout);
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
        if (!currentBranch) return;
        const tz = currentBranch.timezone;
        const formatter = new Intl.DateTimeFormat('sv-SE', { timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit' });
        if (formatter.format(currentTime) === formatter.format(day)) {
            nowLinePos = timeUtils.getTimeOffset(currentTime.toISOString(), startHour, HOUR_HEIGHT, tz);
            branchTime = timeUtils.formatTime(currentTime.toISOString(), tz);
        } else nowLinePos = -1;
    }

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
    <div class="scroll-canvas" style="width: {(staff.length + (apptsByStaff['unassigned'].length > 0 ? 1 : 0)) * STAFF_WIDTH + TIME_COL_WIDTH}px">

        <!-- СТЕКИРУЕМЫЕ ЛИПКИЕ ЭЛЕМЕНТЫ -->
        <div class="sticky-top-left" style="width: {TIME_COL_WIDTH}px">🕒</div>

        <header class="staff-header-sticky">
            <div class="staff-row">
                {#each staff as s (s.id)}
                    <button class="staff-cell btn-reset" style="width: {STAFF_WIDTH}px" on:click={() => dispatch('staffTap', s)}>
                        <div class="avatar-wrap">
                            <div class="avatar" class:is-off={s.dayOff}>{s.name.charAt(0)}</div>
                        </div>
                        <div class="meta">
                            <span class="n">{s.name}</span>
                            <span class="s">{s.specialty || 'Специалист'}</span>
                        </div>
                    </button>
                {/each}
                {#if apptsByStaff['unassigned'].length > 0}
                    <div class="staff-cell unassigned" style="width: {STAFF_WIDTH}px">
                        <div class="avatar">?</div>
                        <div class="meta"><span class="n">...</span></div>
                    </div>
                {/if}
            </div>
        </header>

        <aside class="time-axis-sticky" style="width: {TIME_COL_WIDTH}px">
            {#each hours as h}
                <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                    <span class="h-label">{h}:00</span>
                </div>
            {/each}
            <TimelineNowIndicator {nowLinePos} label={branchTime} mode="dot" />
        </aside>

        <main class="grid-body">
            <div class="cols-container">
                {#each [...staff, ...(apptsByStaff['unassigned'].length > 0 ? [{id: null}] : [])] as s ( (s.id || 'unassigned') )}
                    <div class="staff-col" style="width: {STAFF_WIDTH}px">
                        {#each Array(hours.length * 4) as _, i}
                            {@const h = hours[Math.floor(i/4)]}
                            {@const m = (i%4)*15}
                            {@const status = timeUtils.getSlotStatus(s.id ? s : null, h, m)}
                            <button class="slot-btn"
                                    class:is-break={status === 'BREAK'}
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

    .mobile-timeline-unified {
        height: 100%; width: 100%;
        overflow: auto;
        -webkit-overflow-scrolling: touch;
        scrollbar-width: none;
        scroll-snap-type: x mandatory;
        scroll-padding-left: 48px; /* TIME_COL_WIDTH */
        background: #fdf6e3;
    }
    .mobile-timeline-unified::-webkit-scrollbar { display: none; }

    .scroll-canvas { display: grid; grid-template-areas: "corner header" "axis grid"; grid-template-columns: auto 1fr; position: relative; }

    .sticky-top-left { grid-area: corner; position: sticky; top: 0; left: 0; z-index: 500; background: #eee8d5; border-right: 1.5px solid #ddd6c1; border-bottom: 1.5px solid #ddd6c1; height: 60px; display: flex; align-items: center; justify-content: center; color: #93a1a1; font-size: 14px; }
    .staff-header-sticky { grid-area: header; position: sticky; top: 0; z-index: 400; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; height: 60px; }
    .time-axis-sticky { grid-area: axis; position: sticky; left: 0; z-index: 300; background: #eee8d5; border-right: 1.5px solid #ddd6c1; }
    .grid-body { grid-area: grid; position: relative; background: #fdf6e3; }

    .staff-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 8px; gap: 8px; border-right: 1px solid #ddd6c1; overflow: hidden; }

    .avatar-wrap { flex-shrink: 0; }
    .avatar { width: 32px; height: 32px; background: var(--primary-gradient); color: white; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 13px; }

    .meta { display: flex; flex-direction: column; gap: 1px; min-width: 0; overflow: hidden; }
    .n { display: block; font-size: 11px; font-weight: 850; color: #073642; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .s { display: block; font-size: 8px; color: #93a1a1; font-weight: 700; text-transform: uppercase; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .cols-container { display: flex; height: 100%; }
    .staff-col { position: relative; height: 100%; border-right: 1.5px solid #ddd6c1; flex-shrink: 0; scroll-snap-align: start; scroll-snap-stop: always; }

    .slot-btn { width: 100%; border: none; cursor: pointer; display: block; background: #fdf6e3; }
    .slot-btn.zebra { background: #f5efdc; }
    .slot-btn.is-break { background: #f5efdc !important; border-left: 3px solid #b58900; }

    .grid-lines { position: absolute; inset: 0; pointer-events: none; z-index: 50; }
    .l { position: absolute; left: 0; right: 0; height: 1px; }
    .l.bold { background: #ddd6c1; height: 1.5px; opacity: 0.6; }
    .l.dashed { border-top: 1px dashed #ddd6c1; height: 0; opacity: 0.3; }
</style>
