<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';
    import { fade, slide, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';

    export let contactId;
    const dispatch = createEventDispatcher();

    let contact = null;
    let isLoading = true;
    let currentStatus = 'SCHEDULED'; // По умолчанию

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
            // Здесь можно было бы получить статус последнего визита,
            // но пока используем локальное состояние для демонстрации дизайна
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
            dispatch('updated', contact);
        } catch (e) {
            alert('Ошибка при сохранении');
            cancelAllEdits();
        }
    }

    function setStatus(status) {
        currentStatus = status;
    }
</script>

<div class="profile-card status-{currentStatus.toLowerCase()}" in:scale={{duration: 500, start: 0.9, easing: quintOut}}>
    {#if isLoading && !contact}
        <div class="center-loader"><span class="spinner"></span></div>
    {:else if contact}
        <header class="card-header" in:fade>
            <div class="avatar-section">
                <div class="avatar-big">{contact.name.charAt(0).toUpperCase()}</div>
                <div class="status-indicator"></div>
            </div>

            <div class="title-section">
                {#if editMode.name}
                    <div class="edit-row">
                        <input type="text" bind:value={tempValues.name} class="name-input" autofocus />
                        <button class="save-btn-icon" on:click={() => saveField('name')}>✓</button>
                    </div>
                {:else}
                    <h2 on:click={() => editMode.name = true}>{contact.name} <span>✎</span></h2>
                {/if}

                <div class="status-selector">
                    <button class:active={currentStatus === 'SCHEDULED'} on:click={() => setStatus('SCHEDULED')}>Ожидается</button>
                    <button class:active={currentStatus === 'NEEDS_CALL'} on:click={() => setStatus('NEEDS_CALL')}>Звонок</button>
                    <button class:active={currentStatus === 'COMPLETED'} on:click={() => setStatus('COMPLETED')}>Оказана</button>
                    <button class:active={currentStatus === 'CANCELLED'} on:click={() => setStatus('CANCELLED')}>Отмена</button>
                </div>
            </div>
        </header>

        <div class="details-grid">
            <!-- КОНТАКТЫ -->
            <section class="info-group">
                <label>Контактные данные</label>
                <div class="tiles-container">
                    {#each contact.phones as phone, i}
                        <div class="tile">
                            {#if editMode.phoneIdx === i}
                                <div class="tile-edit">
                                    <input type="tel" bind:value={tempValues.phones[i]} autofocus />
                                    <button class="save-mini" on:click={() => saveField('phones')}>✓</button>
                                </div>
                            {:else}
                                <span class="phone-val" on:click={() => editMode.phoneIdx = i}>
                                    {phoneUtils.format(phone)}
                                </span>
                                <a href="tel:{phone}" class="btn-call">📞</a>
                            {/if}
                        </div>
                    {/each}
                    <button class="btn-add-tile" on:click={() => editMode.isAddingPhone = true}>+ Добавить</button>
                </div>
            </section>

            <!-- EMAIL -->
            <section class="info-group">
                <label>E-mail</label>
                <div class="tile full">
                    {#if editMode.email}
                        <div class="tile-edit">
                            <input type="email" bind:value={tempValues.email} autofocus />
                            <button class="save-mini" on:click={() => saveField('email')}>✓</button>
                        </div>
                    {:else}
                        <span class="email-val" on:click={() => editMode.email = true}>
                            {contact.email || 'Нажмите, чтобы добавить почту'}
                        </span>
                    {/if}
                </div>
            </section>

            <!-- ЗАМЕТКИ -->
            <section class="info-group">
                <label>Заметки о клиенте</label>
                <div class="tile full notes-area" on:click={() => !editMode.notes && (editMode.notes = true)}>
                    {#if editMode.notes}
                        <div class="notes-edit-box" transition:slide>
                            <textarea bind:value={tempValues.notes} rows="4" autofocus></textarea>
                            <div class="actions-row">
                                <button class="btn-text" on:click|stopPropagation={cancelAllEdits}>Отмена</button>
                                <button class="btn-save-pill" on:click|stopPropagation={() => saveField('notes')}>✓ Сохранить</button>
                            </div>
                        </div>
                    {:else}
                        <p class="notes-text">{contact.notes || 'Нет описания...'}</p>
                    {/if}
                </div>
            </section>
        </div>
    {/if}
</div>

<style>
    .profile-card {
        background: white;
        border-radius: 32px;
        padding: 32px;
        transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
        box-shadow: 0 20px 50px rgba(0,0,0,0.05);
        border: 1px solid #f1f5f9;
        position: relative;
        overflow: hidden;
    }

    /* ТЕМЫ В ЗАВИСИМОСТИ ОТ СТАТУСА */
    .status-scheduled { background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%); border-color: #3b82f6; }
    .status-completed { background: linear-gradient(135deg, #ffffff 0%, #f0fdf4 100%); border-color: #10b981; }
    .status-needs_call { background: linear-gradient(135deg, #ffffff 0%, #fffbeb 100%); border-color: #f59e0b; }
    .status-cancelled { background: linear-gradient(135deg, #ffffff 0%, #fff1f2 100%); border-color: #ef4444; }

    .card-header { display: flex; align-items: center; gap: 24px; margin-bottom: 32px; }

    .avatar-big {
        width: 84px; height: 84px; background: white; border-radius: 28px;
        display: flex; align-items: center; justify-content: center;
        font-size: 36px; font-weight: 900; color: var(--primary-color);
        box-shadow: 0 10px 20px rgba(0,0,0,0.05);
    }

    .title-section { flex: 1; }
    .title-section h2 { margin: 0; font-size: 24px; font-weight: 800; color: #0f172a; cursor: pointer; }
    .title-section h2 span { font-size: 16px; opacity: 0.2; margin-left: 8px; }

    .status-selector { display: flex; gap: 6px; margin-top: 12px; }
    .status-selector button {
        padding: 4px 10px; border-radius: 8px; border: 1px solid #e2e8f0;
        background: white; font-size: 10px; font-weight: 800; color: #64748b;
        cursor: pointer; text-transform: uppercase; transition: all 0.2s;
    }
    .status-selector button.active { background: var(--primary-color); color: white; border-color: var(--primary-color); }

    .details-grid { display: flex; flex-direction: column; gap: 24px; }
    label {
        display: block; font-size: 10px; font-weight: 800; color: #94a3b8;
        text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; margin-left: 4px;
    }

    .tiles-container { display: flex; flex-wrap: wrap; gap: 12px; }
    .tile {
        background: white; padding: 14px 18px; border-radius: 18px;
        border: 1px solid #f1f5f9; display: flex; align-items: center; gap: 12px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.02);
    }
    .tile.full { width: 100%; box-sizing: border-box; }

    .phone-val { font-weight: 700; color: #1e293b; cursor: pointer; font-size: 16px; }
    .btn-call { text-decoration: none; font-size: 16px; }

    .btn-add-tile {
        background: none; border: 2px dashed #e2e8f0; padding: 12px 20px;
        border-radius: 18px; color: #94a3b8; font-weight: 700; cursor: pointer;
    }

    .notes-area { min-height: 100px; align-items: flex-start; cursor: pointer; }
    .notes-text { margin: 0; color: #64748b; font-size: 14px; line-height: 1.6; font-style: italic; }

    .edit-row, .tile-edit, .notes-edit-box { display: flex; gap: 10px; width: 100%; }
    .notes-edit-box { flex-direction: column; }

    input, textarea {
        width: 100%; padding: 10px 14px; border: 2px solid var(--primary-color);
        border-radius: 12px; font-size: 15px; outline: none; background: #f8fafc;
    }

    .save-btn-icon, .save-mini {
        background: #10b981; color: white; border: none; width: 40px; height: 40px;
        border-radius: 12px; cursor: pointer;
    }

    .actions-row { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
    .btn-text { background: none; border: none; color: #94a3b8; font-weight: 700; cursor: pointer; }
    .btn-save-pill { background: var(--primary-color); color: white; border: none; padding: 10px 24px; border-radius: 12px; font-weight: 700; cursor: pointer; }

    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center-loader { display: flex; justify-content: center; padding: 60px; }
</style>
