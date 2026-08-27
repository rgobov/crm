<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import { timeSyncService } from '$lib/services/timeSyncService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { branchService } from '$lib/services/branchService.js';
    import TimelineAppointment from '../TimelineAppointment.svelte';
    import TimelineNowIndicator from '../TimelineNowIndicator.svelte';
    import { fade } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    export let day = new Date();
    export let appointments = [];
    export let columns = [];
    export let columnKey = 'staffMemberId';

    let TIME_COL_WIDTH = 0;
    let HOUR_HEIGHT = 72;
    let SLOT_HEIGHT = HOUR_HEIGHT / 4;
    let STAFF_WIDTH = 0;

    let startHour = 8, endHour = 22, hours = [];
    let currentTime = new Date(), nowLinePos = -1, branchTime = "";
    let currentBranch = null;
    let scrollHeader, scrollBody;
    let branchRequest = null;
    let timer;
    let scrollTimeout;

    $: apptsByStaff = (() => {
        const map = { 'unassigned': [] };
        columns.forEach(s => map[s.id] = []);
        appointments.forEach(a => {
            const sid = a[columnKey];
            if (sid && map[sid]) map[sid].push(a);
            else map['unassigned'].push(a);
        });
        return map;
    })();

    $: isResourceMode = columnKey === 'resourceId';

    // РЕАКТИВНЫЙ РАСЧЕТ ВЕРСТКИ (Золотое сечение 1.618)
    $: {
        const width = typeof window !== 'undefined' ? window.innerWidth : 375;

        // TIME_COL_WIDTH пропорциональна ширине экрана (9%)
        TIME_COL_WIDTH = Math.floor(width * 0.09);

        const availableWidth = width - TIME_COL_WIDTH;
        const totalCols = columns.length + (apptsByStaff['unassigned']?.length > 0 ? 1 : 0);

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

    onMount(() => {
        let isDestroyed = false;

        async function initialize() {
            // Если филиалы уже загружены, линия может рассчитаться сразу по его timezone.
            const cachedBranch = $branchStore.find(branch => branch.id === $activeBranchId);
            if (cachedBranch) {
                currentBranch = cachedBranch;
                currentTime = timeSyncService.getNow();
                updateNowPosition();
            }

            // Время и данные филиала не зависят друг от друга, поэтому загружаются параллельно.
            await Promise.all([
                timeSyncService.sync(),
                fetchBranchData()
            ]);
            if (isDestroyed) return;

            currentTime = timeSyncService.getNow();
            updateNowPosition();

            timer = setInterval(() => {
                currentTime = timeSyncService.getNow();
                updateNowPosition();
            }, 5000);
            scrollTimeout = setTimeout(() => {
                if (scrollBody && nowLinePos > 0) {
                    scrollBody.scrollTo({ top: nowLinePos - (scrollBody.clientHeight / 3), behavior: 'smooth' });
                }
            }, 600);
        }

        initialize();

        return () => {
            isDestroyed = true;
            if (timer) clearInterval(timer);
            if (scrollTimeout) clearTimeout(scrollTimeout);
        };
    });

    async function fetchBranchData() {
        const branchId = $activeBranchId;
        if (!branchId) return;

        // Реактивный блок и onMount могут запустить один и тот же запрос.
        if (branchRequest?.branchId === branchId) {
            return branchRequest.promise;
        }

        const promise = branchService.getBranches()
            .then(branches => {
                currentBranch = branches.find(branch => branch.id === branchId) || null;
                if (currentBranch) {
                    branchStore.setBranches(branches);
                }
            })
            .catch(() => {
                // При наличии кэша сохраняем его, чтобы линия не исчезала из-за временной ошибки сети.
            })
            .finally(() => {
                if (branchRequest?.promise === promise) {
                    branchRequest = null;
                }
            });

        branchRequest = { branchId, promise };
        return promise;
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
            const currentHourInTz = parseInt(new Intl.DateTimeFormat('en-GB', { hour: '2-digit', hour12: false, timeZone: tz }).format(currentTime));
            if (currentHourInTz >= startHour && currentHourInTz < endHour) {
                nowLinePos = timeUtils.getTimeOffset(currentTime.toISOString(), startHour, HOUR_HEIGHT, tz);
                branchTime = timeUtils.formatTime(currentTime.toISOString(), tz);
            } else {
                nowLinePos = -1;
                branchTime = "";
            }
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
        columns.forEach(s => {
            if (s.workStartTime) minH = Math.min(minH, parseInt(s.workStartTime.split(':')[0]));
            if (s.workEndTime) maxH = Math.max(maxH, parseInt(s.workEndTime.split(':')[0]));
        });
        startHour = Math.max(0, minH);
        endHour = Math.min(24, maxH + 1);
        hours = Array.from({length: endHour - startHour + 1}, (_, i) => startHour + i);
    }

    function handleEmptySlotClick(staffId, hour, minute, status) {
        if (status === 'OFF' || status === 'BREAK') return;
        dispatch('emptySlotTap', { hour, min: minute, staffId });
    }

    function syncScroll() {
        if (scrollHeader && scrollBody) {
            scrollHeader.scrollLeft = scrollBody.scrollLeft;
        }
    }

    function syncHeaderScroll() {
        if (scrollBody && scrollHeader) {
            scrollBody.scrollLeft = scrollHeader.scrollLeft;
        }
    }
</script>

<div class="mobile-timeline-wrapper">
    <header class="staff-header-fixed">
        <div class="time-corner-fixed" style="width: {TIME_COL_WIDTH}px">🕒</div>
        <div class="staff-scroll-area" bind:this={scrollHeader} on:scroll={syncHeaderScroll}>
            <div class="staff-row" style="width: {(columns.length + (apptsByStaff['unassigned']?.length > 0 ? 1 : 0)) * STAFF_WIDTH}px">
                {#each columns as s (s.id)}
                    <button class="staff-cell btn-reset" class:is-off={s.dayOff} style="width: {STAFF_WIDTH}px" on:click={() => { isResourceMode ? dispatch('resourceTap', s) : dispatch('staffTap', s); }}>
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
        </div>
    </header>

    <div class="timeline-body-scroll" bind:this={scrollBody} on:scroll={syncScroll}>
        <div class="body-layout-wrapper" style="width: {(columns.length + (apptsByStaff['unassigned']?.length > 0 ? 1 : 0)) * STAFF_WIDTH + TIME_COL_WIDTH}px">
            <aside class="time-axis-col" style="width: {TIME_COL_WIDTH}px">
                {#each hours as h (h)}
                    <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                        <span class="h-label">{h}:00</span>
                    </div>
                {/each}
                <TimelineNowIndicator {nowLinePos} label={branchTime} mode="dot" />
            </aside>

            <main class="grid-canvas">
                <div class="cols-container">
                    {#each [...columns, ...(apptsByStaff['unassigned']?.length > 0 ? [{id: null}] : [])] as s ( (s.id || 'unassigned') )}
                        <div class="staff-col" style="width: {STAFF_WIDTH}px">
                            {#each Array(hours.length * 4) as _, i}
                                {@const h = hours[Math.floor(i/4)]}
                                {@const m = (i%4)*15}
                                {@const status = timeUtils.getSlotStatus(s.id ? s : null, h, m)}
                                <button class="slot-btn"
                                        class:is-break={status === 'BREAK'}
                                        class:is-off={status === 'OFF'}
                                        style="height: {SLOT_HEIGHT}px"
                                        on:click|stopPropagation={() => handleEmptySlotClick(s.id, h, m, status)}>
                                </button>
                            {/each}
                            {#each apptsByStaff[s.id || 'unassigned'] || [] as appt (appt.id)}
                                <TimelineAppointment {appt} {day} {startHour} hourHeight={HOUR_HEIGHT} timezone={currentBranch?.timezone} on:click={(e) => dispatch('appointmentTap', e.detail)} />
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
</div>

<style>
    * { box-sizing: border-box; }
    .btn-reset { background: none; border: none; padding: 0; margin: 0; text-align: left; cursor: pointer; font-family: inherit; }
    .mobile-timeline-wrapper { position: relative; height: 100%; display: flex; flex-direction: column; background: #fdf6e3; }
    .staff-header-fixed { display: flex; height: 60px; background: #eee8d5; z-index: 900; border-bottom: 1.5px solid #ddd6c1; flex-shrink: 0; position: absolute; top: 0; left: 0; right: 0; }
    .time-corner-fixed { display: flex; align-items: center; justify-content: center; color: #93a1a1; font-size: 14px; border-right: 1.5px solid #ddd6c1; background: #eee8d5; z-index: 910; flex-shrink: 0; }
    .staff-scroll-area { flex: 1; overflow-x: auto; scrollbar-width: none; }
    .staff-scroll-area::-webkit-scrollbar { display: none; }
    .timeline-body-scroll { flex: 1; overflow: auto; position: relative; -webkit-overflow-scrolling: touch; padding-top: 60px; }
    .timeline-body-scroll::-webkit-scrollbar { display: none; }
    .body-layout-wrapper { display: flex; min-height: 100%; position: relative; }
    .time-axis-col { flex-shrink: 0; background: #eee8d5; border-right: 1.5px solid #ddd6c1; position: sticky; left: 0; z-index: 410; }
    .grid-canvas { position: relative; flex: 1; background: #fdf6e3; }
    .staff-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 8px; gap: 8px; border-right: 1px solid #ddd6c1; overflow: hidden; transition: opacity 0.2s; }
    .staff-cell.is-off { opacity: 0.5; background: #eee8d5; }
    .staff-cell.is-off .n, .staff-cell.is-off .s { color: #93a1a1; }
    .avatar { width: 32px; height: 32px; background: var(--primary-gradient); color: white; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 13px; object-fit: cover; image-orientation: from-image; }
    img.avatar { background: #ddd; }
    .avatar.is-off { background: #93a1a1; }
    .meta { display: flex; flex-direction: column; gap: 1px; min-width: 0; overflow: hidden; }
    .n { display: block; font-size: 11px; font-weight: 850; color: #073642; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .s { display: block; font-size: 8px; color: #93a1a1; font-weight: 700; text-transform: uppercase; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .hour-cell { position: relative; }
    .h-label { position: absolute; top: 0; left: 50%; transform: translate(-50%, 0); font-size: 9px; font-weight: 900; color: #586e75; background: #fdf6e3; padding: 1px 4px; border-radius: 4px; border: 1px solid #ddd6c1; }
    .cols-container { display: flex; height: 100%; }
    .staff-col { position: relative; height: 100%; border-right: 1.5px solid #ddd6c1; flex-shrink: 0; }
    .slot-btn { width: 100%; border: none; display: block; background: #fdf6e3; }
    .slot-btn.is-off { background-color: #eee8d5 !important; background-image: repeating-linear-gradient(45deg, transparent, transparent 10px, rgba(147, 161, 161, 0.05) 10px, rgba(147, 161, 161, 0.05) 20px) !important; opacity: 0.6; }
    .slot-btn.is-break { background: #f5efdc !important; border-left: 3px solid #b58900; }
    .grid-lines { position: absolute; inset: 0; pointer-events: none; z-index: 50; }
    .l { position: absolute; left: 0; right: 0; height: 1px; }
    .l.bold { background: #ddd6c1; height: 1.5px; opacity: 0.6; }
    .l.dashed { border-top: 1px dashed #ddd6c1; height: 0; opacity: 0.3; }
</style>
