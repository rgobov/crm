<script>
    import { onMount } from 'svelte';
    import { managerService } from '$lib/services/managerService.js';
    import { goto } from '$app/navigation';

    let settings = {
        apiKey: '',
        profileId: '',
        reminderTemplate: '',
        isEnabled: false,
        messengerType: 'TELEGRAM',
        leadTimeMinutes: 1440
    };

    let isLoading = true;
    let isSaving = false;
    let testPhone = '+7';
    let showTestModal = false;

    // Конвертация минут в текст (как во Flutter)
    $: formattedTime = () => {
        const h = Math.floor(settings.leadTimeMinutes / 60);
        const m = settings.leadTimeMinutes % 60;
        let res = '';
        if (h > 0) res += `${h} ч. `;
        if (m > 0) res += `${m} мин.`;
        return res || '0 мин.';
    };

    onMount(async () => {
        // Проверка: этот экран нельзя открывать в Telegram
        if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.initData) {
            goto('/admin');
            return;
        }

        try {
            const data = await managerService.getWappiSettings();
            if (data) settings = { ...settings, ...data };
        } catch (e) {
            console.error('Failed to load Wappi settings');
        } finally {
            isLoading = false;
        }
    });

    async function handleSave() {
        isSaving = true;
        try {
            await managerService.updateWappiSettings(settings);
            alert('Настройки успешно сохранены');
        } catch (e) {
            alert('Ошибка при сохранении: ' + (e.response?.data?.message || 'Неизвестная ошибка'));
        } finally {
            isSaving = false;
        }
    }

    async function sendTest() {
        if (testPhone.length < 10) return alert('Введите корректный номер');
        try {
            await managerService.sendTestMessage(testPhone.replace(/\D/g, ''));
            alert('Тестовое сообщение отправлено! Проверьте мессенджер.');
            showTestModal = false;
        } catch (e) {
            alert('Ошибка теста: ' + (e.response?.data?.message || 'Проверьте API Key'));
        }
    }
</script>

<div class="page">
    <div class="header">
        <button class="back-btn" on:click={() => goto('/admin')}>← Назад</button>
        <h1>Настройки Wappi.pro</h1>
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="form-container">
            <div class="card toggle-card">
                <div class="info">
                    <h3>Автоматические напоминания</h3>
                    <p>{settings.isEnabled ? 'Включены' : 'Отключены'}</p>
                </div>
                <label class="switch">
                    <input type="checkbox" bind:checked={settings.isEnabled}>
                    <span class="slider"></span>
                </label>
            </div>

            <div class="card time-card">
                <label>Время напоминания до визита</label>
                <div class="time-inputs">
                    <div class="input-group">
                        <input type="number" min="0" max="48"
                               value={Math.floor(settings.leadTimeMinutes / 60)}
                               on:input={(e) => settings.leadTimeMinutes = (parseInt(e.target.value) * 60) + (settings.leadTimeMinutes % 60)}>
                        <span>часов</span>
                    </div>
                    <div class="input-group">
                        <input type="number" min="0" max="59"
                               value={settings.leadTimeMinutes % 60}
                               on:input={(e) => settings.leadTimeMinutes = (Math.floor(settings.leadTimeMinutes / 60) * 60) + parseInt(e.target.value)}>
                        <span>минут</span>
                    </div>
                </div>
                <p class="hint">Итого: {formattedTime()}</p>
            </div>

            <div class="card settings-card">
                <h3>Технические данные</h3>
                <div class="field">
                    <label>Wappi API Key</label>
                    <input type="password" bind:value={settings.apiKey} placeholder="Ваш секретный ключ" />
                </div>
                <div class="field">
                    <label>Profile ID</label>
                    <input type="text" bind:value={settings.profileId} placeholder="Напр: 12345" />
                </div>
                <div class="field">
                    <label>Мессенджер</label>
                    <select bind:value={settings.messengerType}>
                        <option value="TELEGRAM">Telegram</option>
                        <option value="WHATSAPP">WhatsApp</option>
                    </select>
                </div>
            </div>

            <div class="card template-card">
                <h3>Шаблон сообщения</h3>
                <textarea bind:value={settings.reminderTemplate} rows="5" placeholder="Здравствуйте, напоминаем вам о записи..."></textarea>
                <p class="hint">Используйте переменные: %CLIENT%, %DATE%, %TIME%, %SERVICE%</p>
            </div>

            <div class="actions">
                <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                    {isSaving ? 'Сохранение...' : 'СОХРАНИТЬ НАСТРОЙКИ'}
                </button>
                <button class="test-btn" on:click={() => showTestModal = true}>ОТПРАВИТЬ ТЕСТ</button>
            </div>
        </div>
    {/if}
