<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { fade, slide, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';

    export let contactId;
    const dispatch = createEventDispatcher();

    let contact = null;
    let isLoading = true;

    // Константы для локализации (в будущем можно брать из настроек компании)
    const ASSET_LABEL = "Связанные объекты"; // Универсальное название
    const ASSET_ICON = "🚗"; // Можно менять на 🐾 или 🧪
    const ASSET_PLACEHOLDER = "Марка, госномер, S/N...";

    let editMode = { name: false, email: false, notes: false, phoneIdx: -1, isAddingPhone: false, isAddingTag: false };
    let tempValues = { name: '', phones: [], email: '', notes: '', tags: [], newPhone: '', newTag: '' };

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
            tags: [...(contact.tags || [])],
            newPhone: '',
            newTag: ''
        };
    }

    function cancelAllEdits() {
        editMode = { name: false, email: false, notes: false, phoneIdx: -1, isAddingPhone: false, isAddingTag: false };
        syncTempValues();
    }

    async function saveField(type) {
        try {
            let finalPhones = tempValues.phones.map(p => phoneUtils.clean(p)).filter(p => p);
            if (type === 'addPhone') {
                finalPhones = [...finalPhones, phoneUtils.clean(tempValues.newPhone)];
            }

            let finalTags = [...tempValues.tags];
            if (type === 'addTag' && tempValues.newTag.trim()) {
                const tag = tempValues.newTag.trim();
                if (!finalTags.includes(tag)) {
                    finalTags = [...finalTags, tag];
                }
            }

            if (!tempValues.name) return alert('Имя обязательно');
            if (finalPhones.length === 0) return alert('У клиента должен быть минимум один телефон');

            const payload = {
                ...contact,
                name: tempValues.name,
                email: tempValues.email || null,
                notes: tempValues.notes || null,
                phones: finalPhones,
                tags: finalTags
            };

            const res = await api.put(`/admin/clients/${contactId}`, payload);
            contact = res.data;
            cancelAllEdits();

            // Обновляем таймлайн, так как теги могли измениться
            scheduleRefreshSignal.set({ ts: Date.now(), source: 'local' });
            dispatch('updated', contact);
        } catch (e) {
            alert('Ошибка при сохранении');
            cancelAllEdits();
        }
    }

    async function removeTag(index) {
        tempValues.tags = tempValues.tags.filter((_, i) => i !== index);
        await saveField('removeTag');
    }
</script>

