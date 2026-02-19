<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { fade, slide, scale } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    export let day = new Date();
    export let appointments = [];
    export let staff = [];

    let HOUR_HEIGHT = 140;
    let SLOT_HEIGHT = HOUR_HEIGHT / 4;
    let STAFF_WIDTH = 200;
    let TIME_COL_WIDTH = 75;

    let startHour = 8;
    let endHour = 22;
    let hours = [];
    let timer;
    let currentTime = new Date();
    let nowLinePos = -1;

    let scrollHeader;
    let scrollBody;

    let isDown = false;
    let startX;
    let scrollLeft;
    let bodyDragging = false;

    // АДАПТИВНОСТЬ
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

    onMount(() => {
        updateLayoutSizes();
        window.addEventListener('resize', updateLayoutSizes);
        timer = setInterval(() => {
            currentTime = new Date();
            updateNowPosition();
        }, 60000);

        updateNowPosition();
        setTimeout(scrollToCurrentTime, 600);

        return () => {
            window.removeEventListener('resize', updateLayoutSizes);
            clearInterval(timer);
        };
    });

    function updateNowPosition() {
        const isToday = currentTime.toDateString() === day.toDateString();
        if (isToday) {
            const h = currentTime.getHours();
            const m = currentTime.getMinutes();
            nowLinePos = ((h - startHour) * 60 + m) * (HOUR_HEIGHT / 60);
        } else {
            nowLinePos = -1;
        }
    }

    $: if (day) updateNowPosition();

    $: staffIds = new Set(staff.map(s => s.id));
    $: unassignedAppts = appointments.filter(a => !a.staffMemberId || !staffIds.has(a.staffMemberId));

    function handleStart(e) {
        isDown = true;
        if (scrollBody) scrollBody.style.scrollBehavior = 'auto';
        const pageX = e.pageX || (e.touches ? e.touches[0].pageX : 0);
        startX = pageX - scrollBody.offsetLeft;
        scrollLeft = scrollBody.scrollLeft;
        bodyDragging = false;
    }

    function handleEnd() {
        isDown = false;
        if (scrollBody) scrollBody.style.scrollBehavior = 'smooth';
        setTimeout(() => { bodyDragging = false; }, 50);
    }

    function handleMove(e) {
        if (!isDown) return;
        const pageX = e.pageX || (e.touches ? e.touches[0].pageX : null);
        if (pageX === null) return;
        const x = pageX - scrollBody.offsetLeft;
        const walk = (x - startX) * 2.2;
        if (Math.abs(walk) > 5) {
            bodyDragging = true;
            if (e.cancelable) e.preventDefault();
            scrollBody.scrollLeft = scrollLeft - walk;
        }
    }

    function onApptClick(appt) {
        if (bodyDragging) return;
        const master = staff.find(s => s.id === appt.staffMemberId);
        dispatch('appointmentTap', { ...appt, staffName: master ? master.name : 'Не назначен' });
    }

    function handleSlotClick(h, m, sId) {
        if (bodyDragging) return;
        dispatch('emptySlotTap', { hour: h, min: m, staffId: sId });
    }

    function getSlotStatus(s, h, m) {
        if (!s || !s.workStartTime || !s.workEndTime) return 'WORK';
        const slotTime = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`;
        if (s.isDayOff) return 'OFF';
        if (slotTime < s.workStartTime || slotTime >= s.workEndTime) return 'OFF';
        if (s.breakStartTime && s.breakEndTime) {
            if (slotTime >= s.breakStartTime && slotTime < s.breakEndTime) return 'BREAK';
        }
        return 'WORK';
    }

    function isBreakStart(s, h, m) {
        const slotTime = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`;
        return s.breakStartTime === slotTime;
    }

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

    function scrollToCurrentTime() {
        if (scrollBody && nowLinePos > 0) {
            const viewportHeight = scrollBody.clientHeight;
            const targetScroll = nowLinePos - (viewportHeight / 2) + 40;
            scrollBody.scrollTo({ top: Math.max(0, targetScroll), behavior: 'smooth' });
        }
    }

    function getApptStyle(appt) {
        const start = new Date(appt.startTime);
        const top = ((start.getHours() - startHour) * 60 + start.getMinutes()) * (HOUR_HEIGHT / 60);
        const actualHeight = appt.durationInMinutes * (HOUR_HEIGHT / 60);
        return `top: ${top}px; height: ${actualHeight - 2}px; z-index: 60;`;
    }

    function getStatusData(status) {
        const config = {
            'SCHEDULED': { color: '#3b82f6', label: 'Ожидается' },
            'CONFIRMED': { color: '#10b981', label: 'Подтвержден' },
            'NEEDS_CALL': { color: '#f59e0b', label: 'Звонок' },
            'COMPLETED': { color: '#64748b', label: 'Завершен' },
            'CANCELLED': { color: '#ef4444', label: 'Отменен' }
        };
        return config[status] || config['SCHEDULED'];
    }
</script>

