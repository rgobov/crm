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
    let timer;
    let currentTime = new Date();
    let nowLinePos = -1;

    let scrollHeader;
    let scrollBody;

    let isDown = false;
    let startX;
    let startY;
    let scrollLeft;
    let bodyDragging = false;

    function handleMouseDown(e) {
        isDown = true;
        startX = e.pageX - scrollBody.offsetLeft;
        startY = e.pageY - scrollBody.offsetTop;
        scrollLeft = scrollBody.scrollLeft;
        bodyDragging = false;
    }

    function handleMouseLeave() { isDown = false; }

    function handleMouseUp() {
        isDown = false;
        setTimeout(() => { bodyDragging = false; }, 100);
    }

    function handleMouseMove(e) {
        if (!isDown) return;
        const x = e.pageX - scrollBody.offsetLeft;
        const y = e.pageY - scrollBody.offsetTop;
        const dist = Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
        if (dist > 5) {
            bodyDragging = true;
            e.preventDefault();
            const walk = (x - startX) * 1.5;
            scrollBody.scrollLeft = scrollLeft - walk;
        }
    }

    // ФУНКЦИЯ ДЛЯ КЛИКА ПО ЗАПИСИ
    function onApptClick(appt) {
        if (bodyDragging) return;

        // ФИКС: Ищем мастера в текущем списке staff, чтобы передать его имя в модалку
        const master = staff.find(s => s.id === appt.staffMemberId);
        const enrichedAppt = {
            ...appt,
            staffName: master ? master.name : 'Не назначен'
        };

        dispatch('appointmentTap', enrichedAppt);
    }

    $: unassignedAppts = appointments.filter(a => !a.staffMemberId);
    $: displayStaff = [...staff];

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

        const isToday = currentTime.toDateString() === day.toDateString();
        if (isToday) {
            const h = currentTime.getHours();
            const m = currentTime.getMinutes();
            nowLinePos = ((h - startHour) * 60 + m) * (HOUR_HEIGHT / 60);
        } else {
            nowLinePos = -1;
        }
    }

    function syncScroll(e) {
        if (scrollHeader && e.target === scrollBody) scrollHeader.scrollLeft = scrollBody.scrollLeft;
    }

    onMount(() => {
        timer = setInterval(() => { currentTime = new Date(); }, 30000);
        setTimeout(scrollToCurrentTime, 600);
    });

    onDestroy(() => clearInterval(timer));

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
            'CONFIRMED': { color: '#26a69a', label: 'Подтвержден' },
            'NEEDS_CALL': { color: '#ffa726', label: 'Звонок' },
            'COMPLETED': { color: '#94a3b8', label: 'Завершен' },
            'CANCELLED': { color: '#ef5350', label: 'Отменен' }
        };
        return config[status] || config['SCHEDULED'];
    }
</script>

