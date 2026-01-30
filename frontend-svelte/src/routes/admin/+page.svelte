<script>
    import { onMount } from 'svelte';
    import { user, logout } from '$lib/stores/auth.js';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';

    let stats = {
        totalClients: 0,
        todaysAppointmentsCount: 0,
        totalResources: 0
    };
    let isLoading = true;

    onMount(async () => {
        try {
            stats = await adminService.getDashboardStats();
        } catch (e) {
            console.error('Failed to load dashboard stats');
        } finally {
            isLoading = false;
        }
    });

    const directories = [
        { id: 'staff', title: 'Персонал', icon: '👤', desc: 'Сотрудники и роли' },
        { id: 'resources', title: 'Ресурсы', icon: '🛠️', desc: 'Оборудование и залы' },
        { id: 'services', title: 'Услуги', icon: '✂️', desc: 'Ваш прайс-лист' }
    ];

    const integrations = [
        { id: 'wappi', title: 'Напоминания Wappi.pro', icon: '🔔', desc: 'Автоматизация уведомлений' }
    ];

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="page">
    <div class="header">
        <div class="user-card">
            <div class="avatar">{$user?.name?.charAt(0) || 'A'}</div>
            <div class="user-text">
                <h2>{$user?.name || 'Администратор'}</h2>
                <p>Панель управления 999</p>
            </div>
        </div>
        <button class="logout-btn" on:click={handleLogout}>Выйти</button>
    </div>

    {#if isLoading}
        <div class="loading-overlay">
            <span class="spinner"></span>
            <p>Загрузка статистики...</p>
        </div>
    {:else}
        <!-- Сетка статистики (Stats Grid из Flutter) -->
        <div class="stats-grid">
            <div class="stat-card blue">
                <span class="stat-icon">👥</span>
                <span class="stat-value">{stats.totalClients}</span>
                <span class="stat-label">Клиенты</span>
            </div>
            <div class="stat-card orange">
                <span class="stat-icon">📅</span>
                <span class="stat-value">{stats.todaysAppointmentsCount}</span>
                <span class="stat-label">Сегодня</span>
            </div>
            <div class="stat-card green">
                <span class="stat-icon">🛠️</span>
                <span class="stat-value">{stats.totalResources}</span>
                <span class="stat-label">Ресурсы</span>
            </div>
        </div>

        <section class="menu-section">
            <h3>Управление справочниками</h3>
            <div class="menu-list">
                {#each directories as item}
                    <button class="menu-item" on:click={() => goto(`/admin/${item.id}`)}>
                        <span class="menu-icon">{item.icon}</span>
                        <div class="menu-info">
                            <h4>{item.title}</h4>
                            <p>{item.desc}</p>
                        </div>
                        <span class="chevron">›</span>
                    </button>
                {/each}
            </div>
        </section>

        <section class="menu-section">
            <h3>Интеграции</h3>
            <div class="menu-list">
                {#each integrations as item}
                    <button class="menu-item" on:click={() => goto(`/admin/settings/${item.id}`)}>
                        <span class="menu-icon">{item.icon}</span>
                        <div class="menu-info">
                            <h4>{item.title}</h4>
                            <p>{item.desc}</p>
                        </div>
                        <span class="chevron">›</span>
                    </button>
                {/each}
            </div>
        </section>
    {/if}
</div>

<style>
    .page {
        padding: 20px;
        background-color: var(--bg-color);
        max-width: 600px;
        margin: 0 auto;
    }

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
    }

    .user-card {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .avatar {
        width: 44px;
        height: 44px;
        background: var(--primary-gradient);
        color: white;
        border-radius: 14px;
        display: flex;
        justify-content: center;
        align-items: center;
        font-weight: 800;
        font-size: 18px;
    }

    h2 { font-size: 17px; margin: 0; color: #0f172a; }
    .user-text p { margin: 2px 0 0 0; font-size: 12px; color: var(--hint-color); }

    .logout-btn {
        background: #fee2e2;
        color: #ef4444;
        border: none;
        padding: 8px 14px;
        border-radius: 10px;
        font-size: 12px;
        font-weight: 700;
        cursor: pointer;
    }

    /* Stats Grid */
    .stats-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 12px;
        margin-bottom: 32px;
    }

    .stat-card {
        background: white;
        padding: 16px 8px;
        border-radius: 20px;
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        box-shadow: var(--shadow);
    }

    .stat-icon { font-size: 20px; margin-bottom: 8px; }
    .stat-value { font-size: 20px; font-weight: 800; color: #0f172a; }
    .stat-label { font-size: 11px; color: var(--hint-color); margin-top: 2px; }

    .stat-card.blue .stat-icon { color: #3897f0; }
    .stat-card.orange .stat-icon { color: #f59e0b; }
    .stat-card.green .stat-icon { color: #10b981; }

    /* Sections */
    .menu-section { margin-bottom: 24px; }
    h3 { font-size: 14px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; padding-left: 4px; }

    .menu-list { background: white; border-radius: 20px; overflow: hidden; box-shadow: var(--shadow); }

    .menu-item {
        width: 100%;
        display: flex;
        align-items: center;
        padding: 16px;
        background: white;
        border: none;
        border-bottom: 1px solid #f1f5f9;
        cursor: pointer;
        text-align: left;
        transition: background 0.2s;
    }

    .menu-item:last-child { border-bottom: none; }
    .menu-item:active { background: #f8fafc; }

    .menu-icon { font-size: 24px; margin-right: 16px; }
    .menu-info { flex: 1; }
    h4 { font-size: 15px; margin: 0; color: #1e293b; font-weight: 600; }
    .menu-info p { margin: 2px 0 0 0; font-size: 12px; color: var(--hint-color); }
    .chevron { font-size: 20px; color: #cbd5e1; margin-left: 8px; }

    .loading-overlay { text-align: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
