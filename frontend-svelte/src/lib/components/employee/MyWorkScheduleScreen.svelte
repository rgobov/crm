<script>
    import { onMount, onDestroy } from 'svelte';
    import { employeeService } from '$lib/services/employeeService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { goto } from '$app/navigation';

    $: currentBranchId = branchId || $activeBranchId;
    export let branchId = null;

    let selectedDate = new Date().toISOString().split('T')[0];

    // Подписка на обновления
    const unsubscribe = scheduleRefreshSignal.subscribe(signal => {
        if (signal && signal.ts > 0) {
            const { type, staffId, date, branchId: sigBranchId } = signal;

            // Если обновился график именно этого сотрудника на эту дату
            if (type === 'STAFF_SHIFT_UPDATED' && date === selectedDate) {
                 console.log('🎯 WS: My shift updated from elsewhere, refreshing...');
                 loadShift();
            }
        }
    });

    onMount(() => {
        return () => unsubscribe();
    });

    onDestroy(() => unsubscribe());
    let shiftData = {
        isDayOff: false,
        workStartTime: '09:00',
        workEndTime: '18:00',
        breakStartTime: '13:00',
        breakEndTime: '14:00'
    };

    let isLoading = true;
    let isSaving = false;
    let showRepeatModal = false;

    $: if (selectedDate) loadShift();

    async function loadShift() {
        if (!currentBranchId) return;
        isLoading = true;
        try {
            const profile = await employeeService.getMyProfile(new Date(selectedDate), currentBranchId);
            if (profile) {
                shiftData = {
                    isDayOff: profile.isDayOff || false,
                    workStartTime: profile.workStartTime || '09:00',
                    workEndTime: profile.workEndTime || '18:00',
                    breakStartTime: profile.breakStartTime || '13:00',
                    breakEndTime: profile.breakEndTime || '14:00'
                };
            }
        } finally {
            isLoading = false;
        }
    }

    async function handleSave() {
        if (!currentBranchId) {
            alert('Ошибка: филиал не выбран');
            return;
        }
        isSaving = true;
        try {
            const payload = {
                date: selectedDate,
                branchId: currentBranchId,
                ...shiftData
            };
            console.log('💾 Saving shift:', payload);
            await employeeService.updateMyShift(payload);
            console.log('✅ Shift saved successfully');
            alert('График сохранен');
        } catch (e) {
            console.error('❌ Error saving shift:', e);
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }

    async function repeatSchedule(days) {
        if (!currentBranchId) return;
        isSaving = true;
        try {
            await employeeService.repeatSchedule({
                sourceDate: selectedDate,
                days: days,
                branchId: currentBranchId,
                ...shiftData
            });
            alert(`График скопирован на ${days} дней вперед`);
            showRepeatModal = false;
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="schedule-container">
    <div class="header">
        <h1>Настройка смены</h1>
        <input type="date" bind:value={selectedDate} class="date-input" />
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="form card">
            <div class="toggle-row">
                <div class="label-col">
                    <h3>Выходной день</h3>
                    <p>Вы будете недоступны для записи</p>
                </div>
                <label class="switch">
                    <input type="checkbox" bind:checked={shiftData.isDayOff}>
                    <span class="slider"></span>
                </label>
            </div>

            {#if !shiftData.isDayOff}
                <div class="section">
                    <h4>Рабочее время</h4>
                    <div class="time-row">
                        <div class="time-field">
                            <label>С</label>
                            <input type="time" bind:value={shiftData.workStartTime} />
                        </div>
                        <div class="time-field">
                            <label>До</label>
                            <input type="time" bind:value={shiftData.workEndTime} />
                        </div>
                    </div>
                </div>

                <div class="section">
                    <h4>Перерыв (обед)</h4>
                    <div class="time-row">
                        <div class="time-field">
                            <label>С</label>
                            <input type="time" bind:value={shiftData.breakStartTime} />
                        </div>
                        <div class="time-field">
                            <label>До</label>
                            <input type="time" bind:value={shiftData.breakEndTime} />
                        </div>
                    </div>
                </div>
            {/if}

            <div class="actions">
                <button class="repeat-btn" on:click={() => showRepeatModal = true}>ПОВТОРИТЬ...</button>
                <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                    {isSaving ? '...' : 'СОХРАНИТЬ'}
                </button>
            </div>
        </div>
    {/if}
</div>

{#if showRepeatModal}
    <div class="modal-overlay">
        <div class="modal card">
            <h3>Применить на период:</h3>
            <div class="modal-buttons">
                <button on:click={() => repeatSchedule(7)}>на 7 дней</button>
                <button on:click={() => repeatSchedule(14)}>на 14 дней</button>
                <button on:click={() => repeatSchedule(30)}>на 30 дней</button>
                <button class="cancel" on:click={() => showRepeatModal = false}>ОТМЕНА</button>
            </div>
        </div>
    </div>
{/if}

<style>
    .schedule-container { padding: 20px; max-width: 500px; margin: 0 auto; }
    .header { margin-bottom: 24px; text-align: center; }
    h1 { font-size: 22px; font-weight: 800; margin-bottom: 12px; }
    .date-input { width: 100%; padding: 12px; border-radius: 12px; border: 1.5px solid #e2e8f0; font-weight: 700; text-align: center; }

    .card { background: white; padding: 24px; border-radius: 24px; box-shadow: var(--shadow); }
    .toggle-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; }
    .label-col h3 { margin: 0; font-size: 16px; }
    .label-col p { margin: 4px 0 0 0; font-size: 12px; color: var(--hint-color); }

    .section { margin-bottom: 24px; }
    .section h4 { font-size: 12px; font-weight: 800; color: #94a3b8; text-transform: uppercase; margin-bottom: 12px; }
    .time-row { display: flex; gap: 16px; }
    .time-field { flex: 1; }
    .time-field label { display: block; font-size: 11px; color: #94a3b8; margin-bottom: 4px; }
    .time-field input { width: 100%; padding: 12px; border-radius: 12px; border: 1.5px solid #f1f5f9; background: #f8fafc; font-weight: 700; }

    .actions { display: grid; grid-template-columns: 1fr 1.5fr; gap: 12px; margin-top: 32px; }
    .repeat-btn { padding: 16px; background: white; color: var(--primary-color); border: 2.5px solid var(--primary-color); border-radius: 16px; font-weight: 800; cursor: pointer; }
    .save-btn { padding: 16px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-weight: 800; cursor: pointer; }

    .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal { width: 100%; text-align: center; }
    .modal-buttons { display: flex; flex-direction: column; gap: 10px; margin-top: 20px; }
    .modal-buttons button { padding: 14px; border-radius: 12px; border: 1.5px solid #e2e8f0; background: white; font-weight: 700; cursor: pointer; }
    .modal-buttons button.cancel { border: none; color: #ef4444; margin-top: 8px; }

    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { text-align: center; padding: 40px; }
</style>
