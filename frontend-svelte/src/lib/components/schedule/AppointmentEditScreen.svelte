<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { contactService } from '$lib/services/contactService.js';
    import { serviceService } from '$lib/services/serviceService.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import SearchDropdownItem from './SearchDropdownItem.svelte';
    import { fade, slide, scale } from 'svelte/transition';

    export let appointment = null;
    export let preselected = { date: new Date(), hour: 10, min: 0, staffId: null };

    const dispatch = createEventDispatcher();
    const isEditing = !!appointment;

    let formData = {
        startTime: '',
        durationInMinutes: 60,
        contactId: null,
        clientName: '',
        clientPhone: '',
        service: '',
        staffMemberId: null,
        staffMemberIds: [],
        resourceId: null,
        branchId: '',
        status: 'SCHEDULED',
        comment: '',
        referenceTag: '',
        allowReminder: true,
        reminderLeadTimeHours: 24,
        groupId: null
    };

    let isNewClientMode = false;
    let newClientPhone = "";
    let availablePhones = [];

    let durationHours = 1;
    let durationMinutes = 0;
    let showDurationPicker = false;

    const HOURS_OPTIONS = Array.from({length: 13}, (_, i) => i);
    const MINS_OPTIONS = [0, 5, 10, 15, 20, 30, 45];

    let searchInput = '';
    let searchResults = [];
    let selectedContact = null;
    let serviceSearchInput = '';
    let filteredServices = [];
    let showServiceDropdown = false;
    let isNewService = false;
    let selectedService = null;
    let isLoading = true;
    let isSaving = false;
    let debounceTimer;

    let staffList = [];
    let services = [];
    let resources = [];
    let currentBranchData = null;

    $: formData.durationInMinutes = (durationHours * 60) + durationMinutes;

    onMount(async () => {
        await loadInitialData();
    });

    async function loadInitialData() {
        isLoading = true;
        try {
            const allBranches = await branchService.getBranches();
            currentBranchData = allBranches.find(b => b.id === $activeBranchId);

            const [servicesData, resourcesData, staffData] = await Promise.all([
                serviceService.getServices(),
                resourceService.getResources($activeBranchId),
                adminService.getStaffForSchedule(isEditing ? new Date(appointment.startTime) : preselected.date, $activeBranchId)
            ]);

            services = servicesData;
            resources = resourcesData;
                staffList = (staffData || []).map(s => ({
                ...s,
                id: s.id ? String(s.id) : null // Гарантируем строковый ID для биндинга в select
            }));

            if (isEditing) {
                formData = {
                    ...appointment,
                    staffMemberId: appointment.staffMemberId ? String(appointment.staffMemberId) : (appointment.staffMember?.id ? String(appointment.staffMember.id) : null),
                    staffMemberIds: appointment.staffMemberIds ? appointment.staffMemberIds.map(String) : (appointment.staffMemberId ? [String(appointment.staffMemberId)] : []),
                    branchId: appointment.branchId || (appointment.branch ? appointment.branch.id : $activeBranchId),
                    allowReminder: appointment.allowReminder ?? true,
                    reminderLeadTimeHours: appointment.reminderLeadTimeHours ?? 24,
                    status: appointment.status || 'SCHEDULED',
                    comment: appointment.comment || '',
                    referenceTag: appointment.referenceTag || '',
                    clientPhone: appointment.clientPhone || '',
                    groupId: appointment.groupId || null
                };
                durationHours = Math.floor(formData.durationInMinutes / 60);
                durationMinutes = formData.durationInMinutes % 60;
                formData.startTime = timeUtils.toBranchLocalISO(appointment.startTime, currentBranchData?.timezone);
                serviceSearchInput = appointment.service;
                
                // Находим услугу в списке услуг
                selectedService = services.find(s => s.name === appointment.service) || null;

                if (appointment.contactId) {
                    const c = await contactService.getContactById(appointment.contactId);
                    if (c) selectContact(c, true);
                }
            } else {
                const initialStaff = preselected.staffId ? [String(preselected.staffId)] : [];
                formData.staffMemberId = preselected.staffId ? String(preselected.staffId) : null;
                formData.staffMemberIds = initialStaff;
                formData.branchId = $activeBranchId;
                const d = new Date(preselected.date);
                d.setHours(preselected.hour, preselected.min, 0, 0);
                const pad = n => n < 10 ? '0'+n : n;
                formData.startTime = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
            }
        } catch (e) {
            console.error('Load failed', e);
        } finally {
            isLoading = false;
        }
    }

    $: {
        const query = serviceSearchInput.trim().toLowerCase();
        if (query.length >= 2) {
            filteredServices = services.filter(s => s.name.toLowerCase().includes(query));
            isNewService = query !== '' && !services.some(s => s.name.toLowerCase() === query);
        } else {
            filteredServices = [];
            isNewService = false;
        }
    }

    // Автоматически закрываем дропдаун при очистке поля
    $: if (serviceSearchInput.trim().length < 2) {
        showServiceDropdown = false;
    }

    function selectService(s) {
        selectedService = s;
        formData.service = s.name;
        durationHours = Math.floor(s.durationInMinutes / 60);
        durationMinutes = s.durationInMinutes % 60;
        serviceSearchInput = s.name;
        showServiceDropdown = false;
        filteredServices = [];
    }

    function handleServiceInput() {
        if (selectedService) return;
        clearTimeout(debounceTimer);
        const q = serviceSearchInput.trim();
        if (q.length < 2) { 
            showServiceDropdown = false; 
            return; 
        }

        debounceTimer = setTimeout(() => {
            showServiceDropdown = true;
        }, 300);
    }

    function startServiceCreation() {
        selectedService = null;
        formData.service = serviceSearchInput.trim();
        showServiceDropdown = false;
        filteredServices = [];
    }

    function selectContact(contact, keepExistingPhone = false) {
        isNewClientMode = false;
        selectedContact = contact;
        formData.contactId = contact.id;
        formData.clientName = contact.name;
        searchInput = contact.name;
        searchResults = [];

        availablePhones = contact.phones || [];
        if (!keepExistingPhone) {
            formData.clientPhone = availablePhones.length > 0 ? availablePhones[0] : '';
        }
    }

    function startInlineCreation() {
        isNewClientMode = true;
        selectedContact = null;
        formData.contactId = null;
        formData.clientName = searchInput;
        availablePhones = [];
        const digits = searchInput.replace(/\D/g, "");
        if (digits.length >= 10) newClientPhone = digits;
        searchResults = [];
    }

    async function handleSave() {
        if (!searchInput.trim()) return alert('Укажите имя клиента');
        let finalPhone = isNewClientMode ? newClientPhone.trim() : formData.clientPhone;
        if (!finalPhone) return alert('Укажите номер телефона');

        isSaving = true;
        try {
            let contactId = formData.contactId;
            let clientName = searchInput.trim();

            if (isNewClientMode) {
                const newContact = await contactService.addContact({
                    name: clientName,
                    phones: [finalPhone],
                    tags: formData.referenceTag ? [formData.referenceTag] : []
                }, $activeBranchId);
                contactId = newContact.id;
            }

            let sName = selectedService ? selectedService.name : (serviceSearchInput.trim() || "Стандарт");
            if (!selectedService && sName !== "Стандарт" && !services.some(s => s.name === sName)) {
                const ns = await serviceService.addService({ name: sName, durationInMinutes: formData.durationInMinutes });
                sName = ns.name;
            }

            const correctedStartTime = timeUtils.fromBranchLocalToUTC(formData.startTime, currentBranchData.timezone);

            const firstStaffId = formData.staffMemberIds.length > 0 ? formData.staffMemberIds[0] : null;

            const payload = {
                ...formData,
                staffMemberId: firstStaffId,
                service: sName,
                clientName: clientName,
                clientPhone: finalPhone,
                contactId: contactId,
                startTime: correctedStartTime,
                branchId: $activeBranchId
            };

            try {
                if (isEditing) {
                    await adminService.updateAppointment(appointment.id, payload, false, updateMode);
                } else {
                    await adminService.createAppointment(payload, false);
                }
            } catch (err) {
                if (err.response && err.response.status === 409) {
                    if (confirm('Один из выбранных сотрудников занят или не работает в это время. Все равно сохранить запись?')) {
                        if (isEditing) {
                            await adminService.updateAppointment(appointment.id, payload, true, updateMode);
                        } else {
                            await adminService.createAppointment(payload, true);
                        }
                    } else {
                        isSaving = false;
                        return;
                    }
                } else {
                    throw err;
                }
            }
            dispatch('saved');
        } catch (e) {
            console.error('Save failed', e);
            alert('Ошибка сохранения');
        } finally {
            isSaving = false;
        }
    }

    function handleClientInput() {
        if (selectedContact || isNewClientMode) return;
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(async () => {
            const q = searchInput.trim();
            if (q.length < 2) { searchResults = []; return; }
            const res = await contactService.getContacts(q, true, 0, 5);
            searchResults = res.content || [];
        }, 400);
    }

    let updateMode = 'all';

    function addStaff(event) {
        const id = event.target.value;
        if (id && !formData.staffMemberIds.includes(id)) {
            formData.staffMemberIds = [...formData.staffMemberIds, id];
        }
        event.target.value = ""; // reset select
    }

    function removeStaff(id) {
        formData.staffMemberIds = formData.staffMemberIds.filter(sid => sid !== id);
    }
