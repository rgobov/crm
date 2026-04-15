<script>

    import { onMount, createEventDispatcher } from 'svelte';

    import { adminService } from '$lib/services/adminService.js';

    import { contactService } from '$lib/services/contactService.js';

    import { serviceService } from '$lib/services/serviceService.js';

    import { resourceService } from '$lib/services/resourceService.js';

    import { branchService } from '$lib/services/branchService.js';

    import { activeBranchId } from '$lib/stores/dashboardStore.js';

    import { timeUtils } from '$lib/utils/timeUtils.js';

    import SearchDropdownItem from '../SearchDropdownItem.svelte';

    import { fade, scale, slide } from 'svelte/transition';



    export let appointment = null;

    export let preselected = { date: new Date(), hour: 10, min: 0, staffId: null };



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

        resourceId: '',

        branchId: '',

        status: 'SCHEDULED',

        comment: '',

        referenceTag: '',

        allowReminder: true,

        reminderLeadTimeHours: 24

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

            staffList = staffData.filter(s => s.role === 'EMPLOYEE' || s.role === 'ROLE_EMPLOYEE');



            if (isEditing) {

                formData = {

                    ...appointment,

                    staffMemberId: appointment.staffMemberId || (appointment.staffMember ? appointment.staffMember.id : null),

                    branchId: appointment.branchId || (appointment.branch ? appointment.branch.id : $activeBranchId),

                    allowReminder: appointment.allowReminder ?? true,

                    reminderLeadTimeHours: appointment.reminderLeadTimeHours ?? 24,

                    status: appointment.status || 'SCHEDULED',

                    comment: appointment.comment || '',

                    referenceTag: appointment.referenceTag || '',

                    clientPhone: appointment.clientPhone || ''

                };

                durationHours = Math.floor(formData.durationInMinutes / 60);

                durationMinutes = formData.durationInMinutes % 60;

                formData.startTime = timeUtils.toBranchLocalISO(appointment.startTime, currentBranchData?.timezone);

                serviceSearchInput = appointment.service;



                if (appointment.contactId) {

                    const c = await contactService.getContactById(appointment.contactId);

                    if (c) selectContact(c);

                }

            } else {

                formData.staffMemberId = preselected.staffId || '';

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

        filteredServices = query ? services.filter(s => s.name.toLowerCase().includes(query)) : services;

        isNewService = query !== '' && !services.some(s => s.name.toLowerCase() === query);

    }



    function selectService(s) {

        formData.service = s.name;

        durationHours = Math.floor(s.durationInMinutes / 60);

        durationMinutes = s.durationInMinutes % 60;

        serviceSearchInput = s.name;

        showServiceDropdown = false;

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



    async function handleSave() {

        if (!searchInput.trim()) return alert('Укажите имя клиента');

        if (isNewClientMode && !newClientPhone.trim()) return alert('Укажите телефон нового клиента');

        if (!currentBranchData) return alert('Данные филиала еще загружаются...');



        isSaving = true;

        try {

            let contactId = formData.contactId;

            let clientName = searchInput.trim();



            if (isNewClientMode) {

                const newContact = await contactService.addContact({

                    name: clientName,

                    phones: [newClientPhone.trim()],

                    tags: formData.referenceTag ? [formData.referenceTag] : []

                }, $activeBranchId);

                contactId = newContact.id;

            }



            let sName = serviceSearchInput.trim() || "Стандарт";

            if (isNewService && sName !== "Стандарт") {

                const ns = await serviceService.addService({ name: sName, durationInMinutes: formData.durationInMinutes });

                sName = ns.name;

            }



            const correctedStartTime = timeUtils.fromBranchLocalToUTC(formData.startTime, currentBranchData.timezone);



            const payload = {

                ...formData,

                service: sName,

                clientName: clientName,

                contactId: contactId,

                startTime: correctedStartTime,

                branchId: $activeBranchId

            };



            if (isEditing) {

                await adminService.updateAppointment(appointment.id, payload);

            } else {

                await adminService.createAppointment(payload);

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



<div class="appt-edit-desktop" on:click|stopPropagation on:keydown={(e) => e.key === 'Escape' && dispatch('cancel')} role="presentation">

    {#if isLoading}

        <div class="loader-center"><span class="spinner"></span></div>

    {:else}

        <div class="tiles-layout" in:fade>

            <section class="tile-hero" class:is-new={isNewClientMode}>

                <div class="avatar" aria-hidden="true">{isNewClientMode ? '✨' : (selectedContact ? selectedContact.name.charAt(0).toUpperCase() : '?')}</div>

                <div class="hero-body">

                    <label for="client-search-input">{isNewClientMode ? 'НОВЫЙ КЛИЕНТ' : 'КЛИЕНТ ЗАПИСИ'}</label>

                    <div class="search-box rel-pos" role="search">

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

                        |{/if}



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

                                            subtitle={(c.tags && c.tags.length > 0) ? `🚗 ${c.tags.join(', ')}` : (c.phones[0] || 'Нет номера')}

                                            type="client"

                                        />

                                    </button>

                                {/each}

                            </div>

                        {/if}

                    </div>



                    {#if isNewClientMode}

                        <div class="inline-phone-field" in:slide>

                            <label for="new-client-phone" class="invisible-label">Телефон</label>

                            <input id="new-client-phone" type="tel" bind:value={newClientPhone} placeholder="Номер телефона..." on:keydown={(e) => e.key === 'Enter' && handleSave()} />

                            <button class="btn-cancel-new" on:click={() => isNewClientMode = false} type="button">✕</button>

                        </div>

                    {/if}

                </div>

            </section>



            <div class="tiles-stack">

                <div class="tile-card reference-card">

                    <label for="ref-tag-id">АВТОМОБИЛЬ / ОБЪЕКТ</label>

                    <div class="tag-input-wrap">

                        <input id="ref-tag-id" type="text" bind:value={formData.referenceTag} placeholder="Марка, модель, госномер..." />

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

                        <input id="service-search-id" type="text" bind:value={serviceSearchInput} placeholder="Поиск услуги..." on:focus={() => showServiceDropdown = true} />

                        {#if showServiceDropdown}

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



                <div class="tile-card">

                    <label for="staff-select-id">ИСПОЛНИТЕЛЬ</label>

                    <select id="staff-select-id" bind:value={formData.staffMemberId}>

                        <option value="">Не назначен</option>

                        {#each staffList as s}<option value={s.id}>{s.name}</option>{/each}

                    </select>

                </div>



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

    .appt-edit-desktop { height: 100%; display: flex; flex-direction: column; background: #fdf6e3; position: relative; overflow-x: hidden; }

    .tiles-layout { padding: 24px; max-width: 500px; margin: 0 auto; width: 100%; padding-bottom: 40px; }



    .tile-hero {

        background: #eee8d5; padding: 20px; border-radius: 28px;

        display: flex; align-items: center; gap: 16px;

        border: 1.5px solid #ddd6c1; margin-bottom: 16px; transition: all 0.3s;

    }

    .tile-hero.is-new { border-color: #b58900; background: rgba(181, 137, 0, 0.05); }



    .avatar {

        width: 56px; height: 56px;

        background: #268bd2; color: #fdf6e3;

        border-radius: 20px; display: flex; align-items: center; justify-content: center;

        font-size: 24px; font-weight: 900;

    }



    label { display: block; font-size: 9px; font-weight: 900; color: #93a1a1; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }



    .search-box { position: relative; width: 100%; }

    .search-box input {

        width: 100%; padding: 10px 14px; border-radius: 14px;

        border: 1.5px solid #ddd6c1; background: #fdf6e3;

        font-size: 14px; outline: none; color: #073642; font-weight: 700;

    }

    .badge { position: absolute; left: 4px; right: 4px; top: 4px; bottom: 4px; background: #eee8d5; border-radius: 10px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; border: 1.5px solid #268bd2; z-index: 5; }

    .badge .txt { font-weight: 800; color: #268bd2; }

    .x { background: #fdf6e3; border: 1px solid #ddd6c1; color: #dc322f; width: 24px; height: 24px; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: 800; }



    .tiles-stack { display: flex; flex-direction: column; gap: 10px; }

    .tile-card { background: #eee8d5; padding: 14px 18px; border-radius: 22px; border: 1.5px solid #ddd6c1; }



    .reference-card { background: rgba(133, 153, 0, 0.05); border-color: rgba(133, 153, 0, 0.2); }

    .tag-chip { background: #fdf6e3; border: 1px solid #ddd6c1; padding: 4px 10px; border-radius: 8px; font-size: 11px; font-weight: 700; color: #859900; cursor: pointer; }

    .tag-chip.active { background: #859900; color: #fdf6e3; border-color: #859900; }



    .drop { position: absolute; top: calc(100% + 8px); left: 0; right: 0; background: #fdf6e3; border-radius: 22px; z-index: 2000; border: 1.5px solid #ddd6c1; max-height: 280px; overflow-y: auto; padding: 8px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); }

    .dropdown-action-btn { width: 100%; border: none; background: transparent; padding: 0; text-align: left; cursor: pointer; border-radius: 12px; transition: 0.2s; }

    .dropdown-action-btn:hover { background: #eee8d5; }



    .dual { display: grid; grid-template-columns: 1fr 140px; padding: 0; overflow: hidden; }

    .date-part { padding: 14px 18px; border-right: 1.5px solid #ddd6c1; }

    .duration-part { padding: 14px 18px; background: rgba(147, 161, 161, 0.05); }



    .duration-v2-popover { position: absolute; top: 100%; right: 0; background: #fdf6e3; border-radius: 20px; border: 1.5px solid #ddd6c1; z-index: 3000; min-width: 180px; margin-top: 8px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); }

    .duration-v2-col-list button.active { background: #268bd2; color: #fdf6e3; }



    .solar-toggle { width: 44px; height: 22px; background: #93a1a1; border-radius: 11px; border: none; position: relative; cursor: pointer; transition: 0.3s; }

    .solar-toggle.on { background: #859900; }

    .solar-toggle .dot { width: 16px; height: 16px; background: #fdf6e3; border-radius: 50%; position: absolute; top: 3px; left: 3px; transition: 0.3s; }

    .solar-toggle.on .dot { transform: translateX(22px); }



    textarea, select { width: 100%; border: none; background: transparent; font-size: 15px; font-weight: 700; color: #073642; outline: none; }

    textarea { font-size: 14px; min-height: 80px; resize: none; }



    .footer-actions { display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-top: 24px; }

    .btn-primary-solar { background: #268bd2; color: #fdf6e3; border: none; padding: 16px; border-radius: 18px; font-weight: 900; cursor: pointer; border-bottom: 3px solid #2aa198; transition: 0.2s; }

    .btn-primary-solar:active { transform: translateY(2px); border-bottom-width: 1px; }

    .btn-secondary-solar { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 16px; border-radius: 18px; font-weight: 800; cursor: pointer; }



    .spinner { width: 28px; height: 28px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }

    @keyframes spin { to { transform: rotate(360deg); } }

    .loader-center { display: flex; justify-content: center; align-items: center; height: 200px; }

    .invisible-label { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); border: 0; }

    .solar-divider-thin { height: 1px; background: #ddd6c1; margin: 4px 8px; }

</style>

