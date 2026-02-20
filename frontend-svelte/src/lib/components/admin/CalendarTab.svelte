<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import AppointmentEditScreen from '$lib/components/schedule/AppointmentEditScreen.svelte';
    import AppointmentDetailScreen from '$lib/components/schedule/AppointmentDetailScreen.svelte';
    import AddContactModal from '$lib/components/admin/AddContactModal.svelte';
    import ContactDetailScreen from '$lib/components/contacts/ContactDetailScreen.svelte';
    import ShiftEditScreen from '$lib/components/employee/ShiftEditScreen.svelte';
    import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { fade, scale } from 'svelte/transition';

    export let forcedDate = null;

    let viewMode = 'month';
    let showModal = null;
    let showNestedAddContact = false;
    let selectedClientId = null;
    let selectedStaffForShift = null;

    let currentAppointment = null;
    let preselectedData = null;
    let appointmentEditRef;

    let onlyBusyStaff = false;
    let onlyWorkingStaff = false;

    $: if (forcedDate) {
        selectedDate.set(new Date(forcedDate));
        viewMode = 'day';
    }

    $: if ($activeTab === 'timeline') {
        viewMode = 'day';
    } else if ($activeTab === 'calendar') {
        viewMode = 'month';
    }

    function handleDateSelected(event) {
        selectedDate.set(event.detail.date);
        viewMode = 'day';
    }

    function openNewAppointment(event) {
        preselectedData = {
            date: $selectedDate,
            hour: event?.detail?.hour || 10,
            min: event?.detail?.min || 0,
            staffId: event?.detail?.staffId || null
        };
        currentAppointment = null;
        showModal = 'edit';
    }

    function handleStaffTap(event) {
        selectedStaffForShift = event.detail;
        showModal = 'shift';
    }

    function openDetail(event) {
        currentAppointment = event.detail;
        showModal = 'detail';
    }

    function closeModal() {
        showModal = null;
        showNestedAddContact = false;
        selectedClientId = null;
        selectedStaffForShift = null;
    }

    function handleContactAdded(event) {
        const newContact = event.detail;
        if (appointmentEditRef) {
            appointmentEditRef.setCreatedContact(newContact);
        }
        showNestedAddContact = false;
    }

    function handleOpenClient(id) {
        selectedClientId = id;
        showModal = 'client-profile';
    }
</script>

