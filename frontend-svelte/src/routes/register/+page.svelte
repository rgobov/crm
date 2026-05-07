<script>
    import { onMount } from 'svelte';
    import api from '$lib/api.js';
    import { user, token } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import { FeedbackUtils } from '$lib/utils/feedback.js';

    let companyName = '';
    let adminName = '';
    let email = '';
    let password = '';
    let companyAddress = '';
    let error = '';
    let isLoading = false;
    let tg = null;

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            if (tg.BackButton) {
                tg.BackButton.show();
                tg.BackButton.onClick(() => goto('/'));
            }
        }
    });

    async function handleRegister() {
        if (!companyName || !adminName || !email || !password) {
            error = 'Заполните все поля';
            FeedbackUtils.error(); // Вспышка + вибрация при ошибке валидации
            return;
        }

        isLoading = true;
        error = '';

        try {
            // 1. Регистрация
            await api.post('/companies/register', {
                companyName,
                adminName,
                adminEmail: email.trim(),
                adminPassword: password,
                companyAddress: companyAddress || 'Не указан'
            });

            // 2. АВТОМАТИЧЕСКИЙ ВХОД (как вы просили для бесшовности)
            const loginResponse = await api.post('/auth/login', {
                email: email.trim(),
                password: password
            });

            if (loginResponse.data && loginResponse.data.token) {
                const newToken = loginResponse.data.token;
                localStorage.setItem('token', newToken);
                token.set(newToken);

                // Получаем данные пользователя
                const userResponse = await api.get('/auth/me');
                user.set(userResponse.data);

                FeedbackUtils.success(); // Вспышка + вибрация при успехе

                // Сразу в админку!
                goto('/admin');
            }
        } catch (e) {
            console.error('Registration failed:', e);
            error = e.response?.data?.message || 'Ошибка регистрации. Проверьте данные.';
            FeedbackUtils.error(); // Вспышка + вибрация при ошибке
        } finally {
            isLoading = false;
        }
    }
</script>

<div class="auth-wrapper">
    <div class="auth-card">
        <div class="header">
            <div class="logo">999</div>
            <h1>Регистрация</h1>
            <p>Создайте аккаунт вашей компании</p>
        </div>

        {#if error}
            <div class="error-box">{error}</div>
        {/if}

        <div class="form">
            <div class="form-group">
                <label for="company">Название компании</label>
                <input type="text" id="company" bind:value={companyName} placeholder="Напр: Салон Красоты" />
            </div>

            <div class="form-group">
                <label for="admin">Ваше имя</label>
                <input type="text" id="admin" bind:value={adminName} placeholder="Александр" />
            </div>

            <div class="form-group">
                <label for="email">Email администратора</label>
                <input type="email" id="email" bind:value={email} placeholder="admin@mail.com" />
            </div>

            <div class="form-group">
                <label for="password">Пароль</label>
                <input type="password" id="password" bind:value={password} placeholder="••••••••" />
            </div>

            <button class="login-btn" on:click={handleRegister} disabled={isLoading}>
                {isLoading ? 'Создание...' : 'Создать и войти'}
            </button>

            <button class="back-link" on:click={() => goto('/')}>
                Уже есть аккаунт? Войти
            </button>
        </div>
    </div>
</div>

<style>
    .auth-wrapper { width: 100vw; min-height: 100vh; display: flex; justify-content: center; align-items: center; background-color: var(--bg-color); padding: 20px; box-sizing: border-box; }
    .auth-card { width: 100%; max-width: 420px; background: white; padding: 40px; border-radius: 32px; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05); text-align: center; }
    .logo { font-size: 48px; font-weight: 900; background: var(--primary-gradient); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 12px; }
    h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .header p { color: var(--hint-color); font-size: 14px; margin: 12px 0 32px 0; }
    .form-group { margin-bottom: 20px; text-align: left; }
    label { display: block; font-size: 11px; font-weight: 700; color: var(--primary-color); margin-bottom: 8px; text-transform: uppercase; }
    input { width: 100%; padding: 16px; border: 2px solid #f1f5f9; border-radius: 16px; font-size: 16px; background: #f8fafc; box-sizing: border-box; }
    .login-btn { width: 100%; padding: 18px; background: var(--primary-gradient); color: white; border: none; border-radius: 18px; font-size: 16px; font-weight: 700; cursor: pointer; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3); }
    .back-link { background: none; border: none; color: var(--hint-color); font-size: 14px; margin-top: 24px; cursor: pointer; }
    .error-box { background-color: #fef2f2; color: var(--error-color); padding: 12px; border-radius: 12px; margin-bottom: 20px; font-size: 13px; }
</style>
