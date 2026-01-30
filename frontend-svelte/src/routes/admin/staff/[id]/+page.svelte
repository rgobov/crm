<script>
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { staffService } from '$lib/services/staffService.js';
    import { user } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';

    const id = $page.params.id;
    const isEditing = id !== 'new';

    let formData = {
        name: '',
        specialty: '',
        phone: '',
        role: 'EMPLOYEE',
        available: true, // Это наше поле active в Java
        email: '',
        password: ''
    };

    let hasAccount = false;
    let isLoading = true;
    let isSaving = false;
    let tg = null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin/staff'));
        }

        if (isEditing) {
            try {
                const member = await staffService.getStaffMember(id);
                formData = { ...formData, ...member };
                if (member.email) hasAccount = true;
            } catch (e) {
                goto('/admin/staff');
            }
        }
        isLoading = false;
    });

    async function handleSave() {
        if (!formData.name) return alert('Введите имя');
        isSaving = true;
        try {
            if (isEditing) {
                await staffService.updateStaffMember(id, formData);
            } else {
                await staffService.addStaffMember(formData);
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
                await staffService.deleteStaffMember(id);
                goto('/admin/staff');
            } catch (e) {
                alert('Ошибка при удалении');
            }
        }
    }
</script>

<div class="page">
    <div class="header">
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
                <label for="name">Имя мастера</label>
                <input type="text" id="name" bind:value={formData.name} />
            </div>

            <div class="form-group">
                <label for="spec">Специальность</label>
                <input type="text" id="spec" bind:value={formData.specialty} />
            </div>

            <div class="form-group">
                <label for="role">Роль</label>
                <select bind:value={formData.role}>
                    <option value="EMPLOYEE">Сотрудник</option>
                    <option value="MANAGER">Менеджер</option>
                </select>
            </div>

            <div class="toggle-row">
                <span class="label">Активен (может работать)</span>
                <label class="switch">
                    <input type="checkbox" bind:checked={formData.available}>
                    <span class="slider"></span>
                </label>
            </div>

            <div class="actions-main">
                <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                    {isSaving ? 'Сохранение...' : 'Сохранить изменения'}
                </button>

                {#if isEditing}
                    <button class="delete-btn-text" on:click={handleSoftDelete}>
                        Удалить сотрудника
                    </button>
                {/if}
            </div>
        </div>
    {/if}
</div>

<style>
    .page { padding: 20px; max-width: 500px; margin: 0 auto; }
    .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
    h1 { font-size: 22px; font-weight: 800; margin: 0; }

    .status-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; text-transform: uppercase; }
    .status-badge.error { background: #fee2e2; color: #ef4444; }

    .form-card { padding: 24px; background: white; border-radius: 24px; box-shadow: var(--shadow); }
    .form-group { margin-bottom: 20px; }
    label { display: block; font-size: 12px; font-weight: 700; color: var(--hint-color); margin-bottom: 8px; text-transform: uppercase; }
    input, select { width: 100%; padding: 14px; border: 2px solid #f1f5f9; border-radius: 14px; font-size: 16px; background: #f8fafc; box-sizing: border-box; }

    .toggle-row { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding: 10px 0; border-top: 1px solid #f1f5f9; }
    .label { font-size: 15px; font-weight: 600; }

    .switch { position: relative; width: 50px; height: 28px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #e2e8f0; transition: .4s; border-radius: 34px; }
    .slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; }
    input:checked + .slider { background-color: var(--primary-color); }
    input:checked + .slider:before { transform: translateX(22px); }

    .actions-main { margin-top: 32px; display: flex; flex-direction: column; gap: 16px; }
    .save-btn { width: 100%; padding: 16px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-size: 16px; font-weight: 700; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }
    .delete-btn-text { background: none; border: none; color: #ef4444; font-weight: 600; font-size: 14px; cursor: pointer; }

    .center { text-align: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
