<script>
    import { onMount } from 'svelte';
    import { employeeService } from '$lib/services/employeeService.js';
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import { user } from '$lib/stores/auth.js';

    let profile = null;
    let appointments = [];
    let isLoading = true;

    onMount(async () => {
        try {
            const [profileData, apptsData] = await Promise.all([
                employeeService.getMyProfile(),
                employeeService.getMyAppointments()
            ]);
            profile = profileData;
            appointments = apptsData;
        } catch (e) {
            console.error('Failed to load employee data', e);
        } finally {
            isLoading = false;
        }
    });

    function formatTime(isoStr) {
        return new Date(isoStr).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    }
</script>

<div class="employee-home">
    <div class="welcome-section">
        <h1>Привет, {$user?.name || 'Мастер'}! 👋</h1>
        <p>Удачного рабочего дня</p>
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="stats-card card">
            <div class="stat-item">
                <span class="value">{appointments.length}</span>
                <span class="label">Записей сегодня</span>
            </div>
            <div class="divider"></div>
            <div class="stat-item">
                <span class="value">{profile?.workStartTime || '--:--'}</span>
                <span class="label">Начало смены</span>
            </div>
        </div>

        <section class="section">
            <h3>Ваш график загрузки</h3>
            <CalendarScreen isEmployeeMode={true} />
        </section>

        <section class="section">
            <h3>Ближайшие записи</h3>
            {#if appointments.length === 0}
                <div class="empty-state card">На сегодня записей пока нет</div>
            {:else}
                <div class="appts-list">
                    {#each appointments as appt}
                        <div class="appt-card card">
                            <div class="time">{formatTime(appt.startTime)}</div>
                            <div class="info">
                                <h4>{appt.clientName}</h4>
                                <p>{appt.service}</p>
                            </div>
                            <div class="status-dot {appt.status.toLowerCase()}"></div>
                        </div>
                    {/each}
                </div>
            {/if}
        </section>
    {/if}
</div>

<style>
    .employee-home { padding: 20px; max-width: 500px; margin: 0 auto; padding-bottom: 80px; }
    .welcome-section { margin-bottom: 24px; }
    h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    p { color: var(--hint-color); margin: 4px 0 0 0; }

    .stats-card { display: flex; padding: 24px; justify-content: space-around; background: var(--primary-gradient); color: white; }
    .stat-item { text-align: center; }
    .stat-item .value { display: block; font-size: 24px; font-weight: 800; }
    .stat-item .label { font-size: 11px; font-weight: 600; opacity: 0.8; text-transform: uppercase; }
    .divider { width: 1px; background: rgba(255,255,255,0.2); }

    .section { margin-top: 32px; }
    .section h3 { font-size: 14px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 16px; }

    .appt-card { display: flex; align-items: center; padding: 16px; gap: 16px; margin-bottom: 10px; background: white; border-radius: 20px; }
    .appt-card .time { font-weight: 800; color: var(--primary-color); font-size: 15px; width: 50px; }
    .appt-card .info { flex: 1; }
    .appt-card h4 { margin: 0; font-size: 15px; font-weight: 700; color: #1e293b; }
    .appt-card p { margin: 2px 0 0 0; font-size: 12px; color: var(--hint-color); }

    .status-dot { width: 8px; height: 8px; border-radius: 50%; }
    .scheduled { background: #3b82f6; }
    .confirmed { background: #10b981; }

    .empty-state { text-align: center; padding: 32px; color: var(--hint-color); font-size: 14px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { text-align: center; padding: 40px; }
</style>
