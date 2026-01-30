<script>
	import { onMount } from 'svelte';
	import api from '$lib/api.js';
	import { user, token } from '$lib/stores/auth.js';
	import { goto } from '$app/navigation';

	let email = '';
	let password = '';
	let error = '';
	let isLoading = true; // Сначала показываем загрузку, пока проверяем Telegram
	let tg = null;
	let isMiniApp = false;

	onMount(async () => {
		// 1. Инициализация Telegram
		if (window.Telegram && window.Telegram.WebApp) {
			tg = window.Telegram.WebApp;
			tg.ready();
			if (tg.initData && tg.initData !== "") {
				isMiniApp = true;
				// Пробуем авто-вход через Telegram
				const success = await tryTelegramAutoLogin(tg.initData);
				if (success) return; // Если вошли - дальше ничего не делаем
			}
		}

		// 2. Если не в TG или авто-вход не сработал - проверяем старый токен
		const savedToken = localStorage.getItem('token');
		if (savedToken) {
			token.set(savedToken);
			const success = await fetchUserData();
			if (success) return;
		}

		isLoading = false; // Показываем форму входа, если всё остальное не сработало
	});

	async function tryTelegramAutoLogin(initData) {
		try {
			const response = await api.post('/auth/telegram', { initData });
			if (response.data && response.data.token) {
				const newToken = response.data.token;
				localStorage.setItem('token', newToken);
				token.set(newToken);
				return await fetchUserData();
			}
		} catch (e) {
			console.log('Telegram auto-login: account not linked yet');
		}
		return false;
	}

	async function fetchUserData() {
		try {
			const response = await api.get('/auth/me');
			user.set(response.data);

			// Редирект по роли
			if (response.data.role === 'ADMIN') goto('/admin');
			else if (response.data.role === 'MANAGER') goto('/manager');
			else goto('/employee');

			if (tg && isMiniApp) tg.MainButton.hide();
			return true;
		} catch (e) {
			localStorage.removeItem('token');
			return false;
		}
	}

	async function handleLogin() {
		if (!email || !password) {
			error = 'Заполните все поля';
			return;
		}
		isLoading = true;
		error = '';
		if (tg && isMiniApp) tg.MainButton.showProgress();

		try {
			const response = await api.post('/auth/login', {
				email: email.trim(),
				password: password
			});
			if (response.data && response.data.token) {
				const newToken = response.data.token;
				localStorage.setItem('token', newToken);
				token.set(newToken);
				if (tg) tg.HapticFeedback.notificationOccurred('success');
				await fetchUserData();
			}
		} catch (e) {
			error = e.response?.data?.message || 'Неверный email или пароль';
			if (tg) tg.HapticFeedback.notificationOccurred('error');
			isLoading = false;
		} finally {
			if (tg && isMiniApp) tg.MainButton.hideProgress();
		}
	}

	// Настройка кнопки Telegram при показе формы
	$: if (!isLoading && isMiniApp && tg) {
		tg.MainButton.setText('ВОЙТИ В CRM');
		tg.MainButton.onClick(handleLogin);
		tg.MainButton.show();
	}
</script>

<div class="auth-wrapper">
	{#if isLoading}
		<div class="loading-screen">
			<span class="spinner"></span>
			<p>Проверка доступа...</p>
		</div>
	{:else}
		<div class="auth-card">
			<div class="header">
				<div class="logo">999</div>
				<h1>CRM Система</h1>
				<p>Добро пожаловать в вашу панель управления</p>
			</div>

			{#if error}
				<div class="error-box">{error}</div>
			{/if}

			<div class="form">
				<div class="form-group">
					<label for="email">Email</label>
					<input type="email" id="email" bind:value={email} placeholder="name@example.com" />
				</div>

				<div class="form-group">
					<label for="password">Пароль</label>
					<input type="password" id="password" bind:value={password} placeholder="••••••••" />
				</div>

				{#if !isMiniApp}
					<button class="login-btn" on:click={handleLogin}>Войти</button>
				{/if}

				<button class="register-link" on:click={() => goto('/register')}>
					Зарегистрировать компанию
				</button>
			</div>

			<div class="footer">
				© 999 • Версия 1.0.0
			</div>
		</div>
	{/if}
</div>

<style>
	.auth-wrapper { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-color: var(--bg-color); }
	.loading-screen { text-align: center; color: var(--hint-color); }
	.auth-card { width: 100%; max-width: 420px; background: white; padding: 48px; border-radius: 32px; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05); text-align: center; }

	@media (max-width: 480px) {
		.auth-card { max-width: 100%; height: 100vh; border-radius: 0; padding: 24px; display: flex; flex-direction: column; justify-content: center; }
		.auth-wrapper { background-color: white; }
	}

	.logo { font-size: 64px; font-weight: 900; background: var(--primary-gradient); -webkit-background-clip: text; -webkit-text-fill-color: transparent; letter-spacing: -3px; margin-bottom: 12px; }
	h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
	.header p { color: var(--hint-color); font-size: 15px; margin: 12px 0 40px 0; }

	.form-group { margin-bottom: 24px; text-align: left; }
	label { display: block; font-size: 12px; font-weight: 700; color: var(--primary-color); margin-bottom: 10px; text-transform: uppercase; }
	input { width: 100%; padding: 18px; border: 2px solid #f1f5f9; border-radius: 18px; font-size: 16px; background: #f8fafc; box-sizing: border-box; }

	.login-btn { width: 100%; padding: 18px; background: var(--primary-gradient); color: white; border: none; border-radius: 18px; font-size: 17px; font-weight: 700; cursor: pointer; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3); }
	.register-link { background: none; border: none; color: var(--primary-color); font-size: 14px; font-weight: 600; margin-top: 24px; cursor: pointer; }

	.error-box { background-color: #fef2f2; color: var(--error-color); padding: 16px; border-radius: 16px; margin-bottom: 24px; font-size: 14px; }
	.footer { margin-top: 48px; font-size: 13px; color: #94a3b8; }

	.spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; margin-bottom: 16px; }
	@keyframes spin { to { transform: rotate(360deg); } }
</style>
