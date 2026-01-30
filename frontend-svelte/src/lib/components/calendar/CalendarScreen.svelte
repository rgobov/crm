<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { createEventDispatcher } from 'svelte';

    const dispatch = createEventDispatcher();

    let now = new Date();
    let currYear = now.getFullYear();
    let currMonth = now.getMonth();

    let days = [];
    let workloadData = {};
    let isLoading = true;

    const monthNames = ["Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"];

    onMount(async () => {
        await loadWorkload();
    });

    async function loadWorkload() {
        isLoading = true;
        try {
            const data = await adminService.getWorkloadForMonth(currYear, currMonth + 1);
            workloadData = {};
            data.forEach(item => {
                workloadData[item.day] = item.appointmentCount;
            });
            renderCalendar();
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
        if (count <= 2) return '#dcfce7';
        if (count <= 5) return '#fef9c3';
        if (count <= 8) return '#ffedd5';
        return '#fee2e2';
    }

    function selectDate(day) {
        const selectedDate = new Date(currYear, currMonth, day);
        dispatch('dateSelected', { date: selectedDate });
    }
</script>

<div class="calendar-wrapper">
    <div class="calendar-container">
        <div class="cal-header">
            <button on:click={() => changeMonth(-1)}>‹</button>
            <h3>{monthNames[currMonth]} {currYear}</h3>
            <button on:click={() => changeMonth(1)}>›</button>
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
                    <span class="day-num">{d.day}</span>
                    {#if d.current && d.count > 0}
                        <div class="workload-dot" style="background-color: {getWorkloadColor(d.count)}">
                            {d.count}
                        </div>
                    {/if}
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
    /* ОБЕРТКА ДЛЯ ЦЕНТРИРОВАНИЯ НА ПК */
    .calendar-wrapper {
        width: 100%;
        max-width: 500px; /* Ограничиваем ширину как у мобильного приложения */
        margin: 0 auto;   /* Центрируем */
        padding: 10px;
        box-sizing: border-box;
    }

    .calendar-container {
        padding: 20px;
        background: white;
        border-radius: 28px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.05);
        border: 1px solid rgba(0,0,0,0.02);
    }

    .cal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .cal-header h3 { margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; }
    .cal-header button { background: #f1f5f9; border: none; width: 44px; height: 44px; border-radius: 14px; font-size: 24px; cursor: pointer; color: var(--primary-color); }

    .weekdays { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; margin-bottom: 12px; }

    .days-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; transition: opacity 0.2s; }
    .loading { opacity: 0.5; }

    .day-cell { aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 16px; cursor: pointer; position: relative; }
    .day-cell:active { background: #f1f5f9; transform: scale(0.92); }
    .day-num { font-size: 16px; font-weight: 600; color: #1e293b; z-index: 2; }
    .inactive { opacity: 0.15; pointer-events: none; }

    .is-today { background: #eff6ff; border: 2px solid var(--primary-color); }
    .is-today .day-num { color: var(--primary-color); font-weight: 800; }

    .workload-dot { position: absolute; bottom: 6px; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 800; color: #1e293b; border: 2px solid white; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }

    .legend { display: flex; justify-content: space-around; margin-top: 24px; padding-top: 16px; border-top: 1px solid #f1f5f9; }
    .legend .item { display: flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 700; color: #94a3b8; }
    .dot { width: 10px; height: 10px; border-radius: 50%; }
    .dot.green { background: #dcfce7; } .dot.yellow { background: #fef9c3; } .dot.orange { background: #ffedd5; } .dot.red { background: #fee2e2; }
</style>
