<script>
    import { createEventDispatcher, onMount } from 'svelte';
    import { resourceService } from '$lib/services/resourceService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { ensureJpeg } from '$lib/utils/heicUtils.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { fade, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';
    import { portal } from '$lib/actions/portal.js';

    export let resource = null;
    const dispatch = createEventDispatcher();

    let branches = [];
    let formData = {
        name: resource?.name || '',
        description: resource?.description || '',
        branchId: resource?.branchId || $activeBranchId || ''
    };

    let isSaving = false;
    let isLoading = true;
    let photoData = null;
    let isUploadingPhoto = false;
    let photoFileInput = null;

    onMount(async () => {
        try {
            branches = await branchService.getBranches();
            if (!formData.branchId && $activeBranchId) {
                formData.branchId = $activeBranchId;
            }
            // Загружаем фото если редактируем существующий ресурс
            if (resource?.id) {
                try {
                    const resp = await resourceService.getResourcePhoto(resource.id);
                    if (resp && resp.photoData) photoData = resp.photoData;
                } catch (e) {
                    console.warn('Resource photo load failed', e);
                }
            }
        } catch (e) {
            console.error('Failed to load branches');
        } finally {
            isLoading = false;
        }
    });

    async function handleSave() {
        if (!formData.name.trim()) return alert('Введите название');
        if (!formData.branchId) return alert('Выберите филиал');

        isSaving = true;
        try {
            let result;
            if (resource?.id) {
                result = await resourceService.updateResource(resource.id, formData);
            } else {
                result = await resourceService.addResource(formData);
            }
            dispatch('success', result);
        } catch (e) {
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }

    async function handlePhotoSelect(e) {
        const file = e.target.files?.[0];
        if (!file) return;
        if (!file.type.startsWith('image/')) {
            alert('Пожалуйста, выберите изображение');
            return;
        }
        if (!resource?.id) {
            alert('Сначала сохраните ресурс, затем загрузите фото');
            if (photoFileInput) photoFileInput.value = '';
            return;
        }
        isUploadingPhoto = true;
        try {
            const jpegFile = await ensureJpeg(file);
            await resourceService.uploadResourcePhoto(resource.id, jpegFile);
            const resp = await resourceService.getResourcePhoto(resource.id);
            photoData = resp?.photoData || null;
            dispatch('photoUpdated');
        } catch (e) {
            alert('Ошибка при загрузке фото: ' + (e.response?.data?.message || e.message));
        } finally {
            isUploadingPhoto = false;
            if (photoFileInput) photoFileInput.value = '';
        }
    }

    async function handlePhotoDelete() {
        if (!resource?.id) return;
        if (!confirm('Удалить фото ресурса?')) return;
        try {
            await resourceService.deleteResourcePhoto(resource.id);
            photoData = null;
            dispatch('photoUpdated');
        } catch (e) {
            alert('Ошибка при удалении фото');
        }
    }
</script>

<div class="modal-backdrop" use:portal on:click|self={() => dispatch('cancel')} transition:fade={{duration: 200}}>
    <div class="modal-content" in:scale={{duration: 400, start: 0.95, easing: quintOut}}>

        <header class="modal-header">
            <div class="header-icon">📦</div>
            <div class="header-text">
                <h3>{resource ? 'Редактировать' : 'Новый ресурс'}</h3>
                <p>Кабинеты и оборудование</p>
            </div>
            <button class="close-btn" on:click={() => dispatch('cancel')}>✕</button>
        </header>

        <div class="modal-body">
            {#if isLoading}
                <div class="loader-wrap"><span class="spinner"></span></div>
            {:else}
                <!-- СЕКЦИЯ ФОТО -->
                {#if resource?.id}
                    <div class="photo-section">
                        <label class="photo-box" for="resource-photo-upload">
                            {#if isUploadingPhoto}
                                <span class="spinner"></span>
                            {:else if photoData}
                                <img src={"data:image/jpeg;base64," + photoData} alt={formData.name} class="photo-preview" />
                            {:else}
                                <span class="photo-placeholder">📷</span>
                                <span class="photo-hint">Загрузить фото</span>
                            {/if}
                        </label>
                        <input
                            bind:this={photoFileInput}
                            id="resource-photo-upload"
                            type="file"
                            accept="image/*"
                            on:change={handlePhotoSelect}
                            style="display: none;"
                        />
                        <div class="photo-actions">
                            <button class="btn-photo-upload" on:click={() => photoFileInput?.click()} disabled={isUploadingPhoto}>
                                {photoData ? 'Заменить' : 'Загрузить'}
                            </button>
                            {#if photoData}
                                <button class="btn-photo-delete" on:click={handlePhotoDelete} disabled={isUploadingPhoto}>Удалить</button>
                            {/if}
                        </div>
                    </div>
                {/if}

                <div class="tiles-grid">
                    <!-- ПЛИТКА: ФИЛИАЛ (НОВОЕ) -->
                    <div class="input-tile accent">
                        <label>Привязка к филиалу</label>
                        <select bind:value={formData.branchId}>
                            <option value="">Выберите филиал...</option>
                            {#each branches as b}
                                <option value={b.id}>{b.name}</option>
                            {/each}
                        </select>
                    </div>

                    <div class="input-tile">
                        <label>Название ресурса</label>
                        <input type="text" bind:value={formData.name} placeholder="Напр: Кабинет №5" autofocus />
                    </div>

                    <div class="input-tile full">
                        <label>Описание и заметки</label>
                        <textarea bind:value={formData.description} rows="4" placeholder="Дополнительная информация..."></textarea>
                    </div>
                </div>
            {/if}
        </div>

        <footer class="modal-footer">
            <button class="btn-cancel" on:click={() => dispatch('cancel')}>ОТМЕНА</button>
            <button class="btn-save" on:click={handleSave} disabled={isSaving || isLoading}>
                {isSaving ? '...' : 'СОХРАНИТЬ'}
            </button>
        </footer>
    </div>
</div>

<style>
    .modal-backdrop {
        position: fixed; inset: 0; background: rgba(7, 54, 66, 0.6);
        backdrop-filter: blur(8px); z-index: 99999;
        display: flex; align-items: center; justify-content: center;
        padding: 20px;
        box-sizing: border-box;
    }

    .modal-content {
        background: #fdf6e3; width: 100%; max-width: 500px;
        border-radius: 32px; overflow: hidden;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        display: flex; flex-direction: column;
        max-height: 90vh;
        border: 1px solid #ddd6c1;
    }

    .modal-header {
        background: #fdf6e3; padding: 24px 32px; display: flex; align-items: center; gap: 20px;
        border-bottom: 1px solid #ddd6c1;
    }
    .header-icon { width: 56px; height: 56px; background: #eee8d5; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #268bd2; }
    .header-text h3 { margin: 0; font-size: 18px; font-weight: 800; color: #073642; }
    .header-text p { margin: 2px 0 0 0; font-size: 13px; color: #586e75; font-weight: 600; }
    .close-btn { margin-left: auto; background: #eee8d5; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #073642; font-weight: bold; }

    .modal-body { padding: 32px; flex: 1; overflow-y: auto; background: #fdf6e3; }
    .tiles-grid { display: flex; flex-direction: column; gap: 16px; }

    .photo-section {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12px;
        margin-bottom: 16px;
    }
    .photo-box {
        width: 96px; height: 96px;
        border-radius: 20px;
        background: #eee8d5;
        border: 2px dashed #ddd6c1;
        display: flex; flex-direction: column;
        align-items: center; justify-content: center;
        cursor: pointer;
        overflow: hidden;
        transition: border-color 0.2s;
    }
    .photo-box:hover { border-color: #268bd2; }
    .photo-preview { width: 100%; height: 100%; object-fit: cover; border-radius: 18px; }
    .photo-placeholder { font-size: 28px; }
    .photo-hint { font-size: 9px; font-weight: 700; color: #93a1a1; margin-top: 4px; text-transform: uppercase; letter-spacing: 0.5px; }
    .photo-actions { display: flex; gap: 8px; }
    .btn-photo-upload {
        background: #268bd2; color: white; border: none;
        padding: 6px 16px; border-radius: 12px;
        font-size: 11px; font-weight: 700; cursor: pointer;
    }
    .btn-photo-upload:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-photo-delete {
        background: #fee2e2; color: #dc2626; border: none;
        padding: 6px 16px; border-radius: 12px;
        font-size: 11px; font-weight: 700; cursor: pointer;
    }

    .input-tile {
        background: #eee8d5; padding: 16px 20px; border-radius: 20px;
        border: 1px solid #ddd6c1;
    }
    .input-tile.accent { border-left: 4px solid #268bd2; background: #eee8d5; }

    label { display: block; font-size: 10px; font-weight: 800; color: #586e75; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }

    input, textarea, select {
        width: 100%; border: none; background: none; font-size: 16px; font-weight: 700; color: #073642; outline: none; padding: 0;
    }
    input::placeholder, textarea::placeholder { color: #93a1a1; }
    select { cursor: pointer; color: #268bd2; }
    textarea { resize: none; line-height: 1.5; font-weight: 500; color: #586e75; }

    .modal-footer {
        padding: 24px 32px; background: #fdf6e3; border-top: 1px solid #ddd6c1;
        display: grid; grid-template-columns: 1fr 2fr; gap: 16px;
    }
    .btn-cancel { background: #eee8d5; color: #586e75; border: 1.5px solid #ddd6c1; padding: 16px; border-radius: 16px; font-weight: 700; cursor: pointer; }
    .btn-save { background: #268bd2; color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 800; cursor: pointer; }

    .loader-wrap { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    @media (max-width: 640px) {
        .modal-content {
            width: 95%;
            max-width: 400px;
            border-radius: 32px;
        }
    }
</style>
