<script>
    import { onMount } from 'svelte';
    import axios from 'axios';

    let email = '';
    let password = '';
    let error = '';
    let isLoading = false;

    // Конфигурация бэкенда (как в AppConfig.dart)
    const API_URL = 'https://tryneuro-backend.t6xfbd.easypanel.host/api';

    onMount(() => {
        // Инициализация Telegram SDK если мы внутри него
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.ready();
            window.Telegram.WebApp.expand();
        }
    });

    async function handleLogin() {
        error = '';
        isLoading = true;
        try {
            const response = await axios.post(`${API_URL}/auth/login`, {
                email,
                password
            });

            const { token, tenantId } = response.data;
            localStorage.setItem('token', token);
            localStorage.setItem('tenantId', tenantId);

            // После успешного входа перенаправим на расписание
            alert('Успешный вход! Скоро здесь будет таймлайн.');

        } catch (err) {
            error = 'Неверный email или пароль';
            console.error(err);
        } finally {
            isLoading = false;
        }
    }
</script>

<main class="login-container">
    <div class="card">
        <h1>999 CRM</h1>
        <p class="subtitle">Добро пожаловать!</p>

        <form on:submit|preventDefault={handleLogin}>
            <div class="input-group">
                <label for="email">Email</label>
                <input
                    type="email"
                    id="email"
                    bind:value={email}
                    placeholder="example@mail.ru"
                    required
                />
            </div>

            <div class="input-group">
                <label for="password">Пароль</label>
                <input
                    type="password"
                    id="password"
                    bind:value={password}
                    placeholder="••••••"
                    required
                />
            </div>

            {#if error}
                <p class="error">{error}</p>
            {/if}

            <button type="submit" disabled={isLoading}>
                {#if isLoading} Вход... {:else} Войти {/if}
            </button>
        </form>
    </div>
</main>

<style>
    :global(body) {
        margin: 0;
        padding: 0;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
        background-color: var(--tg-theme-bg-color, #f4f4f9);
        color: var(--tg-theme-text-color, #222);
    }

    .login-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-height: 100vh;
        padding: 20px;
    }

    .card {
        background: var(--tg-theme-secondary-bg-color, #ffffff);
        padding: 32px;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        width: 100%;
        max-width: 400px;
        text-align: center;
    }

    h1 { margin: 0; font-size: 28px; color: var(--tg-theme-button-color, #2481cc); }
    .subtitle { margin-top: 8px; color: #666; font-size: 16px; }

    form { margin-top: 32px; text-align: left; }

    .input-group { margin-bottom: 20px; }
    label { display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; }

    input {
        width: 100%;
        padding: 12px;
        border: 1px solid #ddd;
        border-radius: 12px;
        font-size: 16px;
        box-sizing: border-box;
        /* Использование цветов Telegram */
        background: var(--tg-theme-bg-color, #fff);
        color: var(--tg-theme-text-color, #000);
    }

    input:focus {
        outline: none;
        border-color: var(--tg-theme-button-color, #2481cc);
    }

    button {
        width: 100%;
        padding: 14px;
        background-color: var(--tg-theme-button-color, #2481cc);
        color: var(--tg-theme-button-text-color, #ffffff);
        border: none;
        border-radius: 12px;
        font-size: 16px;
        font-weight: bold;
        cursor: pointer;
        margin-top: 12px;
    }

    button:disabled { opacity: 0.6; }

    .error { color: #e53935; font-size: 14px; margin-bottom: 16px; text-align: center; }
</style>
