<script>
    import { dashboardStore } from '$lib/stores/dashboardStore.js';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';

    let isTelegram = false;

    onMount(() => {
        // Проверка: запущен ли проект в Telegram (наличие initData)
        if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.initData) {
            isTelegram = true;
        }

        const oneMinute = 60 * 1000;
        if (!$dashboardStore.lastUpdated || (new Date() - $dashboardStore.lastUpdated > oneMinute)) {
            dashboardStore.refresh();
        }
    });

    const directories = [
        { id: 'staff', title: 'Персонал', icon: '👤', desc: 'Сотрудники и роли' },
        { id: 'resources', title: 'Ресурсы', icon: '🛠️', desc: 'Оборудование и залы' },
        { id: 'services', title: 'Услуги', icon: '✂️', desc: 'Ваш прайс-лист' },
        { id: 'clients', title: 'Клиенты', icon: '💎', desc: 'База клиентов' }
    ];

    const integrations = [
        { id: 'wappi', title: 'Напоминания Wappi.pro', icon: '🔔', desc: 'Настройка уведомлений в мессенджеры' }
    ];
</script>

<div class="tab-content">
    {#if $dashboardStore.isLoading && !$dashboardStore.lastUpdated}
        <div class="center-loading">
            <span class="spinner"></span>
            <p>Загрузка статистики...</p>
        </div>
    {:else}
        <div class="stats-grid">
            <div class="stat-card blue">
                <span class="stat-value">{$dashboardStore.stats.totalClients}</span>
                <span class="stat-label">Клиенты</span>
            </div>
            <div class="stat-card orange">
                <span class="stat-value">{$dashboardStore.stats.todaysAppointmentsCount}</span>
                <span class="stat-label">Сегодня</span>
            </div>
            <div class="stat-card green">
                <span class="stat-value">{$dashboardStore.stats.totalResources}</span>
                <span class="stat-label">Ресурсы</span>
            </div>
        </div>

        <section class="menu-section">
            <h3>Управление справочниками</h3>
            <div class="menu-list shadow-card">
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

        <!-- ОТОБРАЖАЕМ ИНТЕГРАЦИИ ТОЛЬКО В БРАУЗЕРЕ (НЕ В ТЕЛЕГРАМЕ) -->
        {#if !isTelegram}
            <section class="menu-section">
                <h3>Интеграции и настройки</h3>
                <div class="menu-list shadow-card">
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
    {/if}
</div>

<style>
    .tab-content { padding: 20px; animation: fadeIn 0.3s ease-out; padding-bottom: 40px; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

    .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 32px; }
    .stat-card { background: white; padding: 16px 8px; border-radius: 20px; display: flex; flex-direction: column; align-items: center; box-shadow: var(--shadow); }
    .stat-value { font-size: 20px; font-weight: 800; color: #0f172a; }
    .stat-label { font-size: 11px; color: var(--hint-color); margin-top: 2px; }

    .menu-section { margin-bottom: 24px; }
    .menu-section h3 { font-size: 14px; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 12px; padding-left: 4px; }
    .menu-list { background: white; border-radius: 24px; overflow: hidden; }
    .shadow-card { box-shadow: var(--shadow); border: 1px solid rgba(0,0,0,0.02); }

    .menu-item { width: 100%; display: flex; align-items: center; padding: 18px; background: white; border: none; border-bottom: 1px solid #f1f5f9; cursor: pointer; text-align: left; }
    .menu-item:last-child { border-bottom: none; }
    .menu-item:active { background: #f8fafc; }

    .menu-icon { font-size: 24px; margin-right: 16px; }
    .menu-info { flex: 1; }
    h4 { font-size: 15px; margin: 0; color: #1e293b; font-weight: 700; }
    .menu-info p { margin: 2px 0 0 0; font-size: 12px; color: var(--hint-color); }
    .chevron { font-size: 20px; color: #cbd5e1; }

    .center-loading { text-align: center; padding: 60px 20px; color: var(--hint-color); }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; margin-bottom: 12px; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
