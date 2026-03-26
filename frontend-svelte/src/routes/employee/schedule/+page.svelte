<script>

    import { onMount } from 'svelte';

    import { employeeService } from '$lib/services/employeeService.js';

    import { fade, slide } from 'svelte/transition';

    import { goto } from '$app/navigation';



    let selectedDate = new Date();

    let appointments = [];

    let isLoading = true;



    // Подгрузка при смене даты

    $: if (selectedDate) {

        loadAppointments();

    }



    onMount(async () => {

        if (window.Telegram && window.Telegram.WebApp) {

            window.Telegram.WebApp.BackButton.show();

            window.Telegram.WebApp.BackButton.onClick(() => goto('/employee'));

        }

        await loadAppointments();

    });



    async function loadAppointments() {

        isLoading = true;

        try {

            appointments = await employeeService.getMyAppointments(selectedDate);

        } catch (e) {

            console.error('Schedule load failed', e);

        } finally {

            isLoading = false;

        }

    }



    function formatTime(isoStr) {

        return new Date(isoStr).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });

    }



    function changeDate(days) {

        const newDate = new Date(selectedDate);

        newDate.setDate(newDate.getDate() + days);

        selectedDate = newDate;

    }

</script>



<div class="schedule-page">

    <header class="date-header">

        <button class="nav-btn" on:click={() => changeDate(-1)}>‹</button>

        <div class="current-date">

            <h2>{selectedDate.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })}</h2>

            <p>{selectedDate.toLocaleDateString('ru-RU', { weekday: 'long' })}</p>

        </div>

        <button class="nav-btn" on:click={() => changeDate(1)}>›</button>

    </header>



    <main class="list-container">

        {#if isLoading}

            <div class="center"><span class="loader"></span></div>

        {:else if appointments.length === 0}

            <div class="empty-state" in:fade>

                <span class="icon">☕</span>

                <p>На этот день записей пока нет.<br>Можно отдохнуть!</p>

            </div>

        {:else}

            <div class="appt-list">

                {#each appointments as appt}

                    <div class="appt-card card" in:slide>

                        <div class="time-box">

                            <span class="start">{formatTime(appt.startTime)}</span>

                            <span class="dur">{appt.durationInMinutes} мин</span>

                        </div>

                        <div class="info-box">

                            <h3>{appt.clientName}</h3>

                            <p class="svc">{appt.service}</p>

                            {#if appt.comment}

                                <div class="comment-badge">💬 {appt.comment}</div>

                            {/if}

                        </div>

                        <div class="status-indicator {appt.status.toLowerCase()}"></div>

                    </div>

                {/each}

            </div>

        {/if}

    </main>

</div>



<style>

    .schedule-page { min-height: 100vh; background: #f8fafc; }



    .date-header { background: white; padding: 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; position: sticky; top: 0; z-index: 100; }

    .nav-btn { background: #f1f5f9; border: none; width: 44px; height: 44px; border-radius: 14px; font-size: 20px; cursor: pointer; color: var(--primary-color); }

    .current-date { text-align: center; }

    .current-date h2 { margin: 0; font-size: 17px; font-weight: 800; color: #0f172a; }

    .current-date p { margin: 0; font-size: 12px; color: var(--primary-color); font-weight: 700; text-transform: uppercase; }



    .list-container { padding: 16px; }

    .appt-list { display: flex; flex-direction: column; gap: 12px; }



    .appt-card { background: white; padding: 16px; border-radius: 24px; border: 1px solid #f1f5f9; display: flex; align-items: center; gap: 16px; position: relative; overflow: hidden; }



    .time-box { display: flex; flex-direction: column; align-items: center; min-width: 60px; padding-right: 16px; border-right: 1px solid #f1f5f9; }

    .start { font-size: 16px; font-weight: 900; color: #0f172a; }

    .dur { font-size: 10px; font-weight: 700; color: #94a3b8; }



    .info-box { flex: 1; }

    .info-box h3 { margin: 0; font-size: 16px; font-weight: 800; color: #1e293b; }

    .svc { margin: 2px 0 0 0; font-size: 13px; color: #64748b; font-weight: 500; }



    .comment-badge { margin-top: 8px; font-size: 11px; background: #f1f5f9; padding: 4px 10px; border-radius: 8px; color: #475569; display: inline-block; }



    .status-indicator { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; }

    .status-indicator.scheduled { background: #64748b; }      // Мягкий синий

    .status-indicator.completed { background: #059669; }      // Мягкий зеленый

    .status-indicator.cancelled { background: #dc2626; }      // Мягкий красный



    .empty-state { text-align: center; padding: 80px 20px; color: #94a3b8; }

    .empty-state .icon { font-size: 48px; display: block; margin-bottom: 16px; }

    .empty-state p { font-weight: 600; line-height: 1.5; }



    .loader { width: 24px; height: 24px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; }

    @keyframes spin { to { transform: rotate(360deg); } }

    .center { display: flex; justify-content: center; padding: 40px; }

</style>

