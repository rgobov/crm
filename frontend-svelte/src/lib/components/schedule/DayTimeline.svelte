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

    // Константы размеров
    let HOUR_HEIGHT = 140;
    let SLOT_HEIGHT = HOUR_HEIGHT / 4;
    let STAFF_WIDTH = 200;
    let TIME_COL_WIDTH = 75;

    let startHour = 8;
    let endHour = 22;
    let hours = [];
    let currentTime = new Date();
    let nowLinePos = -1;
    let branchTime = "";
    let currentBranch = null;

    let scrollHeader;
    let scrollBody;
    let gridCanvas;

    let isDown = false;
    let isTouch = false; // НОВОЕ: Флаг режима касания
    let startX;
    let scrollLeft;

    let hoverY = -1;
    let hoverTimeStr = "";
    let showGuide = false;

    let refreshKey = 0;
    $: if (staff) refreshKey++;

    function updateLayoutSizes() {
        const width = window.innerWidth;
        if (width < 768) {
            TIME_COL_WIDTH = 60;
            STAFF_WIDTH = (width - TIME_COL_WIDTH) / 3;
            HOUR_HEIGHT = 120;
        } else {
            TIME_COL_WIDTH = 75;
            STAFF_WIDTH = 200;
            HOUR_HEIGHT = 140;
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
        } catch (e) {
            console.error('Failed to fetch branch data', e);
        }
    }

    function updateNowPosition() {
        if (!currentBranch) return;
        const formatter = new Intl.DateTimeFormat('sv-SE', { timeZone: currentBranch.timezone, year: 'numeric', month: '2-digit', day: '2-digit' });
        const branchTodayStr = formatter.format(currentTime);
        const daySelectedStr = formatter.format(day);
        if (branchTodayStr === daySelectedStr) {
            nowLinePos = timeUtils.getTimeOffset(currentTime.toISOString(), startHour, HOUR_HEIGHT, currentBranch.timezone);
            branchTime = timeUtils.formatTime(currentTime.toISOString(), currentBranch.timezone);
        } else {
            nowLinePos = -1;
            branchTime = "";
        }
    }

    $: if ($activeBranchId) fetchBranchData();
    $: if (day || startHour || currentBranch) updateNowPosition();

    function handleStart(e) {
        isTouch = e.type === 'touchstart'; // Проверяем, касание это или клик
        isDown = true;
        const pageX = e.pageX || (e.touches ? e.touches[0].pageX : 0);
        startX = pageX - scrollBody.offsetLeft;
        scrollLeft = scrollBody.scrollLeft;
        showGuide = false;
    }

    function handleMove(e) {
        // Гайд-линия при наведении (только для мыши)
        if (!isTouch && gridCanvas) {
            const rect = gridCanvas.getBoundingClientRect();
            const clientY = e.clientY || (e.touches ? e.touches[0].clientY : -1);
            const y = clientY - rect.top;
            if (y >= 0 && y <= rect.height) {
                hoverY = y;
                const totalMinutes = (y / (HOUR_HEIGHT / 60));
                const h = Math.floor(totalMinutes / 60) + startHour;
                const m = Math.floor(totalMinutes % 60);
                hoverTimeStr = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
                if (!isDown) showGuide = true;
            } else {
                showGuide = false;
            }
        }

        if (!isDown || isTouch) return; // Если это тач - не мешаем нативному скроллу браузера

        e.preventDefault(); // Предотвращаем выделение текста при перетаскивании мышью
        const pageX = e.pageX;
        const x = pageX - scrollBody.offsetLeft;
        const walk = (x - startX) * 1.5;
        scrollBody.scrollLeft = scrollLeft - walk;
        showGuide = false;
    }

    function handleEnd() { isDown = false; }

    function syncScroll(e) {
        if (scrollHeader && e.target === scrollBody) scrollHeader.scrollLeft = scrollBody.scrollLeft;
    }

    function syncHeaderScroll(e) {
        if (scrollBody && e.target === scrollHeader) scrollBody.scrollLeft = scrollHeader.scrollLeft;
    }

    function scrollToCurrentTime() {
        if (scrollBody && nowLinePos > 0) {
            const targetScroll = nowLinePos - (scrollBody.clientHeight / 2) + 40;
            scrollBody.scrollTo({ top: Math.max(0, targetScroll), behavior: 'smooth' });
        }
    }

    $: {
        let minH = 9, maxH = 20;
        const tz = currentBranch?.timezone || 'Europe/Moscow';
        appointments.forEach(a => {
            const date = new Date(a.startTime);
            const formatter = new Intl.DateTimeFormat('en-GB', { timeZone: tz, hour: 'numeric', hour12: false });
            const h = parseInt(formatter.format(date));
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

    $: staffIds = new Set(staff.map(s => s.id));
    $: unassignedAppts = appointments.filter(a => !a.staffMemberId || !staffIds.has(a.staffMemberId));
</script>

<div class="timeline-root" on:mouseleave={() => showGuide = false}>
    <header class="staff-header-fixed">
        <div class="time-corner-empty" style="width: {TIME_COL_WIDTH}px">🕒</div>
        <div class="staff-scroll-area" bind:this={scrollHeader} on:scroll={syncHeaderScroll}>
            <div class="staff-inner-row" style="width: {(staff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH}px">
                {#each staff as s (s.id + refreshKey)}
                    <button class="staff-cell btn-reset" style="width: {STAFF_WIDTH}px" on:click={() => dispatch('staffTap', s)}>
                        <div class="avatar" class:is-off={s.dayOff}>{s.name.charAt(0)}</div>
                        <div class="meta">
                            <span class="n">{s.name}</span>
                            <span class="s">{s.specialty || 'Специалист'}</span>
                            {#if s.dayOff}
                                <span class="off-badge">ВЫХОДНОЙ</span>
                            {:else}
                                <span class="work-time">{s.workStartTime?.slice(0,5)} - {s.workEndTime?.slice(0,5)}</span>
                            {/if}
                        </div>
                    </button>
                {/each}
                {#if unassignedAppts.length > 0}
                    <div class="staff-cell unassigned" style="width: {STAFF_WIDTH}px">
                        <div class="avatar">?</div>
                        <div class="meta"><span class="n">Не назначен</span></div>
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
         on:mouseup={handleEnd}
         on:mouseleave={handleEnd}
         on:touchstart={handleStart}
         on:touchmove={handleMove}
         on:touchend={handleEnd}
         class:grabbing={isDown && !isTouch}>

        <div class="body-layout-wrapper" style="width: {(staff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH + TIME_COL_WIDTH}px">
            <!-- ШКАЛА ВРЕМЕНИ -->
            <div class="time-axis-col" style="width: {TIME_COL_WIDTH}px">
                {#each hours as h}
                    <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                        <span class="h-label">{h}:00</span>
                    </div>
                {/each}

                <!-- 1. ТОЧКА ВРЕМЕНИ (В САМОМ КОНЦЕ СЛОЯ) -->
                <TimelineNowIndicator {nowLinePos} label={branchTime} mode="dot" />

                {#if showGuide}
                    <TimelineCursorGuide y={hoverY} timeStr={hoverTimeStr} mode="label" />
                {/if}
            </div>

            <div class="grid-canvas" bind:this={gridCanvas}>
                <div class="columns-container">
                    {#each [...staff, ...(unassignedAppts.length > 0 ? [{id: null}] : [])] as s ( (s.id || 'unassigned') + refreshKey )}
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
                                        on:click|stopPropagation={() => dispatch('emptySlotTap', { hour: h, min: m, staffId: s.id })}>
                                    {#if status === 'BREAK' && m === 0}
                                        <div class="break-overlay"><span class="break-txt">ОБЕД ДО {s.breakEndTime?.slice(0,5)}</span></div>
                                    {/if}
                                </button>
                            {/each}
                            {#each appointments.filter(a => (a.staffMemberId === s.id) || (!s.id && !staffIds.has(a.staffMemberId))) as appt (appt.id)}
                                <TimelineAppointment {appt} {startHour} hourHeight={HOUR_HEIGHT} timezone={currentBranch?.timezone} on:click={(e) => dispatch('appointmentTap', e.detail)} />
                            {/each}
                        </div>
                    {/each}
                </div>

                <div class="grid-lines-overlay">
                    {#each Array(hours.length * 4) as _, i}
                        {@const isHour = i % 4 === 0}
                        <div class="l"
                             class:bold={isHour}
                             class:dashed={!isHour}
                             style="top: {i * SLOT_HEIGHT}px"></div>
                    {/each}
                </div>

                {#if showGuide}
                    <TimelineCursorGuide y={hoverY} mode="line" />
                {/if}

                <!-- 2. ЛИНИЯ ВРЕМЕНИ (В САМОМ КОНЦЕ СЛОЯ) -->
                <TimelineNowIndicator {nowLinePos} mode="line" />
            </div>
        </div>
    </div>
</div>

<style>
    * { box-sizing: border-box; }
    .btn-reset { background: none; border: none; padding: 0; margin: 0; text-align: left; cursor: pointer; }

    .timeline-root { height: 100vh; display: flex; flex-direction: column; background: #fdf6e3; overflow: hidden; user-select: none; }

    .staff-header-fixed { display: flex; height: 84px; background: #eee8d5; z-index: 300; border-bottom: 2.5px solid #ddd6c1; flex-shrink: 0; }
    .time-corner-empty { display: flex; align-items: center; justify-content: center; color: #93a1a1; font-size: 20px; border-right: 2.5px solid #ddd6c1; background: #eee8d5; z-index: 310; }

    .staff-scroll-area { flex: 1; overflow: hidden; }
    .staff-inner-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 12px; gap: 10px; border-right: 1px solid #ddd6c1; transition: background 0.2s; }
    .staff-cell:hover { background: #eee8d5; }

    .avatar { width: 44px; height: 44px; background: var(--primary-gradient); color: white; border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 18px; }
    .avatar.is-off { background: #93a1a1; }

    .n { display: block; font-size: 14px; font-weight: 850; color: #586e75; }
    .s { display: block; font-size: 9px; color: #93a1a1; font-weight: 700; text-transform: uppercase; }
    .off-badge { display: inline-block; font-size: 8px; font-weight: 900; background: #dc322f; color: white; padding: 1px 4px; border-radius: 4px; margin-top: 2px; }
    .work-time { font-size: 9px; color: #859900; font-weight: 800; }

    .timeline-body-scroll {
        flex: 1;
        overflow: auto;
        position: relative;
        cursor: grab;
        touch-action: pan-x pan-y; /* ВАЖНО: Разрешаем нативную прокрутку */
        -webkit-overflow-scrolling: touch; /* Включаем инерцию на iOS */
    }
    .timeline-body-scroll.grabbing { cursor: grabbing; }

    .body-layout-wrapper { display: flex; min-height: 100%; align-items: flex-start; position: relative; }
    .time-axis-col { flex-shrink: 0; background: #eee8d5; border-right: 2.5px solid #ddd6c1; position: sticky; left: 0; z-index: 200; }
    .hour-cell { position: relative; }
    .h-label { position: absolute; top: 0; left: 50%; transform: translate(-50%, -50%); font-size: 11px; font-weight: 900; color: #586e75; background: #fdf6e3; padding: 2px 6px; border-radius: 6px; border: 1.5px solid #ddd6c1; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }

    .grid-canvas { position: relative; flex: 1; margin: 0; padding: 0; background: #fdf6e3; }

    .grid-lines-overlay { position: absolute; inset: 0; pointer-events: none; z-index: 50; }
    .l { position: absolute; left: 0; right: 0; height: 1px; }
    .l.bold { background: #ddd6c1; height: 2px; opacity: 0.8; }
    .l.dashed { border-top: 1.5px dashed #ddd6c1; height: 0; opacity: 0.4; }

    .columns-container { display: flex; height: 100%; position: relative; z-index: 10; }
    .staff-col { position: relative; height: 100%; border-right: 2px solid #ddd6c1; flex-shrink: 0; }

    .slot-btn { width: 100%; border: none; margin: 0; padding: 0; cursor: pointer; display: block; outline: none; transition: all 0.1s; position: relative; background: #fdf6e3; }
    .slot-btn.zebra { background: #f5efdc; }
    .slot-btn:hover { background: #eee8d5 !important; z-index: 60; box-shadow: inset 0 0 0 2px #268bd2; border-radius: 4px; }

    .slot-btn.is-off {
        background-color: #eee8d5 !important;
        background-image: repeating-linear-gradient(45deg, transparent, transparent 10px, rgba(147, 161, 161, 0.05) 10px, rgba(147, 161, 161, 0.05) 20px) !important;
        cursor: not-allowed;
        box-shadow: inset 0 0 10px rgba(0,0,0,0.02);
    }

    .slot-btn.is-break {
        background: #f5efdc !important;
        cursor: not-allowed;
        border-left: 5px solid #b58900;
        opacity: 1;
    }

    .break-overlay { position: absolute; inset: 0; z-index: 20; display: flex; align-items: center; justify-content: center; pointer-events: none; }
    .break-txt { font-size: 10px; font-weight: 950; color: #b58900; text-transform: uppercase; letter-spacing: 0.5px; }
</style>
