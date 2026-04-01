<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import AppointmentEditMobile from '$lib/components/schedule/AppointmentEditMobile.svelte';
    import AppointmentDetailMobile from '$lib/components/schedule/AppointmentDetailMobile.svelte';
    import AddContactModal from '$lib/components/admin/AddContactModal.svelte';
    import ContactDetailScreen from '$lib/components/contacts/ContactDetailScreen.svelte';
    import ShiftEditScreen from '$lib/components/employee/ShiftEditScreen.svelte';
    import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { fade, scale } from 'svelte/transition';

    export let forcedDate = null;

    let viewMode = 'day';
    let showModal = null;
    let showNestedAddContact = false;
    let selectedClientId = null;
    let selectedStaffForShift = null;

    let currentAppointment = null;
    let preselectedData = null;
    let appointmentEditRef;

    let onlyBusyStaff = false;
    let onlyWorkingStaff = false;

    // Блокировка скролла body при открытой модалке (важно для iOS)
    $: if (typeof document !== 'undefined') {
        if (showModal) {
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = '';
        }
    }

    // Функция выбора филиала
    function selectBranch(branchId) {
        activeBranchId.set(branchId);
        viewMode = 'month'; // Возвращаемся к календарю после выбора
    }

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
        if (appointmentEditRef) appointmentEditRef.setCreatedContact(newContact);
        showNestedAddContact = false;
    }

    function handleOpenClient(id) {
        selectedClientId = id;
        showModal = 'client-profile';
    }
</script>

