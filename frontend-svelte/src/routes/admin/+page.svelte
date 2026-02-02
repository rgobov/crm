<script>
    import { onMount, onDestroy } from 'svelte';
    import { user, logout } from '$lib/stores/auth.js';
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { websocketService } from '$lib/services/websocketService.js';
    import { goto } from '$app/navigation';
    import ManagementTab from '$lib/components/admin/ManagementTab.svelte';
    import CalendarTab from '$lib/components/admin/CalendarTab.svelte';

    // Для 'calendar' и 'timeline' используем один компонент,
    // но с разным начальным состоянием внутри него
    const tabs = {
        management: ManagementTab,
        calendar: CalendarTab,
        timeline: CalendarTab
    };

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.hide();
        }
        websocketService.connect();
    });

    onDestroy(() => {
        websocketService.disconnect();
    });

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="shell">
    <div class="header">
        <div class="user-info">
            <div class="avatar">{$user?.name?.charAt(0) || 'A'}</div>
            <div class="text">
                <h2>{$user?.name || 'Администратор'}</h2>
                <p>
                    {#if $activeTab === 'management'}Панель управления{/if}
                    {#if $activeTab === 'calendar'}Календарь месяца{/if}
                    {#if $activeTab === 'timeline'}Расписание дня{/if}
                </p>
            </div>
        </div>
        <button class="logout-btn" on:click={handleLogout}>Выйти</button>
    </div>

    <div class="tab-view">
        <svelte:component this={tabs[$activeTab]} />
    </div>

    <!-- НИЖНЯЯ НАВИГАЦИЯ -->
    <nav class="bottom-nav">
        <button
            class="nav-item"
            class:active={$activeTab === 'management'}
            on:click={() => activeTab.set('management')}
        >
            <span class="icon">📊</span>
            <span class="label">База</span>
        </button>
        <button
            class="nav-item"
            class:active={$activeTab === 'calendar'}
            on:click={() => activeTab.set('calendar')}
        >
            <span class="icon">📅</span>
            <span class="label">Месяц</span>
        </button>
        <button
            class="nav-item"
            class:active={$activeTab === 'timeline'}
            on:click={() => activeTab.set('timeline')}
        >
            <span class="icon">🕒</span>
            <span class="label">Таймлайн</span>
        </button>
    </nav>
</div>

<style>
    .shell { display: flex; flex-direction: column; height: 100vh; background-color: var(--bg-color); }

    .header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 16px 20px; background: white; border-bottom: 1px solid rgba(0,0,0,0.03);
        flex-shrink: 0;
    }
    .user-info { display: flex; align-items: center; gap: 12px; }
    .avatar { width: 36px; height: 40px; background: var(--primary-gradient); color: white; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    h2 { font-size: 15px; margin: 0; color: #0f172a; }
    .text p { margin: 0; font-size: 11px; color: var(--hint-color); font-weight: 600; text-transform: uppercase; }
    .logout-btn { background: #f1f5f9; color: #64748b; border: none; padding: 6px 12px; border-radius: 8px; font-size: 11px; font-weight: 700; cursor: pointer; }

    .tab-view { flex: 1; overflow-y: auto; padding-bottom: 80px; }

    .bottom-nav {
        position: fixed; bottom: 0; left: 0; right: 0;
        background: white; display: flex; justify-content: space-around;
        padding: 10px 0; border-top: 1px solid #f1f5f9; z-index: 1000;
        box-shadow: 0 -4px 20px rgba(0,0,0,0.04);
    }

    .nav-item {
        display: flex; flex-direction: column; align-items: center; gap: 4px;
        border: none; background: none; color: #94a3b8; cursor: pointer; flex: 1;
    }

    .nav-item.active { color: var(--primary-color); }
    .icon { font-size: 20px; margin-bottom: 2px; }
    .label { font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; }
</style>
