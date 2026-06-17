<script>
    import { createEventDispatcher } from 'svelte';
    import { branchService } from '$lib/services/branchService.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { fade, scale } from 'svelte/transition';
    import { portal } from '$lib/actions/portal.js';

    export let branch = null;
    const dispatch = createEventDispatcher();

    let name = branch?.name || '';
    let address = branch?.address || '';
    let timezone = branch?.timezone || 'Europe/Moscow';
    let isSaving = false;

    const timezones = [
        { val: 'Europe/Moscow', label: 'Москва (GMT+3)' },
        { val: 'Europe/Kaliningrad', label: 'Калининград (GMT+2)' },
        { val: 'Europe/Samara', label: 'Самара (GMT+4)' },
        { val: 'Asia/Yekaterinburg', label: 'Екатеринбург (GMT+5)' },
        { val: 'Asia/Omsk', label: 'Омск (GMT+6)' },
        { val: 'Asia/Novosibirsk', label: 'Новосибирск (GMT+7)' },
        { val: 'Asia/Krasnoyarsk', label: 'Красноярск (GMT+7)' },
        { val: 'Asia/Irkutsk', label: 'Иркутск (GMT+8)' },
        { val: 'Asia/Yakutsk', label: 'Якутск (GMT+9)' },
        { val: 'Asia/Vladivostok', label: 'Владивосток (GMT+10)' },
        { val: 'Asia/Magadan', label: 'Магадан (GMT+11)' },
        { val: 'Asia/Kamchatka', label: 'Камчатка (GMT+12)' }
    ];

    async function handleSave() {
        if (!name) return alert('Укажите название филиала');
        isSaving = true;
        try {
            const payload = { name, address, timezone };
            if (branch) {
                await branchService.updateBranch(branch.id, payload);
            } else {
                await branchService.createBranch(payload);
            }
            // Обновляем глобальный стор, чтобы изменения увидели все компоненты
            await branchStore.refresh();
            dispatch('success');
        } catch (e) {
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="modal-backdrop" use:portal on:click|self={() => dispatch('close')} in:fade={{duration: 200}}>
    <div class="modal-card" in:scale={{duration: 300, start: 0.95}}>
        <header class="modal-header">
            <h2>{branch ? 'Редактировать филиал' : 'Новый филиал'}</h2>
            <button class="btn-close" on:click={() => dispatch('close')}>✕</button>
        </header>

        <div class="modal-body">
            <div class="form-group">
                <label>Название филиала</label>
                <input type="text" bind:value={name} placeholder="Например: Центр, ТЦ Авиапарк..." />
            </div>

            <div class="form-group">
                <label>Адрес</label>
                <input type="text" bind:value={address} placeholder="Улица, дом, офис..." />
            </div>

            <div class="form-group">
                <label>Часовой пояс</label>
                <select bind:value={timezone}>
                    {#each timezones as tz}
                        <option value={tz.val}>{tz.label}</option>
                    {/each}
                </select>
                <p class="hint">Влияет на время в уведомлениях для этого филиала</p>
            </div>
        </div>

        <footer class="modal-footer">
            <button class="btn-secondary" on:click={() => dispatch('close')}>Отмена</button>
            <button class="btn-save" on:click={handleSave} disabled={isSaving}>
                {isSaving ? 'Сохранение...' : (branch ? 'Обновить' : 'Создать')}
            </button>
        </footer>
    </div>
</div>

<style>
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0, 43, 54, 0.4); backdrop-filter: blur(8px); z-index: 99999; display: flex; align-items: center; justify-content: center; padding: 20px; box-sizing: border-box; }
    .modal-card { background: #fdf6e3; width: 100%; max-width: 480px; border-radius: 32px; overflow: hidden; box-shadow: 0 32px 64px -16px rgba(0, 0, 0, 0.3); border: 1.5px solid #ddd6c1; }

    .modal-header { padding: 20px 24px; border-bottom: 1.5px solid #ddd6c1; display: flex; justify-content: space-between; align-items: center; background: #eee8d5; }
    .modal-header h2 { margin: 0; font-size: 16px; font-weight: 850; color: #073642; text-transform: uppercase; letter-spacing: 0.5px; }
    .btn-close { background: #fdf6e3; border: 1.5px solid #ddd6c1; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; color: #586e75; font-weight: 800; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
    .btn-close:hover { background: white; border-color: #268bd2; color: #268bd2; }

    .modal-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; background: #fdf6e3; }
    .form-group { display: flex; flex-direction: column; gap: 8px; }
    label { font-size: 10px; font-weight: 850; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 2px; }

    input, select {
        background: white; border: 2px solid #ddd6c1; padding: 14px 18px; border-radius: 16px;
        font-size: 16px; font-weight: 600; outline: none; transition: all 0.2s; color: #073642;
    }
    input:focus, select:focus { border-color: #268bd2; box-shadow: 0 0 0 4px rgba(38, 139, 210, 0.1); }
    input::placeholder { color: #93a1a1; font-weight: 500; }

    .hint { font-size: 12px; color: #586e75; margin: 0; font-weight: 600; line-height: 1.4; }

    .modal-footer { padding: 20px 24px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; display: flex; justify-content: flex-end; gap: 12px; }
    .btn-secondary { background: #fdf6e3; border: 1.5px solid #ddd6c1; padding: 14px 24px; border-radius: 14px; font-weight: 850; cursor: pointer; color: #586e75; font-size: 13px; text-transform: uppercase; }
    .btn-save {
        background: #268bd2; color: white; border: none; padding: 14px 28px; border-radius: 14px;
        font-weight: 900; cursor: pointer; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; transition: all 0.2s;
    }
    .btn-save:active { transform: scale(0.98); }
    .btn-save:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
