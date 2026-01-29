<script>
    import { onMount } from 'svelte';
    import api from '$lib/api.js';
    import { goto } from '$app/navigation';

    let companyName = '';
    let adminName = '';
    let email = '';
    let password = '';
    let companyAddress = ''; // Добавлено поле адреса, так как оно есть в DTO
    let error = '';
    let isLoading = false;
    let tg = null;

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/'));
        }
    });

    async function handleRegister() {
        if (!companyName || !adminName || !email || !password) {
            error = 'Пожалуйста, заполните основные поля';
            return;
        }

        isLoading = true;
        error = '';

        try {
            // Исправленный эндпоинт и структура данных
            await api.post('/companies/register', {
                companyName,
                adminName,
                adminEmail: email.trim(),
                adminPassword: password,
                companyAddress: companyAddress || 'Не указан'
            });

            alert('Компания успешно зарегистрирована!');
            goto('/');
        } catch (e) {
            console.error('Registration error:', e);
            error = e.response?.data?.message || 'Ошибка регистрации. Попробуйте другой Email.';
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
            <p>Создайте аккаунт для вашей компании</p>
        </div>

        {#if error}
            <div class="error-box">{error}</div>
        {/if}

        <div class="form">
            <div class="form-group">
                <label for="company">Название компании</label>
                <input type="text" id="company" bind:value={companyName} placeholder="Мой Бизнес" />
            </div>

            <div class="form-group">
                <label for="admin">Ваше имя</label>
                <input type="text" id="admin" bind:value={adminName} placeholder="Иван Иванов" />
            </div>

            <div class="form-group">
                <label for="email">Email администратора</label>
                <input type="email" id="email" bind:value={email} placeholder="admin@company.com" />
            </div>

            <div class="form-group">
                <label for="password">Пароль</label>
                <input type="password" id="password" bind:value={password} placeholder="••••••••" />
            </div>

            <button class="login-btn" on:click={handleRegister} disabled={isLoading}>
                {isLoading ? 'Сборка данных...' : 'Создать компанию'}
            </button>

            <button class="back-link" on:click={() => goto('/')}>
                Вернуться ко входу
            </button>
        </div>
    </div>
</div>

<style>
    .auth-wrapper { width: 100vw; min-height: 100vh; display: flex; justify-content: center; align-items: center; background-color: var(--bg-color); padding: 20px; box-sizing: border-box; }
    .auth-card { width: 100%; max-width: 420px; background: white; padding: 40px; border-radius: 32px; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05); text-align: center; }
    .logo { font-size: 48px; font-weight: 900; background: var(--primary-gradient); -webkit-background-clip: text; -webkit-text-fill-color: transparent; letter-spacing: -2px; margin-bottom: 8px; }
    h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .header p { color: var(--hint-color); font-size: 14px; margin: 8px 0 32px 0; }
    .form-group { margin-bottom: 20px; text-align: left; }
    label { display: block; font-size: 11px; font-weight: 700; color: var(--primary-color); margin-bottom: 8px; text-transform: uppercase; letter-spacing: 1px; }
    input { width: 100%; padding: 14px; border: 2px solid #f1f5f9; border-radius: 16px; font-size: 15px; background: #f8fafc; box-sizing: border-box; }
    .login-btn { width: 100%; padding: 16px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-size: 16px; font-weight: 700; cursor: pointer; margin-top: 10px; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3); }
    .back-link { background: none; border: none; color: var(--hint-color); font-size: 14px; margin-top: 20px; cursor: pointer; }
    .error-box { background-color: #fef2f2; color: var(--error-color); padding: 12px; border-radius: 12px; margin-bottom: 20px; font-size: 13px; border: 1px solid #fee2e2; }
</style>
