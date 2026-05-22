<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { user, logout } from '$lib/stores/auth.js';
	import { selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
	import { clientService } from '$lib/services/clientService.js';
	import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';

	let branches = [];
	let services = [];
	let selectedBranchId = '';
	let staffList = [];

	let showBookingModal = false;
	let showDetailModal = false;
	let currentAppointment = null;
	let bookingForm = {
		service: '',
		comment: '',
		hour: 10,
		min: 0,
		staffId: ''
	};

	let isSubmitting = false;
	let errorMsg = '';
	let successMsg = '';

	// Для нахождения имени сотрудника при показе модальных окон
	let scheduleScreenRef;

	onMount(async () => {
		// Защита роута
		if (!$user) {
			goto('/');
			return;
		}

		// Загружаем филиалы
		try {
			branches = await clientService.getBranches();
			if (branches.length > 0) {
				selectedBranchId = branches[0].id;
				activeBranchId.set(selectedBranchId);
			}

			// Загружаем услуги
			services = await clientService.getServices();
		} catch (e) {
			console.error('Ошибка при инициализации личного кабинета', e);
		}
	});

	// При смене филиала обновляем стор
	$: if (selectedBranchId) {
		activeBranchId.set(selectedBranchId);
	}

	function handlePrevDay() {
		const current = new Date($selectedDate);
		current.setDate(current.getDate() - 1);
		selectedDate.set(current);
	}

	function handleNextDay() {
		const current = new Date($selectedDate);
		current.setDate(current.getDate() + 1);
		selectedDate.set(current);
	}

	function handleToday() {
		selectedDate.set(new Date());
	}

	function formatTime(hour, min) {
		return `${String(hour).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
	}

	function formatDate(date) {
		if (!date) return '';
		return new Date(date).toLocaleDateString('ru-RU', {
			weekday: 'long',
			year: 'numeric',
			month: 'long',
			day: 'numeric'
		});
	}

	// Поиск сотрудника по ID
	function getStaffName(id) {
		if (!id) return 'Мастер';
		// Если компонент ScheduleScreen имеет список сотрудников, мы можем попытаться взять оттуда
		const staffMember = staffList.find(s => s.id === id);
		return staffMember ? staffMember.name : 'Мастер';
	}

	function handleEmptySlotTap(event) {
		errorMsg = '';
		successMsg = '';
		
		const detail = event.detail;
		// Заполняем форму
		bookingForm = {
			service: services.length > 0 ? services[0].name : '',
			comment: '',
			hour: detail.hour,
			min: detail.min,
			staffId: detail.staffId
		};

		// Получаем актуальный список мастеров для правильного отображения имени
		if (scheduleScreenRef) {
			// Напрямую берем список сотрудников, который загрузил дочерний компонент
			// (так как он его вычисляет реактивно)
		}

		showBookingModal = true;
	}

	function handleAppointmentTap(event) {
		const appt = event.detail;
		// Разрешаем просматривать подробности только собственных записей клиента
		if (appt.clientName === 'Ваша запись') {
			currentAppointment = appt;
			showDetailModal = true;
		}
	}

	async function bookAppointment() {
		isSubmitting = true;
		errorMsg = '';

		// Находим длительность выбранной услуги
		const selectedService = services.find(s => s.name === bookingForm.service);
		const duration = selectedService ? selectedService.durationInMinutes : 60;

		// Форматируем startTime в OffsetDateTime
		const start = new Date($selectedDate);
		start.setHours(bookingForm.hour);
		start.setMinutes(bookingForm.min);
		start.setSeconds(0);
		start.setMilliseconds(0);

		const appointmentData = {
			startTime: start.toISOString(),
			durationInMinutes: duration,
			service: bookingForm.service,
			staffMemberId: bookingForm.staffId,
			branchId: selectedBranchId,
			comment: bookingForm.comment
		};

		try {
			await clientService.createAppointment(appointmentData);
			successMsg = 'Запись успешно создана!';
			
			// Перезапускаем расписание
			if (scheduleScreenRef) {
				scheduleScreenRef.handleRefresh();
			}

			setTimeout(() => {
				showBookingModal = false;
			}, 1000);
		} catch (e) {
			errorMsg = e.response?.data?.message || 'Не удалось создать запись. Возможно, это время уже занято.';
		} finally {
			isSubmitting = false;
		}
	}

	async function cancelAppointment() {
		if (!currentAppointment || !currentAppointment.id) return;
		
		isSubmitting = true;
		errorMsg = '';

		try {
			await clientService.deleteAppointment(currentAppointment.id);
			
			// Перезапускаем расписание
			if (scheduleScreenRef) {
				scheduleScreenRef.handleRefresh();
			}

			showDetailModal = false;
		} catch (e) {
			errorMsg = e.response?.data?.message || 'Не удалось отменить запись.';
		} finally {
			isSubmitting = false;
		}
	}

	function handleLogout() {
		logout();
		goto('/');
	}
</script>

<div class="client-portal-container">
	<!-- ВЕРХНЯЯ ШАПКА -->
	<header class="portal-header">
		<div class="header-left">
			<div class="logo-circle">✦</div>
			<div class="user-greeting">
				<h1>Онлайн-запись</h1>
				<span class="user-email">{$user?.email || ''}</span>
			</div>
		</div>

		<div class="header-right">
			<button class="logout-btn" on:click={handleLogout}>Выйти</button>
		</div>
	</header>

	<!-- ПАНЕЛЬ УПРАВЛЕНИЯ ЗАПИСЬЮ (ФИЛИАЛЫ И ДАТА) -->
	<div class="controls-panel">
		<div class="control-group">
			<label class="control-label">ФИЛИАЛ</label>
			<select class="branch-select" bind:value={selectedBranchId}>
				{#each branches as b}
					<option value={b.id}>{b.name}</option>
				{/each}
			</select>
		</div>

		<div class="date-navigator">
			<button class="nav-arrow" on:click={handlePrevDay}>‹</button>
			<div class="date-display" on:click={handleToday}>
				<span class="calendar-icon">📅</span>
				<span class="date-text">{formatDate($selectedDate)}</span>
			</div>
			<button class="nav-arrow" on:click={handleNextDay}>›</button>
		</div>
	</div>

	<!-- ТАЙМЛАЙН -->
	<main class="timeline-wrapper">
		<ScheduleScreen
			bind:this={scheduleScreenRef}
			isClient={true}
			branchId={selectedBranchId}
			on:emptySlotTap={handleEmptySlotTap}
			on:appointmentTap={handleAppointmentTap}
		/>
	</main>

	<!-- МОДАЛЬНОЕ ОКНО СОЗДАНИЯ ЗАПИСИ -->
	{#if showBookingModal}
		<div class="modal-backdrop" on:click|self={() => showBookingModal = false}>
			<div class="modal-card">
				<div class="modal-header">
					<h2>Новая запись</h2>
					<button class="close-btn" on:click={() => showBookingModal = false}>&times;</button>
				</div>

				<div class="modal-body">
					{#if errorMsg}
						<div class="error-banner">{errorMsg}</div>
					{/if}
					{#if successMsg}
						<div class="success-banner">{successMsg}</div>
					{/if}

					<div class="booking-summary-card">
						<div class="summary-item">
							<span class="sum-label">Время:</span>
							<span class="sum-val">{formatTime(bookingForm.hour, bookingForm.min)}</span>
						</div>
						<div class="summary-item">
							<span class="sum-label">Дата:</span>
							<span class="sum-val">{formatDate($selectedDate)}</span>
						</div>
					</div>

					<form on:submit|preventDefault={bookAppointment} class="modal-form">
						<div class="field-group">
							<label for="service-select">Услуга</label>
							<select id="service-select" bind:value={bookingForm.service}>
								{#each services as s}
									<option value={s.name}>{s.name} ({s.durationInMinutes} мин)</option>
								{/each}
							</select>
						</div>

						<div class="field-group">
							<label for="comment-input">Пожелания или комментарий</label>
							<textarea
								id="comment-input"
								bind:value={bookingForm.comment}
								placeholder="Например, особенности маникюра или стрижки..."
								rows="3"
							></textarea>
						</div>

						<button type="submit" class="submit-booking-btn" disabled={isSubmitting || services.length === 0}>
							{#if isSubmitting}
								<span class="spinner"></span>
							{:else}
								Подтвердить запись
							{/if}
						</button>
					</form>
				</div>
			</div>
		</div>
	{/if}

	<!-- МОДАЛЬНОЕ ОКНО ДЕТАЛЕЙ ЗАПИСИ (ОТМЕНА) -->
	{#if showDetailModal}
		<div class="modal-backdrop" on:click|self={() => showDetailModal = false}>
			<div class="modal-card">
				<div class="modal-header">
					<h2>Ваша запись</h2>
					<button class="close-btn" on:click={() => showDetailModal = false}>&times;</button>
				</div>

				<div class="modal-body">
					{#if errorMsg}
						<div class="error-banner">{errorMsg}</div>
					{/if}

					<div class="detail-grid">
						<div class="detail-row">
							<span class="dt-label">Услуга</span>
							<span class="dt-value">{currentAppointment.service}</span>
						</div>
						<div class="detail-row">
							<span class="dt-label">Дата</span>
							<span class="dt-value">{formatDate(currentAppointment.startTime)}</span>
						</div>
						<div class="detail-row">
							<span class="dt-label">Время</span>
							<span class="dt-value">
								{new Date(currentAppointment.startTime).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
							</span>
						</div>
						{#if currentAppointment.comment}
							<div class="detail-row full-width">
								<span class="dt-label">Ваш комментарий</span>
								<p class="dt-comment">{currentAppointment.comment}</p>
							</div>
						{/if}
					</div>

					<button class="cancel-appointment-btn" on:click={cancelAppointment} disabled={isSubmitting}>
						{#if isSubmitting}
							<span class="spinner"></span>
						{:else}
							Отменить эту запись
						{/if}
					</button>
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	.client-portal-container {
		width: 100vw;
		height: 100vh;
		display: flex;
		flex-direction: column;
		background: #fdf6e3;
		overflow: hidden;
		box-sizing: border-box;
	}

	/* ВЕРХНЯЯ ШАПКА */
	.portal-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16px 24px;
		background: #eee8d5;
		border-bottom: 1.5px solid #ddd6c1;
		flex-shrink: 0;
	}

	.header-left {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	.logo-circle {
		width: 38px;
		height: 38px;
		background: linear-gradient(135deg, #3897f0 0%, #0052D4 100%);
		color: white;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 20px;
		font-weight: 800;
	}

	.user-greeting h1 {
		font-size: 16px;
		font-weight: 850;
		color: #073642;
		margin: 0;
	}

	.user-email {
		font-size: 11px;
		color: #93a1a1;
		font-weight: 600;
	}

	.logout-btn {
		background: #fdf6e3;
		border: 1px solid #ddd6c1;
		color: #dc322f;
		padding: 8px 16px;
		border-radius: 10px;
		font-size: 12px;
		font-weight: 750;
		cursor: pointer;
		transition: all 0.2s;
	}

	.logout-btn:hover {
		background: #dc322f;
		color: white;
	}

	/* ПАНЕЛЬ УПРАВЛЕНИЯ (ФИЛИАЛЫ И НАВИГАЦИЯ ДАТЫ) */
	.controls-panel {
		display: flex;
		flex-wrap: wrap;
		gap: 16px;
		align-items: center;
		justify-content: space-between;
		padding: 12px 24px;
		background: #fdf6e3;
		border-bottom: 1px solid #ddd6c1;
		flex-shrink: 0;
	}

	.control-group {
		display: flex;
		flex-direction: column;
		min-width: 200px;
	}

	.control-label {
		font-size: 8px;
		font-weight: 900;
		color: #93a1a1;
		margin-bottom: 4px;
		letter-spacing: 0.5px;
	}

	.branch-select {
		padding: 10px;
		border-radius: 12px;
		border: 1.5px solid #ddd6c1;
		background: #eee8d5;
		color: #073642;
		font-weight: 800;
		font-size: 14px;
		outline: none;
		cursor: pointer;
	}

	.date-navigator {
		display: flex;
		align-items: center;
		gap: 8px;
		background: #eee8d5;
		border: 1.5px solid #ddd6c1;
		padding: 4px;
		border-radius: 14px;
	}

	.nav-arrow {
		width: 32px;
		height: 32px;
		background: #fdf6e3;
		border: 1px solid #ddd6c1;
		border-radius: 10px;
		font-size: 18px;
		font-weight: bold;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #073642;
	}

	.date-display {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 0 16px;
		cursor: pointer;
		user-select: none;
	}

	.date-text {
		font-size: 13px;
		font-weight: 800;
		color: #073642;
	}

	.calendar-icon {
		font-size: 14px;
	}

	/* ТАЙМЛАЙН */
	.timeline-wrapper {
		flex: 1;
		overflow: hidden;
		position: relative;
	}

	/* МОДАЛЬНЫЕ ОКНА (BACKDROP И КАРТОЧКА) */
	.modal-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(7, 54, 66, 0.6);
		backdrop-filter: blur(8px);
		z-index: 9999;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 20px;
		box-sizing: border-box;
	}

	.modal-card {
		width: 100%;
		max-width: 460px;
		background: #fdf6e3;
		border-radius: 24px;
		border: 1.5px solid #ddd6c1;
		box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
		overflow: hidden;
		display: flex;
		flex-direction: column;
		animation: scaleIn 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	@keyframes scaleIn {
		from { transform: scale(0.95); opacity: 0; }
		to { transform: scale(1); opacity: 1; }
	}

	.modal-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16px 24px;
		background: #eee8d5;
		border-bottom: 1.5px solid #ddd6c1;
	}

	.modal-header h2 {
		margin: 0;
		font-size: 18px;
		font-weight: 850;
		color: #073642;
	}

	.close-btn {
		background: none;
		border: none;
		font-size: 24px;
		color: #93a1a1;
		cursor: pointer;
		padding: 0;
		line-height: 1;
	}

	.close-btn:hover {
		color: #dc322f;
	}

	.modal-body {
		padding: 24px;
	}

	.error-banner {
		background: #fef2f2;
		border: 1px solid #fee2e2;
		color: #ef4444;
		padding: 10px 14px;
		border-radius: 10px;
		font-size: 13px;
		margin-bottom: 16px;
	}

	.success-banner {
		background: #f0fdf4;
		border: 1px solid #dcfce7;
		color: #15803d;
		padding: 10px 14px;
		border-radius: 10px;
		font-size: 13px;
		margin-bottom: 16px;
	}

	/* РЕЗЮМЕ ЗАПИСИ В КАРТОЧКЕ */
	.booking-summary-card {
		display: flex;
		gap: 16px;
		background: #eee8d5;
		border: 1px solid #ddd6c1;
		padding: 14px;
		border-radius: 14px;
		margin-bottom: 20px;
	}

	.summary-item {
		display: flex;
		flex-direction: column;
		flex: 1;
	}

	.sum-label {
		font-size: 9px;
		font-weight: 850;
		color: #93a1a1;
		text-transform: uppercase;
		margin-bottom: 2px;
	}

	.sum-val {
		font-size: 14px;
		font-weight: 800;
		color: #073642;
	}

	/* ФОРМА МОДАЛЬНОГО ОКНА */
	.modal-form {
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	.field-group {
		display: flex;
		flex-direction: column;
		text-align: left;
	}

	.field-group label {
		font-size: 11px;
		font-weight: 750;
		color: #586e75;
		margin-bottom: 6px;
	}

	.field-group select,
	.field-group textarea {
		padding: 12px;
		border-radius: 10px;
		border: 1.5px solid #ddd6c1;
		background: white;
		font-size: 14px;
		outline: none;
		box-sizing: border-box;
	}

	.field-group select:focus,
	.field-group textarea:focus {
		border-color: #268bd2;
	}

	.submit-booking-btn {
		width: 100%;
		padding: 14px;
		background: linear-gradient(135deg, #3897f0 0%, #0052D4 100%);
		color: white;
		border: none;
		border-radius: 12px;
		font-size: 15px;
		font-weight: 800;
		cursor: pointer;
		margin-top: 10px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.submit-booking-btn:hover {
		opacity: 0.95;
	}

	/* ДЕТАЛИ И ОТМЕНА */
	.detail-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 16px;
		text-align: left;
		margin-bottom: 24px;
	}

	.detail-row {
		display: flex;
		flex-direction: column;
	}

	.detail-row.full-width {
		grid-column: span 2;
	}

	.dt-label {
		font-size: 10px;
		font-weight: 800;
		color: #93a1a1;
		text-transform: uppercase;
		margin-bottom: 4px;
	}

	.dt-value {
		font-size: 14px;
		font-weight: 750;
		color: #073642;
	}

	.dt-comment {
		font-size: 13px;
		color: #586e75;
		background: #eee8d5;
		border: 1px solid #ddd6c1;
		padding: 12px;
		border-radius: 10px;
		margin: 0;
	}

	.cancel-appointment-btn {
		width: 100%;
		padding: 14px;
		background: #fdf6e3;
		border: 1.5px solid #dc322f;
		color: #dc322f;
		border-radius: 12px;
		font-size: 14px;
		font-weight: 800;
		cursor: pointer;
		transition: all 0.2s;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.cancel-appointment-btn:hover {
		background: #dc322f;
		color: white;
	}

	/* СПИННЕР */
	.spinner {
		width: 18px;
		height: 18px;
		border: 2px solid rgba(255, 255, 255, 0.3);
		border-top-color: white;
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}
</style>
