<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import CommentThreadScreen from './CommentThreadScreen.svelte';

    export let appointment;
    export let staff = [];

    const dispatch = createEventDispatcher();

    let staffName = 'Не назначен';
    let showComments = false; // Состояние отображения чата

    $: if (appointment.staffMemberId) {
        const s = staff.find(sm => sm.id === appointment.staffMemberId);
        staffName = s ? s.name : 'Мастер не найден';
    }

    function formatTime(isoStr) {
        return new Date(isoStr).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
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
    {#if !showComments}
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
                    <p>{formatTime(appointment.startTime)} ({appointment.durationInMinutes} мин)</p>
                </div>
            </div>

            <div class="info-item">
                <span class="icon">🎖</span>
                <div class="text">
                    <label>Мастер</label>
                    <p>{staffName}</p>
                </div>
            </div>
        </div>

        <div class="comment-section">
            <button class="comment-btn" on:click={() => showComments = true}>
                💬 Чат и комментарии
            </button>
        </div>
    {:else}
        <!-- РЕЖИМ ЧАТА -->
        <div class="chat-header">
            <button class="back-link" on:click={() => showComments = false}>← Назад к деталям</button>
            <h3>Обсуждение визита</h3>
        </div>
        <div class="chat-wrapper">
            <CommentThreadScreen appointmentId={appointment.id} />
        </div>
    {/if}
</div>

<style>
    .detail-container { padding: 20px; background: white; height: 100%; display: flex; flex-direction: column; }

    .actions-top { display: flex; justify-content: flex-end; gap: 12px; margin-bottom: 24px; }
    .icon-btn { width: 44px; height: 44px; border-radius: 14px; border: none; background: #f1f5f9; font-size: 18px; cursor: pointer; }
    .icon-btn.del { color: #ef4444; background: #fef2f2; }

    .info-list { display: flex; flex-direction: column; gap: 24px; flex: 1; }
    .info-item { display: flex; gap: 16px; align-items: flex-start; }
    .info-item .icon { font-size: 24px; width: 32px; text-align: center; }

    label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }
    p { margin: 0; font-size: 17px; font-weight: 600; color: #1e293b; }

    .comment-section { margin-top: 40px; padding-bottom: 20px; }
    .comment-btn { width: 100%; padding: 16px; background: #eff6ff; border: 1.5px solid var(--primary-color); border-radius: 16px; color: var(--primary-color); font-weight: 800; cursor: pointer; }

    /* Chat view */
    .chat-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
    .back-link { background: none; border: none; color: var(--primary-color); font-weight: 700; cursor: pointer; }
    .chat-header h3 { font-size: 16px; font-weight: 800; margin: 0; }
    .chat-wrapper { flex: 1; overflow: hidden; margin: 0 -20px; }
</style>
