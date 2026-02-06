<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { contactService } from '$lib/services/contactService.js';
    import { serviceService } from '$lib/services/serviceService.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';
    import { slide, fade } from 'svelte/transition';

    export let appointment = null;
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

    // Клиент
    let searchInput = '';
    let lastSearchQuery = '';
    let searchResults = [];
    let selectedContact = null;
    let isAutoUpdating = false;
    let showNewContactForm = false;
    let newContactName = '';

    // Услуга
    let serviceSearchInput = '';
    let filteredServices = [];
    let showServiceDropdown = false;
    let isNewService = false;
    let durationInput; // Для автофокуса

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
            const [servicesData, resourcesData, staffData] = await Promise.all([
                serviceService.getServices(),
                resourceService.getResources(),
                adminService.getStaffForSchedule(isEditing ? new Date(appointment.startTime) : preselected.date)
            ]);

            services = servicesData;
            resources = resourcesData;
            staffList = staffData.filter(s => s.role === 'EMPLOYEE' || s.role === 'ROLE_EMPLOYEE');

            if (isEditing) {
                formData = { ...appointment };
                serviceSearchInput = appointment.service;
                if (appointment.contactId) {
                    const contact = await contactService.getContactById(appointment.contactId);
                    if (contact) {
                        isAutoUpdating = true;
                        selectedContact = contact;
                        formData.clientName = contact.name;
                        searchInput = contact.name;
                        lastSearchQuery = contact.name;
                        setTimeout(() => isAutoUpdating = false, 100);
                    }
                }
            } else {
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

    // ЛОГИКА ПОИСКА УСЛУГИ
    $: {
        const query = serviceSearchInput.trim().toLowerCase();
        if (query === '') {
            filteredServices = services;
            isNewService = false;
        } else {
            filteredServices = services.filter(s => s.name.toLowerCase().includes(query));
            isNewService = !services.some(s => s.name.toLowerCase() === query);
        }
    }

    function selectService(s) {
        formData.service = s.name;
        formData.durationInMinutes = s.durationInMinutes;
        serviceSearchInput = s.name;
        showServiceDropdown = false;
        isNewService = false;
    }

    function startNewService() {
        showServiceDropdown = false;
        if (durationInput) durationInput.focus();
    }

    // ПОИСК КЛИЕНТА (Дебаунс 800мс)
    $: if (!isAutoUpdating && !selectedContact && !showNewContactForm && searchInput.trim() !== lastSearchQuery) {
        const query = searchInput.trim();
        const digits = query.replace(/\D/g, '');
        const shouldSearch = (digits.length >= 6) || (query.length >= 3 && digits.length < 3);

        if (shouldSearch) {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(async () => {
                lastSearchQuery = query;
                try {
                    const result = await contactService.getContacts(query, true, 0, 5);
                    searchResults = result.content || [];
                } catch (err) { console.warn('Search failed'); }
            }, 800);
        } else {
            searchResults = [];
        }
    }

    function selectContact(contact) {
        isAutoUpdating = true;
        selectedContact = contact;
        formData.contactId = contact.id;
        formData.clientName = contact.name;
        searchInput = contact.name;
        lastSearchQuery = contact.name;
        searchResults = [];
        setTimeout(() => isAutoUpdating = false, 100);
    }

    async function handleQuickCreateContact() {
        if (!newContactName.trim()) return alert('Введите имя клиента');
        try {
            const digits = searchInput.replace(/\D/g, '');
            const newContact = await contactService.addContact({
                name: newContactName,
                phones: digits ? [digits] : [],
                tenantId: ''
            });
            selectContact(newContact);
            showNewContactForm = false;
        } catch (e) { alert('Ошибка при создании клиента'); }
    }

    async function handleSave() {
        if (!selectedContact) return alert('Выберите клиента');
        if (!serviceSearchInput.trim()) return alert('Укажите услугу');

        isSaving = true;
        try {
            if (isNewService) {
                const newSvc = await serviceService.addService({
                    name: serviceSearchInput.trim(),
                    durationInMinutes: formData.durationInMinutes,
                    tenantId: ''
                });
                formData.service = newSvc.name;
            } else {
                formData.service = serviceSearchInput.trim();
            }

            const dataToSave = { ...formData, clientName: selectedContact.name, contactId: selectedContact.id };
            if (isEditing) await adminService.updateAppointment(appointment.id, dataToSave);
            else await adminService.createAppointment(dataToSave);
            dispatch('saved');
        } catch (e) {
            alert('Ошибка сохранения');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="edit-modal-root" on:click={() => { showServiceDropdown = false; searchResults = []; }}>
    {#if isLoading}
        <div class="loader-center"><span class="spinner"></span></div>
    {:else}
        <div class="form-container">
            <!-- КЛИЕНТ -->
            <section class="section-card">
                <label>Клиент</label>
                {#if showNewContactForm}
                    <div class="quick-form" transition:slide>
                        <p class="hint">Новый клиент для {searchInput}</p>
                        <input type="text" bind:value={newContactName} placeholder="Имя Фамилия" autoFocus />
                        <div class="btn-group-mini">
                            <button class="btn-ghost" on:click={() => showNewContactForm = false}>Отмена</button>
                            <button class="btn-prime-mini" on:click={handleQuickCreateContact}>Создать</button>
                        </div>
                    </div>
                {:else}
                    <div class="search-grid-container" on:click|stopPropagation>
                        <div class="input-cell">
                            <input type="text"
                                   bind:value={searchInput}
                                   placeholder="Имя или телефон..."
                                   class:is-hidden={!!selectedContact}/>

                            {#if selectedContact}
                                <div class="badge-layer" in:fade>
                                    <span class="name">👤 {selectedContact.name}</span>
                                    <button class="clear" on:click={() => { selectedContact = null; searchInput = ''; lastSearchQuery = ''; }}>✕</button>
                                </div>
                            {/if}

                            {#if searchResults.length > 0}
                                <div class="dropdown shadow-2xl" transition:fade={{duration: 100}}>
                                    {#each searchResults as c}
                                        <button class="dropdown-item" on:click={() => selectContact(c)}>
                                            <span class="main-text">{c.name}</span>
                                            <span class="sub-text">{c.phones[0] || 'нет номера'}</span>
                                        </button>
                                    {/each}
                                </div>
                            {/if}
                        </div>
                        <div class="button-cell">
                            <button class="btn-square-add" on:click={() => { showNewContactForm = true; newContactName = ''; }}>👤+</button>
                        </div>
                    </div>
                {/if}
            </section>

            <!-- УСЛУГА -->
            <section class="section-card">
                <label>Услуга</label>
                <div class="input-cell" on:click|stopPropagation>
                    <input type="text"
                           bind:value={serviceSearchInput}
                           placeholder="Найти или ввести новую..."
                           on:focus={() => showServiceDropdown = true}
                    />
                    {#if showServiceDropdown && (filteredServices.length > 0 || isNewService)}
                        <div class="dropdown shadow-2xl" transition:fade={{duration: 100}}>
                            {#each filteredServices as s}
                                <button class="dropdown-item" on:click={() => selectService(s)}>
                                    <span class="main-text">{s.name}</span>
                                    <span class="sub-text">{s.durationInMinutes} мин</span>
                                </button>
                            {/each}
                            {#if isNewService && serviceSearchInput.trim() !== ''}
                                <button class="dropdown-item new-mark" on:click={startNewService}>
                                    <span class="main-text">✨ Создать новую: "{serviceSearchInput}"</span>
                                    <span class="sub-text">Нажмите, чтобы задать время</span>
                                </button>
                            {/if}
                        </div>
                    {/if}
                </div>

                <div class="mt-16">
                    <label>Длительность (мин)</label>
                    <div class="input-row-group" class:highlight={isNewService}>
                        <span class="clock">⏳</span>
                        <input type="number"
                               bind:value={formData.durationInMinutes}
                               bind:this={durationInput} />
                    </div>
                </div>
            </section>

            <!-- ВРЕМЯ И МАСТЕР -->
            <section class="section-card">
                <label>Дата и Время</label>
                <input type="datetime-local" value={formData.startTime.slice(0, 16)} on:change={(e) => formData.startTime = new Date(e.target.value).toISOString()} />

                <label class="mt-20">Мастер</label>
                <select bind:value={formData.staffMemberId}>
                    <option value="">Выберите мастера...</option>
                    {#each staffList as s}
                        <option value={s.id}>{s.name}</option>
                    {/each}
                </select>

                <label class="mt-20">Ресурс (Кабинет)</label>
                <select bind:value={formData.resourceId}>
                    <option value="">Без ресурса</option>
                    {#each resources as r}
                        <option value={r.id}>{r.name}</option>
                    {/each}
                </select>
            </section>

            <div class="actions-sticky">
                <button class="btn-cancel-large" on:click={() => dispatch('cancel')}>ОТМЕНА</button>
                <button class="btn-save-large" on:click={handleSave} disabled={isSaving || showNewContactForm}>
                    {isSaving ? '...' : (isEditing ? 'ОБНОВИТЬ' : 'ЗАПИСАТЬ')}
                </button>
            </div>
        </div>
    {/if}
</div>

<style>
    .edit-modal-root { height: 100%; display: flex; flex-direction: column; background: #f8fafc; }
    .form-container { flex: 1; overflow-y: auto; padding: 20px; }
    .section-card { background: white; padding: 20px; border-radius: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.02); margin-bottom: 16px; border: 1px solid #f1f5f9; }
    label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; }
    input, select { width: 100%; padding: 14px; border-radius: 14px; border: 1.5px solid #f1f5f9; background: #f8fafc; font-size: 15px; outline: none; box-sizing: border-box; transition: all 0.2s; }
    input:focus { border-color: var(--primary-color); background: white; }

    .search-grid-container { display: grid; grid-template-columns: 1fr 48px; gap: 12px; align-items: center; width: 100%; }
    .input-cell { position: relative; min-width: 0; }
    .button-cell { width: 48px; height: 48px; flex-shrink: 0; }
    .is-hidden { color: transparent !important; }

    .badge-layer {
        position: absolute; inset: 0; background: #eff6ff; border-radius: 14px;
        border: 1.5px solid var(--primary-color); display: flex; align-items: center;
        justify-content: space-between; padding: 0 14px; z-index: 5; pointer-events: none;
    }
    .badge-layer .clear { pointer-events: auto; background: white; border: none; color: #ef4444; border-radius: 50%; width: 22px; height: 22px; cursor: pointer; font-size: 10px; font-weight: bold; }
    .badge-layer .name { font-weight: 700; color: #1e40af; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }

    .btn-square-add { width: 48px; height: 48px; background: #eff6ff; border: 1.5px solid var(--primary-color); border-radius: 14px; color: var(--primary-color); font-weight: 800; cursor: pointer; }

    .dropdown { position: absolute; top: 100%; left: 0; right: 0; background: white; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.15); z-index: 100; border: 1px solid #e2e8f0; margin-top: 8px; max-height: 240px; overflow-y: auto; padding: 6px; }
    .dropdown-item { width: 100%; padding: 12px 16px; border: none; background: none; text-align: left; cursor: pointer; border-radius: 10px; display: flex; flex-direction: column; gap: 2px; margin-bottom: 2px; }
    .dropdown-item:hover { background: #f8fafc; }
    .new-mark { background: #fffbeb; border: 1.5px dashed #f59e0b; margin-top: 4px; }
    .main-text { font-weight: 700; color: #1e293b; font-size: 14px; }
    .sub-text { font-size: 11px; color: #94a3b8; font-weight: 600; }

    .quick-form { background: #f0f9ff; padding: 16px; border-radius: 16px; border: 1px dashed #3897f0; }
    .btn-group-mini { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
    .btn-ghost { background: none; border: none; color: #64748b; font-weight: 700; cursor: pointer; }
    .btn-prime-mini { background: var(--primary-color); color: white; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 700; }

    .input-row-group { position: relative; display: flex; align-items: center; }
    .input-row-group .clock { position: absolute; left: 12px; font-size: 16px; }
    .input-row-group input { padding-left: 38px; }
    .highlight input { border-color: #f59e0b; background: #fffbeb; }

    .mt-16 { margin-top: 16px; }
    .mt-20 { margin-top: 20px; }
    .actions-sticky { display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-top: 24px; padding-bottom: 40px; }
    .btn-cancel-large { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 16px; border-radius: 16px; font-weight: 700; cursor: pointer; }
    .btn-save-large { background: var(--primary-gradient); color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 800; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); cursor: pointer; }

    .loader-center { display: flex; justify-content: center; align-items: center; height: 300px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
