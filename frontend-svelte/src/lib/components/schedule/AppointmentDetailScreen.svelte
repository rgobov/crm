<script>
    import { createEventDispatcher, onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { fade, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';

    export let appointment;
    const dispatch = createEventDispatcher();

    function getStatusConfig(status) {
        const config = {
            'SCHEDULED': { color: '#3b82f6', label: 'Ожидается' },
            'CONFIRMED': { color: '#10b981', label: 'Подтвержден' },
            'COMPLETED': { color: '#64748b', label: 'Завершен' },
            'CANCELLED': { color: '#ef5350', label: 'Отменен' }
        };
        return config[status] || config['SCHEDULED'];
    }

    async function updateStatus(newStatus) {
        try {
            const updated = { ...appointment, status: newStatus };
            await adminService.updateAppointment(appointment.id, updated);
            appointment = updated;
            dispatch('updated', updated);
        } catch (e) { alert('Ошибка обновления'); }
    }

    async function sendTelegramManual() {
        // Здесь будет вызов Telegram Bot API (вместо Wappi)
        alert('Запрос на отправку напоминания в Telegram отправлен');
    }

    async function handleDelete() {
        if (confirm('Удалить эту запись?')) {
            await adminService.deleteAppointment(appointment.id);
            dispatch('deleted', appointment.id);
        }
    }
</script>

<div class="detail-tiles-container" in:scale={{duration: 400, start: 0.95, easing: quintOut}}>

    <!-- КЛИЕНТ (ГЕРОЙ-ПЛИТКА) -->
    <header class="hero-card">
        <div class="avatar-big">{appointment.clientName?.charAt(0) || '?'}</div>
        <div class="hero-info">
            <label>Карточка визита</label>
            <h2>{appointment.clientName}</h2>
            <div class="status-badge" style="background: {getStatusConfig(appointment.status).color}">
                {getStatusConfig(appointment.status).label}
            </div>
        </div>
    </header>

    <div class="grid-layout">
        <!-- УСЛУГА -->
        <div class="info-tile">
            <div class="tile-icon">✂️</div>
            <div class="tile-body">
                <label>Выбранная услуга</label>
                <p class="val">{appointment.service}</p>
            </div>
        </div>

        <!-- ВРЕМЯ -->
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

        <!-- МАСТЕР -->
        <div class="info-tile">
            <div class="tile-icon">👤</div>
            <div class="tile-body">
                <label>Специалист</label>
                <p class="val">{appointment.staffName || 'Не назначен'}</p>
            </div>
        </div>

        <!-- TELEGRAM УВЕДОМЛЕНИЕ -->
        <button class="info-tile interactive" on:click={sendTelegramManual}>
            <div class="tile-icon tg">✈️</div>
            <div class="tile-body">
                <label>Telegram Напоминание</label>
                <p class="val {appointment.reminderSent ? 'success' : 'pending'}">
                    {appointment.reminderSent ? 'Отправлено ✓' : 'Ожидает отправки'}
                </p>
                <small>Нажмите для ручного пуша</small>
            </div>
        </button>
    </div>

    <!-- КНОПКИ ДЕЙСТВИЯ -->
    <div class="actions-row">
        <button class="action-tile edit" on:click={() => dispatch('edit', appointment)}>
            <span>✎</span> Изменить
        </button>
        <button class="action-tile delete" on:click={handleDelete}>
            <span>🗑</span> Удалить
        </button>
    </div>

    <!-- ТЕГИ СТАТУСОВ -->
    <div class="status-chips-wrap">
        <button class="chip" on:click={() => updateStatus('CONFIRMED')}>Подтвердить</button>
        <button class="chip" on:click={() => updateStatus('COMPLETED')}>Завершить</button>
        <button class="chip cancel" on:click={() => updateStatus('CANCELLED')}>Отменить</button>
    </div>
</div>

<style>
    .detail-tiles-container { padding: 20px; display: flex; flex-direction: column; gap: 12px; background: #f8fafc; min-height: 100%; }

    .hero-card {
        background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
        padding: 24px; border-radius: 32px; display: flex; align-items: center; gap: 20px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.04); border: 1px solid #f1f5f9; margin-bottom: 8px;
    }
    .avatar-big { width: 64px; height: 64px; background: var(--primary-gradient); color: white; border-radius: 22px; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 900; }
    .hero-info label { display: block; font-size: 10px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }
    .hero-info h2 { margin: 0; font-size: 22px; font-weight: 800; color: #0f172a; }
    .status-badge { display: inline-block; padding: 4px 12px; border-radius: 10px; color: white; font-size: 11px; font-weight: 800; margin-top: 8px; }

    .grid-layout { display: flex; flex-direction: column; gap: 10px; }
    .info-tile { background: white; padding: 16px 20px; border-radius: 24px; border: 1px solid #f1f5f9; display: flex; align-items: center; gap: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
    .info-tile.interactive:active { transform: scale(0.98); background: #f0f9ff; }
    .tile-icon { width: 44px; height: 44px; background: #f1f5f9; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
    .tile-icon.tg { background: #e0f2fe; color: #0ea5e9; }

    .tile-body label { display: block; font-size: 9px; font-weight: 800; color: #cbd5e1; text-transform: uppercase; }
    .tile-body .val { margin: 0; font-size: 15px; font-weight: 700; color: #1e293b; }
    .tile-body .val.success { color: #10b981; }
    .tile-body small { font-size: 11px; color: #94a3b8; font-weight: 600; }

    .actions-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 8px; }
    .action-tile { height: 52px; border-radius: 20px; border: 1px solid #f1f5f9; background: white; font-weight: 700; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }
    .action-tile.edit { color: var(--primary-color); }
    .action-tile.delete { color: #ef4444; }

    .status-chips-wrap { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; padding-top: 16px; border-top: 1px solid #f1f5f9; }
    .chip { background: #f1f5f9; border: none; padding: 10px 16px; border-radius: 12px; font-size: 12px; font-weight: 700; color: #64748b; cursor: pointer; transition: 0.2s; }
    .chip:hover { background: #e2e8f0; color: #0f172a; }
    .chip.cancel:hover { background: #fef2f2; color: #ef4444; }
</style>
