<script>
    import { onMount } from 'svelte';
    import { user, logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';

    let selectedDate = new Date();

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.hide();
        }

        // Защита роута
        if ($user && $user.role !== 'MANAGER' && $user.role !== 'ADMIN') {
            goto('/');
        }
    });

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="manager-shell">
    <header class="header">
        <div class="user-info">
            <div class="avatar">M</div>
            <div class="text">
                <h2>{$user?.name || 'Менеджер'}</h2>
                <p>Управление расписанием</p>
            </div>
        </div>
        <button class="logout-btn" on:click={handleLogout}>Выйти</button>
    </header>

    <main class="content">
        <ScheduleScreen initialDate={selectedDate} />
    </main>
</div>

<style>
    .manager-shell { display: flex; flex-direction: column; min-height: 100vh; background: #f8fafc; }

    .header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 16px 20px; background: white; border-bottom: 1px solid #f1f5f9;
    }
    .user-info { display: flex; align-items: center; gap: 12px; }
    .avatar {
        width: 36px; height: 36px; background: #8b5cf6; color: white;
        border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 800;
    }
    h2 { font-size: 15px; margin: 0; color: #0f172a; }
    .text p { margin: 0; font-size: 11px; color: #64748b; }

    .logout-btn { background: #f1f5f9; border: none; padding: 8px 12px; border-radius: 8px; font-size: 12px; color: #64748b; cursor: pointer; }

    .content { flex: 1; overflow: hidden; }
</style>
