<script>
    import { onMount } from 'svelte';
    import { employeeService } from '$lib/services/employeeService.js';
    import { fade, slide } from 'svelte/transition';
    import { goto } from '$app/navigation';

    let selectedDate = new Date();
    let shiftData = null;
    let isLoading = true;
    let isSaving = false;

    // Реактивно подгружаем смену при смене даты
    $: if (selectedDate) {
        loadShift();
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.BackButton) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/employee'));
        }
        await loadShift();
    });

    async function loadShift() {
        isLoading = true;
        try {
            // Метод getMyProfile возвращает StaffMember с данными смены на дату
            shiftData = await employeeService.getMyProfile(selectedDate);
        } catch (e) {
            console.error('Shift load error', e);
        } finally {
            isLoading = false;
        }
    }

    async function handleSave() {
        isSaving = true;
        try {
            const payload = {
                date: selectedDate.toISOString().split('T')[0],
                workStartTime: shiftData.workStartTime,
                workEndTime: shiftData.workEndTime,
                breakStartTime: shiftData.breakStartTime,
                breakEndTime: shiftData.breakEndTime,
                isDayOff: shiftData.dayOff
            };
            await employeeService.updateMyShift(payload);
            alert('График сохранен');
        } catch (e) {
            alert('Ошибка сохранения');
        } finally {
            isSaving = false;
        }
    }

    async function handleRepeat(days) {
        if (!confirm(`Скопировать этот график на ${days} дней вперед?`)) return;
        isSaving = true;
        try {
            const payload = {
                date: selectedDate.toISOString().split('T')[0],
                workStartTime: shiftData.workStartTime,
                workEndTime: shiftData.workEndTime,
                breakStartTime: shiftData.breakStartTime,
                breakEndTime: shiftData.breakEndTime,
                isDayOff: shiftData.dayOff
            };
            await employeeService.copyShift(payload, days);
            alert(`График успешно скопирован на ${days} дней`);
        } catch (e) {
            alert('Ошибка копирования');
        } finally {
            isSaving = false;
        }
    }

    function changeDate(dir) {
        const d = new Date(selectedDate);
        d.setDate(d.getDate() + dir);
        selectedDate = d;
    }
</script>

<div class="shifts-page">
    <header class="date-header">
        <button class="nav-btn" on:click={() => changeDate(-1)}>‹</button>
        <div class="current-date">
            <input type="date" value={selectedDate.toISOString().split('T')[0]}
                   on:change={(e) => selectedDate = new Date(e.target.value)} />
            <p>{selectedDate.toLocaleDateString('ru-RU', { weekday: 'long' })}</p>
        </div>
        <button class="nav-btn" on:click={() => changeDate(1)}>›</button>
    </header>

    <main class="content">
        {#if isLoading}
            <div class="center"><span class="loader"></span></div>
        {:else if shiftData}
            <div class="settings-list">
                <!-- ВЫХОДНОЙ -->
                <section class="card toggle-card">
                    <div class="text">
                        <h3>Выходной день</h3>
                        <p>Вы будете недоступны для записи</p>
                    </div>
                    <label class="switch">
                        <input type="checkbox" bind:checked={shiftData.dayOff} />
                        <span class="slider"></span>
                    </label>
                </section>

                {#if !shiftData.dayOff}
                    <div in:slide>
                        <section class="card time-card">
                            <label class="section-label">🕒 Рабочее время</label>
                            <div class="time-row">
                                <div class="time-field">
                                    <label>Начало</label>
                                    <input type="time" bind:value={shiftData.workStartTime} />
                                </div>
                                <div class="time-field">
                                    <label>Конец</label>
                                    <input type="time" bind:value={shiftData.workEndTime} />
                                </div>
                            </div>
                        </section>

                        <section class="card time-card mt-16">
                            <label class="section-label">☕ Перерыв</label>
                            <div class="time-row">
                                <div class="time-field">
                                    <label>Начало</label>
                                    <input type="time" bind:value={shiftData.breakStartTime} />
                                </div>
                                <div class="time-field">
                                    <label>Конец</label>
                                    <input type="time" bind:value={shiftData.breakEndTime} />
                                </div>
                            </div>
                        </section>
                    </div>
                {/if}

                <div class="actions">
                    <button class="btn-save" on:click={handleSave} disabled={isSaving}>
                        {isSaving ? '...' : 'СОХРАНИТЬ'}
                    </button>

                    <div class="repeat-box card">
                        <label>ПОВТОРИТЬ ЭТОТ ГРАФИК НА:</label>
                        <div class="repeat-btns">
                            <button on:click={() => handleRepeat(7)}>7 дней</button>
                            <button on:click={() => handleRepeat(14)}>14 дней</button>
                            <button on:click={() => handleRepeat(30)}>30 дней</button>
                        </div>
                    </div>
                </div>
            </div>
        {/if}
    </main>
</div>

<style>
    .shifts-page { min-height: 100vh; background: #f8fafc; padding-bottom: 100px; }

    .date-header { background: white; padding: 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; position: sticky; top: 0; z-index: 100; }
    .nav-btn { background: #f1f5f9; border: none; width: 44px; height: 44px; border-radius: 14px; font-size: 20px; cursor: pointer; color: var(--primary-color); }
    .current-date { text-align: center; }
    .current-date input { border: none; font-size: 17px; font-weight: 800; color: #0f172a; text-align: center; background: none; }
    .current-date p { margin: 0; font-size: 12px; color: var(--primary-color); font-weight: 700; text-transform: uppercase; }

    .content { padding: 20px; }
    .card { background: white; border-radius: 24px; padding: 20px; border: 1px solid #f1f5f9; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }

    .toggle-card { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .toggle-card h3 { margin: 0; font-size: 16px; color: #0f172a; }
    .toggle-card p { margin: 4px 0 0 0; font-size: 12px; color: #94a3b8; font-weight: 500; }

    .section-label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 16px; }
    .time-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .time-field label { display: block; font-size: 10px; font-weight: 700; color: #cbd5e1; margin-bottom: 6px; }
    .time-field input { width: 100%; padding: 12px; border-radius: 12px; border: 1.5px solid #f1f5f9; background: #f8fafc; font-size: 16px; font-weight: 700; color: #1e293b; }

    .actions { margin-top: 32px; display: flex; flex-direction: column; gap: 20px; }
    .btn-save { padding: 18px; border-radius: 18px; border: none; background: var(--primary-gradient); color: white; font-weight: 800; font-size: 15px; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.3); }

    .repeat-box label { font-size: 10px; font-weight: 800; color: #94a3b8; display: block; margin-bottom: 12px; text-align: center; }
    .repeat-btns { display: flex; gap: 8px; }
    .repeat-btns button { flex: 1; padding: 10px; border-radius: 10px; border: 1.5px solid #eff6ff; background: #eff6ff; color: var(--primary-color); font-weight: 700; font-size: 12px; cursor: pointer; }

    /* SWITCH STYLES */
    .switch { position: relative; display: inline-block; width: 50px; height: 28px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #e2e8f0; transition: .4s; border-radius: 28px; }
    .slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; }
    input:checked + .slider { background-color: #ef4444; }
    input:checked + .slider:before { transform: translateX(22px); }

    .mt-16 { margin-top: 16px; }
    .loader { width: 24px; height: 24px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { display: flex; justify-content: center; padding: 40px; }
</style>
