<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';
    import TelegramSettingsModal from '../TelegramSettingsModal.svelte';
    import NotificationTemplatesModal from '../NotificationTemplatesModal.svelte';
    import { fade, scale } from 'svelte/transition';
    import { portal } from '$lib/actions/portal.js';
    import { user } from '$lib/stores/auth.js';

    let copied = false;
    let activeBookingTab = 'link'; // 'link' | 'api'

    $: bookingUrl = typeof window !== 'undefined' && $user?.tenantId
        ? `${window.location.origin}/register-client?tenantId=${$user.tenantId}`
        : '';

    async function copyBookingUrl() {
        try {
            await navigator.clipboard.writeText(bookingUrl);
            copied = true;
            setTimeout(() => copied = false, 2000);
        } catch (e) {
            console.error('Copy failed', e);
        }
    }

    // Props от родителя (могут использоваться в будущем)
    export let forcedDate = null;
    export let branchId = null;

    let stats = { totalClients: 0, todayAppointments: 0, totalResources: 0, totalStaff: 0 };
    let isLoading = true;
    let error = null;
    let showTelegramModal = false;
    let showTemplatesModal = false;

    onMount(async () => { await loadStats(); });

    async function loadStats() {
        isLoading = true;
        try { stats = await adminService.getDashboardStats(); }
        catch (e) { error = "Ошибка загрузки данных."; }
        finally { isLoading = false; }
    }

    const menuCards = [
        { id: 'branches', title: 'Филиалы', desc: 'Ваши точки и часовые пояса', icon: '🏢', link: '/admin/branches' },
        { id: 'staff', title: 'Персонал', desc: 'Сотрудники и роли', icon: '👤', link: '/admin/staff' },
        { id: 'resources', title: 'Ресурсы', desc: 'Оборудование и залы', icon: '⚒️', link: '/admin/resources' },
        { id: 'services', title: 'Услуги', desc: 'Ваш прайс-лист', icon: '⭐', link: '/admin/services' },
        { id: 'clients', title: 'Клиенты', desc: 'База клиентов', icon: '💎', link: '/admin/clients' }
    ];
</script>

