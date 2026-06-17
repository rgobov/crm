<script>
    import { createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { fade } from 'svelte/transition';

    export let appointment;
    const dispatch = createEventDispatcher();

    const STATUSES = [
        { id: 'SCHEDULED', label: 'Ожидается', color: '#64748b' },
        { id: 'CONFIRMED', label: 'Подтвержден', color: '#0891b2' },
        { id: 'ARRIVED', label: 'Пришёл', color: '#7c3aed' },
        { id: 'COMPLETED', label: 'Завершен', color: '#16a34a' },
        { id: 'CANCELLED', label: 'Отменен', color: '#dc2626' }
    ];

    async function updateStatus(newStatus) {
        try {
            const updated = { ...appointment, status: newStatus };
            await adminService.updateAppointment(appointment.id, updated);
            appointment = updated;
            dispatch('updated', updated);
        } catch (e) {
            alert('Ошибка обновления статуса');
        }
    }

    async function handleDelete() {
        if (appointment.groupId) {
            if (confirm('Эта запись связана с другими мастерами. Удалить ВСЕ связанные записи? (ОК - удалить все, Отмена - продолжить выбор)')) {
                await adminService.deleteAppointment(appointment.id, 'all');
                dispatch('deleted', appointment.id);
            } else {
                if (confirm('Удалить только текущую запись для этого сотрудника?')) {
                    await adminService.deleteAppointment(appointment.id, 'single');
                    dispatch('deleted', appointment.id);
                }
            }
        } else {
            if (confirm('Удалить эту запись?')) {
                await adminService.deleteAppointment(appointment.id, 'single');
                dispatch('deleted', appointment.id);
            }
        }
    }

    function formatPhone(p) {
        if (!p) return "";
        const c = p.replace(/\D/g, "");
        if (c.length === 11) return `+${c[0]} (${c.slice(1,4)}) ${c.slice(4,7)}-${c.slice(7,9)}-${c.slice(9,11)}`;
        return p;
    }
</script>

<div class="desktop-detail-wrapper" in:fade={{duration: 150}}>
    <header class="header">
        <div class="client-box">
            <div class="avatar">{appointment.clientName?.charAt(0) || '?'}</div>
            <div class="info">
                <h2 on:click={() => dispatch('open-client', appointment.contactId)} class:link={appointment.contactId}>
                    {appointment.clientName}
                </h2>
                <span class="service-name">{appointment.service}</span>
            </div>
        </div>
        <div class="actions">
            <button class="btn-tool" on:click={() => dispatch('edit', appointment)} title="Редактировать">✎</button>
            <button class="btn-tool delete" on:click={handleDelete} title="Удалить">🗑</button>
        </div>
    </header>

    <div class="content-grid">
        <div class="info-side">
            <label>ИНФОРМАЦИЯ</label>

            {#if appointment.clientPhone}
                <div class="data-item phone-row">
                    <span class="icon">📞</span>
                    <a href="tel:{appointment.clientPhone}" class="phone-link">
                        {formatPhone(appointment.clientPhone)}
                    </a>
                </div>
            {/if}

            <div class="data-item">
                <span class="icon">🕒</span>
                <span>{new Date(appointment.startTime).toLocaleTimeString('ru-RU', {hour:'2-digit', minute:'2-digit'})} ({appointment.durationInMinutes} мин)</span>
            </div>
            <div class="data-item">
                <span class="icon">👤</span>
                <span>{appointment.staffName || 'Не назначен'}</span>
            </div>
            {#if appointment.referenceTag}
                <div class="data-item highlight">
                    <span class="icon">�</span>
                    <span>{appointment.referenceTag}</span>
                </div>
            {/if}
            {#if appointment.comment}
                <div class="comment-preview">
                    <label>ЗАМЕТКА</label>
                    <p>{appointment.comment}</p>
                </div>
            {/if}
        </div>

        <div class="status-side">
            <label>СТАТУС ВИЗИТА</label>
            <div class="status-list">
                {#each STATUSES as st}
                    <button
                        class="st-btn"
                        class:active={appointment.status === st.id}
                        style="--st-color: {st.color}"
                        on:click={() => updateStatus(st.id)}>
                        <span class="dot"></span>
                        {st.label}
                    </button>
                {/each}
            </div>
        </div>
    </div>
</div>

<style>
    .desktop-detail-wrapper { background: #fdf6e3; padding: 24px; color: #073642; border-radius: 24px; }

    .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1.5px solid #ddd6c1; padding-bottom: 20px; margin-bottom: 24px; }
    .avatar { width: 52px; height: 52px; background: #eee8d5; border: 1.5px solid #ddd6c1; color: #268bd2; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 900; }

    .info { margin-left: 16px; }
    h2 { margin: 0; font-size: 20px; font-weight: 850; color: #073642; cursor: pointer; }
    h2.link:hover { color: #268bd2; }
    .service-name { font-size: 11px; font-weight: 800; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.5px; }

    .btn-tool { background: #eee8d5; border: 1.5px solid #ddd6c1; width: 38px; height: 38px; border-radius: 10px; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
    .btn-tool:hover { background: #fdf6e3; border-color: transparent; color: #268bd2; }

    .content-grid { display: grid; grid-template-columns: 1fr 180px; gap: 32px; }
    label { display: block; font-size: 10px; font-weight: 900; color: #93a1a1; margin-bottom: 12px; text-transform: uppercase; }

    .data-item { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; font-size: 14px; font-weight: 650; color: #586e75; }
    .data-item.highlight { color: #2aa198; }

    .phone-row { margin-bottom: 16px; border-bottom: 1px dashed #ddd6c1; padding-bottom: 12px; }
    .phone-link { color: #268bd2; text-decoration: none; font-size: 16px; font-weight: 800; }
    .phone-link:hover { text-decoration: underline; }

    .comment-preview { margin-top: 24px; background: #eee8d5; padding: 16px; border-radius: 16px; border: 1px solid #ddd6c1; }
    .comment-preview p { margin: 0; font-size: 13px; line-height: 1.5; color: #073642; word-break: break-word; overflow-wrap: anywhere; }

    .status-list { display: flex; flex-direction: column; gap: 8px; }
    .st-btn {
        display: flex; align-items: center; gap: 10px;
        padding: 12px 16px; border-radius: 12px;
        border: 1.5px solid #ddd6c1;
        background: #eee8d5;
        font-size: 13px; font-weight: 800; color: #586e75;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .dot { width: 8px; height: 8px; border-radius: 50%; background: #93a1a1; transition: all 0.2s; }

    .st-btn:hover:not(.active) {
        background: #fdf6e3;
        border-color: transparent;
        color: #073642;
    }
    .st-btn:hover .dot { background: var(--st-color); }

    .st-btn.active {
        background: var(--st-color);
        color: #fdf6e3;
        border-color: var(--st-color);
        box-shadow: 0 4px 15px -3px var(--st-color);
    }
    .st-btn.active .dot { background: #fdf6e3; }
</style>
