<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { createEventDispatcher } from 'svelte';

    const dispatch = createEventDispatcher();

    const menuItems = [
        { id: 'management', label: 'База', icon: '📊' },
        { id: 'timeline', label: 'Таймлайн', icon: '🕒' }
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
                <span>ADMIN PANEL</span>
            </div>
        </div>

        <nav class="nav-menu">
            {#each menuItems as item}
                <button
                    class="nav-btn"
                    class:active={$activeTab === item.id}
                    on:click={() => activeTab.set(item.id)}
                >
                    <span class="icon">{item.icon}</span>
                    <span class="label">{item.label}</span>
                </button>
            {/each}
        </nav>
    </div>

    <div class="sidebar-calendar">
        <div class="cal-label">ВЫБОР ДАТЫ</div>
        <CalendarScreen on:dateSelected={handleDateSelected} />
    </div>
</aside>

<style>
    .sidebar {
        width: 300px;
        height: 100vh;
        background: white;
        border-right: 1px solid #f1f5f9;
        display: flex;
        flex-direction: column;
        position: sticky;
        top: 0;
        padding: 24px;
        flex-shrink: 0;
        box-sizing: border-box;
    }

    .sidebar-top { flex: 0 0 auto; margin-bottom: 32px; }

    .logo { display: flex; align-items: center; gap: 12px; margin-bottom: 40px; }
    .logo-icon {
        width: 44px; height: 44px; background: var(--primary-gradient);
        color: white; border-radius: 12px; display: flex;
        justify-content: center; align-items: center; font-weight: 900;
        font-size: 18px; box-shadow: 0 4px 15px rgba(56, 151, 240, 0.2);
    }
    .logo-text h1 { font-size: 18px; margin: 0; color: #0f172a; font-weight: 800; }
    .logo-text span { font-size: 9px; color: #94a3b8; font-weight: 800; letter-spacing: 1px; }

    .nav-menu { display: flex; flex-direction: column; gap: 8px; }
    .nav-btn {
        display: flex; align-items: center; gap: 12px; padding: 14px 16px;
        border: none; background: none; border-radius: 14px;
        color: #64748b; font-weight: 700; cursor: pointer; transition: all 0.2s;
        text-align: left; width: 100%;
    }
    .nav-btn:hover { background: #f8fafc; color: #0f172a; }
    .nav-btn.active { background: #eff6ff; color: var(--primary-color); }

    .sidebar-calendar {
        flex: 1;
        border-top: 1px solid #f1f5f9;
        padding-top: 24px;
        overflow: hidden;
    }
    .cal-label {
        font-size: 10px; font-weight: 800; color: #94a3b8;
        margin-bottom: 16px; letter-spacing: 1px;
    }

    /* Обеспечиваем, чтобы календарь не "схлопывался" */
    :global(.sidebar .calendar-container) { padding: 0 !important; border: none !important; box-shadow: none !important; width: 100% !important; }
</style>
