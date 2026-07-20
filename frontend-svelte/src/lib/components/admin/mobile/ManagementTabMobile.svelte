<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';
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

    // Безопасная навигация: fallback на window.location для WebView/Telegram
    function safeGoto(path) {
        try {
            goto(path);
        } catch (e) {
            window.location.href = path;
        }
    }

    // Props от родителя (могут использоваться в будущем)
    export let forcedDate = null;
    export let branchId = null;

    let stats = {
        totalClients: 0,
        todayAppointments: 0,
        totalResources: 0,
        totalStaff: 0,
        returnReminderCount: 0
    };
    let isLoading = true;

    onMount(async () => {
        try {
            const data = await adminService.getDashboardStats();
            stats = data;
        } finally {
            isLoading = false;
        }
    });

    const gridItems = [
        { id: 'branches', title: 'Филиалы', icon: '🏢', link: '/admin/branches' },
        { id: 'staff', title: 'Персонал', icon: '👤', link: '/admin/staff' },
        { id: 'resources', title: 'Ресурсы', icon: '⚒️', link: '/admin/resources' },
        { id: 'services', title: 'Услуги', icon: '⭐', link: '/admin/services' }
    ];

    function formatStat(num) {
        if (!num) return '0';
        if (num >= 1000) return (num/1000).toFixed(1) + 'k';
        return num;
    }

    let exportStartDate = (() => {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
    })();
    let exportEndDate = (() => {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    })();

    let isExportingClients = false;
    let isExportingAppointments = false;

    async function handleExportClients() {
        if (isExportingClients) return;
        isExportingClients = true;
        try {
            const blob = await contactService.exportContacts('', true);
            const url = window.URL.createObjectURL(new Blob([blob]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'clients_all.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (e) {
            console.error(e);
        } finally {
            isExportingClients = false;
        }
    }

    async function handleExportAppointments() {
        if (isExportingAppointments) return;
        isExportingAppointments = true;
        try {
            const blob = await adminService.exportAppointments(exportStartDate, exportEndDate);
            const url = window.URL.createObjectURL(new Blob([blob]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `visits_${exportStartDate}_to_${exportEndDate}.xlsx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (e) {
            console.error(e);
        } finally {
            isExportingAppointments = false;
        }
    }
</script>

<div class="stories-dashboard">

    <!-- СЕКЦИЯ СТОРИС: Финальный дизайн -->
    <div class="stories-container">
        <div class="stories-track">

            <!-- СТОРИС: СЕГОДНЯ -->
            <div class="story-item">
                <div class="story-circle ring-blue">
                    <div class="story-inner">
                        <span class="s-icon">📅</span>
                        <span class="s-val">{formatStat(stats.todayAppointments)}</span>
                    </div>
                </div>
                <span class="story-label">Сегодня</span>
            </div>

            <!-- СТОРИС: КЛИЕНТЫ -->
            <div class="story-item">
                <div class="story-circle ring-magenta">
                    <div class="story-inner">
                        <span class="s-icon">💎</span>
                        <span class="s-val">{formatStat(stats.totalClients)}</span>
                    </div>
                </div>
                <span class="story-label">Клиенты</span>
            </div>

            <!-- СТОРИС: МАСТЕРА -->
            <div class="story-item">
                <div class="story-circle ring-green">
                    <div class="story-inner">
                        <span class="s-icon">👤</span>
                        <span class="s-val">{formatStat(stats.totalStaff)}</span>
                    </div>
                </div>
                <span class="story-label">Мастера</span>
            </div>

            <!-- СТОРИС: РЕСУРСЫ -->
            <div class="story-item">
                <div class="story-circle ring-orange">
                    <div class="story-inner">
                        <span class="s-icon">⚒️</span>
                        <span class="s-val">{formatStat(stats.totalResources)}</span>
                    </div>
                </div>
                <span class="story-label">Ресурсы</span>
            </div>

            <!-- СТОРИС: ВОЗВРАТ -->
            <div class="story-item" on:click={() => safeGoto('/admin/return-reminders')}>
                <div class="story-circle ring-teal">
                    <div class="story-inner">
                        <span class="s-icon">📩</span>
                        <span class="s-val">{formatStat(stats.returnReminderCount)}</span>
                    </div>
                </div>
                <span class="story-label">Возврат</span>
            </div>

        </div>
    </div>

    <!-- СЕКЦИЯ УПРАВЛЕНИЯ -->
    <section class="main-controls">
        <label class="section-caption">ИНСТРУМЕНТЫ</label>

        <div class="phi-grid">
            {#each gridItems as item}
                <button class="phi-card" on:click={() => safeGoto(item.link)} in:scale={{duration: 200, start: 0.95}}>
                    <div class="card-icon">{item.icon}</div>
                    <h3>{item.title}</h3>
                </button>
            {/each}
        </div>

        <button class="phi-wide-card" on:click={() => safeGoto('/admin/clients')} in:fade={{delay: 300}}>
            <div class="wide-inner">
                <div class="card-icon accent-bg">💎</div>
                <div class="wide-text">
                    <h3>База Клиентов</h3>
                    <p>Профили, история и лояльность</p>
                </div>
            </div>
            <span class="phi-arrow">→</span>
        </button>

        <label class="section-caption" style="margin-top: 30px;">ЭКСПОРТ ДАННЫХ</label>
        <div class="export-settings-card">
            <div class="export-row">
                <button class="export-main-btn" on:click={handleExportClients} disabled={isExportingClients}>
                    {isExportingClients ? 'Экспорт базы...' : '📥 Выгрузить всех клиентов'}
                </button>
            </div>
            <div class="export-divider"></div>
            <div class="export-row date-range-row">
                <div class="date-fields">
                    <div class="date-input-wrap">
                        <label>С даты:</label>
                        <input type="date" bind:value={exportStartDate} class="export-date-input" />
                    </div>
                    <div class="date-input-wrap">
                        <label>По дату:</label>
                        <input type="date" bind:value={exportEndDate} class="export-date-input" />
                    </div>
                </div>
                <button class="export-main-btn highlight" on:click={handleExportAppointments} disabled={isExportingAppointments}>
                    {isExportingAppointments ? 'Экспорт визитов...' : '📥 Выгрузить визиты'}
                </button>
            </div>
        </div>

        <label class="section-caption" style="margin-top: 30px;">ОНЛАЙН-ЗАПИСЬ КЛИЕНТОВ</label>
        <div class="booking-settings-card">
            <div class="card-tabs">
                <button class="tab-btn" class:active={activeBookingTab === 'link'} on:click={() => activeBookingTab = 'link'}>
                    Ссылка
                </button>
                <button class="tab-btn" class:active={activeBookingTab === 'api'} on:click={() => activeBookingTab = 'api'}>
                    API
                </button>
            </div>

            <div class="tab-content">
                {#if activeBookingTab === 'link'}
                    <p class="section-desc">Ссылка для размещения на сайте или в соцсетях:</p>
                    <div class="copy-link-wrapper">
                        <input type="text" readonly value={bookingUrl} class="copy-input" />
                        <button class="copy-button" on:click={copyBookingUrl}>
                            {copied ? 'Copied' : 'Copy'}
                        </button>
                    </div>
                {:else}
                    <p class="section-desc">Спецификация для внешних разработчиков:</p>
                    <div class="api-docs-box">
                        <div class="api-endpoint">
                            <span class="method post">POST</span>
                            <span class="url">/api/auth/register-client</span>
                        </div>
                        <pre class="json-schema">{`{
  "name": "Имя",
  "phone": "+79991112233",
  "email": "client@example.com",
  "password": "пароль123",
  "tenantId": "${$user?.tenantId || 'ваш_tenant_id'}"
}`}</pre>
                    </div>
                {/if}
            </div>
        </div>
    </section>

    <div class="bottom-phi-spacer"></div>
</div>

<style>
    .stories-dashboard {
        height: 100%; width: 100%;
        background: #fdf6e3;
        overflow-y: auto;
        display: flex; flex-direction: column;
        box-sizing: border-box;
    }

    .stories-container {
        padding: 24px 0 20px 0;
        background: linear-gradient(to bottom, #eee8d5 0%, #fdf6e3 100%);
        margin-bottom: 16px;
    }
    .stories-track {
        display: flex; gap: 20px;
        padding: 0 20px;
        overflow-x: auto;
        scrollbar-width: none;
    }
    .stories-track::-webkit-scrollbar { display: none; }

    .story-item { display: flex; flex-direction: column; align-items: center; gap: 10px; flex-shrink: 0; }

    .story-circle {
        width: 72px; height: 72px;
        background: #eee8d5;
        border-radius: 50%;
        padding: 3px; /* Место под обводку */
        position: relative;
    }

    /* ЦВЕТНЫЕ ОБВОДКИ (Solarized Palette) */
    .ring-blue { background: linear-gradient(45deg, #268bd2, #2aa198); }
    .ring-magenta { background: linear-gradient(45deg, #d33682, #6c71c4); }
    .ring-green { background: linear-gradient(45deg, #859900, #2aa198); }
    .ring-orange { background: linear-gradient(45deg, #cb4b16, #b58900); }
    .ring-teal { background: linear-gradient(45deg, #0088cc, #5bc0de); }

    .story-inner {
        width: 100%; height: 100%;
        background: #fdf6e3;
        border-radius: 50%;
        display: flex; flex-direction: column;
        align-items: center; justify-content: center;
        gap: 2px;
    }

    .s-icon { font-size: 16px; }
    .s-val { font-size: 18px; font-weight: 900; color: #073642; line-height: 1; }
    .story-label { font-size: 10px; font-weight: 800; color: #586e75; text-transform: uppercase; letter-spacing: 0.5px; }

    /* GRID & CONTROLS */
    .main-controls { padding: 0 20px; flex: 1; }
    .section-caption { display: block; font-size: 11px; font-weight: 950; color: #93a1a1; letter-spacing: 2px; margin-bottom: 16px; text-transform: uppercase; padding-left: 4px; }

    .phi-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
        margin-bottom: 12px;
    }

    .phi-card {
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 24px;
        padding: 24px 20px;
        display: flex; flex-direction: column; align-items: flex-start; gap: 14px;
        aspect-ratio: 1 / 0.8;
        cursor: pointer;
        transition: transform 0.2s;
    }
    .phi-card:active { transform: scale(0.96); background: white; border-color: #268bd2; }
    .card-icon { font-size: 26px; }
    .phi-card h3 { margin: 0; font-size: 16px; font-weight: 850; color: #073642; }

    .phi-wide-card {
        width: 100%;
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 30px;
        padding: 24px;
        display: flex; align-items: center; justify-content: space-between;
        cursor: pointer;
        transition: all 0.2s;
        margin-top: 4px;
    }
    .phi-wide-card:active { transform: scale(0.98); background: white; }
    .wide-inner { display: flex; align-items: center; gap: 20px; }
    .accent-bg { background: #fdf6e3; width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid #ddd6c1; }
    .wide-text { text-align: left; }
    .wide-text h3 { margin: 0; font-size: 18px; font-weight: 850; color: #073642; }
    .wide-text p { margin: 2px 0 0 0; font-size: 12px; color: #586e75; font-weight: 600; }
    .phi-arrow { font-size: 22px; color: #268bd2; font-weight: 300; }

    .bottom-phi-spacer { height: 120px; }

    /* СТИЛИ КАРТОЧКИ НАСТРОЕК ОНЛАЙН-ЗАПИСИ ДЛЯ МОБИЛЬНОЙ ВЕРСИИ */
    .booking-settings-card {
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 20px;
        overflow: hidden;
        margin-top: 10px;
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
        padding: 12px;
        background: transparent;
        border: none;
        font-size: 12px;
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
        padding: 16px;
        text-align: left;
    }

    .section-desc {
        font-size: 11px;
        color: #586e75;
        margin: 0 0 10px 0;
        font-weight: 650;
    }

    .copy-link-wrapper {
        display: flex;
        gap: 6px;
    }

    .copy-input {
        flex: 1;
        padding: 10px;
        border: 1.5px solid #ddd6c1;
        border-radius: 10px;
        background: #fdf6e3;
        color: #586e75;
        font-size: 12px;
        outline: none;
        min-width: 0; /* предотвращает переполнение */
    }

    .copy-button {
        padding: 0 14px;
        background: #268bd2;
        color: white;
        border: none;
        border-radius: 10px;
        font-size: 12px;
        font-weight: 800;
        cursor: pointer;
    }

    .api-docs-box {
        background: #fdf6e3;
        border: 1px solid #ddd6c1;
        border-radius: 12px;
        padding: 12px;
    }

    .api-endpoint {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 6px;
    }

    .method.post {
        background: #859900;
        color: white;
        font-size: 8px;
        font-weight: 900;
        padding: 2px 6px;
        border-radius: 4px;
    }

    .url {
        font-family: monospace;
        font-size: 11px;
        font-weight: 800;
        color: #073642;
    }

    .json-schema {
        background: #eee8d5;
        padding: 8px;
        border-radius: 6px;
        font-family: monospace;
        font-size: 10px;
        color: #586e75;
        margin: 0;
        overflow-x: auto;
    }
    .export-settings-card {
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 20px;
        overflow: hidden;
        margin-top: 10px;
        display: flex;
        flex-direction: column;
        padding: 16px;
        gap: 12px;
    }

    .export-row {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    .export-divider {
        height: 1.5px;
        background: #ddd6c1;
        width: 100%;
    }

    .date-range-row {
        gap: 12px;
    }

    .date-fields {
        display: flex;
        gap: 10px;
    }

    .date-input-wrap {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 4px;
    }

    .date-input-wrap label {
        font-size: 10px;
        font-weight: 800;
        color: #586e75;
        text-transform: uppercase;
    }

    .export-date-input {
        padding: 10px;
        border: 1.5px solid #ddd6c1;
        border-radius: 10px;
        background: #fdf6e3;
        color: #073642;
        font-size: 12px;
        font-weight: 700;
        outline: none;
    }

    .export-main-btn {
        padding: 12px;
        background: #fdf6e3;
        color: #268bd2;
        border: 1.5px solid #ddd6c1;
        border-radius: 10px;
        font-size: 12px;
        font-weight: 800;
        cursor: pointer;
        transition: all 0.2s;
        text-align: center;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
    }

    .export-main-btn:active:not(:disabled) {
        background: #eee8d5;
    }

    .export-main-btn.highlight {
        background: #268bd2;
        color: white;
        border: none;
    }

    .export-main-btn.highlight:active:not(:disabled) {
        opacity: 0.9;
    }

    .export-main-btn:disabled {
        opacity: 0.6;
    }
</style>
