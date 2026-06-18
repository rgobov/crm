<script>
    // Импорты и пропсы
    import { createEventDispatcher, onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { serviceService } from '$lib/services/serviceService.js';
    import { fade, scale, slide } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';

    export let appointment;
    export let service = adminService;
    const dispatch = createEventDispatcher();

    let staffMember = appointment.staffMember;
    let staffName = appointment.staffName;
    let staffList = [];
    let services = [];

    // Загружаем данные сотрудника используя тот же подход что в AppointmentEditScreen
    async function loadStaffMember() {
        if (appointment.staffMemberId && !staffMember) {
            try {
                const staffData = await service.getStaffForSchedule(
                    new Date(appointment.startTime), 
                    appointment.branchId || 'br-virtual'
                );
                staffList = staffData.filter(s => s.role === 'EMPLOYEE' || s.role === 'ROLE_EMPLOYEE');
                
                const foundStaff = staffList.find(s => s.id === appointment.staffMemberId);
                if (foundStaff) {
                    staffMember = foundStaff;
                    staffName = foundStaff.name;
                }
            } catch (e) {
                console.error('Failed to load staff list:', e);
            }
        }
    }

    onMount(async () => {
        await loadStaffMember();
        try {
            services = await serviceService.getServices();
        } catch (e) {
            console.error('Failed to load services:', e);
        }
    });


    let isEditingComment = false;
    let tempComment = "";
    let isSaving = false;

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
            await service.updateAppointment(appointment.id, updated);
            appointment = updated;
            dispatch('updated', updated);
        } catch (e) {
            alert('Ошибка обновления статуса');
        }
    }

    async function saveInlineComment() {
        isSaving = true;
        try {
            const updated = { ...appointment, comment: tempComment };
            await service.updateAppointment(appointment.id, updated);
            appointment.comment = tempComment;
            isEditingComment = false;
        } catch (e) {
            alert('Ошибка сохранения заметки');
        } finally {
            isSaving = false;
        }
    }

    function startEditComment() {
        tempComment = appointment.comment || "";
        isEditingComment = true;
    }

    async function handlePropertyChange() {
        try {
            await service.updateAppointment(appointment.id, {
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
        if (appointment.groupId) {
            if (confirm('Эта запись связана с другими мастерами. Удалить ВСЕ связанные записи? (ОК - удалить все, Отмена - продолжить выбор)')) {
                await service.deleteAppointment(appointment.id, 'all');
                dispatch('deleted', appointment.id);
            } else {
                if (confirm('Удалить только текущую запись для этого сотрудника?')) {
                    await service.deleteAppointment(appointment.id, 'single');
                    dispatch('deleted', appointment.id);
                }
            }
        } else {
            if (confirm('Удалить эту запись?')) {
                await service.deleteAppointment(appointment.id, 'single');
                dispatch('deleted', appointment.id);
            }
        }
    }

    function handleClientClick() {
        if (appointment.contactId) {
            dispatch('open-client', appointment.contactId);
        }
    }

    function formatPhone(p) {
        if (!p) return "";
        const c = p.replace(/\D/g, "");
        if (c.length === 11) return `+${c[0]} (${c.slice(1,4)}) ${c.slice(4,7)}-${c.slice(7,9)}-${c.slice(9,11)}`;
        return p;
    }
</script>

<div class="detail-tiles-container" in:scale={{duration: 400, start: 0.95, easing: quintOut}}>

    <header class="hero-card">
        <div class="avatar-big">{appointment.clientName?.charAt(0) || '?'}</div>
        <div class="hero-info">
            <label>Карточка визита</label>
            <button class="client-link-btn" on:click={handleClientClick}>
                <h2>{appointment.clientName} <span>›</span></h2>
            </button>

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
        <!-- ПЛИТКА ТЕЛЕФОНА (НОВАЯ) -->
        {#if appointment.clientPhone}
            <div class="info-tile phone-tile" in:slide>
                <div class="tile-icon phone">📞</div>
                <div class="tile-body">
                    <label>Телефон клиента</label>
                    <a href="tel:{appointment.clientPhone}" class="val phone-link">
                        {formatPhone(appointment.clientPhone)}
                    </a>
                </div>
                <div class="call-action">
                    <a href="tel:{appointment.clientPhone}" class="btn-call-action">ВЫЗОВ</a>
                </div>
            </div>
        {/if}

        {#if appointment.referenceTag}
            <div class="info-tile reference-tile" in:slide>
                <div class="tile-icon car">�</div>
                <div class="tile-body">
                    <label>Объект визита</label>
                    <p class="val">{appointment.referenceTag}</p>
                </div>
            </div>
        {/if}

        <!-- ИНТЕРАКТИВНАЯ ПЛИТКА ЗАМЕТКИ -->
        <div class="info-tile comment-tile" class:editing={isEditingComment}>
            {#if !isEditingComment}
                <div class="tile-icon note">📝</div>
            {/if}
            <div class="tile-body">
                <label>Внутренняя заметка</label>
                {#if isEditingComment}
                    <div class="inline-editor" in:fade>
                        <textarea bind:value={tempComment} placeholder="Детали визита..." autofocus></textarea>
                        <div class="editor-actions">
                            <button class="btn-save-mini" on:click={saveInlineComment} disabled={isSaving}>
                                {isSaving ? '...' : 'СОХРАНИТЬ ✓'}
                            </button>
                            <button class="btn-cancel-mini" on:click={() => isEditingComment = false}>ОТМЕНА</button>
                        </div>
                    </div>
                {:else}
                    <p class="val clickable-text" on:click={startEditComment}>
                        {#if appointment.comment}
                            <span class="comment-text">"{appointment.comment}"</span>
                        {:else}
                            <span class="placeholder">Нажмите, чтобы добавить...</span>
                        {/if}
                        <span class="edit-icon">✎</span>
                    </p>
                {/if}
            </div>
        </div>

        <div class="info-tile">
            <div class="tile-icon">⭐</div>
            <div class="tile-body">
                <label>Выбранная услуга</label>
                <p class="val">{appointment.service}</p>
                {#if services.find(s => s.name === appointment.service)}
                    {@const s = services.find(s => s.name === appointment.service)}
                    {#if s.priceMin !== null && s.priceMin !== undefined}
                        <small style="font-size: 13px; color: #64748b; font-weight: 600; display: block; margin-top: 2px;">
                            Стоимость: {s.priceMax !== null && s.priceMax !== undefined ? `от ${s.priceMin} до ${s.priceMax}` : s.priceMin} руб.
                        </small>
                    {/if}
                {/if}
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
                <p class="val">{staffName || staffMember?.name || appointment.staffName || appointment.staffMember?.name || 'Не назначен'}</p>
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
            <span>✎</span> Изменить всё
        </button>
        <button class="action-tile delete" on:click={handleDelete}>
            <span>🗑</span> Удалить
        </button>
    </div>
</div>

<style>
    .detail-tiles-container { padding: 20px 20px 40px 20px; display: flex; flex-direction: column; gap: 12px; background: #f8fafc; overflow-x: hidden; }

    .hero-card { background: white; padding: 16px 24px; border-radius: 32px; display: flex; align-items: center; gap: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.04); border: 1px solid #f1f5f9; }
    .avatar-big { width: 64px; height: 64px; background: var(--primary-gradient); color: white; border-radius: 22px; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 900; }

    .hero-info { flex: 1; }
    .hero-info label { display: block; font-size: 9px; font-weight: 850; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 2px; }

    .client-link-btn { background: none; border: none; padding: 0; text-align: left; cursor: pointer; display: block; width: 100%; transition: opacity 0.2s; }
    .client-link-btn:hover { opacity: 0.7; }
    .client-link-btn h2 { margin: 0; font-size: 20px; font-weight: 800; color: #0f172a; line-height: 1.1; }
    .client-link-btn span { color: #0ea5e9; font-size: 24px; font-weight: 300; vertical-align: middle; margin-left: 4px; }

    .status-selector { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
    .status-btn { padding: 6px 10px; border-radius: 10px; border: 1.5px solid #f1f5f9; background: white; font-size: 10px; font-weight: 800; color: #64748b; cursor: pointer; transition: all 0.2s; text-transform: uppercase; }
    .status-btn.active { background: var(--active-bg); color: white; border-color: var(--active-bg); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

    .grid-layout { display: flex; flex-direction: column; gap: 10px; }
    .info-tile { background: white; padding: 16px 20px; border-radius: 24px; border: 1px solid #f1f5f9; display: flex; align-items: center; gap: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.02); text-align: left; transition: all 0.2s; }

    .phone-tile { background: #eff6ff; border-color: #bfdbfe; }
    .tile-icon.phone { background: #dbeafe; color: #3b82f6; }
    .phone-link { color: #2563eb !important; text-decoration: none; font-size: 17px !important; }
    .btn-call-action { background: #3b82f6; color: white; padding: 6px 12px; border-radius: 10px; font-size: 10px; font-weight: 900; text-decoration: none; }

    .reference-tile { background: #f0fdf4; border-color: #dcfce7; }
    .tile-icon.car { background: #dcfce7; color: #10b981; }

    .comment-tile { background: #fffbeb; border-color: #fef3c7; cursor: pointer; }
    .comment-tile:hover { background: #fef3c7; }
    .comment-tile.editing { background: white; border-color: #0ea5e9; cursor: default; align-items: flex-start; }
    .tile-icon.note { background: #fef3c7; color: #d97706; }

    .clickable-text { display: flex; justify-content: space-between; align-items: center; width: 100%; min-height: 24px; }
    .comment-text { font-style: italic; color: #92400e; font-weight: 600; }
    .placeholder { color: #94a3b8; font-weight: 500; font-size: 14px; }
    .edit-icon { opacity: 0.3; font-size: 14px; }

    .inline-editor { width: 100%; margin-top: 8px; padding: 0 15px; box-sizing: border-box; }
    textarea { width: 100%; min-height: 120px; border: 1.5px solid #e2e8f0; border-radius: 14px; padding: 14px; font-size: 15px; font-family: inherit; color: #1e293b; outline: none; resize: none; margin-bottom: 12px; background: #f8fafc; box-sizing: border-box; display: block; }
    textarea:focus { border-color: #0ea5e9; background: white; }

    .editor-actions { display: flex; gap: 10px; margin-top: 4px; }
    .btn-save-mini { flex: 1.6; background: #0ea5e9; color: white; border: none; padding: 14px 10px; border-radius: 12px; font-weight: 900; font-size: 13px; cursor: pointer; text-transform: uppercase; letter-spacing: 0.5px; box-shadow: 0 4px 12px rgba(14, 165, 233, 0.2); }
    .btn-cancel-mini { flex: 1; background: #f1f5f9; color: #64748b; border: none; padding: 14px 10px; border-radius: 12px; font-weight: 800; font-size: 12px; cursor: pointer; text-transform: uppercase; }

    .tile-icon { width: 44px; height: 44px; background: #f1f5f9; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
    .tile-icon.tg { background: #e0f2fe; color: #0ea5e9; }

    .tile-body { flex: 1; min-width: 0; }
    .tile-body label { display: block; font-size: 9px; font-weight: 800; color: #cbd5e1; text-transform: uppercase; }
    .tile-body .val { margin: 0; font-size: 15px; font-weight: 700; color: #1e293b; }
    .tile-body small { font-size: 11px; color: #94a3b8; font-weight: 600; }

    .hours-edit { display: flex; align-items: center; gap: 4px; font-size: 15px; font-weight: 800; color: #1e293b; }
    .hours-edit input { width: 45px; padding: 2px; border: 1.5px solid #e2e8f0; border-radius: 8px; text-align: center; color: #0ea5e9; font-weight: 900; background: #f8fafc; }

    .toggle-switch { width: 44px; height: 24px; background: #e2e8f0; border-radius: 12px; border: none; position: relative; cursor: pointer; transition: background 0.3s; }
    .toggle-switch.on { background: #10b981; }
    .switch-handle { width: 18px; height: 18px; background: white; border-radius: 50%; position: absolute; top: 3px; left: 3px; transition: transform 0.3s; }
    .toggle-switch.on .switch-handle { transform: translateX(20px); }

    .actions-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 12px; padding-bottom: 10px; }
    .action-tile { height: 52px; border-radius: 20px; border: 1.5px solid #f1f5f9; background: white; font-weight: 700; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }
    .action-tile.edit { color: var(--primary-color); }
    .action-tile.delete { color: #ef4444; }
</style>
