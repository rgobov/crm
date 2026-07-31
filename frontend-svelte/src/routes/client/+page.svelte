<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { user, logout } from '$lib/stores/auth.js';
	import { selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
	import { clientService } from '$lib/services/clientService.js';
	import { timeUtils } from '$lib/utils/timeUtils.js';
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
		staffId: '',
		resourceId: null
	};
	let rentEndDate = '';
	let rentEndTime = '10:00';

	let isSubmitting = false;
	let errorMsg = '';
	let successMsg = '';
	let dateStr = '';

	$: isRentMode = branches.find(b => b.id === selectedBranchId)?.niche === 'RENT';

	$: if ($selectedDate) {
		const d = $selectedDate;
		dateStr = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
	}

	$: timeStr = formatTime(bookingForm.hour, bookingForm.min);

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
				// Загружаем услуги для первого филиала (с фильтром по нише)
				services = await clientService.getServices(selectedBranchId);
			}
		} catch (e) {
			console.error('Ошибка при инициализации личного кабинета', e);
		}
	});

	// При смене филиала обновляем стор и перезагружаем услуги (фильтр по нише филиала)
	$: if (selectedBranchId) {
		activeBranchId.set(selectedBranchId);
		loadServicesForBranch(selectedBranchId);
	}

	async function loadServicesForBranch(branchId) {
		try {
			services = await clientService.getServices(branchId);
		} catch (e) {
			console.error('Ошибка загрузки услуг для филиала', e);
			services = [];
		}
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
			staffId: detail.staffId,
			resourceId: detail.resourceId || null
		};
		rentEndDate = dateStr;
		rentEndTime = formatTime((detail.hour + 1) % 24, detail.min);

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
		errorMsg = '';

		// Находим длительность выбранной услуги
		const selectedService = services.find(s => s.name === bookingForm.service);
		let duration = selectedService ? selectedService.durationInMinutes : 60;

		// Конвертируем время филиала в UTC
		const currentBranch = branches.find(b => b.id === selectedBranchId);
		const pad = n => String(n).padStart(2, '0');
		const dateStr = timeUtils.toBranchLocalDateStr($selectedDate, currentBranch?.timezone);
		const localDateStr = `${dateStr}T${pad(bookingForm.hour)}:${pad(bookingForm.min)}`;
		const correctedStart = timeUtils.fromBranchLocalToUTC(localDateStr, currentBranch?.timezone) || new Date(localDateStr).toISOString();

		// RENT: срок аренды задаётся датой/временем окончания
		if (isRentMode) {
			const endLocalStr = `${rentEndDate}T${rentEndTime}`;
			duration = Math.round((new Date(endLocalStr) - new Date(localDateStr)) / 60000);
			if (duration < 15) {
				errorMsg = 'Окончание аренды должно быть позже начала (минимум 15 минут)';
				return;
			}
			if (duration > 43200) {
				errorMsg = 'Максимальная длительность аренды — 30 дней';
				return;
			}
		}

		isSubmitting = true;

		const appointmentData = {
			startTime: correctedStart,
			durationInMinutes: duration,
			service: bookingForm.service,
			staffMemberId: bookingForm.staffId,
			resourceId: bookingForm.resourceId,
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
							<span class="sum-label">Дата</span>
							<input type="date" class="sum-input" value={dateStr} on:change={e => {
								const val = e.target.value;
								if (val) {
									const d = new Date(val + 'T12:00:00');
									if (!isNaN(d.getTime())) {
										selectedDate.set(d);
									}
								}
							}} />
						</div>
					<div class="summary-item">
						<span class="sum-label">Время</span>
						<input type="time" class="sum-input" value={timeStr} on:change={e => {
							const val = e.target.value;
							if (val && val.includes(':')) {
								const [h, m] = val.split(':').map(Number);
								if (!isNaN(h) && !isNaN(m)) {
									bookingForm.hour = h;
									bookingForm.min = m;
								}
							}
						}} />
					</div>
					{#if isRentMode}
						<div class="summary-item">
							<span class="sum-label">Окончание аренды</span>
							<input type="date" class="sum-input" bind:value={rentEndDate} />
						</div>
						<div class="summary-item">
							<span class="sum-label">Время окончания</span>
							<input type="time" class="sum-input" bind:value={rentEndTime} />
						</div>
					{/if}
				</div>

					<form on:submit|preventDefault={bookAppointment} class="modal-form">
						<div class="field-group">
							<label>Услуга</label>
							<div class="service-grid">
								{#each services as s}
									<button
										type="button"
										class="service-card"
										class:selected={bookingForm.service === s.name}
										on:click={() => bookingForm.service = s.name}
									>
										<div class="card-icon">✦</div>
										<div class="card-body">
											<span class="card-name">{s.name}</span>
											<span class="card-duration">{s.durationInMinutes} мин</span>
										</div>
										<div class="card-price">
											{#if s.priceMin !== null && s.priceMin !== undefined}
												{s.priceMax !== null && s.priceMax !== undefined
													? `от ${s.priceMin}₽`
													: `${s.priceMin}₽`}
											{/if}
										</div>
									</button>
								{/each}
							</div>
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
						{#if services.find(s => s.name === currentAppointment.service)}
							{@const s = services.find(s => s.name === currentAppointment.service)}
							{#if s.priceMin !== null && s.priceMin !== undefined}
								<div class="detail-row">
									<span class="dt-label">Стоимость</span>
									<span class="dt-value">
										{s.priceMax !== null && s.priceMax !== undefined ? `от ${s.priceMin} до ${s.priceMax}` : s.priceMin} руб.
									</span>
								</div>
							{/if}
						{/if}
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

	.sum-input {
		font-size: 14px;
		font-weight: 800;
		color: #073642;
		background: transparent;
		border: none;
		outline: none;
		padding: 0;
		font-family: inherit;
		cursor: pointer;
		min-height: 20px;
	}

	.sum-input::-webkit-calendar-picker-indicator {
		opacity: 0.5;
		cursor: pointer;
	}

	.sum-input:focus {
		color: #268bd2;
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

	.field-group textarea {
		padding: 12px;
		border-radius: 10px;
		border: 1.5px solid #ddd6c1;
		background: white;
		font-size: 14px;
		outline: none;
		box-sizing: border-box;
	}

	.field-group textarea:focus {
		border-color: #268bd2;
	}

	.service-grid {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.service-card {
		width: 100%;
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 14px;
		background: white;
		border: 1.5px solid #ddd6c1;
		border-radius: 12px;
		cursor: pointer;
		transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
		text-align: left;
		box-sizing: border-box;
		font-family: inherit;
	}

	.service-card:hover {
		border-color: #268bd2;
		transform: translateX(4px);
	}

	.service-card.selected {
		border-color: #268bd2;
		background: #f0f9ff;
		box-shadow: 0 0 0 2px rgba(38, 139, 210, 0.15);
	}

	.service-card:active {
		transform: scale(0.98);
	}

	.card-icon {
		width: 36px;
		height: 36px;
		background: #fff7ed;
		color: #f59e0b;
		border-radius: 10px;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		font-size: 16px;
	}

	.card-body {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.card-name {
		font-size: 14px;
		font-weight: 750;
		color: #0f172a;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.card-duration {
		font-size: 11px;
		font-weight: 600;
		color: #94a3b8;
	}

	.card-price {
		font-size: 15px;
		font-weight: 850;
		color: #073642;
		white-space: nowrap;
		flex-shrink: 0;
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
