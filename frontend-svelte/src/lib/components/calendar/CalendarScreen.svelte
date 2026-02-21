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

    function getWorkloadColor(count) {
        if (!count || count === 0) return 'transparent';
        if (count <= 2) return '#dcfce7'; // Light Green
        if (count <= 5) return '#fef9c3'; // Light Yellow
        if (count <= 8) return '#ffedd5'; // Light Orange
        return '#fee2e2'; // Light Red
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
            <div class="item"><span class="dot green"></span> 1-2</div>
            <div class="item"><span class="dot yellow"></span> 3-5</div>
            <div class="item"><span class="dot orange"></span> 6-8</div>
            <div class="item"><span class="dot red"></span> 9+</div>
        </div>
    </div>
</div>

<style>
    .calendar-page-limiter { width: 100%; box-sizing: border-box; }
    .calendar-container {
        padding: 16px;
        background: #fdf6e3; /* Base3 - кремовый */
        border-radius: 24px;
        border: 1.5px solid #ddd6c1;
    }

    .cal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .cal-header h3 { margin: 0; font-size: 15px; font-weight: 850; color: #073642; }

    .nav-btn {
        background: #eee8d5; /* Base2 */
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
        color: #93a1a1; /* Base1 */
        text-transform: uppercase; margin-bottom: 10px;
    }

    .days-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }

    .day-cell {
        aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
        border-radius: 50%; cursor: pointer; position: relative; transition: 0.2s;
        border: 2px solid transparent; /* Для выделения сегодня через border */
    }
    .day-cell:hover:not(.inactive) { background: #eee8d5; }

    .day-num { font-size: 14px; font-weight: 750; color: #586e75; /* Base01 */ z-index: 2; }

    .inactive { opacity: 0.15; pointer-events: none; }

    /* ВЫДЕЛЕНИЕ СЕГОДНЯ: СИНЯЯ РАМКА */
    .is-today {
        border-color: #268bd2 !important; /* Яркий синий контур */
    }
    .today-text {
        color: #073642 !important; /* Стандартный темный цвет */
        font-weight: 900 !important; /* Чуть жирнее */
    }

    /* ФОН ЗАГРУЗКИ - ОСТАВЛЯЕМ КАК БЫЛО */
    .workload-bg { position: absolute; width: 30px; height: 30px; border-radius: 50%; z-index: 1; }

    .legend {
        display: flex; justify-content: space-around;
        margin-top: 20px; padding-top: 12px; border-top: 1px solid #ddd6c1;
    }
    .legend .item { display: flex; align-items: center; gap: 4px; font-size: 9px; font-weight: 800; color: #93a1a1; }

    .dot { width: 6px; height: 6px; border-radius: 50%; }
    .dot.green { background: #dcfce7; } .dot.yellow { background: #fef9c3; } .dot.orange { background: #ffedd5; } .dot.red { background: #fee2e2; }
</style>
