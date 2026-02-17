<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';
    import TelegramSettingsModal from './TelegramSettingsModal.svelte';
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
        { id: 'staff', title: 'Персонал', desc: 'Сотрудники и роли', icon: '👤', link: '/admin/staff' },
        { id: 'resources', title: 'Ресурсы', desc: 'Оборудование и залы', icon: '⚒️', link: '/admin/resources' },
        { id: 'services', title: 'Услуги', desc: 'Ваш прайс-лист', icon: '✂️', link: '/admin/services' },
        { id: 'clients', title: 'Клиенты', desc: 'База клиентов', icon: '💎', link: '/admin/clients' }
    ];
</script>

<div class="management-tab">
    {#if error}
        <div class="error-banner">
            <span>⚠️ {error}</span>
            <button on:click={loadStats}>Повторить</button>
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

    <div class="section-label">УПРАВЛЕНИЕ СПРАВОЧНИКАМИ</div>

    <div class="menu-list">
        {#each menuCards as card}
            <button class="menu-item" on:click={() => goto(card.link)}>
                <div class="item-icon">{card.icon}</div>
                <div class="item-text">
                    <h3>{card.title}</h3>
                    <p>{card.desc}</p>
                </div>
                <span class="arrow">›</span>
            </button>
        {/each}

        <button class="menu-item telegram-item" on:click={() => showTelegramModal = true}>
            <div class="item-icon tg-bg">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>
                </svg>
            </div>
            <div class="item-text">
                <h3>Telegram Уведомления</h3>
                <p>Настройка рассылки клиентам</p>
            </div>
            <span class="arrow">›</span>
        </button>
    </div>
</div>

{#if showTelegramModal}
    <div class="modal-overlay" transition:fade={{duration: 200}} on:click|self={() => showTelegramModal = false}>
        <div class="modal-wrapper" in:scale={{start: 0.95, duration: 200}}>
            <TelegramSettingsModal on:close={() => showTelegramModal = false} />
        </div>
    </div>
{/if}

<style>
    .management-tab { padding: 24px; animation: fadeIn 0.3s ease-out; }

    .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 32px; }
    .stat-card { background: white; padding: 16px; border-radius: 20px; text-align: center; border: 1px solid #f1f5f9; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }
    .stat-card .val { display: block; font-size: 20px; font-weight: 900; color: var(--primary-color); margin-bottom: 4px; }
    .stat-card .lbl { font-size: 10px; font-weight: 800; color: #94a3b8; text-transform: uppercase; }

    .section-label { font-size: 11px; font-weight: 800; color: #94a3b8; letter-spacing: 1px; margin-bottom: 16px; }

    .menu-list { display: flex; flex-direction: column; gap: 12px; }
    .menu-item {
        display: flex; align-items: center; padding: 16px; background: white;
        border: 1px solid #f1f5f9; border-radius: 20px; cursor: pointer; text-align: left;
        transition: transform 0.2s, box-shadow 0.2s;
    }
    .menu-item:active { transform: scale(0.98); }

    .item-icon { width: 44px; height: 44px; background: #f8fafc; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 16px; }
    .tg-bg { background: #f0f9ff; color: #0ea5e9; }

    .item-text h3 { margin: 0; font-size: 15px; color: #1e293b; font-weight: 700; }
    .item-text p { margin: 2px 0 0 0; font-size: 12px; color: #94a3b8; }
    .arrow { margin-left: auto; font-size: 24px; color: #cbd5e1; }

    /* ОБНОВЛЕННЫЕ СТИЛИ МОДАЛКИ ДЛЯ ДЕСКТОПА */
    .modal-overlay {
        position: fixed; inset: 0; background: rgba(15, 23, 42, 0.7);
        backdrop-filter: blur(8px); z-index: 1000;
        display: flex; align-items: center; justify-content: center; padding: 20px;
    }
    .modal-wrapper {
        width: 100%; max-width: 440px; height: auto; max-height: 90vh;
        background: white; border-radius: 32px; overflow: hidden;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
    }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
