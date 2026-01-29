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
			// Настраиваем главную кнопку Telegram
			tg.MainButton.setText('ВОЙТИ В CRM');
			tg.MainButton.onClick(handleLogin);
			tg.MainButton.show();
		}
	});

	async function handleLogin() {
		if (!email || !password) {
			error = 'Заполните Email и Пароль';
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
				// Скоро здесь будет переход к расписанию
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

<div class="page">
	<div class="header">
		<div class="logo">999</div>
		<h1>CRM Система</h1>
		<p>Управление вашим бизнесом</p>
	</div>

	<div class="card">
		{#if error}
			<div class="error-box">{error}</div>
		{/if}

		<div class="form-group">
			<label for="email">Электронная почта</label>
			<input type="email" id="email" bind:value={email} placeholder="name@company.com" />
		</div>

		<div class="form-group">
			<label for="password">Пароль</label>
			<input type="password" id="password" bind:value={password} placeholder="••••••••" />
		</div>

		{#if !tg}
			<button class="login-btn" on:click={handleLogin} disabled={isLoading}>
				{isLoading ? 'Вход...' : 'Войти в систему'}
			</button>
		{/if}
	</div>

	<div class="info">
		<p>Если вы зашли через Telegram, кнопка входа появится внизу экрана</p>
	</div>
</div>

<style>
	.page {
		padding: 24px;
		display: flex;
		flex-direction: column;
		min-height: 90vh;
	}

	.header {
		text-align: center;
		margin-bottom: 32px;
		margin-top: 20px;
	}

	.logo {
		font-size: 48px;
		font-weight: 900;
		color: var(--primary-color);
		letter-spacing: -2px;
		margin-bottom: 8px;
	}

	h1 {
		font-size: 24px;
		font-weight: 700;
		margin: 0;
	}

	.header p {
		color: var(--hint-color);
		margin: 4px 0 0 0;
	}

	.form-group {
		margin-bottom: 20px;
	}

	label {
		display: block;
		font-size: 13px;
		font-weight: 600;
		color: var(--primary-color);
		margin-bottom: 8px;
		margin-left: 4px;
		text-transform: uppercase;
		letter-spacing: 0.5px;
	}

	input {
		width: 100%;
		padding: 16px;
		border: 2px solid #eef0f2;
		border-radius: 16px;
		font-size: 16px;
		background: #f8f9fb;
		box-sizing: border-box;
		transition: all 0.2s;
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
		margin-top: 10px;
		box-shadow: 0 8px 16px rgba(0, 136, 204, 0.2);
	}

	.error-box {
		background-color: #fff1f0;
		color: var(--error-color);
		padding: 14px;
		border-radius: 12px;
		margin-bottom: 24px;
		font-size: 14px;
		border: 1px solid #ffa39e;
		text-align: center;
	}

	.info {
		margin-top: auto;
		text-align: center;
		padding: 20px;
	}

	.info p {
		color: var(--hint-color);
		font-size: 13px;
		line-height: 1.5;
	}
</style>
