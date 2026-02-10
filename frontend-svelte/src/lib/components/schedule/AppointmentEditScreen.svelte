<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { contactService } from '$lib/services/contactService.js';
    import { serviceService } from '$lib/services/serviceService.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';
    import { fade, slide, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';
    import ContactEditScreen from '../contacts/ContactEditScreen.svelte';

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

    // Состояния
    let searchInput = '';
    let searchResults = [];
    let selectedContact = null;
    let showFullClientAdd = false;
    let serviceSearchInput = '';
    let filteredServices = [];
    let showServiceDropdown = false;
    let isNewService = false;
    let durationInputEl;

    // ХЕЛПЕР: Форматирование для datetime-local (без Z сдвига)
    function toInputFormat(date) {
        if (!date || isNaN(date.getTime())) return '';
        const pad = n => n < 10 ? '0'+n : n;
        return date.getFullYear() + '-' + pad(date.getMonth()+1) + '-' + pad(date.getDate()) +
               'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
    }

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
                // Важно: переводим время из базы в локальный формат для инпута
                formData.startTime = toInputFormat(new Date(appointment.startTime));
                serviceSearchInput = appointment.service;
                if (appointment.contactId) {
                    const c = await contactService.getContactById(appointment.contactId);
                    if (c) {
                        selectedContact = c;
                        searchInput = c.name;
                    }
                }
            } else {
                formData.staffMemberId = preselected.staffId || '';
                const d = new Date(preselected.date);
                d.setHours(preselected.hour, preselected.min, 0, 0);
                formData.startTime = toInputFormat(d);
            }
        } catch (e) {
            console.error('Data load failed', e);
        } finally {
            isLoading = false;
        }
    }

    $: {
        const query = serviceSearchInput.trim().toLowerCase();
        filteredServices = query ? services.filter(s => s.name.toLowerCase().includes(query)) : services;
        isNewService = query !== '' && !services.some(s => s.name.toLowerCase() === query);
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
        setTimeout(() => { if (durationInputEl) durationInputEl.focus(); }, 50);
    }

    function handleClientInput() {
        if (selectedContact) return;
        const query = searchInput.trim();
        if (query.length < 3) { searchResults = []; return; }
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(async () => {
            try {
                const result = await contactService.getContacts(query, true, 0, 5);
                searchResults = result.content || [];
            } catch (err) { searchResults = []; }
        }, 600);
    }

    function selectContact(contact) {
        selectedContact = contact;
        formData.contactId = contact.id;
        formData.clientName = contact.name;
        searchInput = contact.name;
        searchResults = [];
    }

    function handleContactCreated(event) {
        selectContact(event.detail);
        showFullClientAdd = false;
    }

    async function handleSave() {
        if (!selectedContact) return alert('Выберите клиента');
        if (!serviceSearchInput.trim()) return alert('Укажите услугу');

        isSaving = true;
        try {
            let finalService = serviceSearchInput.trim();
            if (isNewService) {
                const newSvc = await serviceService.addService({
                    name: finalService,
                    durationInMinutes: formData.durationInMinutes
                });
                finalService = newSvc.name;
            }

            const payload = {
                ...formData,
                service: finalService,
                clientName: selectedContact.name,
                contactId: selectedContact.id,
                // ПРИ ОТПРАВКЕ: преобразуем обратно в ISO
                startTime: new Date(formData.startTime).toISOString()
            };

            if (isEditing) await adminService.updateAppointment(appointment.id, payload);
            else await adminService.createAppointment(payload);
            dispatch('saved');
        } catch (e) {
            alert('Ошибка сохранения');
        } finally {
            isSaving = false;
        }
    }

    let staffList = [];
    let services = [];
    let resources = [];
    let isLoading = true;
    let isSaving = false;
    let debounceTimer;
</script>

