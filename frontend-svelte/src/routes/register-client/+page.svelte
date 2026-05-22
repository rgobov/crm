<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import api from '$lib/api.js';
	import { token, user } from '$lib/stores/auth.js';

	let tenantId = '';
	let companyName = '';
	let email = '';
	let name = '';
	let phone = '';
	let password = '';
	let confirmPassword = '';

	let isLoading = false;
	let error = '';
	let successMsg = '';

	onMount(async () => {
		// 1. Получаем tenantId из строки запроса или localStorage
		const urlParams = new URLSearchParams(window.location.search);
		tenantId = urlParams.get('tenantId') || localStorage.getItem('bookingTenantId') || '';

		if (!tenantId) {
			error = 'Ссылка недействительна: отсутствует ID компании. Пожалуйста, запросите правильную ссылку у компании.';
			return;
		}

		// Сохраняем на всякий случай
		localStorage.setItem('bookingTenantId', tenantId);

		// 2. Пытаемся получить информацию о компании, чтобы показать название
		try {
			const response = await api.get(`/companies/${tenantId}`);
			if (response.data && response.data.name) {
				companyName = response.data.name;
			}
		} catch (e) {
			console.error('Не удалось загрузить название компании', e);
		}
	});

	async function handleRegister() {
		error = '';
		successMsg = '';

		if (!name || !phone || !email || !password || !confirmPassword) {
			error = 'Пожалуйста, заполните все обязательные поля';
			return;
		}

		if (password !== confirmPassword) {
			error = 'Пароли не совпадают';
			return;
		}

		isLoading = true;

		try {
			// 1. Отправляем запрос на регистрацию
			await api.post('/auth/register-client', {
				email: email.trim().toLowerCase(),
				password,
				name: name.trim(),
				phone: phone.trim(),
				tenantId
			});

			successMsg = 'Регистрация успешна! Входим...';

			// 2. Автоматический вход
			const loginRes = await api.post('/auth/login', {
				email: email.trim().toLowerCase(),
				password
			});

			if (loginRes.data && loginRes.data.token) {
				const newToken = loginRes.data.token;
				localStorage.setItem('token', newToken);
				token.set(newToken);

				// Получаем данные текущего юзера
				const meRes = await api.get('/auth/me');
				user.set(meRes.data);
				localStorage.setItem('user', JSON.stringify(meRes.data));

				// Перенаправляем в личный кабинет
				setTimeout(() => {
					goto('/client');
				}, 1000);
			} else {
				goto('/');
			}
		} catch (e) {
			error = e.response?.data?.message || 'Ошибка регистрации. Возможно, этот email уже занят.';
			isLoading = false;
		}
	}
</script>

