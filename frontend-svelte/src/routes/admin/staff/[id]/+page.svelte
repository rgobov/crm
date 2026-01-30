<script>
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { staffService } from '$lib/services/staffService.js';
    import { user } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';

    const id = $page.params.id;
    const isEditing = id !== 'new';

    // СИНХРОНИЗАЦИЯ: Все поля из Flutter-модели
    let formData = {
        name: '',
        specialty: '',
        phone: '',
        role: 'EMPLOYEE',
        available: true,
        email: '',
        password: '',
        newPassword: '' // Поле для смены пароля
    };

    let hasAccount = false;
    let isLoading = true;
    let isSaving = false;
    let tg = null;

    // Права доступа как во Flutter
    $: isAdmin = $user?.role === 'ADMIN';
    $: isEmailEditable = !isEditing || isAdmin;

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
                console.error('Failed to load staff member');
                goto('/admin/staff');
            }
        }
        isLoading = false;
    });

    async function handleSave() {
        if (!formData.name || !formData.specialty) {
            alert('Введите имя и специальность');
            return;
        }

        // Валидация аккаунта (как во Flutter)
        if (hasAccount && !isEditing && (!formData.email || !formData.password)) {
            alert('Введите Email и пароль для новой учетной записи');
            return;
        }

        isSaving = true;
        try {
            const payload = { ...formData };

            // Если аккаунт не нужен - не шлем email/pass (для новых)
            if (!hasAccount && !isEditing) {
                delete payload.email;
                delete payload.password;
            }

            if (isEditing) {
                // Передаем newPassword для смены пароля бэкендом
                await staffService.updateStaffMember(id, payload);
            } else {
                await staffService.addStaffMember(payload);
            }

            if (tg) tg.HapticFeedback.notificationOccurred('success');
            goto('/admin/staff');
        } catch (e) {
            alert(e.response?.data?.message || 'Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="page">
    <div class="header">
        <h1>{isEditing ? 'Редактирование' : 'Новый сотрудник'}</h1>
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="card form-card">
            <!-- ОСНОВНЫЕ ДАННЫЕ -->
            <div class="form-group">
                <label for="name">Имя</label>
                <input type="text" id="name" bind:value={formData.name} placeholder="Имя Фамилия" />
            </div>

            <div class="form-group">
                <label for="specialty">Специальность</label>
                <input type="text" id="specialty" bind:value={formData.specialty} placeholder="Должность" />
            </div>

            <div class="form-group">
                <label for="phone">Телефон</label>
                <input type="tel" id="phone" bind:value={formData.phone} placeholder="+7 (___) ___-__-__" />
            </div>

            <div class="form-group">
                <label for="role">Роль в системе</label>
                <select id="role" bind:value={formData.role}>
                    <option value="EMPLOYEE">Сотрудник</option>
                    <option value="MANAGER">Менеджер</option>
                </select>
            </div>

            <div class="toggle-group">
                <span class="label">Доступен для записи</span>
                <label class="switch">
                    <input type="checkbox" bind:checked={formData.available}>
                    <span class="slider"></span>
                </label>
            </div>

            <!-- УЧЕТНАЯ ЗАПИСЬ (СИНХРОНИЗАЦИЯ С FLUTTER) -->
            {#if !isEditing}
                <div class="toggle-group border-top">
                    <span class="label">Создать учетную запись</span>
                    <label class="switch">
                        <input type="checkbox" bind:checked={hasAccount}>
                        <span class="slider"></span>
                    </label>
                </div>
            {/if}

            {#if hasAccount || (isEditing && formData.email)}
                <div class="account-section">
                    <div class="form-group">
                        <label for="email">Email (Логин)</label>
                        <input
                            type="email"
                            id="email"
                            bind:value={formData.email}
                            placeholder="login@mail.com"
                            disabled={!isEmailEditable}
                        />
                    </div>

                    {#if !isEditing}
                        <div class="form-group">
                            <label for="pass">Пароль</label>
                            <input type="password" id="pass" bind:value={formData.password} placeholder="Минимум 6 символов" />
                        </div>
                    {:else if isAdmin}
                        <div class="form-group">
                            <label for="new-pass">Новый пароль</label>
                            <input
                                type="password"
                                id="new-pass"
                                bind:value={formData.password}
                                placeholder="Оставьте пустым, если не меняете"
                            />
                        </div>
                    {/if}
                </div>
            {/if}

            <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                {isSaving ? 'Сохранение...' : 'Сохранить изменения'}
            </button>
        </div>
    {/if}
</div>

<style>
    /* Стили остаются прежними для единства дизайна */
    .page { padding: 20px; max-width: 500px; margin: 0 auto; }
    h1 { font-size: 22px; font-weight: 800; margin-bottom: 24px; color: #0f172a; }
    .form-card { padding: 24px; background: white; border-radius: 24px; box-shadow: var(--shadow); }
    .form-group { margin-bottom: 20px; text-align: left; }
    label { display: block; font-size: 12px; font-weight: 700; color: var(--primary-color); margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
    input, select { width: 100%; padding: 14px; border: 2px solid #f1f5f9; border-radius: 14px; font-size: 16px; background: #f8fafc; box-sizing: border-box; }
    input:focus { outline: none; border-color: var(--primary-color); background: white; }
    input:disabled { opacity: 0.6; cursor: not-allowed; background: #f1f5f9; }
    .toggle-group { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; }
    .toggle-group.border-top { border-top: 1px solid #f1f5f9; margin-top: 12px; padding-top: 20px; }
    .label { font-size: 15px; font-weight: 600; color: #1e293b; }
    .switch { position: relative; width: 50px; height: 28px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #e2e8f0; transition: .4s; border-radius: 34px; }
    .slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; }
    input:checked + .slider { background-color: var(--primary-color); }
    input:checked + .slider:before { transform: translateX(22px); }
    .account-section { margin-top: 12px; padding: 16px; background: #f8fafc; border-radius: 16px; border: 1px dashed #cbd5e1; }
    .save-btn { width: 100%; padding: 16px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-size: 16px; font-weight: 700; margin-top: 24px; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }
    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
