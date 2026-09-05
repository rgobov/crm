<script>
    import { onMount } from 'svelte';
    import api from '$lib/api.js';
    import { user, token } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import { FeedbackUtils } from '$lib/utils/feedback.js';
    import ConsentCheckbox from '$lib/components/ConsentCheckbox.svelte';

    let companyName = '';
    let adminName = '';
    let email = '';
    let password = '';
    let confirmPassword = '';
    let showPassword = false;
    let showConfirmPassword = false;
    let passwordInputEl;
    let confirmPasswordInputEl;

    $: if (passwordInputEl) passwordInputEl.type = showPassword ? 'text' : 'password';
    $: if (confirmPasswordInputEl) confirmPasswordInputEl.type = showConfirmPassword ? 'text' : 'password';

    let companyAddress = '';
    let error = '';
    let isLoading = false;
    let agreedToPolicy = false;
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
        if (!companyName || !adminName || !email || !password || !confirmPassword) {
            error = 'Заполните все поля';
            FeedbackUtils.error();
            return;
        }

        if (!agreedToPolicy) {
            error = 'Необходимо согласие на обработку персональных данных';
            FeedbackUtils.error();
            return;
        }

        if (password !== confirmPassword) {
            error = 'Пароли не совпадают';
            FeedbackUtils.error();
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
                companyAddress: companyAddress || 'Не указан',
                agreedToPolicy
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
                localStorage.setItem('user', JSON.stringify(userResponse.data));

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
                <div class="password-container">
                    <input bind:this={passwordInputEl} type="password" id="password" bind:value={password} placeholder="••••••••" />
                    <button type="button" class="toggle-password-btn" on:click={() => showPassword = !showPassword} aria-label={showPassword ? "Скрыть пароль" : "Показать пароль"}>
                        {#if showPassword}
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                <line x1="1" y1="1" x2="23" y2="23"></line>
                            </svg>
                        {:else}
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                        {/if}
                    </button>
                </div>
            </div>

            <div class="form-group">
                <label for="confirmPassword">Подтвердите пароль</label>
                <div class="password-container">
                    <input bind:this={confirmPasswordInputEl} type="password" id="confirmPassword" bind:value={confirmPassword} placeholder="••••••••" />
                    <button type="button" class="toggle-password-btn" on:click={() => showConfirmPassword = !showConfirmPassword} aria-label={showConfirmPassword ? "Скрыть пароль" : "Показать пароль"}>
                        {#if showConfirmPassword}
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                <line x1="1" y1="1" x2="23" y2="23"></line>
                            </svg>
                        {:else}
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                        {/if}
                    </button>
                </div>
            </div>

            <ConsentCheckbox bind:agreed={agreedToPolicy} disabled={isLoading} />

            <button class="login-btn" on:click={handleRegister} disabled={isLoading}>
                {isLoading ? 'Создание...' : 'Создать и войти'}
            </button>

            <button class="back-link" on:click={() => goto('/')}>
                Уже есть аккаунт? Войти
            </button>
            <div class="privacy-footer">
                <a href="/privacy">Политика конфиденциальности</a>
            </div>
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
    .password-container { position: relative; display: flex; align-items: center; width: 100%; }
    .password-container input { padding-right: 48px; }
    .toggle-password-btn { position: absolute; right: 16px; background: none; border: none; color: #94a3b8; cursor: pointer; padding: 0; display: flex; align-items: center; justify-content: center; transition: color 0.2s; }
    .toggle-password-btn:hover { color: #475569; }
    .login-btn { width: 100%; padding: 18px; background: var(--primary-gradient); color: white; border: none; border-radius: 18px; font-size: 16px; font-weight: 700; cursor: pointer; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3); }
    .back-link { background: none; border: none; color: var(--hint-color); font-size: 14px; margin-top: 24px; cursor: pointer; }
    .privacy-footer { margin-top: 12px; font-size: 13px; }
    .privacy-footer a { color: #3897f0; }
    .error-box { background-color: #fef2f2; color: var(--error-color); padding: 12px; border-radius: 12px; margin-bottom: 20px; font-size: 13px; }
</style>
