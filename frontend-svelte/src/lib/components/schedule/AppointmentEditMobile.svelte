<!-- MOBILE_VERSION_V2_SOLARIZED -->
<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { contactService } from '$lib/services/contactService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { nicheSettings, activeNiche } from '$lib/stores/nicheStore.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import SearchDropdownItem from './SearchDropdownItem.svelte';
    import { fade, scale, slide } from 'svelte/transition';

    export let appointment = null;
    export let preselected = { date: new Date(), hour: 10, min: 0, staffId: null };
    export let service = adminService;

    const dispatch = createEventDispatcher();
    const isEditing = !!appointment;

    let formData = {
        startTime: '',
        durationInMinutes: 60,
        contactId: '',
        clientName: '',
        clientPhone: '',
        service: '',
        staffMemberId: '',
        staffMemberIds: [],
        resourceId: '',
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

    let services = [];
    let resources = [];
    let staffList = [];
    let updateMode = 'all';
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
                serviceService.getServices($activeNiche).catch(e => { console.error('Services load failed:', e); return []; }),
                resourceService.getResources($activeBranchId).catch(e => { console.error('Resources load failed:', e); return []; }),
                service.getStaffForSchedule(isEditing ? new Date(appointment.startTime) : preselected.date, $activeBranchId).catch(e => { console.error('Staff load failed:', e); return []; })
            ]);

            services = servicesData || [];
            resources = resourcesData || [];
            staffList = (staffData || []).map(s => ({
                ...s,
                id: s.id ? String(s.id) : null
            }));

            // Отладочная информация
            console.log('Resources loaded:', resources);
            console.log('Active branch ID:', $activeBranchId);
            console.log('Resources count:', resources.length);

            if (isEditing) {
                formData = {
                    ...appointment,
                    staffMemberId: appointment.staffMemberId ? String(appointment.staffMemberId) : (appointment.staffMember?.id ? String(appointment.staffMember.id) : ''),
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
                    if (c) selectContact(c);
                }
            } else {
                const initialStaff = preselected.staffId ? [String(preselected.staffId)] : [];
                formData.staffMemberId = preselected.staffId ? String(preselected.staffId) : '';
                formData.staffMemberIds = initialStaff;
                formData.resourceId = preselected.resourceId ? String(preselected.resourceId) : null;
                formData.branchId = $activeBranchId;
                const pad = n => n < 10 ? '0'+n : n;
                const dateStr = timeUtils.toBranchLocalDateStr(preselected.date, currentBranchData?.timezone);
                formData.startTime = `${dateStr}T${pad(preselected.hour)}:${pad(preselected.min)}`;
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

    function handleClientInput() {
        if (selectedContact || isNewClientMode) return;
        clearTimeout(debounceTimer);
        const q = searchInput.trim();
        if (q.length < 2) { searchResults = []; return; }

        debounceTimer = setTimeout(async () => {
            const res = await contactService.getContacts(q, true, 0, 10);
            searchResults = res.content || [];
        }, 300);
    }

    function handleKeyDown(e) {
        if (e.key === 'Enter') {
            if (searchResults.length > 0) {
                selectContact(searchResults[0]);
            } else if (searchInput.length >= 2) {
                startInlineCreation();
            }
        }
    }

    function selectContact(contact) {
        isNewClientMode = false;
        selectedContact = contact;
        formData.contactId = contact.id;
        formData.clientName = contact.name;
        formData.clientPhone = contact.phones?.[0] || '';
        searchInput = contact.name;
        searchResults = [];
    }

    function startInlineCreation() {
        isNewClientMode = true;
        selectedContact = null;
        formData.contactId = "";
        formData.clientName = searchInput;
        const digits = searchInput.replace(/\D/g, "");
        if (digits.length >= 10) newClientPhone = digits;
        searchResults = [];
    }

    function addStaff(event) {
        const id = event.target.value;
        if (id && !formData.staffMemberIds.includes(id)) {
            formData.staffMemberIds = [...formData.staffMemberIds, id];
        }
        event.target.value = "";
    }

    function removeStaff(id) {
        formData.staffMemberIds = formData.staffMemberIds.filter(sid => sid !== id);
    }

    async function handleSave() {
        if (!searchInput.trim()) return alert('Укажите имя клиента');
        let finalPhone = isNewClientMode ? newClientPhone.trim() : formData.clientPhone;
        if (!finalPhone) return alert('Укажите номер телефона');
        if (!currentBranchData) return alert('Данные филиала еще загружаются...');
        if (formData.staffMemberIds.length === 0) return alert('Выберите хотя бы одного исполнителя');

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
            const firstStaffId = formData.staffMemberIds[0];

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
                    await service.updateAppointment(appointment.id, payload, false, updateMode);
                } else {
                    await service.createAppointment(payload, false);
                }
            } catch (err) {
                if (err.response && err.response.status === 409) {
                    if (confirm('Один из выбранных сотрудников занят или не работает в это время. Все равно сохранить запись?')) {
                        if (isEditing) {
                            await service.updateAppointment(appointment.id, payload, true, updateMode);
                        } else {
                            await service.createAppointment(payload, true);
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
</script>

<div class="appt-edit-mobile" on:click|stopPropagation on:keydown={(e) => e.key === 'Escape' && dispatch('cancel')} role="presentation">
    {#if isLoading}
        <div class="loader-center"><span class="spinner"></span></div>
    {:else}
        <div class="tiles-layout" in:fade>
            <!-- Секция Клиент (БЕЗ АВАТАРА, НА ВСЮ ШИРИНУ) -->
            <section class="tile-hero" class:is-new={isNewClientMode}>
                <div class="hero-body-full">
                    <label for="client-search-input">{isNewClientMode ? 'НОВЫЙ КЛИЕНТ' : 'КЛИЕНТ ЗАПИСИ'}</label>
                    <div class="search-box">
                        <input
                            id="client-search-input"
                            type="text"
                            bind:value={searchInput}
                            on:input={handleClientInput}
                            on:keydown={handleKeyDown}
                            placeholder="Имя, телефон или номер..."
                            class:invisible={!!selectedContact}
                        />

                        {#if selectedContact}
                            <div class="badge" in:scale>
                                <span class="txt">{selectedContact.name}</span>
                                <button class="x" on:click={() => { selectedContact = null; searchInput = ''; isNewClientMode = false; }} type="button" aria-label="Удалить">✕</button>
                            </div>
                        {/if}

                        {#if searchResults.length > 0 || (searchInput.length >= 2 && !selectedContact && !isNewClientMode)}
                            <div class="drop" role="listbox">
                                {#if searchInput.length >= 2 && !selectedContact}
                                    <button class="dropdown-action-btn" on:click={startInlineCreation} type="button" role="option" aria-selected="false">
                                        <SearchDropdownItem
                                            title="Создать: {searchInput}"
                                            subtitle="Нажмите для регистрации"
                                            icon="✨"
                                            type="action"
                                        />
                                    </button>
                                    <div class="solar-divider-thin"></div>
                                {/if}
                                {#each searchResults as c}
                                    <button class="dropdown-action-btn" on:click={() => selectContact(c)} type="button" role="option" aria-selected="false">
                                        <SearchDropdownItem
                                            title={c.name}
                                            subtitle={(c.tags && c.tags.length > 0) ? `${$nicheSettings.assetIcon} ${c.tags.join(', ')}` : (c.phones[0] || 'Нет номера')}
                                            type="client"
                                        />
                                    </button>
                                {/each}
                            </div>
                        {/if}
                    </div>

                    {#if isNewClientMode}
                        <div class="inline-phone-field" in:slide>
                            <input type="tel" bind:value={newClientPhone} placeholder="Телефон..." />
                            <button class="btn-cancel-new" on:click={() => isNewClientMode = false} type="button" aria-label="Отмена">✕</button>
                        </div>
                    {/if}
                </div>
            </section>

            <div class="tiles-stack">
                <div class="tile-card staff-card">
                    {#if $activeNiche !== 'RENT'}
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
                    {:else}
                        <label for="resource-select-mobile">ОБЪЕКТ АРЕНДЫ</label>
                        <select id="resource-select-mobile" bind:value={formData.resourceId}>
                            <option value="">Выберите объект...</option>
                            {#each resources as r}<option value={r.id}>{r.name}</option>{/each}
                        </select>
                        {#if resources.length === 0}
                            <p style="color: red; font-size: 12px; margin-top: 4px;">
                                Ресурсы не загружены (count: {resources.length})
                            </p>
                        {/if}
                    {/if}
                </div>

                {#if isEditing && formData.groupId}
                    <div class="tile-card group-action-card" in:slide>
                        <div class="group-info">
                            <span class="group-icon">🔗</span>
                            <div>
                                <span class="group-title">Групповая запись</span>
                                <p class="group-desc">Этот визит связан с другими мастерами. Выберите, как применить изменения:</p>
                            </div>
                        </div>
                        <div class="group-radio-options">
                            <label class="radio-label">
                                <input type="radio" name="updateMode" value="all" bind:group={updateMode} />
                                <span class="radio-text">Обновить все связанные записи</span>
                            </label>
                            <label class="radio-label">
                                <input type="radio" name="updateMode" value="single" bind:group={updateMode} />
                                <span class="radio-text">Обновить только текущую запись</span>
                            </label>
                        </div>
                    </div>
                {/if}

                <div class="tile-card reference-card">
                    <label for="ref-tag-id">{$nicheSettings.refLabel}</label>
                    <div class="tag-input-wrap">
                        <input id="ref-tag-id" type="text" bind:value={formData.referenceTag} placeholder={$nicheSettings.refPlaceholder} />
                        {#if selectedContact?.tags?.length > 0}
                            <div class="quick-tags" in:slide>
                                {#each selectedContact.tags as tag}
                                    <button type="button" class="tag-chip" class:active={formData.referenceTag === tag} on:click={() => formData.referenceTag = tag}>
                                        {tag}
                                    </button>
                                {/each}
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="tile-card rel-pos" role="presentation">
                    <label for="service-search-id">УСЛУГА</label>
                    <div class="input-rel">
                        <input 
                            id="service-search-id" 
                            type="text" 
                            bind:value={serviceSearchInput} 
                            placeholder="Поиск услуги..." 
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
                            <div class="drop" role="listbox">
                                {#each filteredServices as s}
                                    <button class="dropdown-action-btn" on:click={() => selectService(s)} type="button" role="option" aria-selected="false">
                                        <SearchDropdownItem
                                            title={s.name}
                                            subtitle="{s.durationInMinutes} мин"
                                            type="service"
                                        />
                                    </button>
                                {/each}

                                {#if isNewService}
                                    <div class="solar-divider-thin"></div>
                                    <button class="dropdown-action-btn" on:click={startServiceCreation} type="button" role="option" aria-selected="false">
                                        <SearchDropdownItem
                                            title="Создать новую услугу: {serviceSearchInput}"
                                            subtitle="Нажмите для регистрации"
                                            icon="✨"
                                            type="action"
                                        />
                                    </button>
                                {/if}

                                {#if filteredServices.length === 0 && !isNewService}
                                    <div class="dropdown-empty" style="padding: 12px; color: #93a1a1; font-size: 13px;">
                                        Начните вводить название услуги (минимум 2 символа)...
                                    </div>
                                {/if}
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="tile-card dual">
                    <div class="part date-part">
                        <label for="start-time-id">ВРЕМЯ ФИЛИАЛА</label>
                        <input id="start-time-id" type="datetime-local" bind:value={formData.startTime} />
                    </div>
                    <div class="part duration-part" role="presentation">
                        <label>ДЛИТЕЛЬНОСТЬ</label>
                        <button class="duration-v2-trigger" on:click={() => showDurationPicker = !showDurationPicker} type="button">
                            <span class="val">{durationHours}ч {durationMinutes}м</span>
                            <span class="chevron" aria-hidden="true">▼</span>
                        </button>
                        {#if showDurationPicker}
                            <div class="duration-v2-popover">
                                <div class="duration-v2-cols">
                                    <div class="duration-v2-col">
                                        <div class="duration-v2-col-label">ЧАСЫ</div>
                                        <div class="duration-v2-col-list">{#each HOURS_OPTIONS as h}<button type="button" class:active={durationHours===h} on:click={() => durationHours=h}>{h}</button>{/each}</div>
                                    </div>
                                    <div class="duration-v2-col border-l">
                                        <div class="duration-v2-col-label">МИНУТЫ</div>
                                        <div class="duration-v2-col-list">{#each MINS_OPTIONS as m}<button type="button" class:active={durationMinutes===m} on:click={() => {durationMinutes=m; showDurationPicker=false;}}>{m.toString().padStart(2, '0')}</button>{/each}</div>
                                    </div>
                                </div>
                            </div>
                        {/if}
                    </div>
                </div>

                {#if $activeNiche !== 'RENT'}
                    <div class="tile-card">
                        <label for="resource-select-id">КАБИНЕТ / РЕСУРС</label>
                        <select id="resource-select-id" bind:value={formData.resourceId}>
                            <option value="">Без ресурса</option>
                            {#each resources as r}<option value={r.id}>{r.name}</option>{/each}
                        </select>
                        <!-- Отладочная информация -->
                        {#if resources.length === 0}
                            <p style="color: red; font-size: 12px; margin-top: 4px;">
                                Ресурсы не загружены (count: {resources.length})
                            </p>
                        {/if}
                    </div>
                {/if}

                <div class="tile-card reminder-panel">
                    <div class="rem-main">
                        <label for="rem-hours-id">НАПОМИНАНИЕ (TG/WA)</label>
                        <div class="rem-settings">
                            {#if formData.allowReminder}
                                <div class="hours-input" in:slide={{axis:'x'}}>
                                    <span>за</span>
                                    <input id="rem-hours-id" type="number" bind:value={formData.reminderLeadTimeHours} min="1" max="168" />
                                    <span>ч. до визита</span>
                                </div>
                            {:else}
                                <p class="rem-off">Отключено</p>
                            {/if}
                        </div>
                    </div>
                    <button class="solar-toggle" class:on={formData.allowReminder} on:click={() => formData.allowReminder = !formData.allowReminder} type="button" aria-label="Переключить напоминание">
                        <div class="dot"></div>
                    </button>
                </div>

                <div class="tile-card comment-card">
                    <label for="comment-id">ВНУТРЕННЯЯ ЗАМЕТКА</label>
                    <textarea id="comment-id" bind:value={formData.comment} placeholder="Например: пожелания клиента..."></textarea>
                </div>
            </div>

            <div class="footer-actions">
                <button class="btn-secondary-solar" on:click={() => dispatch('cancel')} type="button">ОТМЕНА</button>
                <button class="btn-primary-solar" on:click={handleSave} disabled={isSaving} type="button">
                    {isSaving ? '...' : (isEditing ? 'ОБНОВИТЬ' : 'ЗАПИСАТЬ')}
                </button>
            </div>
        </div>
    {/if}
</div>

<style>
    .appt-edit-mobile { 
        height: 100%; 
        display: flex; 
        flex-direction: column; 
        background: #fdf6e3; 
        position: relative; 
        overflow-x: hidden; 
        align-items: center;
    }
    .tiles-layout { 
        padding: 20px; 
        width: 100%; 
        max-width: 500px; 
        padding-bottom: 60px; 
        box-sizing: border-box;
    }

    /* Плитка клиента: убрали flex, сделали блочной */
    .tile-hero {
        background: #eee8d5; 
        padding: 16px; 
        border-radius: 20px;
        border: 1.5px solid #ddd6c1; 
        margin-bottom: 12px;
    }
    .tile-hero.is-new { border-color: #b58900; background: rgba(181, 137, 0, 0.05); }

    .hero-body-full { 
        width: 100%; 
    }

    label { display: block; font-size: 9px; font-weight: 900; color: #93a1a1; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }

    .search-box { position: relative; width: 100%; }
    .search-box input {
        width: 100%; padding: 12px 14px; border-radius: 14px;
        border: 1.5px solid #ddd6c1; background: #fdf6e3;
        font-size: 16px; outline: none; color: #073642; font-weight: 700;
        box-sizing: border-box;
    }
    .badge { position: absolute; left: 4px; right: 4px; top: 4px; bottom: 4px; background: #eee8d5; border-radius: 10px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; border: 1.5px solid #268bd2; z-index: 5; }
    .badge .txt { font-weight: 800; color: #268bd2; font-size: 14px; }
    .x { background: #fdf6e3; border: 1px solid #ddd6c1; color: #dc322f; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: 800; flex-shrink: 0; }

    .tiles-stack { display: flex; flex-direction: column; gap: 8px; }
    .tile-card { background: #eee8d5; padding: 16px 18px; border-radius: 20px; border: 1.5px solid #ddd6c1; }

    .tag-input-wrap input { 
        width: 100%; 
        border: 1.5px solid #ddd6c1; 
        background: #fdf6e3; 
        font-size: 15px; 
        font-weight: 700; 
        color: #073642; 
        outline: none; 
        padding: 12px 14px; 
        border-radius: 12px;
        min-height: 44px;
        box-sizing: border-box;
        transition: all 0.2s;
    }
    
    .tag-input-wrap input:focus {
        border-color: #268bd2;
        background: white;
        box-shadow: 0 0 0 3px rgba(38, 139, 210, 0.1);
    }
    .quick-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
    .tag-chip { 
        background: #fdf6e3; 
        border: 1.5px solid #ddd6c1; 
        padding: 10px 16px; 
        border-radius: 14px; 
        font-size: 13px; 
        font-weight: 700; 
        color: #859900; 
        cursor: pointer; 
        min-height: 44px; 
        display: flex; 
        align-items: center;
        transition: all 0.2s;
    }
    
    .tag-chip:hover {
        border-color: #859900;
        transform: translateY(-1px);
    }
    
    .tag-chip.active { 
        background: #859900; 
        color: #fdf6e3; 
        border-color: #859900; 
    }

    .drop { position: absolute; top: calc(100% + 8px); left: 0; right: 0; background: #fdf6e3; border-radius: 18px; z-index: 2000; border: 1.5px solid #ddd6c1; max-height: 250px; overflow-y: auto; padding: 6px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
    .dropdown-action-btn { width: 100%; border: none; background: transparent; padding: 0; text-align: left; cursor: pointer; border-radius: 10px; }

    /* Разделяем Время и Длительность для мобильных */
    .dual { display: flex; flex-direction: column; padding: 0; overflow: visible; gap: 0; }
    .date-part { padding: 14px 18px; border-right: none; border-bottom: 1.5px solid #ddd6c1; }
    .duration-part { padding: 14px 18px; background: transparent; position: relative; }

    /* Увеличиваем высоту триггера выбора длительности */
    .duration-v2-trigger { width: 100%; border: none; background: #fdf6e3; border: 1px solid #ddd6c1; border-radius: 12px; display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; cursor: pointer; min-height: 48px; }
    .duration-v2-trigger .val { font-size: 15px; font-weight: 700; color: #073642; }
    .duration-v2-trigger .chevron { font-size: 10px; color: #93a1a1; }

    .duration-v2-popover { position: absolute; bottom: 100%; right: 0; background: #fdf6e3; border-radius: 20px; border: 1.5px solid #ddd6c1; z-index: 3000; min-width: 180px; margin-bottom: 8px; box-shadow: 0 -10px 30px rgba(0,0,0,0.1); }
    .duration-v2-cols { display: flex; }
    .duration-v2-col { flex: 1; padding: 10px; }
    .duration-v2-col-list { display: flex; flex-direction: column; max-height: 180px; overflow-y: auto; }
    .duration-v2-col-list button { border: none; background: none; padding: 12px; border-radius: 8px; font-size: 14px; font-weight: 700; color: #586e75; cursor: pointer; }
    .duration-v2-col-list button.active { background: #268bd2; color: #fdf6e3; }

    .reminder-panel { display: flex; align-items: center; justify-content: space-between; }
    .hours-input { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: #586e75; }
    .hours-input input { width: 45px; padding: 4px; border-radius: 8px; border: 1.5px solid #ddd6c1; text-align: center; background: #fdf6e3; font-weight: 800; color: #268bd2; }

    .solar-toggle { width: 40px; height: 20px; background: #93a1a1; border-radius: 10px; border: none; position: relative; cursor: pointer; transition: 0.3s; flex-shrink: 0; }
    .solar-toggle.on { background: #859900; }
    .solar-toggle .dot { width: 14px; height: 14px; background: #fdf6e3; border-radius: 50%; position: absolute; top: 3px; left: 3px; transition: 0.3s; }
    .solar-toggle.on .dot { transform: translateX(20px); }

    textarea, select, .date-part input { 
        width: 100%; 
        border: none; 
        background: transparent; 
        font-size: 16px; 
        font-weight: 700; 
        color: #073642; 
        outline: none; 
        min-height: 44px; /* Увеличиваем для удобства нажатия */
        padding: 8px 0;
    }
    textarea { 
        font-size: 16px; 
        min-height: 80px; 
        resize: none; 
        line-height: 1.4;
        padding: 12px;
        border: 1.5px solid #ddd6c1;
        background: #fdf6e3;
        border-radius: 12px;
        color: #073642;
        font-weight: 600;
    }
    
    /* Увеличиваем шрифт на больших мобильных экранах */
    @media (min-width: 414px) {
        textarea { font-size: 17px; }
    }
    
    @media (min-width: 390px) {
        textarea { font-size: 16.5px; }
    }
    
    textarea:focus {
        border-color: #268bd2;
        background: white;
        box-shadow: 0 0 0 3px rgba(38, 139, 210, 0.1);
        outline: none;
    }

    .footer-actions { 
        display: grid; 
        grid-template-columns: 1fr 2fr; 
        gap: 12px; 
        margin-top: 24px; 
        padding: 0 20px 20px;
    }
    .btn-primary-solar { 
        background: #268bd2; 
        color: #fdf6e3; 
        border: none; 
        padding: 16px; 
        border-radius: 16px; 
        font-weight: 900; 
        cursor: pointer; 
        border-bottom: 3px solid #2aa198; 
        min-height: 52px;
        font-size: 16px;
    }
    .btn-secondary-solar { 
        background: #fdf6e3; 
        color: #586e75; 
        border: 1.5px solid #ddd6c1; 
        padding: 16px; 
        border-radius: 16px; 
        font-weight: 800; 
        cursor: pointer; 
        min-height: 52px;
        font-size: 16px;
    }

    .spinner { width: 24px; height: 24px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .loader-center { display: flex; justify-content: center; align-items: center; height: 200px; }
    .rel-pos { position: relative; }
    
    .input-rel { position: relative; }
    
    .input-rel input {
        width: 100%;
        border: 1.5px solid #ddd6c1;
        background: #fdf6e3;
        font-size: 16px;
        font-weight: 700;
        color: #073642;
        outline: none;
        padding: 12px 14px;
        border-radius: 12px;
        min-height: 44px;
        box-sizing: border-box;
        transition: all 0.2s;
    }
    
    .input-rel input:focus {
        border-color: #268bd2;
        background: white;
        box-shadow: 0 0 0 3px rgba(38, 139, 210, 0.1);
    }
    .invisible-label { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); border: 0; }
    .solar-divider-thin { height: 1px; background: #ddd6c1; margin: 4px 8px; }
    .inline-phone-field { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
    .inline-phone-field input { flex: 1; padding: 10px; border-radius: 12px; border: 1.5px solid #b58900; background: #fdf6e3; font-size: 15px; font-weight: 700; }
    .btn-cancel-new { background: #eee8d5; border: 1.5px solid #ddd6c1; color: #dc322f; width: 36px; height: 36px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 900; }

    /* Мультивыбор сотрудников (Solarized) */
    .staff-card { background: #eee8d5; border-color: #ddd6c1; }
    .selected-staff-container { display: flex; flex-direction: column; gap: 8px; margin-top: 10px; margin-bottom: 12px; }
    .staff-badge { display: flex; align-items: center; gap: 10px; background: #fdf6e3; padding: 8px 12px; border-radius: 14px; border: 1.5px solid #ddd6c1; position: relative; }
    .badge-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
    .badge-avatar-placeholder { width: 32px; height: 32px; border-radius: 50%; background: #268bd2; color: white; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 14px; }
    .badge-info { flex: 1; display: flex; flex-direction: column; }
    .badge-name { font-size: 13px; font-weight: 700; color: #073642; }
    .badge-spec { font-size: 10px; color: #586e75; font-weight: 600; }
    .btn-remove-staff { background: none; border: none; color: #dc322f; font-size: 16px; font-weight: 700; cursor: pointer; padding: 4px; display: flex; align-items: center; justify-content: center; }
    
    .add-staff-select-wrapper { background: #fdf6e3; border: 1.5px dashed #ddd6c1; border-radius: 14px; padding: 8px 12px; position: relative; }
    .add-staff-select-wrapper select { color: #586e75; font-size: 13px; cursor: pointer; }
    
    /* Карточка группового обновления (Solarized) */
    .group-action-card { background: rgba(220, 50, 47, 0.05); border-color: #dc322f; margin-top: 12px; }
    .group-info { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 12px; }
    .group-icon { font-size: 18px; }
    .group-title { font-size: 13px; font-weight: 800; color: #dc322f; display: block; }
    .group-desc { font-size: 11px; color: #586e75; margin: 2px 0 0 0; font-weight: 600; }
    .group-radio-options { display: flex; flex-direction: column; gap: 8px; border-top: 1.5px solid rgba(220, 50, 47, 0.1); padding-top: 10px; }
    .radio-label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
    .radio-label input { width: auto; margin: 0; min-height: auto; }
    .radio-text { font-size: 12px; font-weight: 700; color: #586e75; }
</style>
