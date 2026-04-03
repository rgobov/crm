<script>
    import { onMount } from 'svelte';
    import { employeeService } from '$lib/services/employeeService.js';
    import { user, logout } from '$lib/stores/auth.js';
    import { fade, slide } from 'svelte/transition';
    import { goto } from '$app/navigation';

    let stats = {
        todayAppointmentsCount: 0,
        monthlyWorkload: [],
        staffName: ''
    };
    let isLoading = true;

    onMount(async () => {
        await loadDashboard();
    });

    async function loadDashboard() {
        isLoading = true;
        try {
            stats = await employeeService.getMyStats();
        } catch (e) {
            console.error('Failed to load employee stats', e);
        } finally {
            isLoading = false;
        }
    }

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="employee-dashboard">
    <header class="header">
        <div class="user-meta">
            <div class="avatar">{stats.staffName?.charAt(0) || 'M'}</div>
            <div class="text">
                <h1>Привет, {stats.staffName || 'Мастер'}!</h1>
                <p>Ваша рабочая панель</p>
            </div>
        </div>
        <button class="logout-btn" on:click={handleLogout}>Выйти</button>
    </header>

    <main class="content">
        <!-- КАРТОЧКА: СЕГОДНЯ -->
        <section class="stat-hero card" in:fade>
            <div class="hero-info">
                <span class="count">{stats.todayAppointmentsCount}</span>
                <span class="label">ЗАПИСЕЙ НА СЕГОДНЯ</span>
            </div>
            <button class="action-btn" on:click={() => goto('/employee/schedule')}>
                СМОТРЕТЬ ГРАФИК
            </button>
        </section>

        <div class="grid-menu">
            <button class="menu-card" on:click={() => goto('/employee/schedule')}>
                <span class="icon">📅</span>
                <div class="m-text">
                    <h3>Мои записи</h3>
                    <p>Список клиентов</p>
                </div>
            </button>

            <button class="menu-card" on:click={() => goto('/employee/shifts')}>
                <span class="icon">⏳</span>
                <div class="m-text">
                    <h3>Мои смены</h3>
                    <p>Настройка графика</p>
                </div>
            </button>
        </div>

        <section class="workload-section">
            <label>ВАША ЗАГРУЗКА (МЕСЯЦ)</label>
            <div class="mini-calendar card">
                <!-- Здесь будет визуализация календаря загрузки -->
                <p class="hint">У вас запланировано {stats.monthlyWorkload.length} рабочих дней в этом месяце</p>
            </div>
        </section>
    </main>
</div>

<style>
    .employee-dashboard { min-height: 100vh; background: #f8fafc; padding-bottom: 40px; }

    .header { background: white; padding: 24px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
    .user-meta { display: flex; align-items: center; gap: 12px; }
    .avatar { width: 44px; height: 44px; background: var(--primary-gradient); color: white; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 18px; }
    h1 { margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; }
    .text p { margin: 0; font-size: 12px; color: #94a3b8; font-weight: 600; text-transform: uppercase; }
    .logout-btn { background: #f1f5f9; border: none; padding: 8px 16px; border-radius: 10px; font-size: 12px; font-weight: 700; color: #64748b; cursor: pointer; }

    .content { padding: 20px; display: flex; flex-direction: column; gap: 20px; }
    .card { background: white; border-radius: 24px; border: 1px solid #f1f5f9; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }

    .stat-hero { padding: 32px; background: var(--primary-gradient); color: white; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 24px; border: none; }
    .hero-info { display: flex; flex-direction: column; }
    .count { font-size: 56px; font-weight: 900; line-height: 1; }
    .label { font-size: 12px; font-weight: 800; opacity: 0.8; letter-spacing: 1px; }
    .action-btn { background: white; color: var(--primary-color); border: none; padding: 14px 28px; border-radius: 14px; font-weight: 800; font-size: 13px; cursor: pointer; }

    .grid-menu { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .menu-card { background: white; border: 1px solid #f1f5f9; padding: 20px; border-radius: 24px; text-align: left; display: flex; flex-direction: column; gap: 12px; cursor: pointer; }
    .menu-card .icon { font-size: 24px; }
    .m-text h3 { margin: 0; font-size: 15px; color: #1e293b; }
    .m-text p { margin: 2px 0 0 0; font-size: 11px; color: #94a3b8; font-weight: 600; }

    .workload-section label { font-size: 11px; font-weight: 800; color: #94a3b8; letter-spacing: 1px; margin-left: 8px; margin-bottom: 8px; display: block; }
    .mini-calendar { padding: 20px; min-height: 100px; display: flex; align-items: center; justify-content: center; }
    .hint { color: #64748b; font-size: 13px; font-weight: 500; }
</style>
