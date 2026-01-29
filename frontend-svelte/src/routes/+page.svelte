<script>
	import { onMount } from 'svelte';
	import axios from 'axios';

	let email = '';
	let password = '';
	let error = '';
	let isLoading = false;
	let tg = null;

	const API_URL = 'https://api.109.248.203.156.sslip.io/api';

	onMount(() => {
		if (window.Telegram && window.Telegram.WebApp) {
			tg = window.Telegram.WebApp;
			tg.MainButton.setText('ВОЙТИ В CRM');
			tg.MainButton.onClick(handleLogin);
			tg.MainButton.show();
		}
	});

	async function handleLogin() {
		if (!email || !password) {
			error = 'Заполните все поля';
			if (tg) tg.HapticFeedback.notificationOccurred('error');
			return;
		}

		isLoading = true;
		error = '';
		if (tg) tg.MainButton.showProgress();

		try {
			const response = await axios.post(`${API_URL}/auth/login`, {
				email: email.trim(),
				password: password
			});

			if (response.data && response.data.token) {
				localStorage.setItem('token', response.data.token);
				if (tg) {
					tg.HapticFeedback.notificationOccurred('success');
					tg.MainButton.hide();
				}
				alert('Успешный вход!');
			}
		} catch (e) {
			error = e.response?.data?.message || 'Ошибка входа';
			if (tg) tg.HapticFeedback.notificationOccurred('error');
		} finally {
			isLoading = false;
			if (tg) tg.MainButton.hideProgress();
		}
	}
</script>

<div class="auth-wrapper">
	<div class="auth-card">
		<div class="header">
			<div class="logo">999</div>
			<h1>CRM Система</h1>
			<p>Вход в панель управления</p>
		</div>

		{#if error}
			<div class="error-box">{error}</div>
		{/if}

		<div class="form">
			<div class="form-group">
				<label for="email">Email</label>
				<input type="email" id="email" bind:value={email} placeholder="example@mail.com" />
			</div>

			<div class="form-group">
				<label for="password">Пароль</label>
				<input type="password" id="password" bind:value={password} placeholder="••••••••" />
			</div>

			{#if !tg}
				<button class="login-btn" on:click={handleLogin} disabled={isLoading}>
					{isLoading ? 'Загрузка...' : 'Войти'}
				</button>
			{/if}
		</div>

		<div class="footer">
			© 999
		</div>
	</div>
</div>

<style>
	/* Обертка на весь экран для центрирования */
	.auth-wrapper {
		width: 100vw;
		height: 100vh;
		display: flex;
		justify-content: center;
		align-items: center;
		background-color: var(--bg-color);
	}

	/* Само модальное окно */
	.auth-card {
		width: 100%;
		max-width: 400px;
		background: white;
		padding: 40px;
		border-radius: 28px;
		box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
		box-sizing: border-box;
		text-align: center;
	}

	/* Дизайн для мобилок (чтобы в ТГ выглядело нативно) */
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
		font-size: 52px;
		font-weight: 900;
		color: var(--primary-color);
		letter-spacing: -2px;
		margin-bottom: 8px;
	}

	h1 {
		font-size: 22px;
		font-weight: 700;
		margin: 0;
		color: #1a1a1a;
	}

	.header p {
		color: var(--hint-color);
		font-size: 15px;
		margin: 8px 0 32px 0;
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
		margin-bottom: 8px;
		margin-left: 4px;
		text-transform: uppercase;
		letter-spacing: 0.8px;
	}

	input {
		width: 100%;
		padding: 16px;
		border: 2px solid #f0f2f5;
		border-radius: 16px;
		font-size: 16px;
		background: #f8f9fb;
		box-sizing: border-box;
		transition: all 0.2s ease;
	}

	input:focus {
		outline: none;
		border-color: var(--primary-color);
		background: white;
		box-shadow: 0 0 0 4px rgba(0, 136, 204, 0.1);
	}

	.login-btn {
		width: 100%;
		padding: 18px;
		background-color: var(--primary-color);
		color: white;
		border: none;
		border-radius: 16px;
		font-size: 16px;
		font-weight: 700;
		cursor: pointer;
		margin-top: 8px;
		box-shadow: 0 8px 20px rgba(0, 136, 204, 0.2);
		transition: transform 0.1s;
	}

	.login-btn:active {
		transform: scale(0.98);
	}

	.error-box {
		background-color: #fff1f0;
		color: var(--error-color);
		padding: 14px;
		border-radius: 14px;
		margin-bottom: 24px;
		font-size: 14px;
		border: 1px solid #ffa39e;
	}

	.footer {
		margin-top: 40px;
		font-size: 13px;
		color: #bdc1c6;
	}
</style>
