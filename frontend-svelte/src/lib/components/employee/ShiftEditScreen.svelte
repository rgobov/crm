<script>
    import { createEventDispatcher } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js'; // <<< ИМПОРТ ФИЛИАЛА
    import { fade, slide, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';

    export let staff;
    export let date;

    const dispatch = createEventDispatcher();

    let isSaving = false;

    // Данные формы (смена)
    let shiftData = {
        staffId: staff.id,
        branchId: $activeBranchId, // <<< ПРИВЯЗКА К ТЕКУЩЕМУ ФИЛИАЛУ
        date: date.toISOString().split('T')[0],
        workStartTime: staff.workStartTime || '09:00',
        workEndTime: staff.workEndTime || '18:00',
        breakStartTime: staff.breakStartTime || '13:00',
        breakEndTime: staff.breakEndTime || '14:00',
        dayOff: staff.dayOff || false
    };

    async function handleSave(copyDays = 0) {
        if (!shiftData.branchId) return alert('Ошибка: филиал не выбран');

        isSaving = true;
        try {
            console.log('💾 Admin saving shift:', { staffId: staff.id, ...shiftData });
            // Сохраняем смену (теперь бэкенд ждет и branchId)
            await staffService.updateShift(staff.id, shiftData);
            console.log('✅ Admin shift saved successfully');

            if (copyDays > 0) {
                await staffService.copyShift(staff.id, shiftData, copyDays);
            }

            dispatch('success');
        } catch (e) {
            console.error('❌ Admin error saving shift:', e);
            alert(e.response?.data?.message || 'Ошибка при сохранении графика');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="shift-edit-root">
    <div class="header">
        <div class="avatar" class:is-off={shiftData.dayOff}>{staff.name.charAt(0)}</div>
        <div class="info">
            <h3>{staff.name}</h3>
            <p>{date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })}</p>
        </div>
    </div>

    <div class="form-body">
        <div class="input-card status-card" class:is-off={shiftData.dayOff}>
            <label>СТАТУС ДНЯ</label>
            <div class="row">
                <span class="status-txt">{shiftData.dayOff ? 'ВЫХОДНОЙ' : 'РАБОЧИЙ ДЕНЬ'}</span>
                <button class="toggle-switch" class:on={!shiftData.dayOff} on:click={() => shiftData.dayOff = !shiftData.dayOff} type="button">
                    <div class="switch-handle"></div>
                </button>
            </div>
        </div>

        {#if !shiftData.dayOff}
            <div in:slide>
                <div class="time-grid">
                    <div class="input-card">
                        <label>НАЧАЛО РАБОТЫ</label>
                        <input type="time" bind:value={shiftData.workStartTime} />
                    </div>
                    <div class="input-card">
                        <label>КОНЕЦ РАБОТЫ</label>
                        <input type="time" bind:value={shiftData.workEndTime} />
                    </div>
                </div>

                <div class="divider"><span>ПЕРЕРЫВ (ОБЕД)</span></div>

                <div class="time-grid">
                    <div class="input-card">
                        <label>ОТ</label>
                        <input type="time" bind:value={shiftData.breakStartTime} />
                    </div>
                    <div class="input-card">
                        <label>ДО</label>
                        <input type="time" bind:value={shiftData.breakEndTime} />
                    </div>
                </div>
            </div>
        {/if}
    </div>

    <footer class="actions">
        <button class="btn-save-today" on:click={() => handleSave(0)} disabled={isSaving} type="button">
            {isSaving ? '...' : 'СОХРАНИТЬ НА СЕГОДНЯ'}
        </button>

        <div class="copy-section">
            <p>Установить этот график на неделю вперед?</p>
            <button class="btn-copy" on:click={() => handleSave(7)} disabled={isSaving} type="button">
                ПРИМЕНИТЬ НА 7 ДНЕЙ
            </button>
        </div>
    </footer>
</div>

<style>
    .shift-edit-root { padding: 24px; display: flex; flex-direction: column; gap: 24px; }

    .header { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
    .avatar { width: 56px; height: 56px; background: var(--primary-gradient); color: white; border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 900; }
    .avatar.is-off { background: #cbd5e1; box-shadow: none; }
    .info h3 { margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: #94a3b8; font-weight: 600; }

    .input-card { background: white; padding: 16px; border-radius: 20px; border: 1px solid #f1f5f9; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
    .input-card label { display: block; font-size: 9px; font-weight: 900; color: #cbd5e1; letter-spacing: 1px; margin-bottom: 8px; }

    .status-card { transition: all 0.3s; }
    .status-card.is-off { background: #fef2f2; border-color: #fee2e2; }
    .status-card .row { display: flex; justify-content: space-between; align-items: center; }
    .status-txt { font-weight: 800; font-size: 14px; color: #1e293b; }
    .status-card.is-off .status-txt { color: #ef4444; }

    .time-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    input[type="time"] { width: 100%; border: none; font-size: 18px; font-weight: 800; color: #0f172a; outline: none; background: none; }

    .divider { display: flex; align-items: center; gap: 12px; margin: 8px 0; }
    .divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: #f1f5f9; }
    .divider span { font-size: 9px; font-weight: 900; color: #cbd5e1; }

    .actions { display: flex; flex-direction: column; gap: 20px; margin-top: auto; }
    .btn-save-today { background: var(--primary-color); color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 800; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }

    .copy-section { background: #f8fafc; padding: 16px; border-radius: 20px; border: 1.5px dashed #e2e8f0; text-align: center; }
    .copy-section p { font-size: 12px; color: #64748b; font-weight: 600; margin: 0 0 12px 0; }
    .btn-copy { background: white; color: var(--primary-color); border: 1.5px solid var(--primary-color); padding: 10px 20px; border-radius: 12px; font-weight: 800; font-size: 12px; cursor: pointer; transition: 0.2s; }
    .btn-copy:hover { background: var(--primary-color); color: white; }

    .toggle-switch { width: 44px; height: 24px; background: #e2e8f0; border-radius: 12px; border: none; position: relative; cursor: pointer; transition: background 0.3s; }
    .toggle-switch.on { background: #10b981; }
    .switch-handle { width: 18px; height: 18px; background: white; border-radius: 50%; position: absolute; top: 3px; left: 3px; transition: transform 0.3s; }
    .toggle-switch.on .switch-handle { transform: translateX(20px); }
</style>
