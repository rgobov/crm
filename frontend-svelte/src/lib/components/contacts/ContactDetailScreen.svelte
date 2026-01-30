<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { contactService } from '$lib/services/contactService.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';
    import { goto } from '$app/navigation';

    export let contactId;
    const dispatch = createEventDispatcher();

    let contact = null;
    let appointments = [];
    let activeTab = 'info';
    let isLoading = true;
    let isHistoryLoading = false;

    let editMode = { name: false, email: false, notes: false, phoneIdx: -1 };
    let tempValues = { name: '', phones: [], email: '', notes: '' };

    onMount(async () => {
        await loadContact();
    });

    async function loadContact() {
        isLoading = true;
        try {
            const response = await api.get(`/contacts/${contactId}`);
            contact = response.data;
            resetTempValues();
            dispatch('loaded', { name: contact.name });
        } catch (e) {
            console.error('Load contact failed', e);
        } finally {
            isLoading = false;
        }
    }

    function resetTempValues() {
        if (!contact) return;
        tempValues = {
            name: contact.name,
            phones: [...(contact.phones || [])],
            email: contact.email || '',
            notes: contact.notes || ''
        };
        editMode = { name: false, email: false, notes: false, phoneIdx: -1 };
    }

    // Обработка ввода телефона с маской (реактивно как во Flutter)
    function onPhoneInput(idx, value) {
        tempValues.phones[idx] = phoneUtils.format(value);
    }

    async function saveField(fieldName) {
        try {
            // Перед сохранением очищаем телефоны от маски для БД
            const payload = {
                ...contact,
                ...tempValues,
                phones: tempValues.phones.map(p => phoneUtils.clean(p)).filter(p => p)
            };
            const res = await api.put(`/admin/clients/${contactId}`, payload);
            contact = res.data;
            resetTempValues();
            dispatch('loaded', { name: contact.name });
        } catch (e) {
            alert('Ошибка сохранения');
            resetTempValues();
        }
    }

    async function loadHistory() {
        if (appointments.length > 0) return;
        isHistoryLoading = true;
        try {
            appointments = await contactService.getContactAppointments(contactId);
        } catch (e) {
            console.error('History failed', e);
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
            <button class:active={activeTab === 'info'} on:click={() => activeTab = 'info'}>ИНФО</button>
            <button class:active={activeTab === 'history'} on:click={() => activeTab = 'history'}>ИСТОРИЯ</button>
        </div>

        <div class="tab-content">
            {#if activeTab === 'info'}
                <div class="info-section">

                    <div class="card hero-card">
                        {#if editMode.name}
                            <div class="edit-box">
                                <input type="text" bind:value={tempValues.name} class="big-input" />
                                <div class="actions">
                                    <button class="save" on:click={() => saveField('name')}>✓</button>
                                    <button class="cancel" on:click={resetTempValues}>✕</button>
                                </div>
                            </div>
                        {:else}
                            <h2 on:click={() => editMode.name = true}>{contact.name} <small>✎</small></h2>
                        {/if}
                    </div>

                    <label class="section-title">КОНТАКТНЫЕ НОМЕРА</label>
                    <div class="card p-0">
                        {#each contact.phones as phone, i}
                            <div class="detail-row">
                                {#if editMode.phoneIdx === i}
                                    <div class="edit-box w-100">
                                        <input
                                            type="tel"
                                            value={tempValues.phones[i]}
                                            on:input={(e) => onPhoneInput(i, e.target.value)}
                                            class="mid-input"
                                        />
                                        <button class="save" on:click={() => saveField('phones')}>✓</button>
                                        <button class="cancel" on:click={resetTempValues}>✕</button>
                                    </div>
                                {:else}
                                    <span class="icon">📱</span>
                                    <span class="value" on:click={() => { editMode.phoneIdx = i; resetTempValues(); }}>
                                        {phoneUtils.format(phone)} <small>✎</small>
                                    </span>
                                    <!-- ГАРАНТИЯ ПЛЮСА: ссылка всегда с + перед чистыми цифрами -->
                                    <a href="tel:+{phoneUtils.clean(phone)}" class="call-btn">Вызов</a>
                                {/if}
                            </div>
                        {/each}
                        <button class="add-btn-full" on:click={() => goto(`/admin/clients/${contactId}/edit`)}>+ Добавить номер</button>
                    </div>

                    <label class="section-title">EMAIL</label>
                    <div class="card p-16">
                        {#if editMode.email}
                            <div class="edit-box w-100">
                                <input type="email" bind:value={tempValues.email} class="mid-input" />
                                <button class="save" on:click={() => saveField('email')}>✓</button>
                                <button class="cancel" on:click={resetTempValues}>✕</button>
                            </div>
                        {:else}
                            <div class="clickable-text" on:click={() => editMode.email = true}>
                                {contact.email || 'Добавить почту'} <small>✎</small>
                            </div>
                        {/if}
                    </div>

                    <label class="section-title">ЗАМЕТКИ</label>
                    <div class="card p-16">
                        {#if editMode.notes}
                            <div class="edit-box column w-100">
                                <textarea bind:value={tempValues.notes} rows="4"></textarea>
                                <div class="actions right">
                                    <button class="cancel" on:click={resetTempValues}>Отмена</button>
                                    <button class="save-long" on:click={() => saveField('notes')}>Сохранить</button>
                                </div>
                            </div>
                        {:else}
                            <div class="clickable-text pre" on:click={() => editMode.notes = true}>
                                {contact.notes || 'Добавить заметки...'} <small>✎</small>
                            </div>
                        {/if}
                    </div>
                </div>
            {:else}
                <div class="history-section">
                    {#if isHistoryLoading}
                        <div class="center-spinner"><span class="spinner mini"></span></div>
                    {:else if appointments.length === 0}
                        <div class="empty-state">История пуста</div>
                    {:else}
                        {#each appointments as appt}
                            <div class="history-card card">
                                <div class="main">
                                    <h4>{appt.service}</h4>
                                    <p>{new Date(appt.startTime).toLocaleDateString('ru-RU')} в {new Date(appt.startTime).toLocaleTimeString('ru-RU', {hour:'2-digit', minute:'2-digit'})}</p>
                                </div>
                                <div class="status" style="color: {getStatusInfo(appt.status).color}; background: {getStatusInfo(appt.status).color}15">
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
    .screen { background: var(--bg-color); min-height: 100vh; padding-bottom: 40px; }
    .tab-bar { display: flex; background: white; border-bottom: 1px solid #f1f5f9; position: sticky; top: 0; z-index: 10; }
    .tab-bar button { flex: 1; padding: 16px; border: none; background: none; font-size: 13px; font-weight: 800; color: #94a3b8; cursor: pointer; }
    .tab-bar button.active { color: var(--primary-color); border-bottom: 3px solid var(--primary-color); }

    .tab-content { padding: 16px; }
    .card { background: white; border-radius: 20px; box-shadow: var(--shadow); margin-bottom: 12px; overflow: hidden; }
    .p-0 { padding: 0; } .p-16 { padding: 16px; }

    .hero-card { padding: 24px; text-align: center; }
    .hero-card h2 { margin: 0; font-size: 22px; font-weight: 800; cursor: pointer; }
    small { font-size: 14px; color: var(--primary-color); opacity: 0.6; }

    .section-title { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; letter-spacing: 1px; margin: 20px 0 10px 4px; text-transform: uppercase; }

    .detail-row { display: flex; align-items: center; gap: 12px; padding: 16px; border-bottom: 1px solid #f8fafc; }
    .detail-row .icon { font-size: 20px; }
    .detail-row .value { flex: 1; font-weight: 600; color: #1e293b; cursor: pointer; }

    .edit-box { display: flex; align-items: center; gap: 8px; }
    .edit-box.column { flex-direction: column; align-items: stretch; }
    .w-100 { width: 100%; }

    input, textarea { flex: 1; padding: 10px 14px; border: 2px solid var(--primary-color); border-radius: 12px; font-size: 15px; outline: none; background: white; }
    .big-input { font-size: 20px; font-weight: 800; text-align: center; }

    .actions { display: flex; gap: 8px; }
    .actions.right { justify-content: flex-end; margin-top: 8px; }

    .save { background: #10b981; color: white; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; }
    .save-long { background: var(--primary-color); color: white; border: none; padding: 8px 16px; border-radius: 10px; font-weight: 700; cursor: pointer; }
    .cancel { background: #f1f5f9; color: #64748b; border: none; padding: 8px 12px; border-radius: 10px; cursor: pointer; font-size: 12px; font-weight: 700; }

    .call-btn { font-size: 12px; font-weight: 700; color: var(--primary-color); text-decoration: none; padding: 6px 12px; background: #eff6ff; border-radius: 8px; }
    .add-btn-full { width: 100%; padding: 12px; border: none; background: #f8fafc; color: #94a3b8; font-size: 12px; font-weight: 700; cursor: pointer; }

    .clickable-text { font-size: 15px; color: #1e293b; font-weight: 500; cursor: pointer; }
    .clickable-text.pre { white-space: pre-wrap; font-weight: 400; line-height: 1.5; color: #475569; }

    .history-card { display: flex; justify-content: space-between; align-items: center; padding: 16px; }
    .history-card h4 { margin: 0; font-size: 15px; font-weight: 700; }
    .history-card p { margin: 4px 0 0 0; font-size: 12px; color: #64748b; }
    .status { padding: 4px 10px; border-radius: 8px; font-size: 10px; font-weight: 800; text-transform: uppercase; }

    .center-spinner { display: flex; justify-content: center; padding: 60px; }
    .spinner { width: 28px; height: 28px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
