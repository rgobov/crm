<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';

    export let appointment;
    export let staff = [];

    const dispatch = createEventDispatcher();

    let staffName = 'Не назначен';

    $: if (appointment.staffMemberId) {
        const s = staff.find(sm => sm.id === appointment.staffMemberId);
        staffName = s ? s.name : 'Мастер не найден';
    }

    function formatTime(isoStr) {
        return new Date(isoStr).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    }

    function getEndTime(isoStr, duration) {
        const end = new Date(new Date(isoStr).getTime() + duration * 60000);
        return end.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    }

    async function handleDelete() {
        if (confirm('Вы действительно хотите безвозвратно удалить эту запись?')) {
            try {
                await adminService.deleteAppointment(appointment.id);
                dispatch('deleted');
            } catch (e) {
                alert('Ошибка при удалении записи');
            }
        }
    }
</script>

<div class="detail-container">
    <div class="actions-top">
        <button class="icon-btn" on:click={() => dispatch('edit', appointment)}>✎</button>
        <button class="icon-btn del" on:click={handleDelete}>🗑</button>
    </div>

    <div class="info-list">
        <div class="info-item">
            <span class="icon">👤</span>
            <div class="text">
                <label>Клиент</label>
                <p>{appointment.clientName}</p>
            </div>
        </div>

        <div class="info-item">
            <span class="icon">✨</span>
            <div class="text">
                <label>Услуга</label>
                <p>{appointment.service}</p>
            </div>
        </div>

        <div class="info-item">
            <span class="icon">🕒</span>
            <div class="text">
                <label>Время и длительность</label>
                <p>{formatTime(appointment.startTime)} — {getEndTime(appointment.startTime, appointment.durationInMinutes)} ({appointment.durationInMinutes} мин)</p>
            </div>
        </div>

        <div class="info-item">
            <span class="icon">🎖</span>
            <div class="text">
                <label>Мастер</label>
                <p>{staffName}</p>
            </div>
        </div>

        {#if appointment.resourceId}
            <div class="info-item">
                <span class="icon">🏢</span>
                <div class="text">
                    <label>Кабинет / Ресурс</label>
                    <p>{appointment.resourceId}</p>
                </div>
            </div>
        {/if}
    </div>

    <div class="comment-section">
        <button class="comment-btn" on:click={() => console.log('Open comments')}>
            💬 Обсуждение и комментарии
        </button>
    </div>
</div>

<style>
    .detail-container { padding: 20px; background: white; height: 100%; }

    .actions-top { display: flex; justify-content: flex-end; gap: 12px; margin-bottom: 24px; }
    .icon-btn { width: 44px; height: 44px; border-radius: 14px; border: none; background: #f1f5f9; font-size: 18px; cursor: pointer; }
    .icon-btn.del { color: #ef4444; background: #fef2f2; }

    .info-list { display: flex; flex-direction: column; gap: 24px; }
    .info-item { display: flex; gap: 16px; align-items: flex-start; }
    .info-item .icon { font-size: 24px; width: 32px; text-align: center; }

    label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }
    p { margin: 0; font-size: 17px; font-weight: 600; color: #1e293b; }

    .comment-section { margin-top: 40px; padding-top: 24px; border-top: 1px solid #f1f5f9; }
    .comment-btn { width: 100%; padding: 16px; background: #f8fafc; border: 1.5px dashed #cbd5e1; border-radius: 16px; color: #64748b; font-weight: 700; cursor: pointer; }
</style>
