<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { contactService } from '$lib/services/contactService.js';
    import { serviceService } from '$lib/services/serviceService.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { fade, slide, scale } from 'svelte/transition';

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
        resourceId: '',
        allowReminder: true,
        reminderLeadTimeHours: 24
    };

    let searchInput = '';
    let searchResults = [];
    let selectedContact = null;
    let serviceSearchInput = '';
    let filteredServices = [];
    let showServiceDropdown = false;
    let isNewService = false;
    let isLoading = true;
    let isSaving = false;
    let debounceTimer;

    export function setCreatedContact(contact) {
        if (contact) selectContact(contact);
    }

    function toLocalISO(date) {
        if (!date || isNaN(date.getTime())) return '';
        const offset = date.getTimezoneOffset() * 60000;
        const localDate = new Date(date.getTime() - offset);
        return localDate.toISOString().slice(0, 16);
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
                formData = {
                    ...appointment,
                    staffMemberId: appointment.staffMemberId || (appointment.staffMember ? appointment.staffMember.id : ''),
                    allowReminder: appointment.allowReminder ?? true,
                    reminderLeadTimeHours: appointment.reminderLeadTimeHours ?? 24
                };

                formData.startTime = toLocalISO(new Date(appointment.startTime));
                serviceSearchInput = appointment.service;

                if (appointment.contactId) {
                    const c = await contactService.getContactById(appointment.contactId);
                    if (c) selectContact(c);
                }
            } else {
                formData.staffMemberId = preselected.staffId || '';
                const d = new Date(preselected.date);
                d.setHours(preselected.hour, preselected.min, 0, 0);
                formData.startTime = toLocalISO(d);
            }
        } catch (e) {
            console.error('Load failed', e);
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
    }

    function handleClientInput() {
        if (selectedContact) return;
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(async () => {
            const q = searchInput.trim();
            if (q.length < 3) { searchResults = []; return; }
            const res = await contactService.getContacts(q, true, 0, 5);
            searchResults = res.content || [];
        }, 600);
    }

    function selectContact(contact) {
        selectedContact = contact;
        formData.contactId = contact.id;
        formData.clientName = contact.name;
        searchInput = contact.name;
        searchResults = [];
    }

    async function handleSave() {
        if (!selectedContact) return alert('Выберите клиента');
        if (!serviceSearchInput.trim()) return alert('Укажите услугу');

        isSaving = true;
        try {
            let sName = serviceSearchInput.trim();
            if (isNewService) {
                const ns = await serviceService.addService({ name: sName, durationInMinutes: formData.durationInMinutes });
                sName = ns.name;
            }

            const payload = {
                ...formData,
                service: sName,
                clientName: selectedContact.name,
                contactId: selectedContact.id,
                startTime: new Date(formData.startTime).toISOString()
            };

            if (isEditing) await adminService.updateAppointment(appointment.id, payload);
            else await adminService.createAppointment(payload);

            scheduleRefreshSignal.set({ ts: Date.now() });
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
                        <input type="text" bind:value={searchInput} on:input={handleClientInput} placeholder="Имя или номер..." class:invisible={!!selectedContact} />
                        {#if selectedContact}
                            <div class="badge" in:scale>
                                <span class="txt">{selectedContact.name}</span>
                                <button class="x" on:click={() => { selectedContact = null; searchInput = ''; }}>✕</button>
                            </div>
                        {/if}
                        <button class="btn-plus" on:click={() => dispatch('open-add-contact-modal')}>+</button>
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
                        <input type="text" bind:value={serviceSearchInput} placeholder="Что будем делать?" on:focus={() => showServiceDropdown = true} />
                        {#if showServiceDropdown && (filteredServices.length > 0 || isNewService)}
                            <div class="drop shadow-xl">
                                {#each filteredServices as s}
                                    <button class="item" on:click={() => selectService(s)}><b>{s.name}</b><small>{s.durationInMinutes} мин</small></button>
                                {/each}
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="tile-card dual">
                    <div class="part"><label>КОГДА</label><input type="datetime-local" bind:value={formData.startTime} /></div>
                    <div class="part border-l"><label>МИН</label><input type="number" bind:value={formData.durationInMinutes} /></div>
                </div>

                <div class="tile-card"><label>ИСПОЛНИТЕЛЬ</label><select bind:value={formData.staffMemberId}><option value="">Не назначен</option>{#each staffList as s}<option value={s.id}>{s.name}</option>{/each}</select></div>

                <div class="tile-card"><label>КАБИНЕТ / РЕСУРС</label><select bind:value={formData.resourceId}><option value="">Без ресурса</option>{#each resources as r}<option value={r.id}>{r.name}</option>{/each}</select></div>

                <div class="tile-card reminder-panel">
                    <div class="rem-main">
                        <label>НАПОМИНАНИЕ (ТЕЛЕГРАМ/WA)</label>
                        <div class="rem-settings">
                            {#if formData.allowReminder}
                                <div class="hours-input" in:slide={{axis: 'x'}}>
                                    <span>за</span>
                                    <input type="number" bind:value={formData.reminderLeadTimeHours} min="1" max="168" />
                                    <span>ч. до визита</span>
                                </div>
                            {:else}
                                <p class="rem-off">Отключено</p>
                            {/if}
                        </div>
                    </div>
                    <button class="toggle-switch" class:on={formData.allowReminder} on:click={() => formData.allowReminder = !formData.allowReminder}>
                        <div class="switch-handle"></div>
                    </button>
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
</div>

<style>
    .appt-edit-root { height: 100%; display: flex; flex-direction: column; background: #f8fafc; position: relative; overflow-x: hidden; }
    .tiles-layout { padding: 20px; max-width: 500px; margin: 0 auto; width: 100%; }
    .tile-hero { background: white; padding: 20px; border-radius: 28px; display: flex; align-items: center; gap: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.03); border: 1px solid #f1f5f9; margin-bottom: 16px; }
    .avatar { width: 56px; height: 56px; background: #f0f9ff; border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 900; color: #0ea5e9; }
    .hero-body { flex: 1; position: relative; }
    label { display: block; font-size: 9px; font-weight: 900; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }
    .search-box { display: flex; align-items: center; gap: 8px; position: relative; }
    .search-box input { width: 100%; padding: 10px 14px; border-radius: 14px; border: 1.5px solid #f1f5f9; background: white; font-size: 14px; outline: none; }
    .badge { position: absolute; left: 4px; right: 44px; top: 4px; bottom: 4px; background: #eff6ff; border-radius: 10px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; border: 1.5px solid #0ea5e9; }
    .badge .txt { font-weight: 700; color: #1e40af; font-size: 13px; }
    .btn-plus { width: 38px; height: 38px; border-radius: 12px; border: none; background: #0ea5e9; color: white; font-size: 20px; cursor: pointer; }

    .tiles-stack { display: flex; flex-direction: column; gap: 10px; }
    .tile-card { background: white; padding: 14px 18px; border-radius: 22px; border: 1px solid #f1f5f9; box-shadow: 0 4px 12px rgba(0,0,0,0.01); }

    .input-rel { position: relative; width: 100%; }
    .drop { position: absolute; top: calc(100% + 8px); left: -10px; right: -10px; background: white; border-radius: 18px; box-shadow: 0 20px 50px rgba(0,0,0,0.15); z-index: 2000; border: 1px solid #e2e8f0; max-height: 200px; overflow-y: auto; padding: 6px; }
    .item { width: 100%; padding: 12px 16px; border: none; background: none; text-align: left; cursor: pointer; border-radius: 12px; display: flex; flex-direction: column; }
    .item:hover { background: #f8fafc; }

    .reminder-panel { display: flex; align-items: center; justify-content: space-between; }
    .hours-input { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: #1e293b; }
    .hours-input input { width: 40px; padding: 4px; border-radius: 8px; border: 1.5px solid #e2e8f0; text-align: center; font-weight: 800; color: #0ea5e9; background: #f8fafc; }

    .toggle-switch { width: 40px; height: 22px; background: #e2e8f0; border-radius: 11px; border: none; position: relative; cursor: pointer; transition: background 0.3s; }
    .toggle-switch.on { background: #10b981; }
    .switch-handle { width: 16px; height: 16px; background: white; border-radius: 50%; position: absolute; top: 3px; left: 3px; transition: transform 0.3s; }
    .toggle-switch.on .switch-handle { transform: translateX(18px); }

    .dual { display: grid; grid-template-columns: 1fr 80px; padding: 0; }
    .part { padding: 14px 18px; }
    .border-l { border-left: 1px solid #f1f5f9; background: #f8fafc; }
    input, select { width: 100%; border: none; background: none; font-size: 15px; font-weight: 700; color: #1e293b; outline: none; }

    .footer-actions { display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-top: 24px; padding-bottom: 30px; }
    .btn-cancel { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 14px; border-radius: 18px; font-weight: 700; cursor: pointer; }
    .btn-save { background: #0ea5e9; color: white; border: none; padding: 14px; border-radius: 18px; font-weight: 800; cursor: pointer; }
    .spinner { width: 28px; height: 28px; border: 3px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
