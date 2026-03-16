<script>
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { staffService } from '$lib/services/staffService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { goto } from '$app/navigation';
    import { fade, slide } from 'svelte/transition';

    const id = $page.params.id;
    const isEditing = id !== 'new';

    let formData = {
        name: '',
        specialty: '',
        phone: '',
        role: 'EMPLOYEE',
        available: true,
        email: '',
        password: '',
        branchIds: [] // Инициализируем массив ID филиалов
    };

    let allBranches = [];
    let isLoading = true;
    let isSaving = false;
    let tg = null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin/staff'));
        }

        try {
            // Загружаем филиалы всегда
            allBranches = await branchService.getBranches();

            if (isEditing) {
                const member = await staffService.getStaffById(id);
                // Мапим данные, учитывая branchIds из DTO
                formData = {
                    ...formData,
                    ...member,
                    branchIds: member.branchIds || []
                };
            }
        } catch (e) {
            console.error('Data Load Error:', e);
            if (isEditing) goto('/admin/staff');
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
        if (formData.branchIds.length === 0) return alert('Выберите хотя бы один филиал (обязательно)');

        isSaving = true;
        try {
            if (isEditing) {
                await staffService.updateStaff(id, formData);
            } else {
                await staffService.addStaff(formData);
            }
            goto('/admin/staff');
        } catch (e) {
            alert(e.response?.data?.message || 'Ошибка сохранения');
        } finally {
            isSaving = false;
        }
    }

    async function handleSoftDelete() {
        if (confirm('Вы уверены? Сотрудник потеряет доступ к системе, но история его записей сохранится.')) {
            try {
                await staffService.deleteStaff(id);
                goto('/admin/staff');
            } catch (e) {
                alert('Ошибка при удалении');
            }
        }
    }
</script>

<div class="page">
    <div class="header">
        <button class="back-btn-minimal" on:click={() => goto('/admin/staff')}>←</button>
        <h1>{isEditing ? 'Профиль мастера' : 'Новый сотрудник'}</h1>
        {#if isEditing && !formData.available}
            <span class="status-badge error">Заблокирован</span>
        {/if}
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="card form-card">
            <div class="form-group">
                <label for="name">Имя мастера *</label>
                <input type="text" id="name" bind:value={formData.name} placeholder="Напр. Иван Иванов" />
            </div>

            <div class="form-group">
                <label for="spec">Специальность</label>
                <input type="text" id="spec" bind:value={formData.specialty} placeholder="Напр. Мастер маникюра" />
            </div>

            <!-- НОВОЕ: ВЫБОР ФИЛИАЛОВ (ОБЯЗАТЕЛЬНО) -->
            <div class="form-group branches-section">
                <label>Привязка к филиалам * <small>(минимум один)</small></label>
                <div class="branch-selector-grid">
                    {#each allBranches as b}
                        <button
                            class="branch-chip"
                            class:selected={formData.branchIds.includes(b.id)}
                            on:click={() => toggleBranch(b.id)}
                        >
                            <span class="icon">{formData.branchIds.includes(b.id) ? '✅' : '🏠'}</span>
                            {b.name}
                        </button>
                    {/each}
                </div>
                {#if formData.branchIds.length === 0}
                    <p class="validation-hint" in:slide>Пожалуйста, выберите филиал, где будет работать мастер</p>
                {/if}
            </div>

            <div class="form-group">
                <label for="role">Роль доступа</label>
                <select bind:value={formData.role}>
                    <option value="EMPLOYEE">Сотрудник (только свой график)</option>
                    <option value="MANAGER">Менеджер (видит всё)</option>
                </select>
            </div>

            <div class="toggle-row">
                <span class="label">Статус: {formData.available ? 'Активен' : 'Заблокирован'}</span>
                <label class="switch">
                    <input type="checkbox" bind:checked={formData.available}>
                    <span class="slider"></span>
                </label>
            </div>

            <div class="actions-main">
                <button
                    class="save-btn"
                    on:click={handleSave}
                    disabled={isSaving || !formData.name || formData.branchIds.length === 0}
                >
                    {isSaving ? 'Сохранение...' : isEditing ? 'Сохранить изменения' : 'Создать сотрудника'}
                </button>

                {#if isEditing}
                    <button class="delete-btn-text" on:click={handleSoftDelete}>
                        Удалить сотрудника из системы
                    </button>
                {/if}
            </div>
        </div>
    {/if}
</div>

<style>
    .page { padding: 20px; max-width: 500px; margin: 0 auto; padding-bottom: 100px; }
    .header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    h1 { font-size: 22px; font-weight: 850; margin: 0; color: #0f172a; }

    .back-btn-minimal { background: none; border: none; font-size: 24px; color: #64748b; cursor: pointer; padding: 0; }

    .status-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; text-transform: uppercase; }
    .status-badge.error { background: #fee2e2; color: #ef4444; }

    .form-card { padding: 24px; background: white; border-radius: 28px; box-shadow: 0 10px 30px rgba(0,0,0,0.05); border: 1px solid #f1f5f9; }
    .form-group { margin-bottom: 24px; }
    label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px; }

    input, select {
        width: 100%; padding: 14px 18px; border: 2px solid #f1f5f9; border-radius: 16px;
        font-size: 15px; background: #f8fafc; box-sizing: border-box; transition: all 0.2s;
        font-weight: 600; color: #1e293b;
    }
    input:focus { border-color: var(--primary-color); outline: none; background: white; }

    /* ФИЛИАЛЫ */
    .branch-selector-grid { display: flex; flex-wrap: wrap; gap: 10px; }
    .branch-chip {
        display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-radius: 14px;
        background: #f8fafc; border: 2px solid #f1f5f9; color: #64748b; font-weight: 700;
        font-size: 13px; cursor: pointer; transition: all 0.2s;
    }
    .branch-chip.selected {
        border-color: var(--primary-color); background: #eff6ff; color: var(--primary-color);
        box-shadow: 0 4px 12px rgba(56, 151, 240, 0.15);
    }
    .branch-chip .icon { font-size: 14px; }

    .validation-hint { margin-top: 8px; font-size: 12px; color: #ef4444; font-weight: 600; }

    .toggle-row { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding: 16px 0; border-top: 1px solid #f1f5f9; }
    .label { font-size: 15px; font-weight: 700; color: #334155; }

    .switch { position: relative; width: 50px; height: 28px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #e2e8f0; transition: .4s; border-radius: 34px; }
    .slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; }
    input:checked + .slider { background-color: #10b981; }
    input:checked + .slider:before { transform: translateX(22px); }

    .actions-main { margin-top: 32px; display: flex; flex-direction: column; gap: 16px; }
    .save-btn {
        width: 100%; padding: 18px; background: var(--primary-gradient); color: white;
        border: none; border-radius: 18px; font-size: 16px; font-weight: 800;
        box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3); cursor: pointer;
        transition: all 0.2s;
    }
    .save-btn:disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; opacity: 0.7; }
    .save-btn:active { transform: scale(0.98); }

    .delete-btn-text { background: none; border: none; color: #ef4444; font-weight: 700; font-size: 13px; cursor: pointer; padding: 10px; }

    .center { text-align: center; padding: 60px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
