<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import AppointmentEditMobile from '$lib/components/schedule/AppointmentEditMobile.svelte';
    import AppointmentDetailMobile from '$lib/components/schedule/AppointmentDetailMobile.svelte';
    import AddContactModal from '$lib/components/admin/AddContactModal.svelte';
    import ContactDetailScreen from '$lib/components/contacts/ContactDetailScreen.svelte';
    import ShiftEditScreen from '$lib/components/employee/ShiftEditScreen.svelte';
    import ResourceEditScreen from '$lib/components/resources/ResourceEditScreen.svelte';
    import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { activeNiche } from '$lib/stores/nicheStore.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import { fade, scale } from 'svelte/transition';
    import { portal } from '$lib/actions/portal.js';

    export let forcedDate = null;

    let viewMode = 'day';
    let showModal = null;
    let showNestedAddContact = false;
    let selectedClientId = null;
    let selectedStaffForShift = null;

    let currentAppointment = null;
    let selectedResource = null;
    let preselectedData = null;
    let appointmentEditRef;
    let scheduleScreenRef;

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
    } else if ($activeTab === 'management') {
        // Если мы на вкладке "Главная", но компонент CalendarTab (который переиспользуется для timeline)
        // активен, нам нужно убедиться, что он в режиме дня, если пришел forcedDate,
        // иначе пусть остается в последнем состоянии.
        if (forcedDate) viewMode = 'day';
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
            staffId: event?.detail?.staffId || null,
            resourceId: event?.detail?.resourceId || null
        };
        currentAppointment = null;
        showModal = 'edit';
    }

    function handleStaffTap(event) {
        selectedStaffForShift = event.detail;
        showModal = 'shift';
    }

    async function handleResourceTap(event) {
        const resource = await resourceService.getResourceById(event.detail.id);
        if (resource) {
            selectedResource = resource;
            showModal = 'resource';
        }
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
        selectedResource = null;
        if (scheduleScreenRef && typeof scheduleScreenRef.handleRefresh === 'function') {
            scheduleScreenRef.handleRefresh();
        }
    }

    // RENT: после сохранения с изменённой датой «перепрыгиваем» на дату записи.
    function handleSaved(event) {
        const startTime = event?.detail?.startTime;
        if ($activeNiche === 'RENT' && startTime) {
            const branch = $branchStore.find(b => b.id === $activeBranchId);
            const dayStr = timeUtils.toBranchLocalDateStr(startTime, branch?.timezone);
            if (dayStr) selectedDate.set(new Date(dayStr + 'T12:00:00'));
        }
        closeModal();
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

    let isExporting = false;
    async function exportCurrentMonth() {
        if (isExporting) return;
        isExporting = true;
        try {
            const year = $selectedDate.getFullYear();
            const month = $selectedDate.getMonth();
            const start = new Date(year, month, 1);
            const end = new Date(year, month + 1, 0);

            const startStr = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}-01`;
            const endStr = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')}`;

            const blob = await adminService.exportAppointments(startStr, endStr);
            const url = window.URL.createObjectURL(new Blob([blob]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `visits_month_${year}_${month + 1}.xlsx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (e) {
            console.error('Export month failed', e);
        } finally {
            isExporting = false;
        }
    }

    async function exportCurrentDay() {
        if (isExporting) return;
        isExporting = true;
        try {
            const dateStr = `${$selectedDate.getFullYear()}-${String($selectedDate.getMonth() + 1).padStart(2, '0')}-${String($selectedDate.getDate()).padStart(2, '0')}`;
            const blob = await adminService.exportAppointments(dateStr, dateStr);
            const url = window.URL.createObjectURL(new Blob([blob]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `visits_day_${dateStr}.xlsx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (e) {
            console.error('Export day failed', e);
        } finally {
            isExporting = false;
        }
    }
</script>

<div class="calendar-tab-mobile">
    {#if viewMode === 'month'}
        <div class="month-view-mobile" in:fade>
            <div class="header-row">
                <h2>Календарь</h2>
                <div class="header-actions">
                    <button class="export-cal-btn" class:loading={isExporting} on:click={exportCurrentMonth} title="Выгрузить месяц в Excel" disabled={isExporting}>
                        {isExporting ? '⏳' : '📥'}
                    </button>
                    <button class="today-btn" on:click={() => { selectedDate.set(new Date()); viewMode = 'day'; activeTab.set('timeline'); }}>СЕГОДНЯ</button>
                    <button class="more-btn" on:click={() => viewMode = 'more'}>ЕЩЁ</button>
                </div>
            </div>
            <CalendarScreen on:dateSelected={handleDateSelected} />
        </div>
    {:else if viewMode === 'day'}
        {#if viewMode === 'day'}
        <header class="mobile-filter-bar">
            <!-- ДАТА ТЕПЕРЬ СЛЕВА -->
            <button class="date-chip btn-reset" on:click={() => selectedDate.set(new Date())}>
                <span class="d">{$selectedDate.getDate()}</span>
                <span class="m">{$selectedDate.toLocaleDateString('ru-RU', { month: 'short' }).toUpperCase()}</span>
            </button>
            <button class="export-cal-btn-mobile btn-reset" class:loading={isExporting} on:click={exportCurrentDay} title="Выгрузить день в Excel" disabled={isExporting}>
                {isExporting ? '⏳' : '📥'}
            </button>

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

        <div class="day-view-mobile" in:fade>
            <div class="mobile-timeline-wrapper">
                <ScheduleScreen
                    bind:this={scheduleScreenRef}
                    branchId={$activeBranchId}
                    {onlyBusyStaff}
                    {onlyWorkingStaff}
                    on:emptySlotTap={openNewAppointment}
                    on:appointmentTap={openDetail}
                    on:staffTap={handleStaffTap}
                    on:resourceTap={handleResourceTap}
                />
            </div>
        </div>
        {/if}
    {:else if viewMode === 'more'}
        <div class="more-view-mobile" in:fade>
            <div class="header-row">
                <h2>Ещё</h2>
                <button class="back-btn" on:click={() => viewMode = 'month'}>←</button>
            </div>
            <div class="branch-selector">
                <h3>Выбор филиала</h3>
                <div class="branch-list">
                    {#each $branchStore as branch}
                        <button
                            class="branch-item"
                            class:active={$activeBranchId === branch.id}
                            on:click={() => selectBranch(branch.id)}
                        >
                            <span class="branch-name">{branch.name}</span>
                            {#if $activeBranchId === branch.id}
                                <span class="branch-check">✓</span>
                            {/if}
                        </button>
                    {:else}
                        <div class="empty-state-msg">
                            <p>Филиалы не найдены</p>
                        </div>
                    {/each}
                </div>
            </div>
        </div>
    {/if}

    <!-- МОДАЛКИ — вынесены в <body> через portal для корректной работы z-index на iOS.
         Проблема: .mobile-bottom-ui имеет backdrop-filter, что создаёт новый Stacking Context
         на WebKit/iOS. Любой z-index внутри дочернего контекста не может превысить родительский.
         Portal переносит модалку напрямую в <body>, минуя всю иерархию контекстов. -->
    {#if showModal}
        <div class="modal-backdrop" use:portal on:mousedown|self={closeModal} transition:fade={{duration: 200}}>
            <div class="modal-content-mobile" transition:scale={{start: 0.95, duration: 200}}>
                <header class="modal-header">
                    <h3>
                        {#if showModal === 'edit'}
                            {currentAppointment ? 'Редактирование записи' : 'Создание записи'}
                        {:else if showModal === 'detail'} {$activeNiche === 'RENT' ? 'Детали аренды' : 'Детали визита'}
                        {:else if showModal === 'client-profile'} Карточка клиента
                        {:else if showModal === 'shift'} График работы
                        {/if}
                    </h3>
                    <button class="close-btn" on:click={closeModal}>✕</button>
                </header>
                <div class="modal-body">
                    {#if showModal === 'edit'}
                        <AppointmentEditMobile bind:this={appointmentEditRef} appointment={currentAppointment} preselected={preselectedData} on:cancel={closeModal} on:saved={handleSaved} on:open-add-contact-modal={() => showNestedAddContact = true} />
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

    {#if showModal === 'resource'}
        <div use:portal>
            <ResourceEditScreen resource={selectedResource} on:cancel={closeModal} on:success={closeModal} on:photoUpdated={closeModal} />
        </div>
    {/if}

    {#if showNestedAddContact}
        <div use:portal>
            <AddContactModal on:close={() => showNestedAddContact = false} on:success={handleContactAdded} />
        </div>
    {/if}
</div>

<style>
    .calendar-tab-mobile { height: 100%; width: 100%; background: #fdf6e3; display: flex; flex-direction: column; overflow: hidden; }

    .month-view-mobile { flex: 1; padding: 16px; overflow-y: auto; }
    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header-actions { display: flex; gap: 8px; align-items: center; }
    h2 { font-size: 20px; font-weight: 850; color: #073642; margin: 0; }
    .today-btn { background: #eee8d5; color: #268bd2; border: 1.5px solid #ddd6c1; padding: 8px 16px; border-radius: 12px; font-weight: 800; font-size: 12px; }
    .export-cal-btn { background: #eee8d5; color: #268bd2; border: 1.5px solid #ddd6c1; width: 34px; height: 34px; border-radius: 12px; font-size: 14px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; }
    .export-cal-btn:active { transform: scale(0.92); }
    .export-cal-btn-mobile { background: #fdf6e3; color: #268bd2; border: 1.5px solid #ddd6c1; width: 36px; height: 36px; border-radius: 14px; font-size: 16px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; }
    .export-cal-btn-mobile:active { transform: scale(0.92); }
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

    /* НОВАЯ МОБИЛЬНАЯ ПАНЕЛЬ ФИЛЬТРОВ - sibling к day-view-mobile */
    .mobile-filter-bar {
        display: flex; align-items: center; justify-content: space-between;
        padding: 12px 16px; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1;
        gap: 12px; flex-shrink: 0;
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        z-index: 1000;
    }

    .day-view-mobile { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding-top: 60px; }

    .date-chip {
        display: flex; align-items: baseline; gap: 4px;
        background: #fdf6e3; padding: 6px 12px; border-radius: 14px; border: 1px solid #ddd6c1;
        cursor: pointer;
        transition: all 0.2s;
    }
    .date-chip:hover {
        background: #eee8d5;
        border-color: #268bd2;
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

    /*
     * МОДАЛКИ: стили объявлены через :global() потому что use:portal
     * физически переносит элементы в <body>, они выходят из скоупа
     * компонента и Svelte не может применить к ним scoped-стили.
     *
     * z-index: 99999 теперь работает корректно на iOS, т.к. элемент
     * находится напрямую в <body> — вне любых вложенных Stacking Context'ов.
     */

    :global(.modal-backdrop) {
        position: fixed;
        inset: 0;
        background: rgba(7, 54, 66, 0.8);
        /* z-index максимальный — теперь безопасно, т.к. нет родительского контекста */
        z-index: 99999;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        /* safe-area-inset-top: учитываем notch/Dynamic Island на iPhone */
        padding-top: max(40px, calc(env(safe-area-inset-top, 20px) + 20px));
        box-sizing: border-box;
    }

    :global(.modal-content-mobile) {
        width: 100%;
        max-width: 480px;
        height: calc(100dvh - max(40px, calc(env(safe-area-inset-top, 20px) + 20px)) - 20px);
        background: #fdf6e3;
        border-radius: 24px;
        display: flex;
        flex-direction: column;
        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
        overflow: hidden;
    }

    :global(.modal-backdrop .modal-header) {
        padding: 16px 20px;
        background: #eee8d5;
        border-bottom: 1.5px solid #ddd6c1;
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-shrink: 0;
    }

    :global(.modal-backdrop .modal-header h3) {
        margin: 0;
        font-size: 18px;
        font-weight: 800;
        color: #073642;
        line-height: 1.2;
    }

    :global(.modal-backdrop .close-btn) {
        background: #fdf6e3;
        border: 1px solid #ddd6c1;
        width: 36px;
        height: 36px;
        border-radius: 50%;
        flex-shrink: 0;
        font-size: 18px;
        cursor: pointer;
        transition: all 0.2s;
    }

    :global(.modal-backdrop .close-btn:hover) {
        background: #eee8d5;
        transform: scale(1.1);
    }

    :global(.modal-backdrop .modal-body) {
        flex: 1;
        overflow-y: auto;
        overflow-x: hidden;
        -webkit-overflow-scrolling: touch;
        padding: 0;
        /* safe-area-inset-bottom: учитываем home indicator на iPhone */
        padding-bottom: env(safe-area-inset-bottom, 20px);
        min-height: 0;
        /* предотвращаем "резиновый" скролл, уходящий на фон */
        overscroll-behavior: contain;
    }
</style>
