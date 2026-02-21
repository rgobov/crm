<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { createEventDispatcher } from 'svelte';

    const dispatch = createEventDispatcher();

    let now = new Date();
    let currYear = now.getFullYear();
    let currMonth = now.getMonth();

    let days = [];
    let workloadData = {};
    let isLoading = true;

    const monthNames = ["Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"];

    $: if (workloadData || currMonth || currYear) {
        renderCalendar();
    }

    $: if ($activeBranchId !== undefined) {
        loadWorkload();
    }

    onMount(async () => {
        await loadWorkload();
    });

    async function loadWorkload() {
        if ($activeBranchId === undefined) return;
        isLoading = true;
        try {
            const data = await adminService.getWorkloadForMonth(currYear, currMonth + 1, $activeBranchId);
            workloadData = {};
            data.forEach(item => {
                workloadData[item.day] = item.appointmentCount;
            });
        } catch (e) {
            console.error('Workload load failed', e);
        } finally {
            isLoading = false;
        }
    }

    function renderCalendar() {
        let firstDayOfMonth = new Date(currYear, currMonth, 1).getDay();
        let adjFirstDay = firstDayOfMonth === 0 ? 6 : firstDayOfMonth - 1;
        let lastDateOfMonth = new Date(currYear, currMonth + 1, 0).getDate();
        let lastDayOfLastMonth = new Date(currYear, currMonth, 0).getDate();

        let tempDays = [];
        for (let i = adjFirstDay; i > 0; i--) {
            tempDays.push({ day: lastDayOfLastMonth - i + 1, current: false });
        }
        for (let i = 1; i <= lastDateOfMonth; i++) {
            let isToday = i === now.getDate() && currMonth === now.getMonth() && currYear === now.getFullYear();
            tempDays.push({
                day: i,
                current: true,
                today: isToday,
                count: workloadData[i] || 0
            });
        }
        days = tempDays;
    }

    function changeMonth(dir) {
        currMonth += dir;
        if (currMonth < 0) { currMonth = 11; currYear--; }
        else if (currMonth > 11) { currMonth = 0; currYear++; }
        loadWorkload();
    }

    // Цвета для фона ячеек и легенды
    const colors = {
        low: '#dcfce7',
        medium: '#fef9c3',
        high: '#ffedd5',
        max: '#fee2e2'
    };

    function getWorkloadColor(count) {
        if (!count || count === 0) return 'transparent';
        if (count <= 2) return colors.low;
        if (count <= 5) return colors.medium;
        if (count <= 8) return colors.high;
        return colors.max;
    }

    function selectDate(day) {
        const selectedDate = new Date(currYear, currMonth, day, 12, 0, 0);
        dispatch('dateSelected', { date: selectedDate });
    }
</script>

<div class="calendar-page-limiter">
    <div class="calendar-container">
        <div class="cal-header">
            <button class="nav-btn" on:click={() => changeMonth(-1)}>‹</button>
            <h3>{monthNames[currMonth]} {currYear}</h3>
            <button class="nav-btn" on:click={() => changeMonth(1)}>›</button>
        </div>

        <div class="weekdays">
            <div>Пн</div><div>Вт</div><div>Ср</div><div>Чт</div><div>Пт</div><div>Сб</div><div>Вс</div>
        </div>

        <div class="days-grid" class:loading={isLoading}>
            {#each days as d}
                <div
                    class="day-cell"
                    class:inactive={!d.current}
                    class:is-today={d.today}
                    on:click={() => d.current && selectDate(d.day)}
                >
                    {#if d.current && d.count > 0}
                        <div class="workload-bg" style="background-color: {getWorkloadColor(d.count)}"></div>
                    {/if}
                    <span class="day-num" class:today-text={d.today}>{d.day}</span>
                </div>
            {/each}
        </div>

        <div class="legend">
            <div class="item">
                <span class="dot" style="background: {colors.low}"></span>
                <span class="lbl">1-2</span>
            </div>
            <div class="item">
                <span class="dot" style="background: {colors.medium}"></span>
                <span class="lbl">3-5</span>
            </div>
            <div class="item">
                <span class="dot" style="background: {colors.high}"></span>
                <span class="lbl">6-8</span>
            </div>
            <div class="item">
                <span class="dot" style="background: {colors.max}"></span>
                <span class="lbl">9+</span>
            </div>
        </div>
    </div>
</div>

<style>
    .calendar-page-limiter { width: 100%; box-sizing: border-box; }
    .calendar-container {
        padding: 16px;
        background: #fdf6e3;
        border-radius: 24px;
        border: 1.5px solid #ddd6c1;
    }

    .cal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .cal-header h3 { margin: 0; font-size: 15px; font-weight: 850; color: #073642; }

    .nav-btn {
        background: #eee8d5;
        border: 1px solid #ddd6c1;
        width: 32px; height: 32px; border-radius: 10px;
        font-size: 18px; cursor: pointer; color: #268bd2;
        display: flex; align-items: center; justify-content: center;
        transition: 0.2s;
    }
    .nav-btn:hover { background: #fdf6e3; border-color: #268bd2; }

    .weekdays {
        display: grid; grid-template-columns: repeat(7, 1fr);
        text-align: center; font-size: 10px; font-weight: 800;
        color: #93a1a1;
        text-transform: uppercase; margin-bottom: 10px;
    }

    .days-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }

    .day-cell {
        aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
        border-radius: 50%; cursor: pointer; position: relative; transition: 0.2s;
        border: 2px solid transparent;
    }
    .day-cell:hover:not(.inactive) { background: #eee8d5; }

    .day-num { font-size: 14px; font-weight: 750; color: #586e75; z-index: 2; }

    .inactive { opacity: 0.15; pointer-events: none; }

    .is-today {
        border-color: #268bd2 !important;
    }
    .today-text {
        color: #073642 !important;
        font-weight: 900 !important;
    }

    .workload-bg { position: absolute; width: 30px; height: 30px; border-radius: 50%; z-index: 1; }

    /* УЛУЧШЕННАЯ ЛЕГЕНДА */
    .legend {
        display: flex; justify-content: space-between;
        margin-top: 20px; padding: 12px 4px 0; border-top: 1px solid #ddd6c1;
    }
    .legend .item { display: flex; align-items: center; gap: 6px; }
    .legend .lbl { font-size: 11px; font-weight: 800; color: #586e75; } /* Base01 - более четкий текст */

    .dot {
        width: 12px; height: 12px; /* Увеличено */
        border-radius: 50%;
        display: inline-block;
        border: 1px solid rgba(7, 54, 66, 0.1); /* Тонкий контур для четкости */
    }
</style>