<div class="profile-card" in:scale={{duration: 400, start: 0.95, easing: quintOut}}>
    {#if isLoading && !contact}
        <div class="center-loader"><span class="spinner"></span></div>
    {:else if contact}
        <header class="card-header" in:fade>
            <div class="avatar-section">
                <div class="avatar-big">{contact.name.charAt(0).toUpperCase()}</div>
                <div class="badge-role">КЛИЕНТ</div>
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
                <p class="id-hint">ID: {contact.id.split('-')[0]}</p>
            </div>
        </header>

        <div class="details-grid">
            <!-- СЕКЦИЯ ОБЪЕКТОВ (ТЕГИ) -->
            <section class="info-group">
                <label>{ASSET_LABEL}</label>
                <div class="tags-cloud">
                    {#each contact.tags || [] as tag, i}
                        <div class="tag-badge" in:scale>
                            <span class="tag-icon">{ASSET_ICON}</span>
                            <span class="tag-text">{tag}</span>
                            <button class="tag-remove" on:click={() => removeTag(i)}>✕</button>
                        </div>
                    {/each}

                    {#if !editMode.isAddingTag}
                        <button class="btn-add-tag" on:click={() => editMode.isAddingTag = true}>+ Объект</button>
                    {:else}
                        <div class="tag-edit-inline" transition:slide={{axis:'x'}}>
                            <input type="text" bind:value={tempValues.newTag} placeholder={ASSET_PLACEHOLDER} autofocus
                                   on:keydown={(e) => e.key === 'Enter' && saveField('addTag')} />
                            <button class="save-mini" on:click={() => saveField('addTag')}>✓</button>
                            <button class="btn-close-mini" on:click={() => editMode.isAddingTag = false}>✕</button>
                        </div>
                    {/if}
                </div>
            </section>

            <section class="info-group">
                <label>Контактные телефоны</label>
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
                                <a href="tel:+{phoneUtils.clean(phone)}" class="btn-call" title="Позвонить">📞</a>
                            {/if}
                        </div>
                    {/each}

                    {#if !editMode.isAddingPhone}
                        <button class="btn-add-tile" on:click={() => editMode.isAddingPhone = true}>+ Номер</button>
                    {:else}
                        <div class="tile full" transition:slide>
                            <div class="tile-edit">
                                <input type="tel" bind:value={tempValues.newPhone} placeholder="+7..." autofocus />
                                <button class="save-mini" on:click={() => saveField('addPhone')}>✓</button>
                                <button class="btn-close-mini" on:click={cancelAllEdits}>✕</button>
                            </div>
                        </div>
                    {/if}
                </div>
            </section>

            <section class="info-group">
                <label>E-mail адрес</label>
                <div class="tile full">
                    {#if editMode.email}
                        <div class="tile-edit">
                            <input type="email" bind:value={tempValues.email} autofocus />
                            <button class="save-mini" on:click={() => saveField('email')}>✓</button>
                        </div>
                    {:else}
                        <span class="val-text" on:click={() => editMode.email = true}>
                            {contact.email || 'Добавить почту...'}
                        </span>
                    {/if}
                </div>
            </section>

            <section class="info-group">
                <label>Заметки и особенности</label>
                <div class="tile full notes-area" on:click={() => !editMode.notes && (editMode.notes = true)}>
                    {#if editMode.notes}
                        <div class="notes-edit-box" transition:slide>
                            <textarea bind:value={tempValues.notes} rows="4" autofocus></textarea>
                            <div class="actions-row">
                                <button class="btn-text" on:click|stopPropagation={cancelAllEdits}>Отмена</button>
                                <button class="btn-save-pill" on:click|stopPropagation={() => saveField('notes')}>Сохранить ✓</button>
                            </div>
                        </div>
                    {:else}
                        <p class="notes-text">{contact.notes || 'Нажмите, чтобы добавить описание...'}</p>
                    {/if}
                </div>
            </section>
        </div>
    {/if}
</div>

<style>
    .profile-card { background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%); border-radius: 32px; padding: 32px; transition: all 0.4s ease; box-shadow: 0 20px 50px rgba(0,0,0,0.04); border: 1px solid #f1f5f9; }
    .card-header { display: flex; align-items: center; gap: 24px; margin-bottom: 32px; }
    .avatar-big { width: 84px; height: 84px; background: white; border-radius: 28px; display: flex; align-items: center; justify-content: center; font-size: 36px; font-weight: 950; color: var(--primary-color); box-shadow: 0 10px 20px rgba(56, 151, 240, 0.1); border: 1px solid #eff6ff; }
    .badge-role { margin-top: 8px; font-size: 9px; font-weight: 900; color: var(--primary-color); background: #eff6ff; padding: 2px 8px; border-radius: 6px; letter-spacing: 0.5px; }
    .title-section h2 { margin: 0; font-size: 24px; font-weight: 800; color: #0f172a; cursor: pointer; }
    .title-section h2 span { font-size: 16px; opacity: 0.2; margin-left: 8px; }
    .id-hint { margin: 4px 0 0 4px; font-size: 11px; color: #cbd5e1; font-weight: 700; }
    .details-grid { display: flex; flex-direction: column; gap: 24px; }
    label { display: block; font-size: 10px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1.2px; margin-bottom: 10px; margin-left: 4px; }

    /* ТЕГИ / ОБЪЕКТЫ */
    .tags-cloud { display: flex; flex-wrap: wrap; gap: 8px; }
    .tag-badge { background: #f0fdf4; border: 1.5px solid #dcfce7; padding: 8px 14px; border-radius: 14px; display: flex; align-items: center; gap: 8px; transition: 0.2s; }
    .tag-badge:hover { border-color: #10b981; }
    .tag-icon { font-size: 14px; }
    .tag-text { font-weight: 700; color: #166534; font-size: 13px; }
    .tag-remove { background: none; border: none; color: #10b981; cursor: pointer; font-weight: 800; padding: 0 2px; }
    .tag-remove:hover { color: #ef4444; }

    .btn-add-tag { background: none; border: 1.5px dashed #d1d5db; padding: 8px 16px; border-radius: 14px; color: #94a3b8; font-weight: 700; font-size: 12px; cursor: pointer; transition: 0.2s; }
    .btn-add-tag:hover { border-color: var(--primary-color); color: var(--primary-color); }
    .tag-edit-inline { display: flex; gap: 8px; align-items: center; background: white; padding: 4px; border-radius: 14px; border: 1.5px solid var(--primary-color); }
    .tag-edit-inline input { border: none; padding: 6px 10px; font-size: 13px; width: 160px; background: none; outline: none; }

    .tiles-container { display: flex; flex-wrap: wrap; gap: 12px; }
    .tile { background: white; padding: 14px 18px; border-radius: 18px; border: 1px solid #f1f5f9; display: flex; align-items: center; gap: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
    .tile.full { width: 100%; box-sizing: border-box; }
    .phone-val { font-weight: 700; color: #1e293b; cursor: pointer; font-size: 16px; }
    .val-text { font-weight: 600; color: #1e293b; cursor: pointer; }
    .btn-call { text-decoration: none; font-size: 18px; opacity: 0.8; transition: 0.2s; }
    .btn-add-tile { background: none; border: 2px dashed #e2e8f0; padding: 12px 20px; border-radius: 18px; color: #94a3b8; font-weight: 700; cursor: pointer; }
    .notes-area { min-height: 90px; align-items: flex-start; cursor: pointer; }
    .notes-text { margin: 0; color: #64748b; font-size: 14px; line-height: 1.6; font-style: italic; }
    .edit-row, .tile-edit, .notes-edit-box { display: flex; gap: 10px; width: 100%; }
    .notes-edit-box { flex-direction: column; }
    input, textarea { width: 100%; padding: 10px 14px; border: 2px solid var(--primary-color); border-radius: 12px; font-size: 15px; outline: none; background: #f8fafc; color: #0f172a; }
    .save-btn-icon, .save-mini { background: #10b981; color: white; border: none; width: 40px; height: 40px; border-radius: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
    .btn-close-mini { background: none; border: none; color: #94a3b8; font-size: 18px; cursor: pointer; }
    .actions-row { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
    .btn-text { background: none; border: none; color: #94a3b8; font-weight: 700; cursor: pointer; }
    .btn-save-pill { background: var(--primary-color); color: white; border: none; padding: 10px 24px; border-radius: 12px; font-weight: 700; cursor: pointer; }
    .center-loader { display: flex; justify-content: center; padding: 60px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
