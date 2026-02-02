<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { contactService } from '$lib/services/contactService.js';
    import { serviceService } from '$lib/services/serviceService.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';

    export let appointment = null; // Если передано - режим редактирования
    export let preselected = { date: new Date(), hour: 10, min: 0, staffId: null };

    const dispatch = createEventDispatcher();
    const isEditing = !!appointment;

    let formData = {
        startTime: '',
        durationInMinutes: 60,
        contactId: '',
        clientName: '',
        service: '',
        staffMemberId: '',
        resourceId: ''
    };

    let searchPhone = '';
    let selectedContact = null;
    let staffList = [];
    let services = [];
    let resources = [];
    let isLoading = true;
    let isSaving = false;
    let debounceTimer;

    onMount(async () => {
        await loadInitialData();
    });

    async function loadInitialData() {
        isLoading = true;
        try {
            // 1. Загружаем справочники
            const [servicesData, resourcesData, staffData] = await Promise.all([
                serviceService.getServices(),
                resourceService.getResources(),
                adminService.getStaffForSchedule(isEditing ? new Date(appointment.startTime) : preselected.date)
            ]);

            services = servicesData;
            resources = resourcesData;
            staffList = staffData.filter(s => s.role === 'EMPLOYEE');

            // 2. Инициализируем форму
            if (isEditing) {
                formData = { ...appointment };
                searchPhone = appointment.contactId ? 'Загрузка...' : '';
                if (appointment.contactId) {
                    const contact = await contactService.getContactById(appointment.contactId);
                    if (contact) {
                        selectedContact = contact;
                        searchPhone = phoneUtils.format(contact.phones[0]);
                    }
                }
            } else {
                // Предзаполнение из клика по таймлайну
                formData.staffMemberId = preselected.staffId || '';
                const d = new Date(preselected.date);
                d.setHours(preselected.hour, preselected.min, 0, 0);
                formData.startTime = d.toISOString();
            }
        } catch (e) {
            console.error('Data load failed', e);
        } finally {
            isLoading = false;
        }
    }

    // Умный поиск клиента по телефону (Синхронно с Flutter)
    $: if (searchPhone.replace(/\D/g, '').length >= 6 && !selectedContact) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(async () => {
            const digits = phoneUtils.clean(searchPhone);
            const contact = await contactService.findContactByPhone(digits);
            if (contact) {
                selectedContact = contact;
                formData.contactId = contact.id;
                formData.clientName = contact.name;
                searchPhone = phoneUtils.format(contact.phones[0]);
            }
        }, 800);
    }

    async function handleSave() {
        if (!selectedContact) return alert('Выберите или создайте клиента');
        if (!formData.service) return alert('Выберите услугу');

        isSaving = true;
        try {
            const dataToSave = {
                ...formData,
                clientName: selectedContact.name,
                contactId: selectedContact.id
            };

            if (isEditing) {
                await adminService.updateAppointment(appointment.id, dataToSave);
            } else {
                await adminService.createAppointment(dataToSave);
            }
            dispatch('saved');
        } catch (e) {
            if (e.response?.status === 409) {
                alert('Конфликт времени: этот слот уже занят другим мастером или ресурсом');
            } else {
                alert('Ошибка при сохранении записи');
            }
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="edit-container">
    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="form-scroll">
            <!-- СЕКЦИЯ: КЛИЕНТ -->
            <section class="form-section card">
                <label>Клиент</label>
                <div class="search-input-wrapper">
                    <input type="tel"
                           bind:value={searchPhone}
                           placeholder="+7 (___) ___-__-__"
                           class:has-contact={!!selectedContact}/>
                    {#if selectedContact}
                        <div class="contact-badge">
                            <span class="name">👤 {selectedContact.name}</span>
                            <button class="clear-btn" on:click={() => { selectedContact = null; searchPhone = ''; }}>✕</button>
                        </div>
                    {:else}
                        <button class="quick-add" on:click={() => dispatch('addNewContact')}>+ Новый</button>
                    {/if}
                </div>
            </section>

            <!-- СЕКЦИЯ: УСЛУГА -->
            <section class="form-section card">
                <label>Услуга и Длительность</label>
                <select bind:value={formData.service}>
                    <option value="">Выберите услугу...</option>
                    {#each services as s}
                        <option value={s.name}>{s.name} ({s.durationInMinutes} мин)</option>
                    {/each}
                </select>

                <div class="input-row mt-12">
                    <div class="input-group">
                        <span class="prefix">⏳</span>
                        <input type="number" bind:value={formData.durationInMinutes} />
                        <span class="suffix">мин</span>
                    </div>
                </div>
            </section>

            <!-- СЕКЦИЯ: ВРЕМЯ И МАСТЕР -->
            <section class="form-section card">
                <label>Дата и Время</label>
                <input type="datetime-local"
                       value={formData.startTime.slice(0, 16)}
                       on:change={(e) => formData.startTime = new Date(e.target.value).toISOString()} />

                <label class="mt-20">Мастер</label>
                <select bind:value={formData.staffMemberId}>
                    <option value="">Выберите мастера...</option>
                    {#each staffList as s}
                        <option value={s.id}>{s.name} — {s.specialty}</option>
                    {/each}
                </select>

                <label class="mt-20">Кабинет / Ресурс</label>
                <select bind:value={formData.resourceId}>
                    <option value="">Без привязки к ресурсу</option>
                    {#each resources as r}
                        <option value={r.id}>{r.name}</option>
                    {/each}
                </select>
            </section>

            <div class="actions">
                <button class="cancel-btn" on:click={() => dispatch('cancel')}>ОТМЕНА</button>
                <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                    {isSaving ? 'СОХРАНЕНИЕ...' : (isEditing ? 'ОБНОВИТЬ' : 'ЗАПИСАТЬ')}
                </button>
            </div>
        </div>
    {/if}
</div>

<style>
    .edit-container { height: 100%; display: flex; flex-direction: column; background: #f8fafc; }
    .form-scroll { flex: 1; overflow-y: auto; padding: 20px; }

    .card { background: white; padding: 20px; border-radius: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); margin-bottom: 16px; border: 1px solid #f1f5f9; }
    label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; }

    input, select {
        width: 100%; padding: 14px; border-radius: 14px; border: 1.5px solid #f1f5f9;
        background: #f8fafc; font-size: 15px; box-sizing: border-box; outline: none; transition: border-color 0.2s;
    }
    input:focus, select:focus { border-color: var(--primary-color); background: white; }

    .search-input-wrapper { position: relative; }
    .has-contact { color: transparent; background: #f8fafc !important; }

    .contact-badge {
        position: absolute; top: 0; left: 0; right: 0; bottom: 0;
        display: flex; justify-content: space-between; align-items: center;
        padding: 0 14px; background: #eff6ff; border-radius: 14px; border: 1.5px solid var(--primary-color);
    }
    .contact-badge .name { font-weight: 700; color: #1e40af; font-size: 14px; }
    .clear-btn { background: white; border: none; color: #ef4444; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; font-size: 12px; }

    .quick-add { position: absolute; right: 8px; top: 8px; background: var(--primary-color); color: white; border: none; padding: 6px 12px; border-radius: 10px; font-size: 12px; font-weight: 700; cursor: pointer; }

    .input-row { display: flex; gap: 12px; }
    .input-group { position: relative; flex: 1; display: flex; align-items: center; }
    .prefix { position: absolute; left: 12px; }
    .input-group input { padding-left: 36px; padding-right: 45px; }
    .suffix { position: absolute; right: 12px; font-size: 12px; font-weight: 700; color: #94a3b8; }

    .mt-12 { margin-top: 12px; }
    .mt-20 { margin-top: 20px; }

    .actions { display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-top: 24px; margin-bottom: 40px; }
    .cancel-btn { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 16px; border-radius: 16px; font-weight: 700; cursor: pointer; }
    .save-btn { background: var(--primary-gradient); color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 800; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); cursor: pointer; }
    .save-btn:disabled { opacity: 0.6; }

    .center { display: flex; justify-content: center; align-items: center; height: 300px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