<div class="timeline-root">
    <header class="staff-header-fixed">
        <div class="time-corner-empty" style="width: {TIME_COL_WIDTH}px">🕒</div>
        <div class="staff-scroll-area" bind:this={scrollHeader}>
            <div class="staff-inner-row" style="width: {(staff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH}px">
                {#each staff as s}
                    <div class="staff-cell" style="width: {STAFF_WIDTH}px">
                        <div class="avatar">{s.name.charAt(0)}</div>
                        <div class="meta">
                            <span class="n">{s.name}</span>
                            <span class="s">{s.specialty || 'Специалист'}</span>
                        </div>
                    </div>
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
         on:scroll={syncScroll}
         bind:this={scrollBody}
         on:mousedown={handleStart}
         on:touchstart|passive={handleStart}
         on:mouseleave={handleEnd}
         on:mouseup={handleEnd}
         on:touchend={handleEnd}
         on:mousemove={handleMove}
         on:touchmove={handleMove}
         class:grabbing={isDown}>

        <div class="timeline-spacer top"></div>

        <div class="body-layout-wrapper" style="width: {(staff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH + TIME_COL_WIDTH}px">
            <div class="time-axis-col" style="width: {TIME_COL_WIDTH}px">
                {#each hours as h}
                    <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                        <span class="h-label">{h}:00</span>
                    </div>
                {/each}
            </div>

            <div class="grid-canvas" style="width: {(staff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH}px">
                <div class="grid-lines">
                    {#each Array(hours.length * 4) as _, i}
                        <div class="l" class:bold={i % 4 === 0} style="top: {i * SLOT_HEIGHT}px"></div>
                    {/each}
                </div>

                <div class="columns-container">
                    {#each staff as s}
                        <div class="staff-col" style="width: {STAFF_WIDTH}px">
                            {#each Array(hours.length * 4) as _, i}
                                {@const h = hours[Math.floor(i/4)]}
                                {@const m = (i%4)*15}
                                {@const status = getSlotStatus(s, h, m)}
                                {@const isFirst = isBreakStart(s, h, m)}

                                <button class="slot-btn"
                                        class:is-off={status === 'OFF'}
                                        class:is-break={status === 'BREAK'}
                                        class:is-work={status === 'WORK'}
                                        style="height: {SLOT_HEIGHT}px"
                                        on:click|stopPropagation={() => handleSlotClick(h, m, s.id)}>
                                    {#if isFirst}
                                        <div class="break-overlay" in:fade>
                                            <span class="break-icon">☕</span>
                                            <span class="break-txt">ПЕРЕРЫВ ДО {s.breakEndTime?.slice(0,5)}</span>
                                        </div>
                                    {/if}
                                </button>
                            {/each}

                            {#each appointments.filter(a => a.staffMemberId === s.id) as appt (appt.id)}
                                {@const status = getStatusData(appt.status)}
                                <div class="appt-box" style="{getApptStyle(appt)} --status-color: {status.color}" on:click|stopPropagation={() => onApptClick(appt)}>
                                    <div class="appt-content">
                                        <div class="t-row"><span class="tm">{new Date(appt.startTime).toLocaleTimeString('ru',{hour:'2-digit',minute:'2-digit'})}</span><span class="st-dot" style="background: {status.color}"></span></div>
                                        <div class="cl">{appt.clientName}</div>
                                        <div class="sv">{appt.service}</div>
                                    </div>
                                </div>
                            {/each}
                        </div>
                    {/each}

                    {#if unassignedAppts.length > 0}
                        <div class="staff-col unassigned-col" style="width: {STAFF_WIDTH}px">
                            {#each Array(hours.length * 4) as _, i}
                                <button class="slot-btn is-off" style="height: {SLOT_HEIGHT}px"></button>
                            {/each}
                            {#each unassignedAppts as appt (appt.id)}
                                {@const status = getStatusData(appt.status)}
                                <div class="appt-box" style="{getApptStyle(appt)} --status-color: {status.color}" on:click|stopPropagation={() => onApptClick(appt)}>
                                    <div class="appt-content">
                                        <div class="t-row"><span class="tm">{new Date(appt.startTime).toLocaleTimeString('ru',{hour:'2-digit',minute:'2-digit'})}</span><span class="st-dot" style="background: {status.color}"></span></div>
                                        <div class="cl">{appt.clientName}</div>
                                        <div class="sv">{appt.service}</div>
                                    </div>
                                </div>
                            {/each}
                        </div>
                    {/if}
                </div>

                {#if nowLinePos >= 0}
                    <div class="now-indicator" style="top: {nowLinePos}px">
                        <div class="line"></div>
                        <div class="dot"></div>
                    </div>
                {/if}
            </div>
        </div>
        <div class="timeline-spacer bottom"></div>
    </div>
</div>

<style>
    * { box-sizing: border-box; }
    .timeline-root { height: 100vh; display: flex; flex-direction: column; background: #fffbeb; overflow: hidden; user-select: none; }

    .staff-header-fixed {
        display: flex; height: 84px; background: rgba(255, 255, 255, 0.8);
        backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
        z-index: 200; border-bottom: 1px solid rgba(0,0,0,0.05);
        box-shadow: 0 4px 20px rgba(0,0,0,0.03); flex-shrink: 0;
    }

    .time-corner-empty { display: flex; align-items: center; justify-content: center; color: #94a3b8; font-size: 20px; border-right: 1px solid rgba(0,0,0,0.05); }
    .staff-scroll-area { flex: 1; overflow: hidden; }
    .staff-inner-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 12px; gap: 10px; border-right: 1px solid rgba(0,0,0,0.05); }
    .avatar { width: 44px; height: 44px; background: var(--primary-gradient); color: white; border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 18px; box-shadow: 0 4px 12px rgba(56, 151, 240, 0.2); }

    .n { display: block; font-size: 14px; font-weight: 850; color: #0f172a; letter-spacing: -0.3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .s { display: block; font-size: 9px; color: #94a3b8; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .timeline-body-scroll { flex: 1; overflow: auto; position: relative; cursor: grab; -webkit-overflow-scrolling: touch; touch-action: pan-y; }
    .timeline-body-scroll.grabbing { cursor: grabbing; }
    .timeline-spacer { height: 40px; }
    .timeline-spacer.bottom { height: 120px; }

    .body-layout-wrapper { display: flex; min-height: 100%; align-items: flex-start; }
    .time-axis-col { flex-shrink: 0; background: #f8fafc; border-right: 1px solid #e2e8f0; position: sticky; left: 0; z-index: 150; }
    .hour-cell { position: relative; }
    .h-label { position: absolute; top: 0; left: 50%; transform: translate(-50%, -50%); font-size: 11px; font-weight: 900; color: #64748b; background: #f8fafc; padding: 4px 8px; border-radius: 8px; border: 1px solid #e2e8f0; }

    .grid-canvas { position: relative; flex: 1; margin: 0; padding: 0; }
    .l { position: absolute; left: 0; right: 0; height: 1px; background: rgba(0,0,0,0.03); }
    .l.bold { background: rgba(0,0,0,0.06); height: 1.5px; }

    .columns-container { display: flex; height: 100%; position: relative; z-index: 10; }
    .staff-col { position: relative; height: 100%; border-right: 1px solid rgba(0,0,0,0.05); flex-shrink: 0; }

    .slot-btn { width: 100%; border: none !important; margin: 0 !important; padding: 0 !important; cursor: pointer; display: block; outline: none; transition: background 0.1s; position: relative; }
    .slot-btn.is-work { background: white; }
    .slot-btn.is-work:hover { background: #eff6ff !important; box-shadow: inset 0 0 0 1.5px rgba(59, 130, 240, 0.3); z-index: 5; }
    .slot-btn.is-off { background: #f1f5f9; cursor: not-allowed; }
    .slot-btn.is-break { background: #fef08a; cursor: not-allowed; }

    .break-overlay { position: absolute; inset: 0; z-index: 20; display: flex; align-items: center; justify-content: center; gap: 6px; white-space: nowrap; pointer-events: none; }
    .break-icon { font-size: 14px; }
    .break-txt { font-size: 9px; font-weight: 950; color: #854d0e; letter-spacing: 0.5px; }

    .appt-box { position: absolute; left: 6px; right: 6px; background: white; border-radius: 18px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.08), 0 8px 10px -6px rgba(0,0,0,0.05); cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; border: 1px solid rgba(0,0,0,0.03); overflow: hidden; }
    .appt-box:hover { transform: translateY(-2px) scale(1.02); z-index: 100 !important; }
    .appt-content { height: 100%; border-left: 6px solid var(--status-color); padding: 10px 12px; display: flex; flex-direction: column; gap: 4px; }

    .t-row { display: flex; justify-content: space-between; align-items: center; }
    .tm { font-size: 11px; font-weight: 900; color: #1e293b; }
    .st-dot { width: 8px; height: 8px; border-radius: 50%; }
    .cl { font-size: 13px; font-weight: 850; color: #0f172a; line-height: 1.2; }
    .sv { font-size: 10px; color: #64748b; font-weight: 750; text-transform: uppercase; letter-spacing: 0.3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .now-indicator { position: absolute; left: 0; right: 0; z-index: 300; pointer-events: none; display: flex; align-items: center; transform: translateY(-50%); }
    .now-indicator .line { height: 2px; background: #ef4444; flex: 1; box-shadow: 0 0 12px rgba(239, 68, 68, 0.6); }
    .now-indicator .dot { width: 12px; height: 12px; background: #ef4444; border-radius: 50%; margin-left: -6px; flex-shrink: 0; box-shadow: 0 0 15px rgba(239, 68, 68, 0.8); animation: pulse-red 2s infinite; z-index: 310; }

    @keyframes pulse-red {
        0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
        70% { transform: scale(1.3); box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
        100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
    }

    @media (max-width: 768px) {
        .n { font-size: 12px; }
        .s { font-size: 8px; }
        .avatar { width: 36px; height: 36px; font-size: 14px; }
        .cl { font-size: 11px; }
        .sv { font-size: 8px; }
        .break-txt { font-size: 7px; }
    }
</style>
