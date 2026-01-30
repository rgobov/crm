<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { contactService } from '$lib/services/contactService.js';

    export let contactId;
    const dispatch = createEventDispatcher();

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
            const response = await api.get(`/contacts/${contactId}`);
            contact = response.data;
            // Передаем имя наверх в заголовок страницы
            dispatch('loaded', { name: contact.name });
        } catch (e) {
            console.error('Failed to load contact', e);
        } finally {
            isLoading = false;
        }
    }

    async function loadHistory() {
        if (appointments.length > 0) return;
        isHistoryLoading = true;
        try {
            // Используем метод из contactService
            appointments = await contactService.getContactAppointments(contactId);
        } catch (e) {
            console.error('Failed to load history', e);
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
        return map[status] || { text: status, color: '#64748b' };
    }
</script>

<div class="screen">
    {#if isLoading}
        <div class="center-spinner"><span class="spinner"></span></div>
    {:else if contact}
        <div class="tab-bar">
            <button class:active={activeTab === 'info'} on:click={() => activeTab = 'info'}>
                ИНФО
            </button>
            <button class:active={activeTab === 'history'} on:click={() => activeTab = 'history'}>
                ИСТОРИЯ
            </button>
        </div>

        <div class="tab-content">
            {#if activeTab === 'info'}
                <div class="info-section">
                    <!-- Аватар убран по требованию -->
                    <div class="section-group">
                        <label class="section-title">КОНТАКТНЫЕ НОМЕРА</label>
                        {#each (contact.phones || []) as phone}
                            <div class="detail-item card">
                                <span class="icon">📱</span>
                                <div class="col">
                                    <span class="value">{phone}</span>
                                </div>
                                <a href="tel:{phone}" class="call-action">Вызов</a>
                            </div>
                        {/each}
                    </div>

                    <div class="section-group">
                        <label class="section-title">ПРОЧЕЕ</label>
                        {#if contact.email}
                            <div class="detail-item card">
                                <span class="icon">✉️</span>
                                <div class="col">
                                    <small>Email</small>
                                    <span class="value">{contact.email}</span>
                                </div>
                            </div>
                        {/if}
                        {#if contact.notes}
                            <div class="detail-item card notes-item">
                                <span class="icon">📝</span>
                                <div class="col">
                                    <small>Заметки</small>
                                    <span class="value">{contact.notes}</span>
                                </div>
                            </div>
                        {/if}
                    </div>
                </div>
            {:else}
                <div class="history-section">
                    {#if isHistoryLoading}
                        <div class="center-spinner"><span class="spinner mini"></span></div>
                    {:else if appointments.length === 0}
                        <div class="empty-state">История посещений пуста</div>
                    {:else}
                        {#each appointments as appt}
                            <div class="history-card card">
                                <div class="appt-main">
                                    <h4>{appt.service}</h4>
                                    <p class="appt-date">
                                        {new Date(appt.startTime).toLocaleDateString('ru-RU')} в
                                        {new Date(appt.startTime).toLocaleTimeString('ru-RU', {hour:'2-digit', minute:'2-digit'})}
                                    </p>
                                </div>
                                <div class="appt-status" style="color: {getStatusInfo(appt.status).color}; background: {getStatusInfo(appt.status).color}15">
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
    .screen { min-height: 100%; background: var(--bg-color); }

    .tab-bar {
        display: flex;
        background: white;
        border-bottom: 1px solid #f1f5f9;
        position: sticky;
        top: 0;
        z-index: 5;
    }
    .tab-bar button {
        flex: 1;
        padding: 16px;
        border: none;
        background: none;
        font-size: 13px;
        font-weight: 800;
        color: #94a3b8;
        cursor: pointer;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }
    .tab-bar button.active {
        color: var(--primary-color);
        border-bottom: 3px solid var(--primary-color);
    }

    .tab-content { padding: 16px; }

    .section-group { margin-bottom: 24px; }
    .section-title {
        display: block;
        font-size: 11px;
        font-weight: 800;
        color: #94a3b8;
        letter-spacing: 1px;
        margin: 0 0 12px 4px;
        text-transform: uppercase;
    }

    .detail-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px;
        margin-bottom: 8px;
        background: white;
        border-radius: 16px;
    }
    .detail-item .icon { font-size: 20px; }
    .detail-item .col { display: flex; flex-direction: column; flex: 1; }
    .detail-item small { font-size: 11px; color: #94a3b8; margin-bottom: 2px; font-weight: 600; }
    .detail-item .value { font-weight: 600; color: #1e293b; font-size: 15px; }

    .notes-item .value { white-space: pre-wrap; font-weight: 400; line-height: 1.4; color: #475569; }

    .call-action {
        font-size: 12px;
        font-weight: 700;
        color: var(--primary-color);
        text-decoration: none;
        padding: 8px 16px;
        background: #eff6ff;
        border-radius: 10px;
    }

    .history-card {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px;
        margin-bottom: 8px;
        background: white;
        border-radius: 16px;
    }
    .appt-main h4 { margin: 0; font-size: 15px; font-weight: 700; color: #1e293b; }
    .appt-date { margin: 4px 0 0 0; font-size: 12px; color: #64748b; font-weight: 500; }
    .appt-status {
        padding: 4px 10px;
        border-radius: 8px;
        font-size: 10px;
        font-weight: 800;
        text-transform: uppercase;
    }

    .center-spinner { display: flex; justify-content: center; padding: 60px; }
    .spinner {
        width: 28px;
        height: 28px;
        border: 3px solid #f1f5f9;
        border-top-color: var(--primary-color);
        border-radius: 50%;
        animation: spin 1s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .empty-state { text-align: center; color: #94a3b8; padding: 40px; font-weight: 500; }
</style>
