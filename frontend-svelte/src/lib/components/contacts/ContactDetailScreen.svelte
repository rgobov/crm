<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { user } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';

    export let contactId;

    let contact = null;
    let appointments = [];
    let activeTab = 'info';
    let isLoading = true;
    let isHistoryLoading = false;

    onMount(async () => {
        await loadContact();
    });

    async function loadContact() {
        isLoading = true;
        try {
            // Используем существующий эндпоинт получения одного контакта
            const response = await fetch(`/api/contacts/${contactId}`);
            // Примечание: в api.js уже есть перехватчик токена
            contact = await response.json();
        } catch (e) {
            console.error('Failed to load contact');
        } finally {
            isLoading = false;
        }
    }

    async function loadHistory() {
        if (appointments.length > 0) return; // Не грузим повторно
        isHistoryLoading = true;
        try {
            appointments = await adminService.getContactAppointments(contactId);
        } catch (e) {
            console.error('Failed to load history');
        } finally {
            isHistoryLoading = false;
        }
    }

    $: if (activeTab === 'history') loadHistory();

    function getStatusInfo(status) {
        const map = {
            'SCHEDULED': { text: 'Ожидает', color: '#42A5F5' },
            'CONFIRMED': { text: 'Подтверждено', color: '#26A69A' },
            'NEEDS_CALL': { text: 'Перезвонить', color: '#FFA726' },
            'COMPLETED': { text: 'Выполнено', color: '#90A4AE' },
            'CANCELLED': { text: 'Отменено', color: '#EF5350' }
        };
        return map[status] || { text: status, color: '#ccc' };
    }

    function formatDate(dateStr) {
        return new Date(dateStr).toLocaleDateString('ru-RU');
    }

    function formatTime(dateStr) {
        return new Date(dateStr).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    }
</script>

<div class="screen">
    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else if contact}
        <!-- Tab Bar (как во Flutter) -->
        <div class="tab-bar">
            <button class:active={activeTab === 'info'} on:click={() => activeTab = 'info'}>
                ИНФО
            </button>
            <button class:active={activeTab === 'history'} on:click={() => activeTab = 'history'}>
                ИСТОРИЯ
            </button>
        </div>

        <div class="content">
            {#if activeTab === 'info'}
                <div class="info-tab">
                    <section>
                        <label>КОНТАКТНЫЕ НОМЕРА</label>
                        {#each contact.phones as phone}
                            <div class="detail-card card">
                                <span class="icon">📞</span>
                                <span>{phone}</span>
                            </div>
                        {/each}
                    </section>

                    {#if contact.email || contact.notes}
                        <section style="margin-top: 24px;">
                            <label>ПРОЧЕЕ</label>
                            {#if contact.email}
                                <div class="detail-card card">
                                    <span class="icon">✉️</span>
                                    <div class="col">
                                        <small>Email</small>
                                        <span>{contact.email}</span>
                                    </div>
                                </div>
                            {/if}
                            {#if contact.notes}
                                <div class="detail-card card">
                                    <span class="icon">📝</span>
                                    <div class="col">
                                        <small>Заметки</small>
                                        <span>{contact.notes}</span>
                                    </div>
                                </div>
                            {/if}
                        </section>
                    {/if}
                </div>
            {:else}
                <div class="history-tab">
                    {#if isHistoryLoading}
                        <div class="center-mini"><span class="spinner mini"></span></div>
                    {:else if appointments.length === 0}
                        <div class="empty">История посещений пуста</div>
                    {:else}
                        {#each appointments as appt}
                            <div class="history-item">
                                <div class="main">
                                    <h4>{appt.service}</h4>
                                    <span class="date">📅 {formatDate(appt.startTime)} в {formatTime(appt.startTime)}</span>
                                </div>
                                <div class="status" style="background: {getStatusInfo(appt.status).color}15; color: {getStatusInfo(appt.status).color}">
                                    {getStatusInfo(appt.status).text}
                                </div>
                            </div>
                        {/each}
                    {/if}
                </div>
            {/if}
        </div>
    {/if}
</div>

<style>
    .screen { display: flex; flex-direction: column; background: var(--bg-color); min-height: 80vh; }

    .tab-bar { display: flex; background: white; border-bottom: 1px solid #f1f5f9; position: sticky; top: 0; z-index: 10; }
    .tab-bar button { flex: 1; padding: 16px; border: none; background: none; font-size: 13px; font-weight: 700; color: #94a3b8; cursor: pointer; transition: all 0.2s; }
    .tab-bar button.active { color: var(--primary-color); border-bottom: 3px solid var(--primary-color); }

    .content { padding: 20px; }

    label { font-size: 11px; font-weight: 800; color: #94a3b8; letter-spacing: 1px; margin-bottom: 12px; display: block; }

    .detail-card { display: flex; align-items: center; gap: 16px; padding: 16px; margin-bottom: 10px; background: white; }
    .detail-card .icon { font-size: 20px; }
    .detail-card .col { display: flex; flex-direction: column; }
    .detail-card small { font-size: 11px; color: #94a3b8; margin-bottom: 2px; }

    .history-item { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; border-bottom: 1px solid #f1f5f9; }
    .history-item h4 { margin: 0; font-size: 16px; color: #1e293b; }
    .history-item .date { font-size: 13px; color: #64748b; margin-top: 4px; display: block; }
    .status { padding: 4px 10px; border-radius: 8px; font-size: 11px; font-weight: 800; text-transform: uppercase; }

    .center, .center-mini { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    .spinner.mini { width: 20px; height: 20px; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .empty { text-align: center; color: #94a3b8; padding: 40px; }
</style>
