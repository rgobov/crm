<script>
    import { onMount, onDestroy } from 'svelte';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';

    export let day = new Date();
    export let appointments = [];
    export let staff = [];

    // События
    export let onAppointmentTap = () => {};
    export let onEmptySlotTap = () => {};
    export let onStatusUpdate = () => {};

    const HOUR_HEIGHT = 80;
    const STAFF_WIDTH = 150;
    const TIME_COLUMN_WIDTH = 60;

    let startHour = 8;
    let endHour = 22;
    let nowLinePosition = -1;
    let timer;

    let scrollHeader;
    let scrollBody;

    // Синхронизация горизонтального скролла шапки и тела
    function syncScroll(e) {
        if (e.target === scrollBody) {
            scrollHeader.scrollLeft = scrollBody.scrollLeft;
        }
    }

    onMount(() => {
        updateNowLine();
        timer = setInterval(updateNowLine, 60000); // Обновляем раз в минуту
    });

    onDestroy(() => {
        clearInterval(timer);
    });

    function updateNowLine() {
        const now = new Date();
        if (now.toDateString() === day.toDateString()) {
            const minutes = (now.getHours() - startHour) * 60 + now.getMinutes();
            nowLinePosition = minutes * (HOUR_HEIGHT / 60);
        } else {
            nowLinePosition = -1;
        }
    }

    function getApptStyle(appt) {
        const start = new Date(appt.startTime);
        const minutesFromStart = (start.getHours() - startHour) * 60 + start.getMinutes();
        const top = minutesFromStart * (HOUR_HEIGHT / 60);
        const height = appt.durationInMinutes * (HOUR_HEIGHT / 60);

        return `top: ${top}px; height: ${height}px;`;
    }

    function getStatusColor(status) {
        const colors = {
            'SCHEDULED': '#3897f0',
            'CONFIRMED': '#26A69A',
            'NEEDS_CALL': '#FFA726',
            'COMPLETED': '#94a3b8',
            'CANCELLED': '#ef4444'
        };
        return colors[status] || '#3897f0';
    }
</script>

<div class="timeline-container">
    <!-- ШАПКА МАСТЕРОВ -->
    <div class="header-wrapper">
        <div class="time-corner">
            <span>🕒</span>
        </div>
        <div class="staff-header" bind:this={scrollHeader}>
            {#each staff as s}
                <div class="staff-cell" style="width: {STAFF_WIDTH}px">
                    <span class="name">{s.name}</span>
                    <span class="spec">{s.specialty}</span>
                </div>
            {/each}
        </div>
    </div>

    <!-- ТЕЛО РАСПИСАНИЯ -->
    <div class="body-wrapper" on:scroll={syncScroll} bind:this={scrollBody}>
        <!-- Колонка времени (Sticky) -->
        <div class="time-column" style="width: {TIME_COLUMN_WIDTH}px">
            {#each Array(endHour - startHour + 1) as _, i}
                <div class="hour-cell" style="height: {HOUR_HEIGHT}px">
                    {startHour + i}:00
                </div>
            {/each}
        </div>

        <!-- Сетка мастеров -->
        <div class="grid-content" style="width: {staff.length * STAFF_WIDTH}px">
            <!-- Горизонтальные линии времени -->
            {#each Array(endHour - startHour + 1) as _, i}
                <div class="grid-line" style="top: {i * HOUR_HEIGHT}px"></div>
            {/each}

            <!-- Колонки мастеров -->
            {#each staff as s, sIdx}
                <div class="staff-column" style="left: {sIdx * STAFF_WIDTH}px; width: {STAFF_WIDTH}px">
                    <!-- Записи мастера -->
                    {#each appointments.filter(a => a.staffMemberId === s.id) as appt}
                        <div
                            class="appt-card"
                            style="{getApptStyle(appt)} background-color: {getStatusColor(appt.status)}"
                            on:click|stopPropagation={() => onAppointmentTap(appt)}
                        >
                            <span class="client">{appt.clientName}</span>
                            <span class="service">{appt.service}</span>
                        </div>
                    {/each}

                    <!-- Индикатор текущего времени -->
                    {#if nowLinePosition >= 0}
                        <div class="now-line" style="top: {nowLinePosition}px"></div>
                    {/if}
                </div>
            {/each}
        </div>
    </div>
</div>

<style>
    .timeline-container {
        display: flex;
        flex-direction: column;
        height: 100%;
        background: white;
        overflow: hidden;
    }

    /* Header */
    .header-wrapper {
        display: flex;
        height: 60px;
        background: white;
        border-bottom: 1px solid #f1f5f9;
        z-index: 20;
    }
    .time-corner {
        width: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #94a3b8;
        border-right: 1px solid #f1f5f9;
    }
    .staff-header {
        flex: 1;
        display: flex;
        overflow-x: hidden; /* Скроллится синхронно с телом */
    }
    .staff-cell {
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        padding: 0 10px;
        border-right: 1px solid #f8fafc;
    }
    .staff-cell .name { font-size: 13px; font-weight: 800; color: #1e293b; }
    .staff-cell .spec { font-size: 11px; color: #94a3b8; }

    /* Body */
    .body-wrapper {
        flex: 1;
        display: flex;
        overflow: auto;
        position: relative;
    }

    .time-column {
        flex-shrink: 0;
        background: #f8fafc;
        border-right: 1px solid #f1f5f9;
        position: sticky;
        left: 0;
        z-index: 10;
    }
    .hour-cell {
        display: flex;
        justify-content: center;
        padding-top: 4px;
        font-size: 11px;
        font-weight: 700;
        color: #94a3b8;
    }

    .grid-content {
        position: relative;
        background: white;
    }
    .grid-line {
        position: absolute;
        left: 0;
        right: 0;
        height: 1px;
        background: #f1f5f9;
    }

    .staff-column {
        position: absolute;
        top: 0;
        bottom: 0;
        border-right: 1px solid #f1f5f9;
    }

    /* Appointment Cards */
    .appt-card {
        position: absolute;
        left: 4px;
        right: 4px;
        border-radius: 8px;
        padding: 6px;
        color: white;
        font-size: 11px;
        overflow: hidden;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        cursor: pointer;
        z-index: 5;
    }
    .appt-card .client { display: block; font-weight: 800; margin-bottom: 2px; }
    .appt-card .service { opacity: 0.9; font-size: 10px; }

    /* Indicators */
    .now-line {
        position: absolute;
        left: 0;
        right: 0;
        height: 2px;
        background: #ef4444;
        z-index: 15;
    }
    .now-line::before {
        content: '';
        position: absolute;
        left: 0;
        top: -3px;
        width: 8px;
        height: 8px;
        background: #ef4444;
        border-radius: 50%;
    }
</style>
