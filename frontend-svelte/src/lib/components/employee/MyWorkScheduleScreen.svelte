<script>
    import { onMount } from 'svelte';
    import { employeeService } from '$lib/services/employeeService.js';
    import { goto } from '$app/navigation';

    let shifts = [];
    let isLoading = true;
    let isSaving = false;

    onMount(async () => {
        await loadShifts();
    });

    async function loadShifts() {
        isLoading = true;
        try {
            // Загружаем смены на ближайшую неделю
            const profile = await employeeService.getMyProfile();
            // В реальности здесь будет запрос списка смен
            shifts = [];
        } catch (e) {
            console.error('Failed to load shifts');
        } finally {
            isLoading = false;
        }
    }

    async function saveShift(shift) {
        isSaving = true;
        try {
            // Сохранение смены
            alert('График обновлен');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="schedule-screen">
    <div class="header">
        <h1>Мой график</h1>
        <p>Настройка рабочих смен и перерывов</p>
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="info-card card">
            <span class="icon">ℹ️</span>
            <p>Ваш стандартный график настраивается администратором. Здесь вы можете изменить смены на конкретные дни.</p>
        </div>

        <div class="empty">
            <span class="icon">📅</span>
            <p>Функционал настройки индивидуальных смен в разработке.</p>
            <button class="back-btn" on:click={() => goto('/employee')}>Вернуться на главную</button>
        </div>
    {/if}
</div>

<style>
    .schedule-screen { padding: 20px; max-width: 500px; margin: 0 auto; }
    h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    p { color: #64748b; margin: 4px 0 0 0; font-size: 14px; }

    .info-card { display: flex; gap: 12px; padding: 16px; background: #eff6ff; border: 1px solid #bfdbfe; margin-top: 24px; }
    .info-card p { color: #1e40af; font-size: 13px; line-height: 1.4; }

    .empty { text-align: center; padding: 60px 20px; color: #94a3b8; }
    .empty .icon { font-size: 48px; display: block; margin-bottom: 16px; }
    .back-btn { background: var(--primary-gradient); color: white; border: none; padding: 12px 24px; border-radius: 14px; font-weight: 700; margin-top: 24px; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