<div class="calendar-tab-root">
    {#if viewMode === 'month'}
        <div class="month-view" in:fade>
            <div class="header-row">
                <h2>Календарь записей</h2>
                <button class="today-btn" on:click={() => { selectedDate.set(new Date()); viewMode = 'day'; activeTab.set('timeline'); }}>СЕГОДНЯ</button>
            </div>
            <CalendarScreen on:dateSelected={handleDateSelected} />
        </div>

    {:else if viewMode === 'day'}
        <div class="day-view-wrapper" in:fade>
            <div class="day-top-bar">
                <div class="side-col left">
                    <div class="filters-group">
                        <button
                            class="toggle-pill"
                            class:active={onlyWorkingStaff}
                            on:click={() => onlyWorkingStaff = !onlyWorkingStaff}
                        >
                            <span class="icon">{onlyWorkingStaff ? '⚡' : '💤'}</span>
                            <span class="label">{onlyWorkingStaff ? 'В смене' : 'Все'}</span>
                        </button>

                        <button
                            class="toggle-pill"
                            class:active={onlyBusyStaff}
                            on:click={() => onlyBusyStaff = !onlyBusyStaff}
                        >
                            <span class="icon">{onlyBusyStaff ? '🎯' : '👥'}</span>
                            <span class="label">{onlyBusyStaff ? 'Занятые' : 'Все'}</span>
                        </button>
                    </div>
                </div>

                <div class="date-info">
                    <span class="d">{$selectedDate.getDate()}</span>
                    <span class="m">
                        {$selectedDate.toLocaleDateString('ru-RU', { month: 'long' }).replace(/^./, str => str.toUpperCase())}
                    </span>
                </div>

                <div class="side-col right">
                    <button class="btn-add" on:click={() => openNewAppointment({ detail: {} })}>+ Запись</button>
                </div>
            </div>

            <div class="timeline-container">
                <ScheduleScreen
                    branchId={$activeBranchId}
                    {onlyBusyStaff}
                    {onlyWorkingStaff}
                    on:emptySlotTap={openNewAppointment}
                    on:appointmentTap={openDetail}
                    on:staffTap={handleStaffTap}
                />
            </div>
        </div>
    {/if}

    {#if showModal}
        <div class="modal-backdrop" on:mousedown|self={closeModal} transition:fade={{duration: 200}}>
            <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
                <header class="modal-header">
                    <h3>
                        {#if showModal === 'edit'}
                            {currentAppointment ? 'Редактирование' : 'Новая запись'}
                        {:else if showModal === 'detail'}
                            Детали визита
                        {:else if showModal === 'client-profile'}
                            Карточка клиента
                        {:else if showModal === 'shift'}
                            График работы
                        {/if}
                    </h3>
                    <button class="close-btn" on:click={closeModal}>✕</button>
                </header>

                <div class="modal-body-scroll">
                    {#if showModal === 'edit'}
                        <AppointmentEditScreen
                            bind:this={appointmentEditRef}
                            appointment={currentAppointment}
                            preselected={preselectedData}
                            on:cancel={closeModal}
                            on:saved={closeModal}
                            on:open-add-contact-modal={() => showNestedAddContact = true}
                        />
                    {:else if showModal === 'detail'}
                        <AppointmentDetailScreen
                            appointment={currentAppointment}
                            on:edit={(e) => { currentAppointment = e.detail; showModal = 'edit'; }}
                            on:deleted={closeModal}
                            on:open-client={(e) => handleOpenClient(e.detail)}
                        />
                    {:else if showModal === 'client-profile'}
                        <ContactDetailScreen
                            contactId={selectedClientId}
                            on:updated={closeModal}
                        />
                    {:else if showModal === 'shift'}
                        <ShiftEditScreen
                            staff={selectedStaffForShift}
                            date={$selectedDate}
                            on:success={closeModal}
                        />
                    {/if}
                </div>
            </div>
        </div>
    {/if}

    {#if showNestedAddContact}
        <AddContactModal
            on:close={() => showNestedAddContact = false}
            on:success={handleContactAdded}
        />
    {/if}
</div>

<style>
    .calendar-tab-root { height: 100%; display: flex; flex-direction: column; background: white; position: relative; overflow: hidden; }
    .month-view { padding: 24px; flex: 1; overflow-y: auto; }
    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    h2 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .today-btn { background: var(--primary-gradient); color: white; border: none; padding: 10px 20px; border-radius: 14px; font-weight: 700; cursor: pointer; }

    .day-view-wrapper { flex: 1; display: flex; flex-direction: column; height: 100%; overflow: hidden; }

    /* СИММЕТРИЧНАЯ СЕТКА ДЛЯ ЦЕНТРИРОВАНИЯ ДАТЫ */
    .day-top-bar {
        padding: 12px 24px;
        border-bottom: 1px solid #f1f5f9;
        display: grid;
        grid-template-columns: 250px 1fr 250px;
        align-items: center;
        flex-shrink: 0;
    }

    .side-col { display: flex; align-items: center; }
    .side-col.right { justify-content: flex-end; }

    .filters-group { display: flex; gap: 8px; align-items: center; }

    .toggle-pill {
        display: flex; align-items: center; gap: 6px; padding: 6px 10px;
        border-radius: 16px; border: 1.5px solid #f1f5f9; background: #f8fafc;
        cursor: pointer; transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .toggle-pill.active { background: #eff6ff; border-color: #3b82f6; }
    .toggle-pill .label { font-size: 10px; font-weight: 850; color: #64748b; text-transform: uppercase; letter-spacing: 0.3px; }
    .toggle-pill.active .label { color: #3b82f6; }

    .date-info { display: flex; align-items: baseline; justify-content: center; gap: 8px; }
    .date-info .d { font-size: 24px; font-weight: 900; color: var(--primary-color); letter-spacing: -0.5px; }
    .date-info .m { font-size: 15px; font-weight: 700; color: #64748b; letter-spacing: -0.2px; }

    .btn-add { background: var(--primary-gradient); color: white; border: none; padding: 8px 16px; border-radius: 12px; font-weight: 800; font-size: 13px; cursor: pointer; box-shadow: 0 4px 12px rgba(56, 151, 240, 0.2); }

    .timeline-container { flex: 1; overflow: hidden; position: relative; }

    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(4px); z-index: 2000; display: flex; align-items: center; justify-content: center; padding: 20px; }
    .modal-content { background: white; width: 100%; max-width: 550px; height: 85vh; border-radius: 32px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.3); }
    .modal-header { padding: 24px 32px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; }
    .close-btn { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #64748b; font-weight: bold; }
    .modal-body-scroll { flex: 1; overflow-y: auto; background: #f8fafc; }

    @media (max-width: 850px) {
        .day-top-bar { grid-template-columns: 1fr 1fr; padding: 12px 16px; }
        .side-col.left { display: none; } /* Скрываем фильтры на малых экранах для центрирования даты */
        .date-info { justify-content: flex-start; }
    }
</style>
