<script>
	import { onMount } from 'svelte';
	import api from '$lib/api.js';
	import { user, token } from '$lib/stores/auth.js';
	import { goto } from '$app/navigation';

	let email = '';
	let password = '';
	let error = '';
	let isLoading = false;
	let tg = null;
	let isMiniApp = false; // Отдельный флаг для проверки нахождения в Telegram

	onMount(async () => {
		if (window.Telegram && window.Telegram.WebApp) {
			tg = window.Telegram.WebApp;
			tg.ready();

			// Проверяем, действительно ли мы в Telegram (есть данные пользователя)
			if (tg.initData && tg.initData !== "") {
				isMiniApp = true;
				tg.MainButton.setText('ВОЙТИ В CRM');
				tg.MainButton.onClick(handleLogin);
				tg.MainButton.show();
			}
		}

		const savedToken = localStorage.getItem('token');
		if (savedToken) {
			token.set(savedToken);
			await fetchUserData();
		}
	});

	async function fetchUserData() {
		try {
			const response = await api.get('/auth/me');
			user.set(response.data);
			if (response.data.role === 'ADMIN') goto('/admin');
			else if (response.data.role === 'MANAGER') goto('/manager');
			else goto('/employee');

			if (tg && isMiniApp) tg.MainButton.hide();
		} catch (e) {
			console.error('Session expired');
			localStorage.removeItem('token');
		}
	}

	async function handleLogin() {
		if (!email || !password) {
			error = 'Заполните все поля';
			if (tg) tg.HapticFeedback.notificationOccurred('error');
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
		} finally {
			isLoading = false;
			if (tg && isMiniApp) tg.MainButton.hideProgress();
		}
	}
</script>

<div class="auth-wrapper">
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
				<input
					type="email"
					id="email"
					bind:value={email}
					placeholder="name@example.com"
					disabled={isLoading}
				/>
			</div>

			<div class="form-group">
				<label for="password">Пароль</label>
				<input
					type="password"
					id="password"
					bind:value={password}
					placeholder="••••••••"
					disabled={isLoading}
				/>
			</div>

			<!-- Кнопка теперь показывается, если это НЕ Telegram Mini App -->
			{#if !isMiniApp}
				<button class="login-btn" on:click={handleLogin} disabled={isLoading}>
					{#if isLoading}
						<span class="spinner"></span>
					{:else}
						Войти
					{/if}
				</button>
			{/if}
		</div>

		<div class="footer">
			© 999 • Версия 1.0.0
		</div>
	</div>
</div>

<style>
	/* Стили остаются без изменений */
	.auth-wrapper {
		width: 100vw;
		height: 100vh;
		display: flex;
		justify-content: center;
		align-items: center;
		background-color: var(--bg-color);
	}

	.auth-card {
		width: 100%;
		max-width: 420px;
		background: white;
		padding: 48px;
		border-radius: 32px;
		box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05);
		box-sizing: border-box;
		text-align: center;
	}

	@media (max-width: 480px) {
		.auth-card {
			max-width: 100%;
			height: 100vh;
			border-radius: 0;
			box-shadow: none;
			padding: 24px;
			display: flex;
			flex-direction: column;
			justify-content: center;
		}
		.auth-wrapper {
			background-color: white;
		}
	}

	.logo {
		font-size: 64px;
		font-weight: 900;
		background: var(--primary-gradient);
		-webkit-background-clip: text;
		-webkit-text-fill-color: transparent;
		letter-spacing: -3px;
		margin-bottom: 12px;
	}

	h1 {
		font-size: 24px;
		font-weight: 800;
		margin: 0;
		color: #0f172a;
	}

	.header p {
		color: var(--hint-color);
		font-size: 15px;
		margin: 12px 0 40px 0;
		line-height: 1.5;
	}

	.form-group {
		margin-bottom: 24px;
		text-align: left;
	}

	label {
		display: block;
		font-size: 12px;
		font-weight: 700;
		color: var(--primary-color);
		margin-bottom: 10px;
		margin-left: 4px;
		text-transform: uppercase;
		letter-spacing: 1px;
	}

	input {
		width: 100%;
		padding: 18px;
		border: 2px solid #f1f5f9;
		border-radius: 18px;
		font-size: 16px;
		background: #f8fafc;
		box-sizing: border-box;
		transition: all 0.25s ease;
	}

	input:focus {
		outline: none;
		border-color: var(--primary-color);
		background: white;
		box-shadow: 0 0 0 5px rgba(56, 151, 240, 0.1);
	}

	.login-btn {
		width: 100%;
		padding: 18px;
		background: var(--primary-gradient);
		color: white;
		border: none;
		border-radius: 18px;
		font-size: 17px;
		font-weight: 700;
		cursor: pointer;
		margin-top: 12px;
		box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3);
		transition: all 0.2s;
		display: flex;
		justify-content: center;
		align-items: center;
	}

	.login-btn:hover {
		transform: translateY(-2px);
		box-shadow: 0 15px 30px rgba(56, 151, 240, 0.4);
	}

	.login-btn:active {
		transform: translateY(0);
	}

	.error-box {
		background-color: #fef2f2;
		color: var(--error-color);
		padding: 16px;
		border-radius: 16px;
		margin-bottom: 24px;
		font-size: 14px;
		font-weight: 500;
		border: 1px solid #fee2e2;
	}

	.footer {
		margin-top: 48px;
		font-size: 13px;
		color: #94a3b8;
		font-weight: 500;
	}

	.spinner {
		width: 24px;
		height: 24px;
		border: 3px solid rgba(255,255,255,0.3);
		border-radius: 50%;
		border-top-color: white;
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}
</style>