<div class="appt-edit-root" on:click={() => { showServiceDropdown = false; searchResults = []; }}>
    {#if isLoading}
        <div class="loader-center"><span class="spinner"></span></div>
    {:else}
        <div class="tiles-layout" in:fade>

            <section class="tile-hero">
                <div class="avatar">{selectedContact ? selectedContact.name.charAt(0).toUpperCase() : '?'}</div>
                <div class="hero-body">
                    <label>КЛИЕНТ ЗАПИСИ</label>
                    <div class="search-box" on:click|stopPropagation>
                        <input type="text"
                               bind:value={searchInput}
                               on:input={handleClientInput}
                               placeholder="Имя или номер..."
                               class:invisible={!!selectedContact} />

                        {#if selectedContact}
                            <div class="badge" in:scale>
                                <span class="txt">{selectedContact.name}</span>
                                <button class="x" on:click={() => { selectedContact = null; searchInput = ''; }}>✕</button>
                            </div>
                        {/if}
                        <button class="btn-plus" on:click={() => showFullClientAdd = true}>+</button>

                        {#if searchResults.length > 0}
                            <div class="drop shadow-xl">
                                {#each searchResults as c}
                                    <button class="item" on:click={() => selectContact(c)}>
                                        <b>{c.name}</b>
                                        <small>{c.phones[0] || ''}</small>
                                    </button>
                                {/each}
                            </div>
                        {/if}
                    </div>
                </div>
            </section>

            <div class="tiles-stack">
                <div class="tile-card rel-pos" on:click|stopPropagation>
                    <label>УСЛУГА</label>
                    <div class="input-rel">
                        <input type="text"
                               bind:value={serviceSearchInput}
                               placeholder="Что будем делать?"
                               on:focus={() => showServiceDropdown = true} />

                        {#if showServiceDropdown && (filteredServices.length > 0 || isNewService)}
                            <div class="drop shadow-xl">
                                {#each filteredServices as s}
                                    <button class="item" on:click={() => selectService(s)}>
                                        <b>{s.name}</b>
                                        <small>{s.durationInMinutes} мин</small>
                                    </button>
                                {/each}
                                {#if isNewService && serviceSearchInput.trim()}
                                    <button class="item new-mark" on:click={startNewService}>
                                        <b>✨ Создать новую: "{serviceSearchInput}"</b>
                                        <small>Задать время</small>
                                    </button>
                                {/if}
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="tile-card dual">
                    <div class="part">
                        <label>КОГДА</label>
                        <!-- ФИКС: Добавлен bind:value для реактивного изменения времени -->
                        <input type="datetime-local" bind:value={formData.startTime} />
                    </div>
                    <div class="part border-l">
                        <label>МИН</label>
                        <input type="number" bind:value={formData.durationInMinutes} bind:this={durationInputEl} />
                    </div>
                </div>

                <div class="tile-card">
                    <label>ИСПОЛНИТЕЛЬ</label>
                    <select bind:value={formData.staffMemberId}>
                        <option value="">Не назначен</option>
                        {#each staffList as s}
                            <option value={s.id}>{s.name}</option>
                        {/each}
                    </select>
                </div>

                <div class="tile-card">
                    <label>КАБИНЕТ / РЕСУРС</label>
                    <select bind:value={formData.resourceId}>
                        <option value="">Без ресурса</option>
                        {#each resources as r}
                            <option value={r.id}>{r.name}</option>
                        {/each}
                    </select>
                </div>
            </div>

            <div class="footer-actions">
                <button class="btn-cancel" on:click={() => dispatch('cancel')}>ОТМЕНА</button>
                <button class="btn-save" on:click={handleSave} disabled={isSaving}>
                    {isSaving ? '...' : (isEditing ? 'ОБНОВИТЬ' : 'ЗАПИСАТЬ')}
                </button>
            </div>
        </div>
    {/if}

    {#if showFullClientAdd}
        <div class="full-client-overlay" in:fade>
            <div class="inner-modal" in:scale>
                <header class="inner-head">
                    <h3>Новый клиент</h3>
                    <button class="x-close" on:click={() => showFullClientAdd = false}>✕</button>
                </header>
                <ContactEditScreen on:success={handleContactCreated} on:cancel={() => showFullClientAdd = false} />
            </div>
        </div>
    {/if}
</div>

<style>
    .appt-edit-root { height: 100%; display: flex; flex-direction: column; background: #f8fafc; position: relative; }
    .tiles-layout { padding: 24px; max-width: 500px; margin: 0 auto; width: 100%; }
    .tile-hero { background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%); padding: 24px; border-radius: 32px; display: flex; align-items: center; gap: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.04); border: 1px solid #f1f5f9; margin-bottom: 20px; }
    .avatar { width: 64px; height: 64px; background: white; border-radius: 22px; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 900; color: var(--primary-color); box-shadow: 0 8px 20px rgba(56, 151, 240, 0.1); }
    .hero-body { flex: 1; position: relative; }
    label { display: block; font-size: 10px; font-weight: 900; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; margin-left: 4px; }
    .search-box { display: flex; align-items: center; gap: 8px; position: relative; }
    .search-box input { width: 100%; padding: 12px 16px; border-radius: 16px; border: 1.5px solid #f1f5f9; background: white; font-size: 15px; outline: none; }
    .invisible { color: transparent; }
    .badge { position: absolute; left: 4px; right: 48px; top: 4px; bottom: 4px; background: #eff6ff; border-radius: 12px; display: flex; align-items: center; justify-content: space-between; padding: 0 14px; border: 1.5px solid var(--primary-color); }
    .badge .txt { font-weight: 700; color: #1e40af; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .x { background: none; border: none; color: #ef4444; font-weight: 800; cursor: pointer; }
    .btn-plus { width: 42px; height: 42px; border-radius: 14px; border: none; background: var(--primary-color); color: white; font-size: 24px; cursor: pointer; flex-shrink: 0; }
    .tiles-stack { display: flex; flex-direction: column; gap: 12px; }
    .tile-card { background: white; padding: 16px 20px; border-radius: 24px; border: 1px solid #f1f5f9; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
    .rel-pos { position: relative; }
    .dual { display: grid; grid-template-columns: 1fr 85px; padding: 0; overflow: hidden; }
    .part { padding: 16px 20px; }
    .border-l { border-left: 1px solid #f1f5f9; background: #f8fafc; }
    input, select { width: 100%; border: none; background: none; font-size: 16px; font-weight: 700; color: #1e293b; outline: none; }
    select { appearance: none; color: var(--primary-color); cursor: pointer; }
    .drop { position: absolute; top: calc(100% + 5px); left: 0; right: 0; background: white; border-radius: 18px; box-shadow: 0 20px 50px rgba(0,0,0,0.15); z-index: 1000; border: 1px solid #e2e8f0; max-height: 250px; overflow-y: auto; padding: 8px; }
    .item { width: 100%; padding: 14px 18px; border: none; background: none; text-align: left; cursor: pointer; border-radius: 12px; display: flex; flex-direction: column; gap: 3px; }
    .item:hover { background: #f8fafc; }
    .item b { font-weight: 700; color: #1e293b; font-size: 15px; }
    .item small { font-size: 11px; color: #94a3b8; font-weight: 600; }
    .new-mark { background: #fffbeb; border: 1.5px dashed #f59e0b; }
    .footer-actions { display: grid; grid-template-columns: 1fr 2fr; gap: 16px; margin-top: 32px; padding-bottom: 40px; }
    .btn-cancel { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 16px; border-radius: 20px; font-weight: 700; cursor: pointer; }
    .btn-save { background: var(--primary-gradient); color: white; border: none; padding: 16px; border-radius: 20px; font-weight: 800; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }
    .full-client-overlay { position: absolute; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(4px); z-index: 2000; display: flex; align-items: flex-end; }
    .inner-modal { width: 100%; background: white; border-radius: 32px 32px 0 0; padding: 20px; max-height: 95vh; overflow-y: auto; }
    .inner-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding: 0 8px; }
    .inner-head h3 { margin: 0; font-size: 18px; font-weight: 800; }
    .x-close { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; }
    .loader-center { display: flex; justify-content: center; align-items: center; height: 300px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
