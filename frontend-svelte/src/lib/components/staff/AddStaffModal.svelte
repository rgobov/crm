<script>
    import { createEventDispatcher, onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { fade, scale } from 'svelte/transition';
    import { portal } from '$lib/actions/portal.js';

    const dispatch = createEventDispatcher();

    let formData = {
        name: '',
        specialty: '',
        phone: '',
        role: 'EMPLOYEE',
        available: true,
        email: '',
        password: '',
        branchIds: [],
        photoData: ''
    };

    let photoFileInput = null;
    let photoPreview = null;
    let isUploadingPhoto = false;

    function handlePhotoSelect(event) {
        const file = event.target.files[0];
        if (!file) return;
        if (!file.type.startsWith('image/')) {
            alert('Пожалуйста, выберите изображение');
            return;
        }
        isUploadingPhoto = true;
        const reader = new FileReader();
        reader.onload = (e) => {
            photoPreview = e.target.result;
            // Извлекаем base64 из data URL
            formData.photoData = photoPreview.split(',')[1];
            isUploadingPhoto = false;
        };
        reader.readAsDataURL(file);
    }

    function clearPhoto() {
        photoPreview = null;
        formData.photoData = '';
        if (photoFileInput) photoFileInput.value = '';
    }

    let allBranches = [];
    let isLoading = true;
    let isSaving = false;

    onMount(async () => {
        try {
            allBranches = await branchService.getBranches();
        } catch (e) {
            console.error('Failed to load branches:', e);
        } finally {
            isLoading = false;
        }
    });

    function toggleBranch(branchId) {
        if (formData.branchIds.includes(branchId)) {
            formData.branchIds = formData.branchIds.filter(bid => bid !== branchId);
        } else {
            formData.branchIds = [...formData.branchIds, branchId];
        }
    }

    async function handleSave() {
        if (!formData.name) return alert('Введите имя мастера');
        if (formData.branchIds.length === 0) return alert('Выберите хотя бы один филиал');
        if (formData.email && !formData.password) {
            formData.password = 'qwerty';
        }

        isSaving = true;
        try {
            const result = await staffService.addStaff(formData);
            dispatch('added', result);
            dispatch('close');
        } catch (e) {
            alert(e.response?.data?.message || 'Ошибка при создании сотрудника');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="modal-backdrop" use:portal on:click|self={() => dispatch('close')} transition:fade={{duration: 200}}>
    <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
        <header class="modal-header">
            <h2>Новый сотрудник</h2>
            <button class="btn-close" on:click={() => dispatch('close')}>✕</button>
        </header>

        <div class="modal-body">
            {#if isLoading}
                <div class="center"><span class="spinner"></span></div>
            {:else}
                <div class="form-container">
                    <div class="form-group">
                        <label>Фото профиля</label>
                        <div class="photo-upload-area">
                            {#if photoPreview}
                                <img src={photoPreview} alt="Превью" class="photo-preview" />
                                <button type="button" class="btn-clear-photo" on:click={clearPhoto}>✕</button>
                            {:else}
                                <label for="new-photo-upload" class="photo-placeholder">
                                    <span>📷</span>
                                    <span class="placeholder-text">Нажмите для загрузки фото</span>
                                </label>
                            {/if}
                            <input
                                type="file"
                                accept="image/*"
                                bind:this={photoFileInput}
                                on:change={handlePhotoSelect}
                                style="display: none"
                                id="new-photo-upload"
                            />
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="name">Имя мастера *</label>
                        <input type="text" id="name" bind:value={formData.name} placeholder="Напр. Иван Иванов" />
                    </div>

                    <div class="form-group">
                        <label for="spec">Специальность</label>
                        <input type="text" id="spec" bind:value={formData.specialty} placeholder="Напр. Мастер маникюра" />
                    </div>

                    <div class="form-group">
                        <label>Привязка к филиалам *</label>
                        <div class="branch-selector-grid">
                            {#each allBranches as b}
                                <button
                                    type="button"
                                    class="branch-chip"
                                    class:selected={formData.branchIds.includes(b.id)}
                                    on:click={() => toggleBranch(b.id)}
                                >
                                    {b.name}
                                </button>
                            {/each}
                        </div>
                    </div>

                    <div class="divider">Данные для входа</div>

                    <div class="form-group">
                        <label for="email">Email (для входа)</label>
                        <input type="email" id="email" bind:value={formData.email} placeholder="master@example.com" />
                    </div>

                    <div class="form-group">
                        <label for="password">Пароль</label>
                        <input type="password" id="password" bind:value={formData.password} placeholder="Оставьте пустым для 'qwerty'" />
                    </div>

                    <div class="form-group">
                        <label for="role">Уровень доступа</label>
                        <select bind:value={formData.role}>
                            <option value="EMPLOYEE">Сотрудник</option>
                            <option value="MANAGER">Менеджер</option>
                        </select>
                    </div>
                </div>
            {/if}
        </div>

        <footer class="modal-footer">
            <button class="btn-secondary" on:click={() => dispatch('close')}>Отмена</button>
            <button
                class="btn-primary"
                on:click={handleSave}
                disabled={isSaving || !formData.name || formData.branchIds.length === 0}
            >
                {isSaving ? 'Создание...' : 'Создать'}
            </button>
        </footer>
    </div>
</div>

<style>
    .modal-backdrop { position: fixed; inset: 0; background: rgba(7, 54, 66, 0.7); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 99999; padding: 20px; box-sizing: border-box; }
    .modal-content { background: #fdf6e3; width: 100%; max-width: 480px; border-radius: 28px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); border: 1.5px solid #ddd6c1; display: flex; flex-direction: column; max-height: 90dvh; }

    .modal-header { padding: 20px 24px; display: flex; justify-content: space-between; align-items: center; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .modal-header h2 { margin: 0; font-size: 16px; font-weight: 850; color: #073642; text-transform: uppercase; letter-spacing: 0.5px; }
    .btn-close { background: #fdf6e3; border: 1.5px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; font-weight: bold; transition: 0.2s; }

    .modal-body { padding: 24px; overflow-y: auto; flex: 1; box-sizing: border-box; }
    .form-container { display: flex; flex-direction: column; gap: 18px; }

    .form-group { display: flex; flex-direction: column; }
    label { display: block; font-size: 10px; font-weight: 850; color: #93a1a1; text-transform: uppercase; margin-bottom: 8px; letter-spacing: 0.5px; }

    input, select {
        width: 100%;
        padding: 14px 16px;
        border: 2px solid #ddd6c1;
        border-radius: 16px;
        font-size: 15px;
        background: white;
        font-weight: 600;
        color: #073642;
        outline: none;
        transition: 0.2s;
        box-sizing: border-box; /* ФИКС ОТСТУПА СПРАВА */
    }
    input:focus, select:focus { border-color: #268bd2; }

    .divider { margin: 10px 0; font-size: 10px; font-weight: 900; color: #93a1a1; text-transform: uppercase; letter-spacing: 1.5px; display: flex; align-items: center; gap: 10px; }
    .divider::after { content: ""; flex: 1; height: 1px; background: #ddd6c1; }

    .branch-selector-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .branch-chip { padding: 8px 14px; border-radius: 12px; border: 1.5px solid #ddd6c1; background: white; color: #586e75; font-weight: 700; font-size: 12px; cursor: pointer; transition: 0.2s; }
    .branch-chip.selected { border-color: #268bd2; background: #268bd2; color: white; }

    .photo-upload-area { position: relative; width: 100px; height: 100px; margin: 0 auto; }
    .photo-preview { width: 100%; height: 100%; object-fit: cover; border-radius: 16px; border: 2px solid #ddd6c1; }
    .photo-placeholder { width: 100%; height: 100%; border: 2px dashed #ddd6c1; border-radius: 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; background: white; transition: 0.2s; }
    .photo-placeholder:hover { border-color: #268bd2; background: #f0f8ff; }
    .photo-placeholder span:first-child { font-size: 28px; }
    .placeholder-text { font-size: 9px; color: #93a1a1; text-align: center; margin-top: 4px; }
    .btn-clear-photo { position: absolute; top: -6px; right: -6px; background: #dc2626; color: white; border: none; width: 24px; height: 24px; border-radius: 50%; font-size: 12px; cursor: pointer; }

    .modal-footer { padding: 18px 24px; display: flex; justify-content: flex-end; gap: 12px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; }
    .btn-primary { background: #268bd2; color: white; border: none; padding: 14px 24px; border-radius: 16px; font-weight: 900; cursor: pointer; font-size: 14px; text-transform: uppercase; transition: 0.2s; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-secondary { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 14px 24px; border-radius: 16px; font-weight: 850; cursor: pointer; font-size: 13px; text-transform: uppercase; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 28px; height: 28px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