<div class="management-container">
    <div class="management-tab">
        <!-- СТАТИСТИКА: Золотое сечение в пропорциях карточек -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-content">
                    <span class="val">{stats.totalClients}</span>
                    <span class="lbl">Клиенты</span>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-content">
                    <span class="val">{stats.todayAppointments}</span>
                    <span class="lbl">Сегодня</span>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-content">
                    <span class="val">{stats.totalStaff}</span>
                    <span class="lbl">Мастера</span>
                </div>
            </div>
        </div>

        <h2 class="section-label">УПРАВЛЕНИЕ СПРАВОЧНИКАМИ</h2>

        <nav class="menu-list">
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

            <button class="menu-item highlight" on:click={() => showTemplatesModal = true}>
                <div class="item-icon">📝</div>
                <div class="item-text">
                    <h3>Шаблоны сообщений</h3>
                    <p>Тексты уведомлений клиентам</p>
                </div>
                <span class="arrow">›</span>
            </button>

            <button class="menu-item highlight" on:click={() => showTelegramModal = true}>
                <div class="item-icon">📱</div>
                <div class="item-text">
                    <h3>Telegram Уведомления</h3>
                    <p>Настройка канала связи</p>
                </div>
                <span class="arrow">›</span>
            </button>
        </nav>

        <h2 class="section-label" style="margin-top: 34px;">ОНЛАЙН-ЗАПИСЬ КЛИЕНТОВ</h2>
        <div class="booking-settings-card">
            <div class="card-tabs">
                <button class="tab-btn" class:active={activeBookingTab === 'link'} on:click={() => activeBookingTab = 'link'}>
                    Готовая ссылка
                </button>
                <button class="tab-btn" class:active={activeBookingTab === 'api'} on:click={() => activeBookingTab = 'api'}>
                    Интеграция по API
                </button>
            </div>

            <div class="tab-content">
                {#if activeBookingTab === 'link'}
                    <p class="section-desc">Скопируйте ссылку и разместите её на вашем сайте или в соцсетях:</p>
                    <div class="copy-link-wrapper">
                        <input type="text" readonly value={bookingUrl} class="copy-input" />
                        <button class="copy-button" on:click={copyBookingUrl}>
                            {copied ? 'Скопировано!' : 'Копировать'}
                        </button>
                    </div>
                {:else}
                    <p class="section-desc">Документация для интеграции формы записи вашими разработчиками:</p>
                    <div class="api-docs-box">
                        <div class="api-endpoint">
                            <span class="method post">POST</span>
                            <span class="url">/api/auth/register-client</span>
                        </div>
                        <p class="endpoint-desc">Регистрация и создание контакта в вашей системе</p>
                        <pre class="json-schema">{`{
  "name": "Имя клиента",
  "phone": "+79991112233",
  "email": "client@example.com",
  "password": "пароль123",
  "tenantId": "${$user?.tenantId || 'ваш_tenant_id'}"
}`}</pre>
                    </div>
                {/if}
            </div>
        </div>
    </div>
</div>

{#if showTelegramModal}
    <div class="modal-overlay" use:portal transition:fade={{duration: 200}} on:click|self={() => showTelegramModal = false}>
        <div class="modal-wrapper" in:scale={{start: 0.95, duration: 200}}>
            <TelegramSettingsModal on:close={() => showTelegramModal = false} />
        </div>
    </div>
{/if}

{#if showTemplatesModal}
    <div class="modal-overlay" use:portal transition:fade={{duration: 200}} on:click|self={() => showTemplatesModal = false}>
        <div class="modal-wrapper" in:scale={{start: 0.95, duration: 200}}>
            <NotificationTemplatesModal on:close={() => showTemplatesModal = false} />
        </div>
    </div>
{/if}

<style>
    .management-container {
        height: 100%; width: 100%; overflow-y: auto; background: #fdf6e3;
        box-sizing: border-box; display: flex; justify-content: center;
    }

    .management-tab {
        padding: 40px 20px;
        width: 100%;
        /* Золотое сечение для ширины основной колонки (относительно типичного экрана 1920) */
        max-width: 618px;
        box-sizing: border-box;
    }

    /* СЕТКА СТАТИСТИКИ С ЗОЛОТЫМ СЕЧЕНИЕМ */
    .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 13px; margin-bottom: 34px; }
    .stat-card {
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 21px;
        position: relative;
        /* Соотношение сторон по золотому сечению */
        aspect-ratio: 1 / 0.618;
        display: flex;
        align-items: center;
        justify-content: center;
    }
    .stat-content { text-align: center; }
    .stat-card .val { display: block; font-size: 28px; font-weight: 900; color: #268bd2; line-height: 1; margin-bottom: 4px; }
    .stat-card .lbl { font-size: 10px; font-weight: 850; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.8px; }

    .section-label { font-size: 11px; font-weight: 900; color: #93a1a1; letter-spacing: 1.6px; margin-bottom: 16px; text-transform: uppercase; padding-left: 8px; }

    .menu-list { display: flex; flex-direction: column; gap: 8px; width: 100%; }

    .menu-item {
        display: flex; align-items: center;
        /* Высота пункта меню тоже коррелирует с золотым сечением */
        padding: 18px 24px;
        background: #eee8d5;
        border: 1.5px solid #ddd6c1; border-radius: 24px; cursor: pointer; text-align: left;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .menu-item:hover { transform: scale(1.01); background: #fdf6e3; box-shadow: 0 8px 24px rgba(0,0,0,0.05); }
    .menu-item.highlight { border-color: #268bd2; }

    .item-icon { width: 48px; height: 48px; background: #fdf6e3; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 22px; margin-right: 20px; border: 1px solid #ddd6c1; flex-shrink: 0; }

    .item-text { flex: 1; min-width: 0; }
    .item-text h3 { margin: 0; font-size: 16px; color: #073642; font-weight: 850; }
    .item-text p { margin: 2px 0 0 0; font-size: 12px; color: #586e75; font-weight: 500; }
    .arrow { font-size: 20px; color: #93a1a1; opacity: 0.5; margin-left: 8px; }

    :global(.modal-overlay) {
        position: fixed; inset: 0; background: rgba(7, 54, 66, 0.8);
        backdrop-filter: blur(8px); z-index: 99999;
        display: flex; align-items: center; justify-content: center;
        padding: 20px;
        padding-top: max(20px, calc(env(safe-area-inset-top, 20px) + 12px));
        padding-bottom: max(20px, calc(env(safe-area-inset-bottom, 20px) + 12px));
        box-sizing: border-box;
    }

    /* СТИЛИ ДЛЯ КАРТОЧКИ НАСТРОЕК ОНЛАЙН-ЗАПИСИ */
    .booking-settings-card {
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 24px;
        overflow: hidden;
        margin-top: 12px;
        display: flex;
        flex-direction: column;
    }

    .card-tabs {
        display: flex;
        background: #ddd6c1;
        border-bottom: 1.5px solid #ddd6c1;
    }

    .tab-btn {
        flex: 1;
        padding: 14px;
        background: transparent;
        border: none;
        font-size: 13px;
        font-weight: 800;
        color: #586e75;
        cursor: pointer;
        transition: all 0.2s;
    }

    .tab-btn.active {
        background: #eee8d5;
        color: #073642;
    }

    .tab-content {
        padding: 20px;
        text-align: left;
    }

    .section-desc {
        font-size: 12px;
        color: #586e75;
        margin: 0 0 12px 0;
        font-weight: 650;
    }

    .copy-link-wrapper {
        display: flex;
        gap: 8px;
    }

    .copy-input {
        flex: 1;
        padding: 12px;
        border: 1.5px solid #ddd6c1;
        border-radius: 12px;
        background: #fdf6e3;
        color: #586e75;
        font-size: 13px;
        outline: none;
    }

    .copy-button {
        padding: 0 18px;
        background: #268bd2;
        color: white;
        border: none;
        border-radius: 12px;
        font-size: 13px;
        font-weight: 800;
        cursor: pointer;
        transition: opacity 0.2s;
    }

    .copy-button:hover {
        opacity: 0.9;
    }

    .api-docs-box {
        background: #fdf6e3;
        border: 1px solid #ddd6c1;
        border-radius: 16px;
        padding: 16px;
    }

    .api-endpoint {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 6px;
    }

    .method.post {
        background: #859900;
        color: white;
        font-size: 9px;
        font-weight: 900;
        padding: 3px 8px;
        border-radius: 6px;
    }

    .url {
        font-family: monospace;
        font-size: 12px;
        font-weight: 800;
        color: #073642;
    }

    .endpoint-desc {
        font-size: 11px;
        color: #93a1a1;
        margin: 0 0 10px 0;
        font-weight: 650;
    }

    .json-schema {
        background: #eee8d5;
        padding: 10px;
        border-radius: 8px;
        font-family: monospace;
        font-size: 11px;
        color: #586e75;
        margin: 0;
        overflow-x: auto;
    }
    :global(.modal-wrapper) {
        width: 100%; max-width: 450px;
        background: #fdf6e3; border-radius: 34px; overflow: hidden;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
        border: 1.5px solid #ddd6c1;
        max-height: calc(100dvh - max(40px, env(safe-area-inset-top, 20px) + env(safe-area-inset-bottom, 20px)) - 40px);
    }
</style>
