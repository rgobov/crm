<script>
    import { createEventDispatcher } from 'svelte';
    import { branchService } from '$lib/services/branchService.js';
    import { fade, scale } from 'svelte/transition';

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
            dispatch('success');
        } catch (e) {
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="modal-backdrop" on:click|self={() => dispatch('close')} in:fade={{duration: 200}}>
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
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(8px); z-index: 2000; display: flex; align-items: center; justify-content: center; padding: 20px; }
    .modal-card { background: white; width: 100%; max-width: 450px; border-radius: 32px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.3); }
    .modal-header { padding: 24px 32px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; }
    .modal-header h2 { margin: 0; font-size: 18px; font-weight: 800; color: #1e293b; }
    .btn-close { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #64748b; }

    .modal-body { padding: 32px; display: flex; flex-direction: column; gap: 24px; }
    .form-group { display: flex; flex-direction: column; gap: 8px; }
    label { font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
    input, select { background: #f8fafc; border: 1.5px solid #f1f5f9; padding: 12px 16px; border-radius: 12px; font-size: 15px; font-weight: 600; outline: none; transition: 0.2s; color: #1e293b; }
    input:focus, select:focus { border-color: #0ea5e9; background: white; }
    .hint { font-size: 12px; color: #94a3b8; margin: 0; font-weight: 500; }

    .modal-footer { padding: 20px 32px; background: #f8fafc; display: flex; justify-content: flex-end; gap: 12px; }
    .btn-secondary { background: white; border: 1.5px solid #e2e8f0; padding: 10px 20px; border-radius: 12px; font-weight: 700; cursor: pointer; color: #64748b; }
    .btn-save { background: #0ea5e9; color: white; border: none; padding: 10px 24px; border-radius: 12px; font-weight: 700; cursor: pointer; }
    .btn-save:disabled { opacity: 0.5; }
</style>