</script>

<div class="appt-edit-root">
    {#if isLoading}
        <div class="loader-center"><span class="spinner"></span></div>
    {:else}
        <div class="tiles-layout" in:fade>
            <section class="tile-hero" class:is-new={isNewClientMode}>
                <div class="avatar">{isNewClientMode ? '✨' : (selectedContact ? selectedContact.name.charAt(0).toUpperCase() : '?')}</div>
                <div class="hero-body">
                    <label>{isNewClientMode ? 'НОВЫЙ КЛИЕНТ' : 'КЛИЕНТ ЗАПИСИ'}</label>
                    <div class="search-box rel-pos" on:click|stopPropagation>
                        <input type="text" bind:value={searchInput} on:input={handleClientInput} placeholder="Имя, телефон или ГОСНОМЕР..." class:invisible={!!selectedContact} />

                        {#if selectedContact}
                            <div class="badge" in:scale>
                                <span class="txt">{selectedContact.name}</span>
                                <button class="x" on:click={() => { selectedContact = null; searchInput = ''; isNewClientMode = false; availablePhones = []; }}>✕</button>
                            </div>
                        {/if}

                        {#if searchResults.length > 0 || (searchInput.length >= 2 && !selectedContact && !isNewClientMode)}
                            <div class="drop shadow-xl" on:click|stopPropagation>
                                {#if searchInput.length >= 2 && !selectedContact}
                                    <SearchDropdownItem
                                        title="Создать нового: {searchInput}"
                                        subtitle="Нажмите для инлайн-создания"
                                        icon="✨"
                                        type="action"
                                        on:select={startInlineCreation}
                                    />
                                    <div class="divider"></div>
                                {/if}
                                {#each searchResults as c}
                                    <SearchDropdownItem
                                        title={c.name}
                                        subtitle={(c.tags && c.tags.length > 0) ? `🚗 ${c.tags.join(', ')}` : (c.phones[0] || 'Нет номера')}
                                        type="client"
                                        on:select={() => selectContact(c)}
                                    />
                                {/each}
                            </div>
                        {/if}
                    </div>

                    {#if isNewClientMode}
                        <div class="inline-phone-field" in:slide>
                            <input type="tel" bind:value={newClientPhone} placeholder="Номер телефона..." autofocus />
                            <button class="btn-cancel-new" on:click={() => isNewClientMode = false}>✕</button>
                        </div>
                    {/if}

                    {#if selectedContact && availablePhones.length > 1}
                        <div class="phone-select-area" in:slide>
                            <label class="micro-label">ВЫБЕРИТЕ ТЕЛЕФОН ДЛЯ ЭТОЙ ЗАПИСИ:</label>
                            <div class="phone-chips">
                                {#each availablePhones as ph}
                                    <button
                                        class="ph-chip"
                                        class:active={formData.clientPhone === ph}
                                        on:click={() => formData.clientPhone = ph}>
                                        {ph}
                                    </button>
                                {/each}
                            </div>
                        </div>
                    {:else if selectedContact && availablePhones.length === 1}
                        <div class="phone-info-line" in:fade>
                            <span>📞 {availablePhones[0]}</span>
                        </div>
                    {/if}
                </div>
            </section>

            <div class="tiles-stack">
                <div class="tile-card reference-card">
                    <label>АВТОМОБИЛЬ / ОБЪЕКТ</label>
                    <div class="tag-input-wrap">
                        <input type="text" bind:value={formData.referenceTag} placeholder="Марка, модель, госномер..." />
                        {#if selectedContact?.tags?.length > 0}
                            <div class="quick-tags" in:slide>
                                {#each selectedContact.tags as tag}
                                    <button class="tag-chip" class:active={formData.referenceTag === tag} on:click={() => formData.referenceTag = tag}>
                                        {tag}
                                    </button>
                                {/each}
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="tile-card rel-pos" on:click|stopPropagation>
                    <label>УСЛУГА</label>
                    <div class="input-rel">
                        <input
                            type="text"
                            bind:value={serviceSearchInput}
                            placeholder="Оставьте пустым для 'Стандарт'"
                            autocomplete="off"
                            on:input={handleServiceInput}
                            on:focus={() => {
                                // При повторном фокусе открываем только если уже введено >= 2 символа
                                if (serviceSearchInput.trim().length >= 2) {
                                    showServiceDropdown = true;
                                }
                            }}
                            on:blur={() => {
                                // Задержка чтобы успеть кликнуть по пункту дропдауна
                                setTimeout(() => {
                                    if (serviceSearchInput.trim().length < 2) {
                                        showServiceDropdown = false;
                                    }
                                }, 150);
                            }}
                        />

                        {#if selectedService}
                            <div class="badge" in:scale>
                                <span class="txt">{selectedService.name}</span>
                                <button class="x" on:click={() => { selectedService = null; serviceSearchInput = ''; filteredServices = []; isNewService = false; }} type="button" aria-label="Удалить">✕</button>
                            </div>
                        {/if}

                        {#if showServiceDropdown && !selectedService}
                            <div class="drop shadow-xl" on:click|stopPropagation>
                                {#each filteredServices as s}
                                    <SearchDropdownItem
                                        title={s.name}
                                        subtitle="{s.durationInMinutes} мин"
                                        type="service"
                                        on:select={() => selectService(s)}
                                    />
                                {/each}
                                {#if isNewService}
                                    <div class="divider"></div>
                                    <SearchDropdownItem
                                        title="Создать новую услугу: {serviceSearchInput}"
                                        subtitle="Длительность будет взята из выбора ниже"
                                        icon="➕"
                                        type="action"
                                        on:select={() => startServiceCreation()}
                                    />
                                {/if}
                                {#if filteredServices.length === 0 && !isNewService}
                                    <div class="dropdown-empty" style="padding: 12px; color: #94a3b8; font-size: 13px;">
                                        Начните вводить название услуги (минимум 2 символа)...
                                    </div>
                                {/if}
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="tile-card dual">
                    <div class="part date-part">
                        <label>КОГДА (ВРЕМЯ ФИЛИАЛА)</label>
                        <input type="datetime-local" bind:value={formData.startTime} />
                    </div>
                    <div class="part duration-part" on:click|stopPropagation>
                        <label>ДЛИТЕЛЬНОСТЬ</label>
                        <button class="duration-v2-trigger" on:click={() => showDurationPicker = !showDurationPicker}>
                            <span class="val">{durationHours}ч {durationMinutes}м</span>
                            <span class="chevron">▼</span>
                        </button>
                        {#if showDurationPicker}
                            <div class="duration-v2-popover" in:fade={{duration: 100}}>
                                <div class="duration-v2-cols">
                                    <div class="duration-v2-col">
                                        <div class="duration-v2-col-label">ЧАСЫ</div>
                                        <div class="duration-v2-col-list">{#each HOURS_OPTIONS as h}<button class:active={durationHours===h} on:click={() => durationHours=h}>{h}</button>{/each}</div>
                                    </div>
                                    <div class="duration-v2-col border-l">
                                        <div class="duration-v2-col-label">МИНУТЫ</div>
                                        <div class="duration-v2-col-list">{#each MINS_OPTIONS as m}<button class:active={durationMinutes===m} on:click={() => {durationMinutes=m; showDurationPicker=false;}}>{m.toString().padStart(2, '0')}</button>{/each}</div>
                                    </div>
                                </div>
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="tile-card staff-card">
                    <label>ИСПОЛНИТЕЛИ ({formData.staffMemberIds.length})</label>
                    
                    <div class="selected-staff-container">
                        {#each formData.staffMemberIds as id}
                            {@const member = staffList.find(s => s.id === id)}
                            {#if member}
                                <div class="staff-badge" in:scale>
                                    {#if member.photoData}
                                        <img class="badge-avatar" src="data:image/jpeg;base64,{member.photoData}" alt={member.name} />
                                    {:else}
                                        <div class="badge-avatar-placeholder">{member.name.charAt(0)}</div>
                                    {/if}
                                    <div class="badge-info">
                                        <span class="badge-name">{member.name}</span>
                                        <span class="badge-spec">{member.specialty || 'Специалист'}</span>
                                    </div>
                                    <button class="btn-remove-staff" type="button" on:click={() => removeStaff(id)}>✕</button>
                                </div>
                            {/if}
                        {/each}
                    </div>

                    {#if staffList.filter(s => !formData.staffMemberIds.includes(s.id)).length > 0}
                        <div class="add-staff-select-wrapper">
                            <select on:change={addStaff} value="">
                                <option value="" disabled selected>+ Добавить исполнителя...</option>
                                {#each staffList.filter(s => !formData.staffMemberIds.includes(s.id)) as s}
                                    <option value={s.id}>{s.name} ({s.specialty || 'Специалист'})</option>
                                {/each}
                            </select>
                        </div>
                    {/if}
                </div>
                <div class="tile-card"><label>КАБИНЕТ / РЕСУРС</label>
                    <select bind:value={formData.resourceId}>
                        <option value={null}>Без ресурса</option>
                        {#each resources as r}<option value={r.id}>{r.name}</option>{/each}
                    </select>
                </div>

                <div class="tile-card reminder-panel">
                    <div class="rem-main">
                        <label>НАПОМИНАНИЕ (ТЕЛЕГРАМ/WA)</label>
                        <div class="rem-settings">
                            {#if formData.allowReminder}
                                <div class="hours-input" in:slide={{axis:'x'}}>
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

                <div class="tile-card comment-card">
                    <label>ЗАМЕТКА К ЗАПИСИ (ВНУТРЕННЯЯ)</label>
                    <textarea bind:value={formData.comment} placeholder="Например: аллергия на материалы..."></textarea>
                </div>
            </div>

            {#if isEditing && appointment.groupId}
                <div class="tile-card group-action-card" in:slide>
                    <div class="group-info">
                        <span class="group-icon">🔗</span>
                        <div>
                            <strong class="group-title">Связанная запись</strong>
                            <p class="group-desc">На этот объект/время назначено несколько мастеров.</p>
                        </div>
                    </div>
                    
                    <div class="group-radio-options">
                        <label class="radio-label">
                            <input type="radio" bind:group={updateMode} value="all" />
                            <span class="radio-text">Применить ко всей группе (ко всем мастерам)</span>
                        </label>
                        <label class="radio-label">
                            <input type="radio" bind:group={updateMode} value="single" />
                            <span class="radio-text">Применить только к этой записи</span>
                        </label>
                    </div>
                </div>
            {/if}

            <div class="footer-actions">
                <button class="btn-cancel" on:click={() => dispatch('cancel')}>ОТМЕНА</button>
                <button class="btn-save" on:click={handleSave} disabled={isSaving}>{isSaving ? '...' : (isEditing ? 'ОБНОВИТЬ' : 'ЗАПИСАТЬ')}</button>
            </div>
        </div>
    {/if}
</div>

<style>
    .appt-edit-root { 
        height: 100%; 
        display: flex; 
        flex-direction: column; 
        background: #f8fafc; 
        position: relative; 
        overflow-x: hidden; 
        align-items: center; /* Центрируем контент для мобильных */
    }
    
    .tiles-layout { 
        padding: 20px; 
        max-width: 500px; 
        width: 100%; 
        padding-bottom: 40px; 
        box-sizing: border-box;
        /* Убираем margin: 0 auto так как он не работает в flex контейнерах */
    }
    .tile-hero { background: white; padding: 20px; border-radius: 28px; display: flex; align-items: center; gap: 16px; border: 1px solid #f1f5f9; margin-bottom: 16px; transition: all 0.3s; }
    .tile-hero.is-new { background: #fff7ed; border-color: #ffedd5; }
    .avatar { width: 56px; height: 56px; background: var(--primary-gradient); color: white; border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 900; }
    .hero-body { flex: 1; position: relative; min-width: 0; }
    label { display: block; font-size: 9px; font-weight: 900; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }

    .rel-pos { position: relative; }
    .search-box { display: flex; align-items: center; gap: 8px; }
    .search-box input { width: 100%; padding: 10px 14px; border-radius: 14px; border: 1.5px solid #f1f5f9; background: white; font-size: 14px; outline: none; }
    .badge { position: absolute; left: 4px; right: 4px; top: 4px; bottom: 4px; background: #eff6ff; border-radius: 10px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; border: 1.5px solid #0ea5e9; }
    .badge .txt { font-weight: 700; color: #1e40af; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .x { background: white; border: none; color: #ef4444; font-weight: 800; cursor: pointer; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }

    .inline-phone-field { margin-top: 10px; display: flex; gap: 8px; align-items: center; }
    .inline-phone-field input { flex: 1; background: white; padding: 10px 14px; border-radius: 12px; border: 1.5px solid #fbbf24; font-size: 14px; font-weight: 700; color: #92400e; }
    .btn-cancel-new { background: #fef3c7; border: none; color: #d97706; width: 32px; height: 32px; border-radius: 10px; cursor: pointer; font-weight: 800; }

    .phone-select-area { margin-top: 12px; padding-top: 8px; border-top: 1px solid #f1f5f9; }
    .micro-label { font-size: 8px; color: #cbd5e1; font-weight: 900; margin-bottom: 6px; }
    .phone-chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .ph-chip { background: #f8fafc; border: 1.5px solid #f1f5f9; padding: 4px 10px; border-radius: 10px; font-size: 11px; font-weight: 700; color: #64748b; cursor: pointer; transition: all 0.2s; }
    .ph-chip.active { background: #eff6ff; border-color: #0ea5e9; color: #1e40af; }
    .phone-info-line { font-size: 12px; color: #64748b; font-weight: 700; margin-top: 6px; }

    .tiles-stack { display: flex; flex-direction: column; gap: 10px; }
    .tile-card { background: white; padding: 14px 18px; border-radius: 22px; border: 1px solid #f1f5f9; position: relative;}

    .reference-card { background: #f0fdf4; border-color: #bbf7d0; }
    .quick-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
    .tag-chip { background: white; border: 1px solid #dcfce7; padding: 4px 10px; border-radius: 8px; font-size: 11px; font-weight: 700; color: #166534; cursor: pointer; }
    .tag-chip.active { background: #22c55e; color: white; border-color: #22c55e; }

    .drop { position: absolute; top: calc(100% + 8px); left: 0; right: 0; background: white; border-radius: 22px; box-shadow: 0 25px 60px -15px rgba(0,0,0,0.2); z-index: 2000; border: 1px solid #e2e8f0; max-height: 280px; overflow-y: auto; padding: 8px; }
    .divider { height: 1px; background: #f1f5f9; margin: 4px 0; }

    .dual { display: grid; grid-template-columns: 1fr 140px; padding: 0; }
    .date-part { padding: 14px 18px; border-right: 1px solid #f1f5f9; }
    .duration-part { padding: 14px 18px; position: relative; background: #f8fafc; border-radius: 0 22px 22px 0; }
    .duration-v2-trigger { width: 100%; border: none; background: none; display: flex; align-items: center; justify-content: space-between; cursor: pointer; }
    .duration-v2-popover { position: absolute; top: 100%; right: 0; background: white; border-radius: 20px; border: 1px solid #e2e8f0; z-index: 3000; min-width: 180px; margin-top: 8px; box-shadow: 0 20px 50px rgba(0,0,0,0.15); }
    .duration-v2-cols { display: flex; }
    .duration-v2-col { flex: 1; padding: 8px; }
    .duration-v2-col-list { display: flex; flex-direction: column; max-height: 200px; overflow-y: auto; }
    .duration-v2-col-list button { border: none; background: none; padding: 8px; border-radius: 8px; cursor: pointer; }
    .duration-v2-col-list button.active { background: #0ea5e9; color: white; }

    .reminder-panel { display: flex; align-items: center; justify-content: space-between; }
    .hours-input { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: #1e293b; }
    .hours-input input { width: 40px; padding: 4px; border-radius: 8px; border: 1.5px solid #e2e8f0; text-align: center; font-weight: 800; color: #0ea5e9; background: #f8fafc; }
    .rem-off { margin: 0; font-size: 14px; color: #94a3b8; font-weight: 600; }
    .toggle-switch { width: 44px; height: 24px; background: #e2e8f0; border-radius: 12px; border: none; position: absolute; right: 18px; cursor: pointer; transition: background 0.3s; }
    .toggle-switch.on { background: #10b981; }
    .switch-handle { width: 18px; height: 18px; background: white; border-radius: 50%; position: absolute; top: 3px; left: 3px; transition: transform 0.3s; }
    .toggle-switch.on .switch-handle { transform: translateX(20px); }

    textarea { width: 100%; border: none; background: #f8fafc; border-radius: 14px; padding: 12px; font-size: 14px; color: #1e293b; font-weight: 600; resize: none; min-height: 80px; outline: none; margin-top: 8px; border: 1px solid #f1f5f9; }
    input, select { width: 100%; border: none; background: none; font-size: 15px; font-weight: 700; color: #1e293b; outline: none; }
    .footer-actions { display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-top: 24px; }
    .btn-cancel { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 14px; border-radius: 18px; font-weight: 700; cursor: pointer; }
    .btn-save { background: var(--primary-gradient); color: white; border: none; padding: 14px; border-radius: 18px; font-weight: 800; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }
    .spinner { width: 28px; height: 28px; border: 3px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .loader-center { display: flex; justify-content: center; align-items: center; height: 200px; }

    /* Мультивыбор сотрудников */
    .staff-card { background: #f8fafc; border-color: #e2e8f0; }
    .selected-staff-container { display: flex; flex-direction: column; gap: 8px; margin-top: 10px; margin-bottom: 12px; }
    .staff-badge { display: flex; align-items: center; gap: 10px; background: white; padding: 8px 12px; border-radius: 14px; border: 1px solid #e2e8f0; position: relative; }
    .badge-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
    .badge-avatar-placeholder { width: 32px; height: 32px; border-radius: 50%; background: #0ea5e9; color: white; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 14px; }
    .badge-info { flex: 1; display: flex; flex-direction: column; }
    .badge-name { font-size: 13px; font-weight: 700; color: #1e293b; }
    .badge-spec { font-size: 10px; color: #64748b; font-weight: 600; }
    .btn-remove-staff { background: none; border: none; color: #ef4444; font-size: 14px; font-weight: 700; cursor: pointer; padding: 4px; display: flex; align-items: center; justify-content: center; }
    
    .add-staff-select-wrapper { background: white; border: 1px dashed #cbd5e1; border-radius: 14px; padding: 8px 12px; position: relative; }
    .add-staff-select-wrapper select { color: #64748b; font-size: 13px; cursor: pointer; }
    
    /* Карточка группового обновления */
    .group-action-card { background: #fef2f2; border-color: #fca5a5; margin-top: 12px; }
    .group-info { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 12px; }
    .group-icon { font-size: 18px; }
    .group-title { font-size: 13px; font-weight: 800; color: #991b1b; display: block; }
    .group-desc { font-size: 11px; color: #b91c1c; margin: 2px 0 0 0; font-weight: 600; }
    .group-radio-options { display: flex; flex-direction: column; gap: 8px; border-top: 1px solid #fecaca; padding-top: 10px; }
    .radio-label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
    .radio-label input { width: auto; margin: 0; }
    .radio-text { font-size: 12px; font-weight: 700; color: #7f1d1d; }
</style>