<div class="register-wrapper">
	<div class="register-card">
		<div class="header">
			<div class="logo">✦</div>
			<h1>Онлайн-запись</h1>
			{#if companyName}
				<p class="company-badge">{companyName}</p>
			{:else}
				<p>Регистрация нового клиента</p>
			{/if}
		</div>

		{#if error}
			<div class="error-box">{error}</div>
		{/if}

		{#if successMsg}
			<div class="success-box">{successMsg}</div>
		{/if}

		{#if tenantId}
			<form on:submit|preventDefault={handleRegister} class="form">
				<div class="form-group">
					<label for="name">Ваше имя *</label>
					<input
						type="text"
						id="name"
						bind:value={name}
						placeholder="Иван Иванов"
						required
					/>
				</div>

				<div class="form-group">
					<label for="phone">Номер телефона *</label>
					<input
						type="tel"
						id="phone"
						bind:value={phone}
						placeholder="+7 (999) 999-99-99"
						required
					/>
				</div>

				<div class="form-group">
					<label for="email">Email *</label>
					<input
						type="email"
						id="email"
						bind:value={email}
						placeholder="name@example.com"
						required
					/>
				</div>

				<div class="form-group">
					<label for="password">Пароль *</label>
					<input
						type="password"
						id="password"
						bind:value={password}
						placeholder="Минимум 6 символов"
						required
					/>
				</div>

				<div class="form-group">
					<label for="confirmPassword">Подтвердите пароль *</label>
					<input
						type="password"
						id="confirmPassword"
						bind:value={confirmPassword}
						placeholder="••••••••"
						required
					/>
				</div>

				<button type="submit" class="register-btn" disabled={isLoading}>
					{#if isLoading}
						<span class="spinner"></span>
					{:else}
						Зарегистрироваться
					{/if}
				</button>
			</form>
		{/if}

		<div class="footer-links">
			Уже есть аккаунт? <button class="login-link" on:click={() => goto('/')}>Войти</button>
		</div>
	</div>
</div>

<style>
	.register-wrapper {
		width: 100vw;
		min-height: 100vh;
		display: flex;
		justify-content: center;
		align-items: center;
		background: radial-gradient(circle at 10% 20%, rgb(240, 245, 255) 0%, rgb(228, 235, 252) 100%);
		padding: 20px;
		box-sizing: border-box;
	}

	.register-card {
		width: 100%;
		max-width: 440px;
		background: rgba(255, 255, 255, 0.85);
		backdrop-filter: blur(20px);
		padding: 40px;
		border-radius: 30px;
		box-shadow: 0 20px 60px rgba(0, 0, 0, 0.05);
		border: 1px solid rgba(255, 255, 255, 0.5);
		text-align: center;
		box-sizing: border-box;
	}

	.logo {
		font-size: 40px;
		background: linear-gradient(135deg, #3897f0 0%, #0052D4 100%);
		-webkit-background-clip: text;
		-webkit-text-fill-color: transparent;
		margin-bottom: 8px;
		display: inline-block;
	}

	.header h1 {
		font-size: 24px;
		font-weight: 800;
		color: #1e293b;
		margin: 0 0 6px 0;
	}

	.company-badge {
		display: inline-block;
		padding: 6px 16px;
		background: rgba(56, 151, 240, 0.1);
		color: #2563eb;
		border-radius: 20px;
		font-size: 14px;
		font-weight: 700;
		margin: 4px 0 20px 0;
	}

	.form {
		margin-top: 24px;
	}

	.form-group {
		margin-bottom: 18px;
		text-align: left;
	}

	label {
		display: block;
		font-size: 12px;
		font-weight: 700;
		color: #64748b;
		margin-bottom: 6px;
		text-transform: uppercase;
		letter-spacing: 0.5px;
	}

	input {
		width: 100%;
		padding: 14px 16px;
		border: 2px solid #e2e8f0;
		border-radius: 12px;
		font-size: 15px;
		background: #f8fafc;
		box-sizing: border-box;
		transition: all 0.2s;
	}

	input:focus {
		border-color: #2563eb;
		background: white;
		outline: none;
		box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
	}

	.register-btn {
		width: 100%;
		padding: 16px;
		background: linear-gradient(135deg, #3897f0 0%, #0052D4 100%);
		color: white;
		border: none;
		border-radius: 12px;
		font-size: 16px;
		font-weight: 700;
		cursor: pointer;
		margin-top: 12px;
		box-sizing: border-box;
		display: flex;
		justify-content: center;
		align-items: center;
		transition: opacity 0.2s;
	}

	.register-btn:hover {
		opacity: 0.95;
	}

	.register-btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.error-box {
		background: #fef2f2;
		border: 1px solid #fee2e2;
		color: #ef4444;
		padding: 12px;
		border-radius: 12px;
		font-size: 14px;
		margin-bottom: 20px;
		text-align: left;
	}

	.success-box {
		background: #f0fdf4;
		border: 1px solid #dcfce7;
		color: #15803d;
		padding: 12px;
		border-radius: 12px;
		font-size: 14px;
		margin-bottom: 20px;
	}

	.footer-links {
		margin-top: 24px;
		font-size: 14px;
		color: #64748b;
	}

	.login-link {
		background: none;
		border: none;
		color: #2563eb;
		font-weight: 700;
		cursor: pointer;
		padding: 0;
		font-size: 14px;
	}

	.login-link:hover {
		text-decoration: underline;
	}

	.spinner {
		width: 20px;
		height: 20px;
		border: 2px solid rgba(255, 255, 255, 0.3);
		border-top-color: white;
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}
</style>