<div class="calendar-tab-mobile">
    {#if viewMode === 'month'}
        <div class="month-view-mobile" in:fade>
            <div class="header-row">
                <h2>Календарь</h2>
                <div class="header-actions">
                    <button class="today-btn" on:click={() => { selectedDate.set(new Date()); viewMode = 'day'; activeTab.set('timeline'); }}>СЕГОДНЯ</button>
                    <button class="more-btn" on:click={() => viewMode = 'more'}>ЕЩЁ</button>
                </div>
            </div>
            <CalendarScreen on:dateSelected={handleDateSelected} />
        </div>
    {:else if viewMode === 'day'}
        <div class="day-view-mobile" in:fade>

            <header class="mobile-filter-bar">
                <!-- ДАТА ТЕПЕРЬ СЛЕВА -->
                <div class="date-chip">
                    <span class="d">{$selectedDate.getDate()}</span>
                    <span class="m">{$selectedDate.toLocaleDateString('ru-RU', { month: 'short' }).toUpperCase()}</span>
                </div>

                <!-- ФИЛЬТРЫ В ВИДЕ ПОНЯТНЫХ КНОПОК-ПЕРЕКЛЮЧАТЕЛЕЙ -->
                <div class="filter-pills">
                    <button class="pill" class:active={onlyWorkingStaff} on:click={() => onlyWorkingStaff = !onlyWorkingStaff}>
                        <span class="p-icon">⚡</span>
                        <span class="p-label">В СМЕНЕ</span>
                    </button>
                    <button class="pill" class:active={onlyBusyStaff} on:click={() => onlyBusyStaff = !onlyBusyStaff}>
                        <span class="p-icon">🎯</span>
                        <span class="p-label">ЗАНЯТЫЕ</span>
                    </button>
                </div>
            </header>

            <div class="mobile-timeline-wrapper">
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
    {:else if viewMode === 'more'}
        <div class="more-view-mobile" in:fade>
            <div class="header-row">
                <h2>Ещё</h2>
                <button class="back-btn" on:click={() => viewMode = 'month'}>←</button>
            </div>
            <div class="branch-selector">
                <h3>Выбор филиала</h3>
                <div class="branch-list">
                    <button 
                        class="branch-item" 
                        class:active={$activeBranchId === 1}
                        on:click={() => selectBranch(1)}
                    >
                        <span class="branch-name">Основной филиал</span>
                        {#if $activeBranchId === 1}
                            <span class="branch-check">✓</span>
                        {/if}
                    </button>
                    <button 
                        class="branch-item" 
                        class:active={$activeBranchId === 2}
                        on:click={() => selectBranch(2)}
                    >
                        <span class="branch-name">Филиал 2</span>
                        {#if $activeBranchId === 2}
                            <span class="branch-check">✓</span>
                        {/if}
                    </button>
                    <button 
                        class="branch-item" 
                        class:active={$activeBranchId === 3}
                        on:click={() => selectBranch(3)}
                    >
                        <span class="branch-name">Филиал 3</span>
                        {#if $activeBranchId === 3}
                            <span class="branch-check">✓</span>
                        {/if}
                    </button>
                </div>
            </div>
        </div>
    {/if}

    <!-- МОДАЛКИ -->
    {#if showModal}
        <div class="modal-backdrop" on:mousedown|self={closeModal} transition:fade={{duration: 200}}>
            <div class="modal-content-mobile" transition:scale={{start: 0.95, duration: 200}}>
                <header class="modal-header">
                    <h3>
                        {#if showModal === 'edit'}
                            {currentAppointment ? 'Редактирование записи' : 'Создание записи'}
                        {:else if showModal === 'detail'} Детали визита
                        {:else if showModal === 'client-profile'} Карточка клиента
                        {:else if showModal === 'shift'} График работы
                        {/if}
                    </h3>
                    <button class="close-btn" on:click={closeModal}>✕</button>
                </header>
                <div class="modal-body">
                    {#if showModal === 'edit'}
                        <AppointmentEditMobile bind:this={appointmentEditRef} appointment={currentAppointment} preselected={preselectedData} on:cancel={closeModal} on:saved={closeModal} on:open-add-contact-modal={() => showNestedAddContact = true} />
                    {:else if showModal === 'detail'}
                        <AppointmentDetailMobile appointment={currentAppointment} on:edit={(e) => { currentAppointment = e.detail; showModal = 'edit'; }} on:deleted={closeModal} on:open-client={(e) => handleOpenClient(e.detail)} />
                    {:else if showModal === 'client-profile'}
                        <ContactDetailScreen contactId={selectedClientId} on:updated={closeModal} />
                    {:else if showModal === 'shift'}
                        <ShiftEditScreen staff={selectedStaffForShift} date={$selectedDate} on:success={closeModal} />
                    {/if}
                </div>
            </div>
        </div>
    {/if}

    {#if showNestedAddContact}
        <AddContactModal on:close={() => showNestedAddContact = false} on:success={handleContactAdded} />
    {/if}
</div>

<style>
    .calendar-tab-mobile { height: 100%; width: 100%; background: #fdf6e3; display: flex; flex-direction: column; overflow: hidden; }

    .month-view-mobile { flex: 1; padding: 16px; overflow-y: auto; }
    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header-actions { display: flex; gap: 8px; align-items: center; }
    h2 { font-size: 20px; font-weight: 850; color: #073642; margin: 0; }
    .today-btn { background: #eee8d5; color: #268bd2; border: 1.5px solid #ddd6c1; padding: 8px 16px; border-radius: 12px; font-weight: 800; font-size: 12px; }
    .more-btn { background: #268bd2; color: #fdf6e3; border: 1.5px solid #268bd2; padding: 8px 16px; border-radius: 12px; font-weight: 800; font-size: 12px; }
    .back-btn { background: #859900; color: #fdf6e3; border: 1.5px solid #859900; padding: 8px 12px; border-radius: 12px; font-weight: 800; font-size: 14px; }
    .more-view-mobile { flex: 1; padding: 16px; overflow-y: auto; }
    .branch-selector { background: #eee8d5; border-radius: 16px; padding: 20px; margin-top: 16px; }
    .branch-selector h3 { font-size: 18px; font-weight: 800; color: #073642; margin: 0 0 16px 0; }
    .branch-list { display: flex; flex-direction: column; gap: 8px; }
    .branch-item { 
        display: flex; 
        justify-content: space-between; 
        align-items: center; 
        background: #fdf6e3; 
        border: 2px solid #ddd6c1; 
        padding: 16px; 
        border-radius: 12px; 
        cursor: pointer; 
        transition: all 0.2s ease;
    }
    .branch-item:hover { 
        background: #eee8d5; 
        border-color: #268bd2; 
        transform: translateY(-1px);
    }
    .branch-item.active { 
        background: #268bd2; 
        border-color: #268bd2; 
        color: #fdf6e3;
    }
    .branch-name { font-size: 16px; font-weight: 600; }
    .branch-check { font-size: 18px; font-weight: bold; }

    .day-view-mobile { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

    /* НОВАЯ МОБИЛЬНАЯ ПАНЕЛЬ ФИЛЬТРОВ */
    .mobile-filter-bar {
        display: flex; align-items: center; justify-content: space-between;
        padding: 12px 16px; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1;
        gap: 12px;
    }

    .date-chip {
        display: flex; align-items: baseline; gap: 4px;
        background: #fdf6e3; padding: 6px 12px; border-radius: 14px; border: 1px solid #ddd6c1;
    }
    .date-chip .d { font-size: 18px; font-weight: 900; color: #073642; }
    .date-chip .m { font-size: 11px; font-weight: 850; color: #93a1a1; }

    .filter-pills { display: flex; gap: 6px; flex: 1; justify-content: flex-end; }
    .pill {
        display: flex; align-items: center; gap: 6px; padding: 8px 10px;
        background: #fdf6e3; border: 1px solid #ddd6c1; border-radius: 14px;
        cursor: pointer; transition: all 0.2s;
    }
    .pill.active { background: #268bd2; border-color: #268bd2; box-shadow: 0 4px 10px rgba(38, 139, 210, 0.2); }
    .pill .p-icon { font-size: 12px; }
    .pill .p-label { font-size: 9px; font-weight: 900; color: #586e75; }
    .pill.active .p-label { color: white; }

    .mobile-timeline-wrapper { flex: 1; overflow: hidden; position: relative; }

    /* МОДАЛКИ ДЛЯ МОБИЛОК (ОПТИМИЗАЦИЯ ДЛЯ iOS) */
    .modal-backdrop { 
        position: fixed; 
        inset: 0; 
        background: rgba(0, 0, 0, 0.6);
        z-index: 9999; /* Выше всех элементов */
        display: flex; 
        align-items: center; /* Возвращаем в центр */
        justify-content: center; 
        padding: 20px; /* Отступы от краев экрана */
        box-sizing: border-box; 
    }
    
    .modal-content-mobile { 
        width: 100%; 
        max-width: 500px; 
        max-height: 90dvh; /* Чуть уменьшим, чтобы окно "парило" */
        background: #fdf6e3; 
        border-radius: 24px; /* Скругление всех углов */
        display: flex; 
        flex-direction: column; 
        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3); 
        overflow: hidden;
        position: relative;
    }
    
    .modal-header { 
        padding: 16px 20px; 
        background: #eee8d5; 
        border-bottom: 1.5px solid #ddd6c1; 
        display: flex; 
        justify-content: space-between; 
        align-items: center; 
        flex-shrink: 0; 
    }
    
    .modal-header h3 { 
        margin: 0; 
        font-size: 16px; 
        font-weight: 800; 
        color: #073642; 
        line-height: 1.2; 
    }
    
    .close-btn { 
        background: #fdf6e3; 
        border: 1px solid #ddd6c1; 
        width: 32px; 
        height: 32px; 
        border-radius: 50%; 
        flex-shrink: 0; 
    }
    
    .modal-body { 
        flex: 1; 
        overflow-y: auto; 
        overflow-x: hidden;
        -webkit-overflow-scrolling: touch; 
        padding: 0; 
        min-height: 0;
    }
</style>
