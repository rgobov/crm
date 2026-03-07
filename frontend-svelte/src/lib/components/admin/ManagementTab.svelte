<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';
    import TelegramSettingsModal from './TelegramSettingsModal.svelte';
    import NotificationTemplatesModal from './NotificationTemplatesModal.svelte';
    import { fade, scale } from 'svelte/transition';

    let stats = {
        totalClients: 0,
        todayAppointments: 0,
        totalResources: 0,
        totalStaff: 0
    };
    let isLoading = true;
    let error = null;
    let showTelegramModal = false;
    let showTemplatesModal = false;

    onMount(async () => {
        await loadStats();
    });

    async function loadStats() {
        isLoading = true;
        error = null;
        try {
            const data = await adminService.getDashboardStats();
            stats = data;
        } catch (e) {
            console.error('Failed to load dashboard stats:', e);
            error = "Ошибка загрузки данных.";
        } finally {
            isLoading = false;
        }
    }

    const menuCards = [
        { id: 'branches', title: 'Филиалы', desc: 'Ваши точки и часовые пояса', icon: '🏢', link: '/admin/branches' },
        { id: 'staff', title: 'Персонал', desc: 'Сотрудники и роли', icon: '👤', link: '/admin/staff' },
        { id: 'resources', title: 'Ресурсы', desc: 'Оборудование и залы', icon: '⚒️', link: '/admin/resources' },
        { id: 'services', title: 'Услуги', desc: 'Ваш прайс-лист', icon: '✂️', link: '/admin/services' },
        { id: 'clients', title: 'Клиенты', desc: 'База клиентов', icon: '💎', link: '/admin/clients' }
    ];
</script>

<div class="management-container">
    <div class="management-tab">
        {#if error}
            <div class="error-banner">
                <span>⚠️ {error}</span>
                <button on:click={loadStats} type="button">Повторить</button>
            </div>
        {/if}

        <div class="stats-grid">
            <div class="stat-card">
                <span class="val">{stats.totalClients}</span>
                <span class="lbl">Клиенты</span>
            </div>
            <div class="stat-card">
                <span class="val">{stats.todayAppointments}</span>
                <span class="lbl">Сегодня</span>
            </div>
            <div class="stat-card">
                <span class="val">{stats.totalStaff}</span>
                <span class="lbl">Мастера</span>
            </div>
        </div>

        <h2 class="section-label">УПРАВЛЕНИЕ СПРАВОЧНИКАМИ</h2>

        <nav class="menu-list">
            {#each menuCards as card}
                <button class="menu-item" on:click={() => goto(card.link)} type="button">
                    <div class="item-icon" aria-hidden="true">{card.icon}</div>
                    <div class="item-text">
                        <h3>{card.title}</h3>
                        <p>{card.desc}</p>
                    </div>
                    <span class="arrow" aria-hidden="true">›</span>
                </button>
            {/each}

            <button class="menu-item templates-item" on:click={() => showTemplatesModal = true} type="button">
                <div class="item-icon templates-bg" aria-hidden="true">📝</div>
                <div class="item-text">
                    <h3>Шаблоны сообщений</h3>
                    <p>Тексты уведомлений клиентам</p>
                </div>
                <span class="arrow" aria-hidden="true">›</span>
            </button>

            <button class="menu-item telegram-item" on:click={() => showTelegramModal = true} type="button">
                <div class="item-icon tg-bg" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>
                    </svg>
                </div>
                <div class="item-text">
                    <h3>Telegram Уведомления</h3>
                    <p>Настройка канала связи</p>
                </div>
                <span class="arrow" aria-hidden="true">›</span>
            </button>
        </nav>

        <div class="bottom-spacer"></div>
    </div>
</div>

{#if showTelegramModal}
    <div class="modal-overlay" transition:fade={{duration: 200}} on:click|self={() => showTelegramModal = false} role="presentation">
        <div class="modal-wrapper" in:scale={{start: 0.95, duration: 200}}>
            <TelegramSettingsModal on:close={() => showTelegramModal = false} />
        </div>
    </div>
{/if}

{#if showTemplatesModal}
    <div class="modal-overlay" transition:fade={{duration: 200}} on:click|self={() => showTemplatesModal = false} role="presentation">
        <div class="modal-wrapper" in:scale={{start: 0.95, duration: 200}}>
            <NotificationTemplatesModal on:close={() => showTemplatesModal = false} />
        </div>
    </div>
{/if}

<style>
    .management-container {
        height: 100%;
        width: 100%;
        overflow-y: auto;
        background: #fdf6e3;
        -webkit-overflow-scrolling: touch;
        box-sizing: border-box;
    }

    .management-tab {
        padding: 20px;
        animation: fadeIn 0.3s ease-out;
        max-width: 500px; /* Сузили для лучшего вида на мобильных */
        margin: 0 auto;
        box-sizing: border-box;
    }

    .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 24px; }
    .stat-card {
        background: #eee8d5;
        padding: 14px 8px; border-radius: 18px; text-align: center;
        border: 1.5px solid #ddd6c1;
    }
    .stat-card .val { display: block; font-size: 20px; font-weight: 900; color: #268bd2; margin-bottom: 2px; }
    .stat-card .lbl { font-size: 8px; font-weight: 850; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.5px; }

    .section-label { font-size: 9px; font-weight: 900; color: #93a1a1; letter-spacing: 1.2px; margin-bottom: 12px; text-transform: uppercase; padding-left: 4px; }

    .menu-list { display: flex; flex-direction: column; gap: 8px; width: 100%; }
    .menu-item {
        display: flex; align-items: center; padding: 14px;
        background: #eee8d5;
        border: 1.5px solid #ddd6c1; border-radius: 20px; cursor: pointer; text-align: left;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        width: 100%;
        box-sizing: border-box;
    }
    .menu-item:active { transform: scale(0.98); background: #fdf6e3; }

    .item-icon { width: 44px; height: 44px; background: #fdf6e3; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 14px; border: 1px solid #ddd6c1; flex-shrink: 0; }
    .tg-bg { color: #268bd2; }
    .templates-bg { color: #d33682; }

    .item-text { flex: 1; min-width: 0; }
    .item-text h3 { margin: 0; font-size: 15px; color: #073642; font-weight: 850; letter-spacing: -0.2px; }
    .item-text p { margin: 1px 0 0 0; font-size: 11px; color: #586e75; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .arrow { font-size: 20px; color: #93a1a1; font-weight: 300; margin-left: 8px; }

    .bottom-spacer { height: 80px; }

    .modal-overlay {
        position: fixed; inset: 0; background: rgba(7, 54, 66, 0.85);
        backdrop-filter: blur(8px); z-index: 2000;
        display: flex; align-items: center; justify-content: center; padding: 16px;
    }
    .modal-wrapper {
        width: 100%; max-width: 400px; height: auto; max-height: 85vh;
        background: #fdf6e3; border-radius: 28px; overflow: hidden;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.6);
        border: 1.5px solid #ddd6c1;
    }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
</style>
