<script>
    import { createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { fade, scale, slide } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';

    export let appointment;
    const dispatch = createEventDispatcher();

    const STATUSES = [
        { id: 'SCHEDULED', label: 'Ожидается', color: '#3b82f6' },
        { id: 'CONFIRMED', label: 'Подтвержден', color: '#10b981' },
        { id: 'COMPLETED', label: 'Завершен', color: '#64748b' },
        { id: 'CANCELLED', label: 'Отменен', color: '#ef4444' }
    ];

    async function updateStatus(newStatus) {
        try {
            const updated = { ...appointment, status: newStatus };
            await adminService.updateAppointment(appointment.id, updated);
            appointment = updated;
            scheduleRefreshSignal.set({ ts: Date.now(), source: 'local' });
            dispatch('updated', updated);
        } catch (e) {
            alert('Ошибка обновления статуса');
        }
    }

    async function handlePropertyChange() {
        try {
            await adminService.updateAppointment(appointment.id, {
                ...appointment
            });
        } catch (e) {
            alert('Ошибка при сохранении');
        }
    }

    async function toggleReminder() {
        appointment.allowReminder = !appointment.allowReminder;
        await handlePropertyChange();
    }

    async function handleDelete() {
        if (confirm('Удалить эту запись?')) {
            await adminService.deleteAppointment(appointment.id);
            scheduleRefreshSignal.set({ ts: Date.now() });
            dispatch('deleted', appointment.id);
        }
    }
</script>

<div class="detail-tiles-container" in:scale={{duration: 400, start: 0.95, easing: quintOut}}>

    <header class="hero-card">
        <div class="avatar-big">{appointment.clientName?.charAt(0) || '?'}</div>
        <div class="hero-info">
            <label>Карточка визита</label>
            <h2>{appointment.clientName}</h2>

            <div class="status-selector">
                {#each STATUSES as st}
                    <button
                        class="status-btn"
                        class:active={appointment.status === st.id}
                        style="--active-bg: {st.color}"
                        on:click={() => updateStatus(st.id)}>
                        {st.label}
                    </button>
                {/each}
            </div>
        </div>
    </header>

    <div class="grid-layout">
        <div class="info-tile">
            <div class="tile-icon">✂️</div>
            <div class="tile-body">
                <label>Выбранная услуга</label>
                <p class="val">{appointment.service}</p>
            </div>
        </div>

        <div class="info-tile">
            <div class="tile-icon">🕒</div>
            <div class="tile-body">
                <label>Дата и время</label>
                <p class="val">
                    {new Date(appointment.startTime).toLocaleDateString('ru-RU', {day:'numeric', month:'long'})}
                    в {new Date(appointment.startTime).toLocaleTimeString('ru-RU', {hour:'2-digit', minute:'2-digit'})}
                </p>
                <small>{appointment.durationInMinutes} мин. длительность</small>
            </div>
        </div>

        <div class="info-tile">
            <div class="tile-icon">👤</div>
            <div class="tile-body">
                <label>Специалист</label>
                <p class="val">{appointment.staffName || 'Не назначен'}</p>
            </div>
        </div>

        <div class="info-tile reminder-tile" class:disabled={!appointment.allowReminder}>
            <div class="tile-icon tg">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>
                </svg>
            </div>
            <div class="tile-body">
                <label>Напоминание (Telegram/WA)</label>
                {#if appointment.allowReminder}
                    <div class="hours-edit" in:slide={{axis:'x'}}>
                        <span>за</span>
                        <input type="number"
                               bind:value={appointment.reminderLeadTimeHours}
                               on:change={handlePropertyChange}
                               min="1" max="168" />
                        <span>ч. до визита</span>
                    </div>
                {:else}
                    <p class="val" style="color: #94a3b8">Отключено</p>
                {/if}
                <small>{appointment.reminderSent ? 'Отправлено ✓' : 'Ожидает очереди'}</small>
            </div>
            <button class="toggle-switch" class:on={appointment.allowReminder} on:click={toggleReminder}>
                <div class="switch-handle"></div>
            </button>
        </div>
    </div>

    <div class="actions-row">
        <button class="action-tile edit" on:click={() => dispatch('edit', appointment)}>
            <span>✎</span> Изменить
        </button>
        <button class="action-tile delete" on:click={handleDelete}>
            <span>🗑</span> Удалить
        </button>
    </div>
</div>

<style>
    /* УБИРАЕМ ПРИНУДИТЕЛЬНУЮ ВЫСОТУ И ЛИШНИЙ ПАДДИНГ ВНИЗУ */
    .detail-tiles-container {
        padding: 20px 20px 4px 20px;
        display: flex;
        flex-direction: column;
        gap: 12px;
        background: #f8fafc;
        overflow-x: hidden;
    }

    /* УЖИМАЕМ ШАПКУ (МЕНЬШЕ ПУСТОТЫ) */
    .hero-card {
        background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
        padding: 16px 24px; border-radius: 32px; display: flex; align-items: center; gap: 20px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.04); border: 1px solid #f1f5f9;
    }
    .avatar-big { width: 64px; height: 64px; background: var(--primary-gradient); color: white; border-radius: 22px; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 900; }

    .hero-info { flex: 1; }
    .hero-info label { display: block; font-size: 9px; font-weight: 850; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 2px; }
    .hero-info h2 { margin: 0; font-size: 20px; font-weight: 800; color: #0f172a; line-height: 1.1; }

    .status-selector { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
    .status-btn {
        padding: 6px 10px; border-radius: 10px; border: 1.5px solid #f1f5f9; background: white;
        font-size: 10px; font-weight: 800; color: #64748b; cursor: pointer; transition: all 0.2s;
        text-transform: uppercase;
    }
    .status-btn.active {
        background: var(--active-bg); color: white; border-color: var(--active-bg);
        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }

    .grid-layout { display: flex; flex-direction: column; gap: 10px; }
    .info-tile { background: white; padding: 16px 20px; border-radius: 24px; border: 1px solid #f1f5f9; display: flex; align-items: center; gap: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.02); text-align: left; }

    .tile-icon { width: 44px; height: 44px; background: #f1f5f9; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
    .tile-icon.tg { background: #e0f2fe; color: #0ea5e9; }

    .tile-body label { display: block; font-size: 9px; font-weight: 800; color: #cbd5e1; text-transform: uppercase; }
    .tile-body .val { margin: 0; font-size: 15px; font-weight: 700; color: #1e293b; }
    .tile-body small { font-size: 11px; color: #94a3b8; font-weight: 600; }

    .hours-edit { display: flex; align-items: center; gap: 4px; font-size: 15px; font-weight: 800; color: #1e293b; }
    .hours-edit input { width: 45px; padding: 2px; border: 1.5px solid #e2e8f0; border-radius: 8px; text-align: center; color: #0ea5e9; font-weight: 900; background: #f8fafc; }

    .toggle-switch {
        width: 44px; height: 24px; background: #e2e8f0; border-radius: 12px; border: none;
        position: relative; cursor: pointer; transition: background 0.3s;
    }
    .toggle-switch.on { background: #10b981; }
    .switch-handle {
        width: 18px; height: 18px; background: white; border-radius: 50%;
        position: absolute; top: 3px; left: 3px; transition: transform 0.3s;
    }
    .toggle-switch.on .switch-handle { transform: translateX(20px); }

    /* ПРИЖИМАЕМ КНОПКИ К КОНТЕНТУ */
    .actions-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 0; padding-bottom: 10px; }
    .action-tile { height: 52px; border-radius: 20px; border: 1.5px solid #f1f5f9; background: white; font-weight: 700; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }
    .action-tile.edit { color: var(--primary-color); }
    .action-tile.delete { color: #ef4444; }
</style>
