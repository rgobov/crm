<script>
    import { onMount } from 'svelte';
    import { user, logout } from '$lib/stores/auth.js';
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { goto } from '$app/navigation';
    import ManagementTab from '$lib/components/admin/ManagementTab.svelte';
    import CalendarTab from '$lib/components/admin/CalendarTab.svelte';

    // Мгновенное переключение вкладок без перезагрузки (как во Flutter)
    const tabs = {
        management: ManagementTab,
        calendar: CalendarTab
    };

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.hide();
        }
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
                <p>{$activeTab === 'management' ? 'Панель управления' : 'Календарь загрузки'}</p>
            </div>
        </div>
        <button class="logout-btn" on:click={handleLogout}>Выйти</button>
    </div>

    <!-- КОНТЕНТ ВКЛАДКИ (Переключается мгновенно) -->
    <div class="tab-view">
        <svelte:component this={tabs[$activeTab]} />
    </div>
</div>

<style>
    .shell {
        display: flex;
        flex-direction: column;
        min-height: 100vh;
        background-color: var(--bg-color);
    }

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 20px 20px 10px 20px;
        background: white;
        border-bottom: 1px solid rgba(0,0,0,0.03);
    }

    .user-info { display: flex; align-items: center; gap: 12px; }

    .avatar {
        width: 40px; height: 44px;
        background: var(--primary-gradient);
        color: white; border-radius: 12px;
        display: flex; justify-content: center; align-items: center;
        font-weight: 800;
    }

    h2 { font-size: 16px; margin: 0; color: #0f172a; }
    .text p { margin: 0; font-size: 12px; color: var(--hint-color); }

    .logout-btn {
        background: #f1f5f9; color: #64748b;
        border: none; padding: 8px 12px;
        border-radius: 10px; font-size: 12px; font-weight: 600;
    }

    .tab-view { flex: 1; overflow-y: auto; }
</style>
