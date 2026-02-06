<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import AppointmentEditScreen from '$lib/components/schedule/AppointmentEditScreen.svelte';
    import AppointmentDetailScreen from '$lib/components/schedule/AppointmentDetailScreen.svelte';
    import { activeTab, selectedDate } from '$lib/stores/dashboardStore.js';
    import { fade, scale } from 'svelte/transition';

    export let forcedDate = null;

    let viewMode = 'month'; // 'month', 'day'
    let showModal = null;   // null, 'edit', 'detail'

    let currentAppointment = null;
    let preselectedData = null;

    // Реактивность: Следим за датой и вкладкой
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

    function openDetail(event) {
        currentAppointment = event.detail;
        showModal = 'detail';
    }

    function closeModal() {
        showModal = null;
    }

    function handleSaved() {
        closeModal();
        // Можно добавить уведомление
    }
</script>

<div class="calendar-tab-root">
    {#if viewMode === 'month'}
        <div class="month-view" in:fade>
            <div class="header-row">
                <h2>Календарь</h2>
                <button class="today-btn" on:click={() => { selectedDate.set(new Date()); viewMode = 'day'; activeTab.set('timeline'); }}>СЕГОДНЯ</button>
            </div>
            <CalendarScreen on:dateSelected={handleDateSelected} />
        </div>

    {:else if viewMode === 'day'}
        <div class="day-view-wrapper" in:fade>
            <div class="day-top-bar">
                <div class="date-info">
                    <span class="d">{$selectedDate.getDate()}</span>
                    <span class="m">{$selectedDate.toLocaleDateString('ru-RU', { month: 'long' })}</span>
                </div>
                <button class="btn-add" on:click={() => openNewAppointment({ detail: {} })}>+ Новая запись</button>
            </div>

            <div class="timeline-container">
                <ScheduleScreen
                    on:emptySlotTap={openNewAppointment}
                    on:appointmentTap={openDetail}
                />
            </div>
        </div>
    {/if}

    <!-- МОДАЛЬНОЕ ОКНО (SPA СТИЛЬ) -->
    {#if showModal}
        <div class="modal-backdrop" on:click|self={closeModal} transition:fade={{duration: 200}}>
            <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
                <header class="modal-header">
                    <h3>{showModal === 'edit' ? (currentAppointment ? 'Редактирование' : 'Новая запись') : 'Детали визита'}</h3>
                    <button class="close-btn" on:click={closeModal}>✕</button>
                </header>

                <div class="modal-body-scroll">
                    {#if showModal === 'edit'}
                        <AppointmentEditScreen
                            appointment={currentAppointment}
                            preselected={preselectedData}
                            on:cancel={closeModal}
                            on:saved={handleSaved}
                        />
                    {:else if showModal === 'detail'}
                        <AppointmentDetailScreen
                            appointment={currentAppointment}
                            on:edit={(e) => { currentAppointment = e.detail; showModal = 'edit'; }}
                            on:deleted={closeModal}
                        />
                    {/if}
                </div>
            </div>
        </div>
    {/if}
</div>

<style>
    .calendar-tab-root { height: 100%; display: flex; flex-direction: column; background: white; position: relative; overflow: hidden; }

    .month-view { padding: 24px; flex: 1; overflow-y: auto; }
    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    h2 { font-size: 24px; font-weight: 800; margin: 0; }
    .today-btn { background: var(--primary-gradient); color: white; border: none; padding: 10px 20px; border-radius: 14px; font-weight: 700; cursor: pointer; }

    .day-view-wrapper { flex: 1; display: flex; flex-direction: column; height: 100%; overflow: hidden; }
    .day-top-bar { padding: 16px 24px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
    .date-info { display: flex; align-items: baseline; gap: 8px; }
    .date-info .d { font-size: 24px; font-weight: 900; color: var(--primary-color); }
    .date-info .m { font-size: 14px; font-weight: 700; color: #64748b; text-transform: uppercase; }
    .btn-add { background: #eff6ff; color: var(--primary-color); border: none; padding: 10px 20px; border-radius: 12px; font-weight: 800; cursor: pointer; }

    .timeline-container { flex: 1; overflow: hidden; position: relative; }

    /* МОДАЛКА */
    .modal-backdrop {
        position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6);
        backdrop-filter: blur(4px); z-index: 2000;
        display: flex; align-items: center; justify-content: center; padding: 20px;
    }
    .modal-content {
        background: white; width: 100%; max-width: 550px; height: 85vh;
        border-radius: 32px; display: flex; flex-direction: column;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); overflow: hidden;
    }
    .modal-header { padding: 24px 32px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; }
    .modal-header h3 { margin: 0; font-size: 18px; font-weight: 800; }
    .close-btn { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; }
    .modal-body-scroll { flex: 1; overflow-y: auto; background: #f8fafc; }

    @media (max-width: 640px) {
        .modal-backdrop { padding: 0; }
        .modal-content { height: 100vh; border-radius: 0; }
    }
</style>
