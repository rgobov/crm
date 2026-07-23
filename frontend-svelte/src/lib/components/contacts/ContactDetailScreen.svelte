<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';
    import { adminService } from '$lib/services/adminService.js';
    import { nicheSettings } from '$lib/stores/nicheStore.js';
    import { fade, slide, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';

    export let contactId;
    const dispatch = createEventDispatcher();

    let contact = null;
    let isLoading = true;

    // Адаптивные лейблы/иконки под выбранную нишу филиала
    $: ASSET_LABEL = $nicheSettings.assetLabel;
    $: ASSET_ICON = $nicheSettings.assetIcon;
    $: ASSET_PLACEHOLDER = $nicheSettings.assetPlaceholder;

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
                const cleaned = phoneUtils.clean(tempValues.newPhone);
                if (cleaned) finalPhones = [...finalPhones, cleaned];
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

    let isExporting = false;
    async function exportClientVisits() {
        if (isExporting) return;
        isExporting = true;
        try {
            const blob = await adminService.exportAppointments(null, null, contactId);
            const url = window.URL.createObjectURL(new Blob([blob]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `visits_${contact.name.replace(/\s+/g, '_')}.xlsx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (e) {
            console.error('Export client visits failed', e);
        } finally {
            isExporting = false;
        }
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
                        <input id="edit-name" type="text" bind:value={tempValues.name} class="name-input" autofocus
                               on:keydown={(e) => e.key === 'Enter' && saveField('name')} />
                        <button class="save-btn-icon" on:click={() => saveField('name')} type="button" aria-label="Сохранить имя">✓</button>
                    </div>
                {:else}
                    <button class="h2-btn" on:click={() => editMode.name = true} type="button" aria-label="Редактировать имя">
                        <h2>{contact.name} <span>✎</span></h2>
                    </button>
                {/if}
                <p class="id-hint">ID: {contact.id.split('-')[0]}</p>
            </div>

            <div class="header-export-wrap" style="margin-left: auto;">
                <button class="export-client-btn" class:loading={isExporting} on:click={exportClientVisits} title="Выгрузить визиты клиента в Excel" disabled={isExporting}>
                    {isExporting ? '⏳ Экспорт...' : '📥 Выгрузить визиты'}
                </button>
            </div>
        </header>

        <div class="details-grid">
            <!-- СЕКЦИЯ ОБЪЕКТОВ -->
            <section class="info-group">
                <label for="new-tag-input">{ASSET_LABEL}</label>
                <div class="tags-cloud">
                    {#each contact.tags || [] as tag, i}
                        <div class="tag-badge" in:scale>
                            <span class="tag-icon">{ASSET_ICON}</span>
                            <span class="tag-text">{tag}</span>
                            <button class="tag-remove" on:click={() => removeTag(i)} type="button" aria-label={$nicheSettings.assetAriaLabel}>✕</button>
                        </div>
                    {/each}

                    {#if !editMode.isAddingTag}
                        <button class="btn-add-tag" on:click={() => editMode.isAddingTag = true} type="button">{$nicheSettings.assetAddBtn}</button>
                    {:else}
                        <div class="tag-edit-inline" transition:slide={{axis:'x'}}>
                            <input id="new-tag-input" type="text" bind:value={tempValues.newTag} placeholder={ASSET_PLACEHOLDER} autofocus
                                   on:keydown={(e) => e.key === 'Enter' && saveField('addTag')} />
                            <button class="save-mini" on:click={() => saveField('addTag')} type="button">✓</button>
                            <button class="btn-close-mini" on:click={() => editMode.isAddingTag = false} type="button">✕</button>
                        </div>
                    {/if}
                </div>
            </section>

            <section class="info-group">
                <label for="add-phone-input">Контактные телефоны</label>
                <div class="tiles-container">
                    {#each contact.phones as phone, i}
                        <div class="tile">
                            {#if editMode.phoneIdx === i}
                                <div class="tile-edit">
                                    <input id="edit-phone-{i}" type="tel" bind:value={tempValues.phones[i]} autofocus
                                           on:keydown={(e) => e.key === 'Enter' && saveField('phones')} />
                                    <button class="save-mini" on:click={() => saveField('phones')} type="button">✓</button>
                                </div>
                            {:else}
                                <button class="phone-val-btn" on:click={() => editMode.phoneIdx = i} type="button">
                                    <span class="phone-val">{phoneUtils.format(phone)}</span>
                                </button>
                                <a href="tel:+{phoneUtils.clean(phone)}" class="btn-call" title="Позвонить">📞</a>
                                <a href="tg://resolve?phone={phoneUtils.clean(phone)}" class="btn-tg" title="Написать в Telegram" target="_blank">
                                    <svg viewBox="0 0 24 24" width="18" height="18" fill="#0088cc"><path d="M11.944 0A12 12 0 000 12a12 12 0 0012 12 12 12 0 0012-12A12 12 0 0012 0a12 12 0 00-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 01.171.325c.016.127-.007.352-.086.62-.272 1.063-1.444 5.147-1.444 5.147s-.187.58-.583.595c-.2.014-.465-.147-.59-.252-.13-.108-1.094-.768-1.414-.944-.31-.176-.666-.2-.95-.056-.58.297-1.17 1.147-1.526 1.487-.168.168-.344.26-.563.245-.396-.035-.56-.422-.56-.422s-.352-1.09-.622-2.01a3.9 3.9 0 01-.198-.618c-.11-.468.307-.68.495-.75l2.958-1.05c.165-.06.31-.022.414.076.083.078.12.194.104.31-.04.226-.26.78-.34 1.006-.08.226-.163.498-.05.698.072.13.235.178.384.13.226-.08 1.186-.656 1.78-1.098.35-.26.62-.392.77-.434.093-.026.2-.01.28.04z"/></svg>
                                </a>
                            {/if}
                        </div>
                    {/each}

                    {#if !editMode.isAddingPhone}
                        <button class="btn-add-tile" on:click={() => editMode.isAddingPhone = true} type="button">+ Номер</button>
                    {:else}
                        <div class="tile full" transition:slide>
                            <div class="tile-edit">
                                <input id="add-phone-input" type="tel" bind:value={tempValues.newPhone} placeholder="+7..." autofocus
                                       on:keydown={(e) => e.key === 'Enter' && saveField('addPhone')} />
                                <button class="save-mini" on:click={() => saveField('addPhone')} type="button">✓</button>
                                <button class="btn-close-mini" on:click={cancelAllEdits} type="button">✕</button>
                            </div>
                        </div>
                    {/if}
                </div>
            </section>

            <section class="info-group">
                <label for="edit-email">E-mail адрес</label>
                <div class="tile full">
                    {#if editMode.email}
                        <div class="tile-edit">
                            <input id="edit-email" type="email" bind:value={tempValues.email} autofocus
                                   on:keydown={(e) => e.key === 'Enter' && saveField('email')} />
                            <button class="save-mini" on:click={() => saveField('email')} type="button">✓</button>
                        </div>
                    {:else}
                        <button class="email-val-btn" on:click={() => editMode.email = true} type="button">
                            <span class="val-text">{contact.email || 'Добавить почту...'}</span>
                        </button>
                    {/if}
                </div>
            </section>

            <section class="info-group">
                <label for="notes-textarea">Заметки и особенности</label>
                <div class="tile full notes-area-wrapper">
                    {#if editMode.notes}
                        <div class="notes-edit-box" transition:slide>
                            <textarea id="notes-textarea" bind:value={tempValues.notes} rows="4" autofocus></textarea>
                            <div class="actions-row">
                                <button class="btn-text" on:click={cancelAllEdits} type="button">Отмена</button>
                                <button class="btn-save-pill" on:click={() => saveField('notes')} type="button">Сохранить ✓</button>
                            </div>
                        </div>
                    {:else}
                        <button class="notes-display-btn" on:click={() => editMode.notes = true} type="button">
                            <p class="notes-text">{contact.notes || 'Нажмите, чтобы добавить описание...'}</p>
                        </button>
                    {/if}
                </div>
            </section>
        </div>
    {/if}
</div>

<style>
    .profile-card { background: #fdf6e3; border-radius: 32px; padding: 32px; transition: all 0.4s ease; border: 1px solid #ddd6c1; }
    .card-header { display: flex; align-items: center; gap: 24px; margin-bottom: 32px; }
    .avatar-big { width: 84px; height: 84px; background: #eee8d5; border-radius: 28px; display: flex; align-items: center; justify-content: center; font-size: 36px; font-weight: 950; color: #268bd2; border: 1px solid #ddd6c1; }
    .badge-role { margin-top: 8px; font-size: 9px; font-weight: 900; color: #268bd2; background: #eee8d5; padding: 2px 8px; border-radius: 6px; letter-spacing: 0.5px; }

    .h2-btn, .phone-val-btn, .email-val-btn, .notes-display-btn { background: none; border: none; padding: 0; text-align: left; cursor: pointer; width: 100%; display: block; }
    .title-section h2 { margin: 0; font-size: 24px; font-weight: 800; color: #073642; }
    .title-section h2 span { font-size: 16px; opacity: 0.2; margin-left: 8px; color: #586e75; }
    .id-hint { margin: 4px 0 0 4px; font-size: 11px; color: #93a1a1; font-weight: 700; }
    .details-grid { display: flex; flex-direction: column; gap: 24px; }
    label { display: block; font-size: 10px; font-weight: 800; color: #586e75; text-transform: uppercase; letter-spacing: 1.2px; margin-bottom: 10px; margin-left: 4px; }

    .tags-cloud { display: flex; flex-wrap: wrap; gap: 8px; }
    .tag-badge { background: #eee8d5; border: 1.5px solid #ddd6c1; padding: 8px 14px; border-radius: 14px; display: flex; align-items: center; gap: 8px; transition: 0.2s; }
    .tag-text { font-weight: 700; color: #073642; font-size: 13px; }
    .tag-remove { background: none; border: none; color: #dc322f; cursor: pointer; font-weight: 800; padding: 0 2px; }

    .btn-add-tag { background: none; border: 1.5px dashed #93a1a1; padding: 8px 16px; border-radius: 14px; color: #586e75; font-weight: 700; font-size: 12px; cursor: pointer; }
    .tag-edit-inline { display: flex; gap: 8px; align-items: center; background: #fdf6e3; padding: 4px; border-radius: 14px; border: 1.5px solid #268bd2; }
    .tag-edit-inline input { border: none; padding: 6px 10px; font-size: 13px; width: 160px; background: none; outline: none; color: #073642; }

    .tiles-container { display: flex; flex-wrap: wrap; gap: 12px; }
    .tile { background: #eee8d5; padding: 14px 18px; border-radius: 18px; border: 1px solid #ddd6c1; display: flex; align-items: center; gap: 12px; }
    .tile.full { width: 100%; box-sizing: border-box; }

    .phone-val { font-weight: 700; color: #073642; font-size: 16px; }
    .val-text { font-weight: 600; color: #073642; }

    .btn-call { text-decoration: none; font-size: 18px; opacity: 0.8; transition: 0.2s; }
    .btn-tg { text-decoration: none; display: inline-flex; align-items: center; opacity: 0.7; transition: opacity 0.2s; }
    .btn-tg:hover { opacity: 1; }
    .btn-add-tile { background: none; border: 2px dashed #93a1a1; padding: 12px 20px; border-radius: 18px; color: #586e75; font-weight: 700; cursor: pointer; }

    .notes-area-wrapper { min-height: 90px; }
    .notes-text { margin: 0; color: #586e75; font-size: 14px; line-height: 1.6; font-style: italic; }

    .edit-row, .tile-edit, .notes-edit-box { display: flex; gap: 10px; width: 100%; }
    .notes-edit-box { flex-direction: column; }

    input, textarea { width: 100%; padding: 10px 14px; border: 2px solid #268bd2; border-radius: 12px; font-size: 15px; outline: none; background: #fdf6e3; color: #073642; }
    input::placeholder, textarea::placeholder { color: #93a1a1; }
    .save-btn-icon, .save-mini { background: #859900; color: white; border: none; width: 40px; height: 40px; border-radius: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
    .btn-close-mini { background: none; border: none; color: #586e75; font-size: 18px; cursor: pointer; }
    .actions-row { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
    .btn-text { background: none; border: none; color: #586e75; font-weight: 700; cursor: pointer; }
    .btn-save-pill { background: #268bd2; color: white; border: none; padding: 10px 24px; border-radius: 12px; font-weight: 700; cursor: pointer; }
    .center-loader { display: flex; justify-content: center; padding: 60px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .export-client-btn {
        background: #eee8d5;
        color: #268bd2;
        border: 1.5px solid #ddd6c1;
        padding: 8px 16px;
        border-radius: 12px;
        font-weight: 800;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.2s;
    }
    .export-client-btn:hover {
        background: #fdf6e3;
        box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    }
    .export-client-btn:active {
        transform: scale(0.95);
    }
    .export-client-btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
    }
</style>
