<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import { onMount } from 'svelte';

    // Режим просмотра: 'month' (сетка) или 'day' (таймлайн)
    let viewMode = 'month';
    let selectedDate = new Date();

    function handleDateSelected(event) {
        selectedDate = event.detail.date;
        viewMode = 'day';
    }

    function backToMonth() {
        viewMode = 'month';
    }

    function setToday() {
        selectedDate = new Date();
        viewMode = 'day';
    }
</script>

<div class="calendar-tab">
    {#if viewMode === 'month'}
        <div class="tab-content">
            <div class="header-row">
                <h2>Календарь загрузки</h2>
                <button class="today-btn" on:click={setToday}>СЕГОДНЯ</button>
            </div>
            <CalendarScreen on:dateSelected={handleDateSelected} />
        </div>
    {:else}
        <div class="day-view-container">
            <div class="day-header">
                <button class="back-link" on:click={backToMonth}>‹ Назад к месяцу</button>
                <div class="date-title">
                    {selectedDate.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })}
                </div>
            </div>
            <ScheduleScreen initialDate={selectedDate} />
        </div>
    {/if}
</div>

<style>
    .calendar-tab { height: 100%; display: flex; flex-direction: column; }
    .tab-content { padding: 20px; animation: fadeIn 0.3s ease-out; }

    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    h2 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }

    .today-btn {
        background: var(--primary-gradient); color: white; border: none;
        padding: 10px 20px; border-radius: 14px; font-weight: 700;
        font-size: 13px; box-shadow: 0 4px 15px rgba(56, 151, 240, 0.2);
        cursor: pointer;
    }

    /* Стили для вида Дня */
    .day-view-container { flex: 1; display: flex; flex-direction: column; animation: slideIn 0.3s ease-out; }
    .day-header {
        padding: 12px 20px; background: white; border-bottom: 1px solid #f1f5f9;
        display: flex; align-items: center; justify-content: space-between;
    }
    .back-link { background: none; border: none; color: var(--primary-color); font-weight: 700; font-size: 14px; cursor: pointer; }
    .date-title { font-weight: 800; color: #1e293b; font-size: 15px; }

    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideIn { from { transform: translateX(20px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
</style>
