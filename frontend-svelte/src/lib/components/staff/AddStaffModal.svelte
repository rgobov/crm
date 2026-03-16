<script>
    import { createEventDispatcher, onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { fade, scale, slide } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    let formData = {
        name: '',
        specialty: '',
        phone: '',
        role: 'EMPLOYEE',
        available: true,
        email: '', // Добавлено
        password: '', // Добавлено
        branchIds: []
    };

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
            formData.password = 'qwerty'; // Пароль по умолчанию, если указан email
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

<div class="modal-backdrop" on:click|self={() => dispatch('close')} transition:fade={{duration: 200}}>
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
                        <label for="name">Имя мастера *</label>
                        <input type="text" id="name" bind:value={formData.name} placeholder="Напр. Иван Иванов" />
                    </div>

                    <div class="form-group">
                        <label for="spec">Специальность</label>
                        <input type="text" id="spec" bind:value={formData.specialty} placeholder="Напр. Мастер маникюра" />
                    </div>

                    <div class="form-group">
                        <label>Привязка к филиалам * <small>(обязательно)</small></label>
                        <div class="branch-selector-grid">
                            {#each allBranches as b}
                                <button
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
                        <label for="email">Email (для входа в систему)</label>
                        <input type="email" id="email" bind:value={formData.email} placeholder="master@example.com" />
                    </div>

                    <div class="form-group">
                        <label for="password">Пароль (мин. 6 символов)</label>
                        <input type="password" id="password" bind:value={formData.password} placeholder="Оставьте пустым для 'qwerty'" />
                    </div>

                    <div class="form-group">
                        <label for="role">Уровень доступа</label>
                        <select bind:value={formData.role}>
                            <option value="EMPLOYEE">Сотрудник (свой график)</option>
                            <option value="MANAGER">Менеджер (весь филиал)</option>
                        </select>
                    </div>

                    <div class="actions">
                        <button
                            class="save-btn"
                            on:click={handleSave}
                            disabled={isSaving || !formData.name || formData.branchIds.length === 0}
                        >
                            {isSaving ? 'Создание...' : 'Создать сотрудника'}
                        </button>
                    </div>
                </div>
            {/if}
        </div>
    </div>
</div>

<style>
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; z-index: 2100; padding: 20px; }
    .modal-content { background: white; width: 100%; max-width: 460px; border-radius: 32px; overflow: hidden; box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.4); max-height: 90vh; display: flex; flex-direction: column; }

    .modal-header { padding: 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
    .modal-header h2 { margin: 0; font-size: 18px; font-weight: 850; color: #0f172a; }
    .btn-close { background: #f1f5f9; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; color: #94a3b8; font-weight: bold; }

    .modal-body { padding: 24px; overflow-y: auto; }

    .form-group { margin-bottom: 20px; }
    label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; margin-bottom: 8px; letter-spacing: 0.5px; }
    input, select { width: 100%; padding: 14px; border: 2px solid #f1f5f9; border-radius: 14px; font-size: 15px; background: #f8fafc; font-weight: 600; color: #1e293b; outline: none; transition: 0.2s; }
    input:focus { border-color: var(--primary-color); background: white; }

    .divider { margin: 32px 0 20px; font-size: 10px; font-weight: 900; color: #cbd5e1; text-transform: uppercase; letter-spacing: 2px; display: flex; align-items: center; gap: 10px; }
    .divider::after { content: ""; flex: 1; height: 1px; background: #f1f5f9; }

    .branch-selector-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .branch-chip { padding: 8px 14px; border-radius: 12px; border: 2px solid #f1f5f9; background: #f8fafc; color: #64748b; font-weight: 700; font-size: 12px; cursor: pointer; transition: 0.2s; }
    .branch-chip.selected { border-color: var(--primary-color); background: #eff6ff; color: var(--primary-color); }

    .actions { margin-top: 32px; }
    .save-btn { width: 100%; padding: 16px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-size: 16px; font-weight: 800; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); cursor: pointer; }
    .save-btn:disabled { opacity: 0.6; cursor: not-allowed; filter: grayscale(1); }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 24px; height: 24px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
