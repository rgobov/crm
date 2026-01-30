<script>
    import { onMount } from 'svelte';
    import { dashboardStore } from '$lib/stores/dashboardStore.js';

    onMount(() => {
        // Здесь будет инициализация календаря
    });
</script>

<div class="tab-content">
    <div class="header-row">
        <h2>Календарь записей</h2>
        <button class="today-btn">Сегодня</button>
    </div>

    <div class="calendar-placeholder card">
        <div class="date-strip">
            <!-- Здесь будет горизонтальный календарь как во Flutter -->
            <p>Загрузка сетки расписания...</p>
        </div>

        <div class="empty-view">
            <span>📅</span>
            <p>Выберите дату для просмотра записей</p>
        </div>
    </div>
</div>

<style>
    .tab-content { padding: 20px; animation: fadeIn 0.2s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }

    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h2 { font-size: 20px; font-weight: 800; margin: 0; color: #0f172a; }

    .today-btn { background: #eff6ff; color: var(--primary-color); border: none; padding: 8px 16px; border-radius: 10px; font-weight: 700; font-size: 13px; }

    .calendar-placeholder { min-height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--hint-color); }
    .empty-view { text-align: center; }
    .empty-view span { font-size: 48px; display: block; margin-bottom: 12px; }
</style>
