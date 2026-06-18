<script>
	import { onMount } from 'svelte';
	import api from '$lib/api.js';
	import { user, token } from '$lib/stores/auth.js';
	import { goto } from '$app/navigation';

	let email = '';
	let password = '';
	let showPassword = false;
	let error = '';
	let isLoading = true;
	let tg = null;
	let isMiniApp = false;
	let emailInput;
	let passwordInputEl;

	$: if (passwordInputEl) passwordInputEl.type = showPassword ? 'text' : 'password';

	onMount(async () => {
		if (typeof window !== 'undefined') {
			const urlParams = new URLSearchParams(window.location.search);
			const tenantIdParam = urlParams.get('tenantId');
			if (tenantIdParam) {
				localStorage.setItem('bookingTenantId', tenantIdParam);
			}
		}

		if (window.Telegram && window.Telegram.WebApp) {
			tg = window.Telegram.WebApp;
			tg.ready();
			if (tg.initData && tg.initData !== "") {
				isMiniApp = true;
				const success = await tryTelegramAutoLogin(tg.initData);
				if (success) return;
			}
		}

		const savedToken = localStorage.getItem('token');
		if (savedToken) {
			token.set(savedToken);
			const success = await fetchUserData();
			if (success) return;
		}

		isLoading = false;

		// ПРИНУДИТЕЛЬНЫЙ ФОКУС ДЛЯ ЭМУЛЯТОРА
		setTimeout(() => {
			if (emailInput) emailInput.focus();
		}, 500);
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
			localStorage.setItem('user', JSON.stringify(response.data));
			if (response.data.role === 'ADMIN') goto('/admin');
			else if (response.data.role === 'MANAGER') goto('/manager');
			else if (response.data.role === 'CLIENT') goto('/client');
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
		try {
			const response = await api.post('/auth/login', {
				email: email.trim(),
				password: password
			});
			if (response.data && response.data.token) {
				const newToken = response.data.token;
				localStorage.setItem('token', newToken);
				token.set(newToken);
				await fetchUserData();
			}
		} catch (e) {
			error = e.response?.data?.message || 'Неверный email или пароль';
			isLoading = false;
		}
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
				<p>Добро пожаловать</p>
			</div>

			{#if error}
				<div class="error-box">{error}</div>
			{/if}

			<form on:submit|preventDefault={handleLogin} class="form">
				<div class="form-group">
					<label for="email">Email</label>
					<input
						bind:this={emailInput}
						type="email"
						id="email"
						bind:value={email}
						placeholder="name@example.com"
						inputmode="email"
						required
					/>
				</div>

				<div class="form-group">
					<label for="password">Пароль</label>
					<div class="password-container">
						<input
							bind:this={passwordInputEl}
							type="password"
							id="password"
							bind:value={password}
							placeholder="••••••••"
							required
						/>
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

				<button type="submit" class="login-btn">Войти</button>

				<button type="button" class="register-link" on:click={() => goto('/register')}>
					Зарегистрировать компанию
				</button>
			</form>

			<div class="footer">
				© 999 • Версия 1.0.0
			</div>
		</div>
	{/if}
</div>

<style>
	.auth-wrapper { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-color: #f8fafc; }
	.loading-screen { text-align: center; }
	.auth-card { width: 100%; max-width: 420px; background: white; padding: 48px; border-radius: 32px; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05); text-align: center; }
	.logo { font-size: 64px; font-weight: 900; background: linear-gradient(135deg, #3897f0 0%, #0052D4 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 12px; }
	.form-group { margin-bottom: 24px; text-align: left; }
	label { display: block; font-size: 12px; font-weight: 700; color: #3897f0; margin-bottom: 10px; }
	input { width: 100%; padding: 18px; border: 2px solid #f1f5f9; border-radius: 18px; font-size: 16px; background: #f8fafc; box-sizing: border-box; }
	input:focus { border-color: #3897f0; background: white; outline: none; }
	.password-container { position: relative; display: flex; align-items: center; width: 100%; }
	.password-container input { padding-right: 48px; }
	.toggle-password-btn { position: absolute; right: 16px; background: none; border: none; color: #94a3b8; cursor: pointer; padding: 0; display: flex; align-items: center; justify-content: center; transition: color 0.2s; }
	.toggle-password-btn:hover { color: #475569; }
	.login-btn { width: 100%; padding: 18px; background: linear-gradient(135deg, #3897f0 0%, #0052D4 100%); color: white; border: none; border-radius: 18px; font-size: 17px; font-weight: 700; cursor: pointer; }
	.register-link { background: none; border: none; color: #3897f0; font-size: 14px; margin-top: 24px; cursor: pointer; }
	.spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: #3897f0; border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
	@keyframes spin { to { transform: rotate(360deg); } }
</style>
