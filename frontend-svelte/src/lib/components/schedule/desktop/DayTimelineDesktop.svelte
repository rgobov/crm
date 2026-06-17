<script context="module">
    // Глобальный кэш форматировщиков для исключения нагрузки на GC
    const dayFormatterMap = new Map();
    const hourFormatterMap = new Map();

    function getFormatters(tz) {
        if (!dayFormatterMap.has(tz)) {
            dayFormatterMap.set(tz, new Intl.DateTimeFormat('sv-SE', { timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit' }));
            hourFormatterMap.set(tz, new Intl.DateTimeFormat('en-GB', { hour: '2-digit', hour12: false, timeZone: tz }));
        }
        return { dayF: dayFormatterMap.get(tz), hourF: hourFormatterMap.get(tz) };
    }
</script>

<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import { timeSyncService } from '$lib/services/timeSyncService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchService } from '$lib/services/branchService.js';
    import TimelineAppointment from '../TimelineAppointment.svelte';
    import TimelineNowIndicator from '../TimelineNowIndicator.svelte';
    import TimelineCursorGuide from '../TimelineCursorGuide.svelte';
    import { fade } from 'svelte/transition';

const dispatch = createEventDispatcher();

    export let day = new Date();
    export let appointments = [];
    export let staff = [];

    let HOUR_HEIGHT = 124;
    let STAFF_WIDTH = 200;
    let TIME_COL_WIDTH = 75;
    let SLOT_HEIGHT = HOUR_HEIGHT / 4;

    let startHour = 8, endHour = 22, hours = [];
    let currentTime = new Date(), nowLinePos = -1, branchTime = "";
    let currentBranch = null;
    let isBranchLoading = false;
    let isDestroyed = false;

    let scrollHeader, scrollBody, gridCanvas;
    let isDown = false, startX, scrollLeft;
    let hoverY = -1, hoverTimeStr = "", showGuide = false;

    let containerWidth = 800;
    let refreshKey = 0;

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

    $: STAFF_WIDTH = 200;
    $: maxCols = Math.floor((containerWidth - TIME_COL_WIDTH) / STAFF_WIDTH);
    $: actualCols = staff.length + (apptsByStaff['unassigned']?.length > 0 ? 1 : 0);
    $: placeholderColsCount = actualCols < maxCols ? (maxCols - actualCols + 1) : 0;
    $: totalColsCount = actualCols + placeholderColsCount;

    let timer;
    let scrollTimeout;
    onMount(async () => {
        await timeSyncService.sync();
        if (isDestroyed) return;

        await fetchBranchData();
        if (isDestroyed) return;

        updateNowPosition();

        timer = setInterval(() => {
            if (isDestroyed) {
                clearInterval(timer);
                return;
            }
            currentTime = timeSyncService.getNow();
            updateNowPosition();
        }, 500);

        updateNowPosition();
        scrollTimeout = setTimeout(() => {
            if (!isDestroyed && scrollBody) scrollToCurrentTime();
        }, 600);
    });

    onDestroy(() => {
        isDestroyed = true;
        if (timer) clearInterval(timer);
        if (scrollTimeout) clearTimeout(scrollTimeout);
    });

    async function fetchBranchData() {
        if (!$activeBranchId) return;
        isBranchLoading = true;
        try {
            const branches = await branchService.getBranches();
            if (isDestroyed) return;
            currentBranch = branches.find(b => b.id === $activeBranchId);
        } catch (e) {
        } finally {
            isBranchLoading = false;
            if (!isDestroyed) updateNowPosition();
        }
    }

    function updateNowPosition() {
        if (!currentBranch || isBranchLoading || isDestroyed) {
            nowLinePos = -1;
            branchTime = "";
            return;
        }

        const tz = currentBranch.timezone;
        const { dayF, hourF } = getFormatters(tz);

        const isToday = dayF.format(currentTime) === dayF.format(day);

        if (isToday) {
            const currentHourInTz = parseInt(hourF.format(currentTime));

            if (currentHourInTz >= startHour && currentHourInTz < endHour) {
                const pos = timeUtils.getTimeOffset(currentTime.toISOString(), startHour, HOUR_HEIGHT, tz);
                const maxPos = (endHour - startHour) * HOUR_HEIGHT;

                if (pos < 0 || pos > maxPos) {
                    nowLinePos = -1;
                    branchTime = "";
                } else {
                    nowLinePos = pos;
                    branchTime = timeUtils.formatTime(currentTime.toISOString(), tz);
                }
            } else {
                nowLinePos = -1;
                branchTime = "";
            }
        } else {
            nowLinePos = -1;
            branchTime = "";
        }
    }

    $: if ($activeBranchId) {
        nowLinePos = -1;
        currentBranch = null;
        isBranchLoading = true;
        fetchBranchData();
    }
    $: if (day || startHour || currentBranch) updateNowPosition();

    function handleStart(e) { isDown = true; startX = e.pageX - scrollBody.offsetLeft; scrollLeft = scrollBody.scrollLeft; }
    function handleMove(e) {
        if (gridCanvas) {
            const rect = gridCanvas.getBoundingClientRect();
            const y = e.clientY - rect.top;
            if (y >= 0 && y <= rect.height) {
                hoverY = y;
                const totalMins = (y / (HOUR_HEIGHT / 60));
                const h = Math.floor(totalMins / 60) + startHour;
                const m = Math.floor(totalMins % 60);
                hoverTimeStr = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
                if (!isDown) showGuide = true;
            } else showGuide = false;
        }
        if (!isDown) return;
        e.preventDefault();
        const walk = (e.pageX - scrollBody.offsetLeft - startX) * 1.5;
        scrollBody.scrollLeft = scrollLeft - walk;
    }
    function syncScroll(e) { if (scrollHeader) scrollHeader.scrollLeft = e.target.scrollLeft; }
    function syncHeaderScroll(e) { if (scrollBody) scrollBody.scrollLeft = e.target.scrollLeft; }
    function scrollToCurrentTime() { if (scrollBody && nowLinePos > 0) scrollBody.scrollTo({ top: nowLinePos - (scrollBody.clientHeight / 3), behavior: 'smooth' }); }

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

<div class="timeline-root" bind:clientWidth={containerWidth} on:mouseleave={() => showGuide = false}>
    <header class="staff-header-fixed">
        <div class="time-corner-fixed" style="width: {TIME_COL_WIDTH}px">🕒</div>
        <div class="staff-scroll-area" bind:this={scrollHeader} on:scroll={syncHeaderScroll}>
            <div class="staff-inner-row" style="width: {totalColsCount * STAFF_WIDTH}px">
                {#each staff as s (s.id + refreshKey)}
                    <button class="staff-cell btn-reset" style="width: {STAFF_WIDTH}px" on:click={() => dispatch('staffTap', s)}>
                        <div class="avatar-box">
                            {#if s.photoData}
                                <img class="avatar" class:is-off={s.dayOff} src="data:image/jpeg;base64,{s.photoData}" alt={s.name} />
                            {:else}
                                <div class="avatar" class:is-off={s.dayOff}>{s.name.charAt(0)}</div>
                            {/if}
                        </div>
                        <div class="meta">
                            <span class="n">{s.name}</span>
                            <span class="s">{s.specialty || 'Специалист'}</span>
                        </div>
                    </button>
                {/each}
                {#each Array(placeholderColsCount) as _}
                    <div class="staff-cell placeholder-cell" style="width: {STAFF_WIDTH}px">
                        <div class="avatar-box">
                            <div class="avatar is-off">—</div>
                        </div>
                        <div class="meta">
                            <span class="n">—</span>
                            <span class="s">Резерв</span>
                        </div>
                    </div>
                {/each}
            </div>
        </div>
    </header>

    <div class="timeline-body-scroll" bind:this={scrollBody} on:scroll={syncScroll} on:mousedown={handleStart} on:mousemove={handleMove} on:mouseup={() => isDown = false}>
        <div class="body-layout-wrapper" style="width: {totalColsCount * STAFF_WIDTH + TIME_COL_WIDTH}px">
            <div class="time-axis-col" style="width: {TIME_COL_WIDTH}px">
                {#each hours as h (h)}
                    <div class="hour-cell" style="height: {HOUR_HEIGHT}px"><span class="h-label">{h}:00</span></div>
                {/each}
                <TimelineNowIndicator {nowLinePos} label={branchTime} mode="dot" />
                {#if showGuide}
                    <TimelineCursorGuide y={hoverY} timeStr={hoverTimeStr} mode="label" />
                {/if}
            </div>
            <div class="grid-canvas" bind:this={gridCanvas}>
                <div class="columns-container">
                    {#each [...staff, ...(apptsByStaff['unassigned'].length > 0 ? [{id: null}] : [])] as s ( (s.id || 'unassigned') + refreshKey )}
                        <div class="staff-col" style="width: {STAFF_WIDTH}px">
                            {#each Array(hours.length * 4) as _, i}
                                {@const h = hours[Math.floor(i/4)]}
                                {@const m = (i%4)*15}
                                {@const status = timeUtils.getSlotStatus(s.id ? s : null, h, m)}
                                <button class="slot-btn"
                                        class:is-break={status === 'BREAK'}
                                        class:is-off={status === 'OFF'}
                                        style="height: {SLOT_HEIGHT}px"
                                        on:click|stopPropagation={() => handleEmptySlotClick(s.id, h, m, status)}></button>
                            {/each}
                            {#each apptsByStaff[s.id || 'unassigned'] || [] as appt (appt.id)}
                                <TimelineAppointment {appt} {startHour} hourHeight={HOUR_HEIGHT} timezone={currentBranch?.timezone} on:click={(e) => dispatch('appointmentTap', e.detail)} />
                            {/each}
                        </div>
                    {/each}
                    {#each Array(placeholderColsCount) as _}
                        <div class="staff-col placeholder-col" style="width: {STAFF_WIDTH}px">
                            {#each Array(hours.length * 4) as _, i}
                                {@const h = hours[Math.floor(i/4)]}
                                <div class="slot-placeholder" style="height: {SLOT_HEIGHT}px"></div>
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
                {#if showGuide}
                    <TimelineCursorGuide y={hoverY} mode="line" />
                {/if}
                <TimelineNowIndicator {nowLinePos} mode="line" />
            </div>
        </div>
    </div>
</div>

<style>
    * { box-sizing: border-box; }
    .btn-reset { background: none; border: none; padding: 0; margin: 0; text-align: left; cursor: pointer; font-family: inherit; }
    .timeline-root { height: 100%; display: flex; flex-direction: column; background: #fdf6e3; overflow: hidden; }
    .staff-header-fixed { display: flex; height: 72px; background: #eee8d5; z-index: 300; border-bottom: 1.5px solid #ddd6c1; flex-shrink: 0; }
    .time-corner-fixed { display: flex; align-items: center; justify-content: center; color: #93a1a1; font-size: 14px; border-right: 1.5px solid #ddd6c1; background: #eee8d5; z-index: 310; }
    .staff-scroll-area { flex: 1; overflow-x: auto; scrollbar-width: none; }
    .staff-scroll-area::-webkit-scrollbar { display: none; }
    .staff-inner-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 12px; gap: 12px; border-right: 1px solid #ddd6c1; overflow: hidden; }

    .avatar-box { flex-shrink: 0; }
    .avatar { width: 40px; height: 40px; background: var(--primary-gradient); color: white; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 16px; object-fit: cover; }
    img.avatar { background: #ddd; }
    .avatar.is-off { background: #93a1a1; }

    .meta { display: flex; flex-direction: column; gap: 2px; min-width: 0; overflow: hidden; }
    .n { display: block; font-size: 13px; font-weight: 850; color: #073642; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .s { display: block; font-size: 10px; color: #93a1a1; font-weight: 700; text-transform: uppercase; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .timeline-body-scroll { flex: 1; overflow: auto; position: relative; }
    .body-layout-wrapper { display: flex; min-height: 100%; position: relative; }
    .time-axis-col { flex-shrink: 0; background: #eee8d5; border-right: 1.5px solid #ddd6c1; position: sticky; left: 0; z-index: 410; }
    .hour-cell { position: relative; }
    .h-label { position: absolute; top: 0; left: 50%; transform: translate(-50%, -50%); font-size: 9px; font-weight: 900; color: #586e75; background: #fdf6e3; padding: 1px 4px; border-radius: 4px; border: 1px solid #ddd6c1; }

    .grid-canvas { position: relative; flex: 1; background: #fdf6e3; }
    .columns-container { display: flex; height: 100%; }
    .staff-col { position: relative; height: 100%; border-right: 1.5px solid #ddd6c1; flex-shrink: 0; }

    .slot-btn { width: 100%; border: none; cursor: pointer; display: block; background: #fdf6e3; transition: background 0.1s; }
    .slot-btn:hover { background: rgba(38, 139, 210, 0.05); }

    .slot-btn.is-off { background-color: #eee8d5 !important; background-image: repeating-linear-gradient(45deg, transparent, transparent 10px, rgba(147, 161, 161, 0.05) 10px, rgba(147, 161, 161, 0.05) 20px) !important; cursor: not-allowed; opacity: 0.6; }
    .slot-btn.is-break { background: #f5efdc !important; border-left: 4px solid #b58900; cursor: not-allowed; }

    .grid-lines-overlay { position: absolute; inset: 0; pointer-events: none; z-index: 50; }
    .l { position: absolute; left: 0; right: 0; height: 1px; }
    .l.bold { background: #ddd6c1; height: 1.5px; opacity: 0.6; }
    .l.dashed { border-top: 1px dashed #ddd6c1; height: 0; opacity: 0.3; }

    /* Стили для колонок-заглушек */
    .placeholder-cell { pointer-events: none; opacity: 0.5; }
    .placeholder-col { pointer-events: none; opacity: 0.5; }
    .slot-placeholder { width: 100%; display: block; background: #fdf6e3; }
</style>
