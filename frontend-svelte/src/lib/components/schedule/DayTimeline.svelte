<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import { timeSyncService } from '$lib/services/timeSyncService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchService } from '$lib/services/branchService.js';
    import TimelineAppointment from './TimelineAppointment.svelte';
    import TimelineNowIndicator from './TimelineNowIndicator.svelte';
    import TimelineCursorGuide from './TimelineCursorGuide.svelte';
    import { fade } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    export let day = new Date();
    export let appointments = [];
    export let staff = [];

    // ГАРМОНИЧНЫЕ КОНСТАНТЫ (Золотое сечение и сетка 8px)
    let HOUR_HEIGHT = 120;
    let STAFF_WIDTH = 200;
    let TIME_COL_WIDTH = 64;
    let SLOT_HEIGHT = HOUR_HEIGHT / 4;

    let startHour = 8, endHour = 22, hours = [];
    let currentTime = new Date(), nowLinePos = -1, branchTime = "";
    let currentBranch = null;

    let scrollHeader, scrollBody, gridCanvas;
    let isDown = false, isTouch = false, startX, scrollLeft;
    let hoverY = -1, hoverTimeStr = "", showGuide = false;

    // Группировка записей для оптимизации (как в прошлых шагах)
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

    function updateLayoutSizes() {
        const width = window.innerWidth;
        if (width < 768) {
            TIME_COL_WIDTH = 48; // Компактная колонка времени
            STAFF_WIDTH = (width - TIME_COL_WIDTH) / 3;
            HOUR_HEIGHT = 80; // Гармоничная высота для мобильных (плотнее)
        } else {
            TIME_COL_WIDTH = 64;
            STAFF_WIDTH = 180; // Чуть уже для элегантности
            HOUR_HEIGHT = 112; // Пропорция ~1.6 к ширине
        }
        SLOT_HEIGHT = HOUR_HEIGHT / 4;
    }

    onMount(async () => {
        updateLayoutSizes();
        window.addEventListener('resize', updateLayoutSizes);
        await timeSyncService.sync();
        await fetchBranchData();
        const timer = setInterval(() => {
            currentTime = timeSyncService.getNow();
            updateNowPosition();
        }, 30000);
        updateNowPosition();
        setTimeout(scrollToCurrentTime, 600);
        return () => {
            window.removeEventListener('resize', updateLayoutSizes);
            clearInterval(timer);
        };
    });

    async function fetchBranchData() {
        if (!$activeBranchId) return;
        try {
            const branches = await branchService.getBranches();
            currentBranch = branches.find(b => b.id === $activeBranchId);
        } catch (e) { /* error logic */ }
    }

    function updateNowPosition() {
        if (!currentBranch) return;
        const tz = currentBranch.timezone;
        const formatter = new Intl.DateTimeFormat('sv-SE', { timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit' });
        if (formatter.format(currentTime) === formatter.format(day)) {
            nowLinePos = timeUtils.getTimeOffset(currentTime.toISOString(), startHour, HOUR_HEIGHT, tz);
            branchTime = timeUtils.formatTime(currentTime.toISOString(), tz);
        } else {
            nowLinePos = -1;
        }
    }

    $: if ($activeBranchId) fetchBranchData();
    $: if (day || startHour || currentBranch) updateNowPosition();

    function handleStart(e) {
        isTouch = e.type === 'touchstart';
        isDown = true;
        const pageX = e.pageX || (e.touches ? e.touches[0].pageX : 0);
        startX = pageX - scrollBody.offsetLeft;
        scrollLeft = scrollBody.scrollLeft;
    }

    function handleMove(e) {
        if (!isTouch && gridCanvas) {
            const rect = gridCanvas.getBoundingClientRect();
            const y = (e.clientY || e.touches?.[0].clientY) - rect.top;
            if (y >= 0 && y <= rect.height) {
                hoverY = y;
                const totalMins = (y / (HOUR_HEIGHT / 60));
                const h = Math.floor(totalMins / 60) + startHour;
                const m = Math.floor(totalMins % 60);
                hoverTimeStr = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
                if (!isDown) showGuide = true;
            } else showGuide = false;
        }
        if (!isDown || isTouch) return;
        e.preventDefault();
        const walk = ((e.pageX || e.touches[0].pageX) - scrollBody.offsetLeft - startX) * 1.5;
        scrollBody.scrollLeft = scrollLeft - walk;
    }

    function syncScroll(e) { if (scrollHeader && e.target === scrollBody) scrollHeader.scrollLeft = scrollBody.scrollLeft; }
    function syncHeaderScroll(e) { if (scrollBody && e.target === scrollHeader) scrollBody.scrollLeft = scrollHeader.scrollLeft; }

    function scrollToCurrentTime() {
        if (scrollBody && nowLinePos > 0) {
            const target = nowLinePos - (scrollBody.clientHeight / 3);
            scrollBody.scrollTo({ top: Math.max(0, target), behavior: 'smooth' });
        }
    }

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

<div class="timeline-root" on:mouseleave={() => showGuide = false}>
    <header class="staff-header-fixed">
        <div class="time-corner-empty" style="width: {TIME_COL_WIDTH}px">🕒</div>
        <div class="staff-scroll-area" bind:this={scrollHeader} on:scroll={syncHeaderScroll}>
            <div class="staff-inner-row" style="width: {(staff.length + (apptsByStaff['unassigned'].length > 0 ? 1 : 0)) * STAFF_WIDTH}px">
                {#each staff as s (s.id)}
                    <button class="staff-cell btn-reset" style="width: {STAFF_WIDTH}px" on:click={() => dispatch('staffTap', s)}>
                        <div class="avatar" class:is-off={s.dayOff}>{s.name.charAt(0)}</div>
                        <div class="meta">
                            <span class="n">{s.name.split(' ')[0]}</span>
                            <span class="s">{s.workStartTime?.slice(0,5)}</span>
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
        </div>
    </header>

    <div class="timeline-body-scroll"
         bind:this={scrollBody}
         on:scroll={syncScroll}
         on:mousedown={handleStart}
         on:mousemove={handleMove}
         on:mouseup={() => isDown = false}
         on:mouseleave={() => isDown = false}
         on:touchstart={handleStart}
         on:touchmove={handleMove}
         on:touchend={() => isDown = false}
         class:grabbing={isDown && !isTouch}>

        <div class="body-layout-wrapper" style="width: {(staff.length + (apptsByStaff['unassigned'].length > 0 ? 1 : 0)) * STAFF_WIDTH + TIME_COL_WIDTH}px">
            <div class="time-axis-col" style="width: {TIME_COL_WIDTH}px">
                {#each hours as h (h)}
                    <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                        <span class="h-label">{h}:00</span>
                    </div>
                {/each}
                <TimelineNowIndicator {nowLinePos} label={branchTime} mode="dot" />
            </div>

            <div class="grid-canvas" bind:this={gridCanvas}>
                <div class="columns-container">
                    {#each [...staff, ...(apptsByStaff['unassigned'].length > 0 ? [{id: null}] : [])] as s ( (s.id || 'unassigned') )}
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

                <div class="grid-lines-overlay">
                    {#each Array(hours.length * 4) as _, i}
                        {@const isHour = i % 4 === 0}
                        <div class="l" class:bold={isHour} class:dashed={!isHour} style="top: {i * SLOT_HEIGHT}px"></div>
                    {/each}
                </div>
                <TimelineNowIndicator {nowLinePos} mode="line" />
            </div>
        </div>
    </div>
</div>

<style>
    * { box-sizing: border-box; }
    .btn-reset { background: none; border: none; padding: 0; margin: 0; text-align: left; cursor: pointer; }

    .timeline-root { height: 100%; display: flex; flex-direction: column; background: #fdf6e3; overflow: hidden; }

    .staff-header-fixed { display: flex; height: 64px; background: #eee8d5; z-index: 300; border-bottom: 1.5px solid #ddd6c1; flex-shrink: 0; }
    .time-corner-empty { display: flex; align-items: center; justify-content: center; color: #93a1a1; font-size: 14px; border-right: 1.5px solid #ddd6c1; background: #eee8d5; z-index: 310; }

    .staff-scroll-area { flex: 1; overflow-x: auto; scrollbar-width: none; -ms-overflow-style: none; }
    .staff-scroll-area::-webkit-scrollbar { display: none; }

    .staff-inner-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 8px; gap: 8px; border-right: 1px solid #ddd6c1; }

    .avatar { width: 32px; height: 32px; background: var(--primary-gradient); color: white; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 13px; }
    .n { display: block; font-size: 12px; font-weight: 850; color: #073642; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .s { display: block; font-size: 9px; color: #93a1a1; font-weight: 700; }

    .timeline-body-scroll { flex: 1; overflow: auto; position: relative; -webkit-overflow-scrolling: touch; }
    .body-layout-wrapper { display: flex; min-height: 100%; position: relative; }

    .time-axis-col { flex-shrink: 0; background: #eee8d5; border-right: 1.5px solid #ddd6c1; position: sticky; left: 0; z-index: 200; }
    .hour-cell { position: relative; }
    .h-label { position: absolute; top: 0; left: 50%; transform: translate(-50%, -50%); font-size: 9px; font-weight: 900; color: #586e75; background: #fdf6e3; padding: 1px 4px; border-radius: 4px; border: 1px solid #ddd6c1; }

    .grid-canvas { position: relative; flex: 1; background: #fdf6e3; }
    .columns-container { display: flex; height: 100%; }
    .staff-col { position: relative; height: 100%; border-right: 1.5px solid #ddd6c1; flex-shrink: 0; }

    .slot-btn { width: 100%; border: none; cursor: pointer; display: block; background: #fdf6e3; }
    .slot-btn.zebra { background: #f5efdc; }
    .slot-btn.is-off { background-color: #eee8d5 !important; opacity: 0.3; }
    .slot-btn.is-break { background: #f5efdc !important; border-left: 3px solid #b58900; }

    .grid-lines-overlay { position: absolute; inset: 0; pointer-events: none; z-index: 50; }
    .l { position: absolute; left: 0; right: 0; height: 1px; }
    .l.bold { background: #ddd6c1; height: 1.5px; opacity: 0.6; }
    .l.dashed { border-top: 1px dashed #ddd6c1; height: 0; opacity: 0.3; }

    @media (max-width: 768px) {
        .staff-scroll-area, .timeline-body-scroll { scroll-snap-type: x mandatory; }
        .staff-cell, .staff-col { scroll-snap-align: start; }
    }
</style>
