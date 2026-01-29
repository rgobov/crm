<script>
	import { onMount } from 'svelte';
	import axios from 'axios';

	let email = '';
	let password = '';
	let error = '';
	let isLoading = false;
	let isTelegram = false;

	// Адрес бэкенда (как в Flutter)
	const API_URL = 'https://tryneuro-backend.t6xfbd.easypanel.host/api';

	onMount(() => {
		// Проверяем Telegram WebApp
		if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.initData) {
			isTelegram = true;
			window.Telegram.WebApp.expand();
			console.log('Запущено в Telegram');
		}
	});

	async function handleLogin() {
		if (!email || !password) {
			error = 'Заполните все поля';
			return;
		}

		isLoading = true;
		error = '';

		try {
			const response = await axios.post(`${API_URL}/auth/login`, {
				email: email.trim(),
				password: password
			});

			if (response.data && response.data.token) {
				localStorage.setItem('token', response.data.token);
				alert('Успешный вход!');
				// В будущем здесь будет редирект на расписание
			}
		} catch (e) {
			error = e.response?.data?.message || 'Ошибка входа. Проверьте данные.';
		} finally {
			isLoading = false;
		}
	}
</script>

<div class="login-container">
	<div class="card">
		<h1>999 CRM</h1>
		<p class="subtitle">{isTelegram ? 'Вход через Telegram' : 'Вход в систему'}</p>

		{#if error}
			<div class="error">{error}</div>
		{/if}

		<div class="input-group">
			<label for="email">Email</label>
			<input type="email" id="email" bind:value={email} placeholder="example@mail.com" />
		</div>

		<div class="input-group">
			<label for="password">Пароль</label>
			<input type="password" id="password" bind:value={password} placeholder="••••••••" />
		</div>

		<button on:click={handleLogin} disabled={isLoading}>
			{isLoading ? 'Загрузка...' : 'Войти'}
		</button>

		<div class="footer">
			© 2026 TryNeuro
		</div>
	</div>
</div>

<style>
	:global(body) {
		margin: 0;
		padding: 0;
		font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
		background-color: #f4f7f9;
		display: flex;
		justify-content: center;
		align-items: center;
		height: 100vh;
	}

	.login-container {
		width: 100%;
		max-width: 400px;
		padding: 20px;
	}

	.card {
		background: white;
		padding: 32px;
		border-radius: 24px;
		box-shadow: 0 10px 25px rgba(0,0,0,0.05);
		text-align: center;
	}

	h1 {
		color: #1a73e8;
		margin: 0 0 8px 0;
		font-size: 28px;
	}

	.subtitle {
		color: #5f6368;
		margin-bottom: 32px;
	}

	.input-group {
		text-align: left;
		margin-bottom: 20px;
	}

	label {
		display: block;
		font-size: 14px;
		color: #3c4043;
		margin-bottom: 8px;
		margin-left: 4px;
	}

	input {
		width: 100%;
		padding: 14px;
		border: 1.5px solid #dadce0;
		border-radius: 12px;
		font-size: 16px;
		box-sizing: border-box;
		transition: border-color 0.2s;
	}

	input:focus {
		outline: none;
		border-color: #1a73e8;
	}

	button {
		width: 100%;
		padding: 16px;
		background-color: #1a73e8;
		color: white;
		border: none;
		border-radius: 12px;
		font-size: 16px;
		font-weight: 600;
		cursor: pointer;
		margin-top: 12px;
		transition: background-color 0.2s;
	}

	button:disabled {
		background-color: #ccc;
	}

	.error {
		background-color: #fde8e8;
		color: #c53030;
		padding: 12px;
		border-radius: 8px;
		margin-bottom: 20px;
		font-size: 14px;
	}

	.footer {
		margin-top: 32px;
		font-size: 12px;
		color: #bdc1c6;
	}
</style>
