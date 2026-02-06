<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { contactService } from '$lib/services/contactService.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';

    export let contactId;
    const dispatch = createEventDispatcher();

    let contact = null;
    let appointments = [];
    let activeTab = 'info';
    let isLoading = true;
    let isHistoryLoading = false;

    // Состояния редактирования
    let editMode = { name: false, email: false, notes: false, phoneIdx: -1, isAddingPhone: false };
    let tempValues = { name: '', phones: [], email: '', notes: '', newPhone: '' };

    onMount(async () => {
        await loadContact();
    });

    async function loadContact() {
        isLoading = true;
        try {
            const response = await api.get(`/admin/clients/${contactId}`);
            contact = response.data;
            syncTempValues();
            dispatch('loaded', { name: contact.name });
        } catch (e) {
            console.error('Load contact failed', e);
        } finally {
            isLoading = false;
        }
    }

    function syncTempValues() {
        if (!contact) return;
        tempValues = {
            name: contact.name,
            phones: [...(contact.phones || [])],
            email: contact.email || '',
            notes: contact.notes || '',
            newPhone: ''
        };
    }

    function cancelAllEdits() {
        editMode = { name: false, email: false, notes: false, phoneIdx: -1, isAddingPhone: false };
        syncTempValues();
    }

    async function saveField(type) {
        try {
            const finalPhones = type === 'addPhone'
                ? [...tempValues.phones, phoneUtils.clean(tempValues.newPhone)]
                : tempValues.phones.map(p => phoneUtils.clean(p)).filter(p => p);

            if (!tempValues.name) return alert('Имя обязательно');
            if (finalPhones.length === 0) return alert('Нужен минимум один телефон');

            const payload = {
                ...contact,
                name: tempValues.name,
                email: tempValues.email || null,
                notes: tempValues.notes || null,
                phones: finalPhones
            };

            const res = await api.put(`/admin/clients/${contactId}`, payload);
            contact = res.data;
            cancelAllEdits();
            dispatch('loaded', { name: contact.name });

            // РЕАКТИВНЫЙ СТЕК: Уведомляем список клиентов на фоне об обновлении
            dispatch('updated', contact);

        } catch (e) {
            alert('Ошибка при сохранении');
            cancelAllEdits();
        }
    }

    async function removePhone(idx) {
        if (contact.phones.length <= 1) return alert('Нельзя удалить единственный номер');
        if (confirm('Удалить этот номер?')) {
            tempValues.phones = tempValues.phones.filter((_, i) => i !== idx);
            await saveField('phones');
        }
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
                    <!-- ИМЯ -->
                    <div class="card hero-card">
                        {#if editMode.name}
                            <div class="edit-box">
                                <input type="text" bind:value={tempValues.name} class="big-input" autofocus />
                                <div class="actions">
                                    <button class="save" on:click={() => saveField('name')}>✓</button>
                                    <button class="cancel" on:click={cancelAllEdits}>✕</button>
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
                                        <input type="tel" bind:value={tempValues.phones[i]} class="mid-input" autofocus />
                                        <button class="save" on:click={() => saveField('phones')}>✓</button>
                                        <button class="cancel" on:click={cancelAllEdits}>✕</button>
                                    </div>
                                {:else}
                                    <span class="icon">📱</span>
                                    <div class="value-col" on:click={() => editMode.phoneIdx = i}>
                                        <span class="value">{phoneUtils.format(phone)}</span>
                                        <small>изменить ✎</small>
                                    </div>
                                    <button class="icon-btn-del" on:click={() => removePhone(i)}>🗑</button>
                                {/if}
                            </div>
                        {/each}
                        <div class="detail-row add-row">
                            {#if editMode.isAddingPhone}
                                <div class="edit-box w-100">
                                    <input type="tel" bind:value={tempValues.newPhone} placeholder="+7 (___) ___" class="mid-input" autofocus />
                                    <button class="save" on:click={() => saveField('addPhone')}>✓</button>
                                    <button class="cancel" on:click={cancelAllEdits}>✕</button>
                                </div>
                            {:else}
                                <button class="add-link" on:click={() => editMode.isAddingPhone = true}>+ Добавить номер</button>
                            {/if}
                        </div>
                    </div>

                    <label class="section-title">ПРОЧЕЕ</label>
                    <div class="card p-16">
                        {#if editMode.email}
                            <div class="edit-box w-100">
                                <input type="email" bind:value={tempValues.email} class="mid-input" autofocus />
                                <button class="save" on:click={() => saveField('email')}>✓</button>
                                <button class="cancel" on:click={cancelAllEdits}>✕</button>
                            </div>
                        {:else}
                            <div class="clickable-text" on:click={() => editMode.email = true}>✉️ {contact.email || 'Добавить Email'} <small>✎</small></div>
                        {/if}
                        <div class="divider"></div>
                        {#if editMode.notes}
                            <div class="edit-box column w-100">
                                <textarea bind:value={tempValues.notes} rows="4" autofocus></textarea>
                                <div class="actions right">
                                    <button class="cancel" on:click={cancelAllEdits}>✕ Отмена</button>
                                    <button class="save-long" on:click={() => saveField('notes')}>✓ Сохранить</button>
                                </div>
                            </div>
                        {:else}
                            <div class="clickable-text pre" on:click={() => editMode.notes = true}>📝 {contact.notes || 'Добавить заметки...'} <small>✎</small></div>
                        {/if}
                    </div>
                </div>
            {/if}
        </div>
    {/if}
</div>

<style>
    .screen { background: #f8fafc; min-height: 100%; border-radius: 32px; overflow: hidden; }
    .tab-bar { display: flex; background: white; border-bottom: 1px solid #f1f5f9; }
    .tab-bar button { flex: 1; padding: 16px; border: none; background: none; font-size: 13px; font-weight: 800; color: #94a3b8; cursor: pointer; }
    .tab-bar button.active { color: var(--primary-color); border-bottom: 3px solid var(--primary-color); }
    .card { background: white; border-radius: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.02); margin-bottom: 16px; border: 1px solid #f1f5f9; }
    .p-0 { padding: 0; } .p-16 { padding: 16px; }
    .hero-card { padding: 24px; text-align: center; }
    .hero-card h2 { margin: 0; font-size: 22px; font-weight: 800; cursor: pointer; }
    small { font-size: 13px; color: var(--primary-color); opacity: 0.5; margin-left: 4px; }
    .section-title { display: block; font-size: 10px; font-weight: 800; color: #94a3b8; letter-spacing: 1px; margin: 0 0 10px 4px; text-transform: uppercase; }
    .detail-row { display: flex; align-items: center; gap: 12px; padding: 16px; border-bottom: 1px solid #f8fafc; }
    .value-col { flex: 1; display: flex; flex-direction: column; cursor: pointer; }
    .value { font-weight: 600; color: #1e293b; font-size: 16px; }
    .add-link { background: none; border: none; color: var(--primary-color); font-weight: 700; font-size: 14px; cursor: pointer; }
    .edit-box { display: flex; align-items: center; gap: 8px; }
    input, textarea { flex: 1; padding: 10px 14px; border: 2px solid var(--primary-color); border-radius: 12px; font-size: 15px; outline: none; background: #f8fafc; }
    .save { background: #10b981; color: white; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; }
    .save-long { background: var(--primary-color); color: white; border: none; padding: 10px 20px; border-radius: 12px; font-weight: 700; }
    .cancel { background: #f1f5f9; color: #64748b; border: none; padding: 8px 12px; border-radius: 10px; cursor: pointer; font-size: 12px; font-weight: 700; }
    .icon-btn-del { background: #fef2f2; color: #ef4444; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; }
    .clickable-text { font-size: 15px; color: #1e293b; font-weight: 500; cursor: pointer; min-height: 32px; display: flex; align-items: center; }
    .divider { height: 1px; background: #f1f5f9; margin: 12px 0; }
    .center-spinner { display: flex; justify-content: center; padding: 60px; }
    .spinner { width: 28px; height: 28px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