<div class="timeline-root">

    <header class="staff-header-fixed">
        <div class="time-corner-empty">🕒</div>
        <div class="staff-scroll-area" bind:this={scrollHeader}>
            <div class="staff-inner-row" style="width: {(displayStaff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH}px">
                {#each displayStaff as s}
                    <div class="staff-cell" style="width: {STAFF_WIDTH}px">
                        {#if s.photoUrl}
                            <img src={s.photoUrl} alt={s.name} class="avatar img" />
                        {:else}
                            <div class="avatar">{s.name.charAt(0)}</div>
                        {/if}
                        <div class="meta">
                            <span class="n">{s.name}</span>
                            <span class="s">{s.specialty}</span>
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
         on:mousedown={handleMouseDown}
         on:mouseleave={handleMouseLeave}
         on:mouseup={handleMouseUp}
         on:mousemove={handleMouseMove}
         class:grabbing={isDown}>

        <div class="timeline-spacer top"></div>

        <div class="body-layout-wrapper" style="width: {(displayStaff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH + TIME_COL_WIDTH}px">
            <div class="time-axis-col" style="width: {TIME_COL_WIDTH}px">
                {#each hours as h}
                    <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                        <span class="h-label">{h}:00</span>
                    </div>
                {/each}
            </div>

            <div class="grid-canvas" style="width: {(displayStaff.length + (unassignedAppts.length > 0 ? 1 : 0)) * STAFF_WIDTH}px">
                <div class="grid-lines">
                    {#each Array(hours.length * 4) as _, i}
                        <div class="l" class:bold={i % 4 === 0} style="top: {i * SLOT_HEIGHT}px"></div>
                    {/each}
                </div>

                <div class="columns-container">
                    {#each [...displayStaff, ...(unassignedAppts.length > 0 ? [{id: null}] : [])] as s, sIdx}
                        <div class="staff-col" style="width: {STAFF_WIDTH}px">
                            {#each Array(hours.length * 4) as _, i}
                                <button class="slot-btn"
                                        style="height: {SLOT_HEIGHT}px"
                                        on:click|stopPropagation={() => { if(!bodyDragging) dispatch('emptySlotTap', { hour: hours[Math.floor(i/4)], min: (i%4)*15, staffId: s.id }); }}>
                                </button>
                            {/each}

                            {#each appointments.filter(a => a.staffMemberId === s.id) as appt (appt.id)}
                                {@const status = getStatusData(appt.status)}
                                <div class="appt-box"
                                     style="{getApptStyle(appt)} --status-color: {status.color}"
                                     on:click|stopPropagation={() => onApptClick(appt)}>
                                    <div class="appt-content">
                                        <div class="t">
                                            <span class="tm">{new Date(appt.startTime).toLocaleTimeString('ru',{hour:'2-digit',minute:'2-digit'})}</span>
                                            <span class="st">{status.label}</span>
                                        </div>
                                        <div class="cl">{appt.clientName}</div>
                                        <div class="sv">{appt.service}</div>
                                    </div>
                                </div>
                            {/each}
                        </div>
                    {/each}
                </div>

                {#if nowLinePos >= 0}
                    <div class="now-indicator" style="top: {nowLinePos}px">
                        <div class="label">{currentTime.toLocaleTimeString('ru', {hour:'2-digit', minute:'2-digit'})}</div>
                        <div class="dot"></div>
                        <div class="line"></div>
                    </div>
                {/if}
            </div>
        </div>

        <div class="timeline-spacer bottom"></div>
    </div>
</div>

<style>
    .timeline-root { height: 100vh; display: flex; flex-direction: column; background: #f1f5f9; overflow: hidden; user-select: none; }
    .staff-header-fixed { display: flex; height: 74px; background: white; z-index: 200; border-bottom: 1px solid #f1f5f9; box-shadow: 0 2px 10px rgba(0,0,0,0.05); flex-shrink: 0; }
    .time-corner-empty { width: 60px; background: white; z-index: 210; border-right: 1px solid #f1f5f9; display: flex; align-items: center; justify-content: center; color: #94a3b8; font-size: 18px; }
    .staff-scroll-area { flex: 1; overflow: hidden; }
    .staff-inner-row { display: flex; height: 100%; }
    .staff-cell { flex-shrink: 0; display: flex; align-items: center; padding: 0 12px; gap: 10px; border-right: 1px solid #f1f5f9; box-sizing: border-box; }
    .avatar { width: 36px; height: 36px; background: #eff6ff; color: #3b82f6; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    .avatar.img { object-fit: cover; border: 1px solid #e2e8f0; }
    .n { font-size: 13px; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .s { font-size: 10px; color: #94a3b8; font-weight: 600; text-transform: uppercase; }
    .timeline-body-scroll { flex: 1; overflow: auto; position: relative; scroll-behavior: smooth; cursor: grab; }
    .timeline-body-scroll.grabbing { cursor: grabbing; scroll-behavior: auto; }
    .timeline-spacer { height: 40px; background: white; width: 100%; position: relative; z-index: 160; }
    .timeline-spacer.bottom { height: 100px; }
    .body-layout-wrapper { display: flex; min-height: 100%; }
    .time-axis-col { flex-shrink: 0; background: white; border-right: 1px solid #f1f5f9; position: sticky; left: 0; z-index: 150; }
    .hour-cell { position: relative; }
    .h-label { position: absolute; top: 0; left: 50%; transform: translate(-50%, -50%); font-size: 11px; font-weight: 800; color: #94a3b8; background: white; padding: 2px 4px; }
    .grid-canvas { position: relative; background: #f8fafc; flex-shrink: 0; }
    .l { position: absolute; left: 0; right: 0; height: 1px; background: #f1f5f9; }
    .l.bold { background: #e2e8f0; }
    .columns-container { display: flex; height: 100%; }
    .staff-col { position: relative; height: 100%; border-right: 1px solid #f1f5f9; box-sizing: border-box; flex-shrink: 0; }
    .slot-btn { width: 100%; border: none; background: transparent; cursor: pointer; display: block; }
    .appt-box { position: absolute; left: 4px; right: 4px; background: white; border-radius: 14px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); cursor: pointer; transition: transform 0.1s; border: 1px solid #f1f5f9; overflow: hidden; }
    .appt-box:active { transform: scale(0.98); }
    .appt-content { height: 100%; border-left: 5px solid var(--status-color); padding: 8px; display: flex; flex-direction: column; overflow: hidden; }
    .tm { font-size: 10px; font-weight: 800; color: var(--status-color); }
    .st { font-size: 8px; font-weight: 900; text-transform: uppercase; color: white; background: var(--status-color); padding: 1px 5px; border-radius: 4px; }
    .cl { font-size: 12px; font-weight: 800; color: #0f172a; line-height: 1.2; word-break: break-word; }
    .sv { font-size: 10px; color: #64748b; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
    .now-indicator { position: absolute; left: 0; right: 0; z-index: 180; pointer-events: none; }
    .now-indicator .line { height: 2px; background: #ef4444; width: 100%; box-shadow: 0 0 8px rgba(239, 68, 68, 0.4); }
    .now-indicator .dot { position: absolute; left: -4px; top: -3px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; box-shadow: 0 0 10px #ef4444; animation: pulse 2s infinite; }
    .now-indicator .label { position: absolute; left: -50px; top: -10px; background: #ef4444; color: white; font-size: 10px; font-weight: 900; padding: 2px 6px; border-radius: 6px; box-shadow: 0 4px 10px rgba(239, 68, 68, 0.3); }
    @keyframes pulse { 0% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.5); opacity: 0.5; } 100% { transform: scale(1); opacity: 1; } }
</style>