</div>

{#if showTestModal}
    <div class="modal-overlay">
        <div class="modal card">
            <h3>Тестовая отправка</h3>
            <p>Введите номер телефона:</p>
            <input type="tel" bind:value={testPhone} placeholder="+7 (___) ___" />
            <div class="modal-actions">
                <button on:click={() => showTestModal = false}>ОТМЕНА</button>
                <button class="confirm" on:click={sendTest}>ОТПРАВИТЬ</button>
            </div>
        </div>
    </div>
{/if}

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; background: #fdf6e3; min-height: 100vh; }
    .header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .back-btn { background: none; border: none; color: #268bd2; font-weight: 700; cursor: pointer; }
    h1 { font-size: 22px; font-weight: 800; margin: 0; color: #073642; }

    .form-container { display: grid; gap: 16px; padding-bottom: 100px; }

    .card { background: #eee8d5; border: 1px solid #ddd6c1; border-radius: 20px; padding: 20px; }

    .toggle-card { display: flex; justify-content: space-between; align-items: center; }
    .toggle-card h3 { margin: 0; font-size: 16px; color: #073642; }
    .toggle-card p { margin: 4px 0 0 0; font-size: 13px; color: #586e75; }

    .time-card label { display: block; font-size: 12px; font-weight: 700; text-transform: uppercase; margin-bottom: 12px; color: #268bd2; }
    .time-inputs { display: flex; gap: 16px; }
    .input-group { display: flex; align-items: center; gap: 8px; flex: 1; }
    .input-group input { width: 100%; padding: 12px; border-radius: 12px; border: 1px solid #ddd6c1; font-weight: 700; text-align: center; background: #fdf6e3; color: #073642; }

    .settings-card h3, .template-card h3 { font-size: 14px; font-weight: 700; color: #586e75; text-transform: uppercase; margin-bottom: 16px; }
    .field { margin-bottom: 16px; }
    .field label { display: block; font-size: 12px; margin-bottom: 6px; color: #586e75; }
    input, select, textarea { width: 100%; padding: 14px; border-radius: 14px; border: 1.5px solid #ddd6c1; background: #fdf6e3; font-size: 15px; box-sizing: border-box; outline: none; color: #073642; }
    input::placeholder, textarea::placeholder { color: #93a1a1; }
    input:focus, textarea:focus { border-color: #268bd2; }

    .hint { font-size: 12px; color: #586e75; margin-top: 8px; }

    .actions { display: grid; gap: 12px; margin-top: 24px; }
    .save-btn { background: #268bd2; color: white; border: none; padding: 18px; border-radius: 16px; font-weight: 800; cursor: pointer; }
    .test-btn { background: #eee8d5; color: #268bd2; border: 2px solid #268bd2; padding: 16px; border-radius: 16px; font-weight: 800; cursor: pointer; }

    /* Modal */
    .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(7, 54, 66, 0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal { width: 100%; max-width: 400px; padding: 24px; text-align: center; background: #fdf6e3; border: 1px solid #ddd6c1; border-radius: 24px; color: #073642; }
    .modal-actions { display: flex; gap: 12px; margin-top: 24px; }
    .modal-actions button { flex: 1; padding: 12px; border-radius: 12px; border: none; font-weight: 700; cursor: pointer; background: #eee8d5; color: #073642; }
    .confirm { background: #268bd2 !important; color: white !important; }

    .spinner { width: 30px; height: 30px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { text-align: center; padding: 40px; }

    /* Switch styling for Solarized */
    .switch { position: relative; display: inline-block; width: 44px; height: 24px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #ddd6c1; transition: .4s; border-radius: 24px; }
    .slider:before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background-color: white; transition: .4s; border-radius: 50%; }
    input:checked + .slider { background-color: #268bd2; }
    input:checked + .slider:before { transform: translateX(20px); }
</style>
