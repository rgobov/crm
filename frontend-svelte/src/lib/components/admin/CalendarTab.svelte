<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import { activeTab } from '$lib/stores/dashboardStore.js';

    function handleDateSelected(event) {
        const date = event.detail.date;
        console.log('Выбрана дата для расписания:', date);
        // В будущем здесь будет переключение на таймлайн дня
    }
</script>

<div class="tab-content">
    <div class="header-row">
        <h2>Календарь записей</h2>
        <button class="today-btn">СЕГОДНЯ</button>
    </div>

    <!-- Компонент сетки месяца -->
    <CalendarScreen on:dateSelected={handleDateSelected} />
</div>

<style>
    .tab-content { padding: 20px; animation: fadeIn 0.3s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    h2 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }

    .today-btn {
        background: var(--primary-gradient); color: white; border: none;
        padding: 10px 20px; border-radius: 14px; font-weight: 700;
        font-size: 13px; box-shadow: 0 4px 15px rgba(56, 151, 240, 0.2);
        cursor: pointer;
    }
</style>
