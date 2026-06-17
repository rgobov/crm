<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import { createEventDispatcher } from 'svelte';

    export let activeTab = 'schedule';
    const dispatch = createEventDispatcher();

    const menuItems = [
        { id: 'schedule', label: 'Расписание дня', icon: '🕒' },
        { id: 'calendar', label: 'Календарь месяца', icon: '📅' },
        { id: 'clients', label: 'База клиентов', icon: '👥' }
    ];

    function handleDateSelected(event) {
        dispatch('dateChange', event.detail);
    }
</script>

<aside class="sidebar">
    <div class="sidebar-top">
        <div class="logo">
            <div class="logo-icon">999</div>
            <div class="logo-text">
                <h1>CRM Система</h1>
                <span>MANAGER PANEL</span>
            </div>
        </div>

        <nav class="nav-menu">
            {#each menuItems as item}
                <button
                    class="nav-btn"
                    class:active={activeTab === item.id}
                    on:click={() => dispatch('tabChange', item.id)}
                >
                    <span class="icon">{item.icon}</span>
                    <span class="label">{item.label}</span>
                </button>
            {/each}
        </nav>
    </div>

    <div class="sidebar-calendar">
        <div class="cal-label">БЫСТРЫЙ ПЕРЕХОД</div>
        <CalendarScreen on:dateSelected={handleDateSelected} />
    </div>
</aside>

<style>
    /* Стили идентичны админскому сайдбару для единства дизайна */
    .sidebar { width: 320px; height: 100vh; background: white; border-right: 1px solid #f1f5f9; display: flex; flex-direction: column; position: sticky; top: 0; padding: 24px; flex-shrink: 0; }
    .sidebar-top { flex: 1; }
    .logo { display: flex; align-items: center; gap: 12px; margin-bottom: 40px; }
    .logo-icon { width: 44px; height: 44px; background: #8b5cf6; color: white; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 18px; box-shadow: 0 4px 15px rgba(139, 92, 246, 0.2); }
    .logo-text h1 { font-size: 18px; margin: 0; color: #0f172a; font-weight: 800; }
    .logo-text span { font-size: 9px; color: #94a3b8; font-weight: 800; letter-spacing: 1px; }
    .nav-menu { display: flex; flex-direction: column; gap: 8px; }
    .nav-btn { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border: none; background: none; border-radius: 14px; color: #64748b; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    .nav-btn:hover { background: #f8fafc; color: #0f172a; }
    .nav-btn.active { background: #f5f3ff; color: #8b5cf6; }
    .sidebar-calendar { margin-top: auto; padding-top: 24px; border-top: 1px solid #f1f5f9; }
    .cal-label { font-size: 10px; font-weight: 800; color: #94a3b8; margin-bottom: 16px; letter-spacing: 1px; }
    :global(.sidebar .calendar-container) { padding: 0 !important; box-shadow: none !important; border: none !important; }
</style>
