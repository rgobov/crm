<script>
    import { onMount } from 'svelte';
    import { user, logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import EmployeeHomeScreen from '$lib/components/employee/EmployeeHomeScreen.svelte';

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.hide();
        }

        // Защита роута: если не сотрудник - уходим
        if ($user && $user.role !== 'EMPLOYEE') {
            goto('/');
        }
    });

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="employee-shell">
    <header class="header">
        <div class="brand">CRM <span>Master</span></div>
        <button class="logout-link" on:click={handleLogout}>Выйти</button>
    </header>

    <main>
        <EmployeeHomeScreen />
    </main>

    <!-- Нижняя навигация для мастера (упрощенная) -->
    <nav class="bottom-nav">
        <button class="nav-item active">
            <span class="icon">🏠</span>
            <span class="label">Главная</span>
        </button>
        <button class="nav-item" on:click={() => alert('В разработке')}>
            <span class="icon">📅</span>
            <span class="label">Мой график</span>
        </button>
    </nav>
</div>

<style>
    .employee-shell { min-height: 100vh; background: #f8fafc; display: flex; flex-direction: column; }

    .header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 16px 20px; background: white; border-bottom: 1px solid #f1f5f9;
    }
    .brand { font-weight: 900; font-size: 18px; color: #0f172a; }
    .brand span { color: var(--primary-color); }
    .logout-link { background: none; border: none; color: #ef4444; font-size: 13px; font-weight: 600; cursor: pointer; }

    main { flex: 1; }

    .bottom-nav {
        position: fixed; bottom: 0; left: 0; right: 0;
        background: white; display: flex; justify-content: space-around;
        padding: 12px 0; border-top: 1px solid #f1f5f9; z-index: 100;
    }
    .nav-item {
        display: flex; flex-direction: column; align-items: center; gap: 4px;
        border: none; background: none; color: #94a3b8; cursor: pointer;
    }
    .nav-item.active { color: var(--primary-color); }
    .icon { font-size: 20px; }
    .label { font-size: 10px; font-weight: 700; text-transform: uppercase; }
</style>
