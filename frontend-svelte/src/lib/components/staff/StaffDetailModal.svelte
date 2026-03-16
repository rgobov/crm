<script>
    import { createEventDispatcher, onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { phoneUtils } from '$lib/utils/phoneUtils.js';
    import { fade, scale, slide } from 'svelte/transition';

    export let staffId;
    const dispatch = createEventDispatcher();

    let staff = null;
    let allBranches = [];
    let isLoading = true;

    // Режимы редактирования полей
    let editMode = { name: false, specialty: false, phone: false, role: false, photo: false, branches: false };
    let tempValues = {
        name: '', specialty: '', phone: '', role: '',
        active: true, photoUrl: '', branchIds: []
    };

    onMount(async () => {
        await Promise.all([loadStaff(), loadBranches()]);
    });

    async function loadStaff() {
        isLoading = true;
        try {
            staff = await staffService.getStaffById(staffId);
            syncTemp();
        } catch (e) {
            console.error('Staff Load Error');
        } finally {
            isLoading = false;
        }
    }

    async function loadBranches() {
        try {
            allBranches = await branchService.getBranches();
        } catch (e) {
            console.error('Branches Load Error');
        }
    }

    function syncTemp() {
        if (!staff) return;
        console.log('Syncing staff data:', staff);

        tempValues = {
            name: staff.name || '',
            specialty: staff.specialty || '',
            phone: staff.phone || '',
            role: staff.role || 'EMPLOYEE',
            active: staff.active,
            photoUrl: staff.photoUrl || '',
            // ✅ ИСПРАВЛЕНО: Используем branchIds напрямую из DTO бэкенда
            branchIds: (staff.branchIds && Array.isArray(staff.branchIds)) ? [...staff.branchIds] : []
        };

        console.log('Initial branchIds set to:', tempValues.branchIds);
    }

    async function saveField(field) {
        try {
            const requestData = {
                ...tempValues,
                available: tempValues.active, // Маппинг на поле active в бэкенде через CreateStaffRequest
                branchIds: tempValues.branchIds // Отправляем массив ID выбранных филиалов
            };

            const result = await staffService.updateStaff(staffId, requestData);
            staff = result;
            editMode[field] = false;
            dispatch('updated', staff);
        } catch (e) {
            alert('Не удалось сохранить изменения');
            syncTemp();
        }
    }

    function toggleBranch(branchId) {
        if (tempValues.branchIds.includes(branchId)) {
            tempValues.branchIds = tempValues.branchIds.filter(id => id !== branchId);
        } else {
            tempValues.branchIds = [...tempValues.branchIds, branchId];
        }
    }

    async function toggleActive() {
        tempValues.active = !tempValues.active;
        await saveField('active');
    }
</script>

<div class="modal-backdrop" on:click|self={() => dispatch('close')} transition:fade={{duration: 200}}>
    <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
        <div class="modal-header">
            <h2>Профиль сотрудника</h2>
            <button class="btn-close" on:click={() => dispatch('close')}>✕</button>
        </div>

        <div class="modal-body">
            {#if isLoading}
                <div class="center"><span class="spinner"></span></div>
            {:else if staff}
                <!-- ГЕРОЙ-СЕКЦИЯ -->
                <div class="hero-section">
                    <div class="avatar-box">
                        {#if staff.photoUrl}
                            <img src={staff.photoUrl} alt={staff.name} class="avatar-image" />
                        {:else}
                            {staff.name ? staff.name.charAt(0) : '?'}
                        {/if}
                    </div>

                    <div class="inline-edit-wrap">
                        {#if editMode.name}
                            <div class="edit-input-group">
                                <input type="text" bind:value={tempValues.name} autofocus />
                                <button class="btn-tick" on:click={() => saveField('name')}>✓</button>
                                <button class="btn-cross" on:click={() => { editMode.name = false; syncTemp(); }}>✕</button>
                            </div>
                        {:else}
                            <h3 on:click={() => editMode.name = true}>{staff.name} <span>✎</span></h3>
                        {/if}
                    </div>

                    <div class="status-toggle-wrap">
                        <button class="toggle-pill" class:active={tempValues.active} on:click={toggleActive}>
                            <span class="dot"></span>
                            <span class="label">{tempValues.active ? 'АКТИВЕН' : 'ЗАБЛОКИРОВАН'}</span>
                        </button>
                    </div>
                </div>

                <div class="details-list">
                    <!-- СЕКЦИЯ ФИЛИАЛОВ -->
                    <div class="info-tile branch-tile">
                        <label>Доступные филиалы</label>
                        {#if editMode.branches}
                            <div class="branch-selector-grid" in:slide>
                                {#each allBranches as b}
                                    <button
                                        class="branch-chip"
                                        class:selected={tempValues.branchIds.includes(b.id)}
                                        on:click={() => toggleBranch(b.id)}
                                    >
                                        {b.name}
                                    </button>
                                {/each}
                            </div>
                            <div class="edit-actions-row">
                                <button class="btn-save-mini" on:click={() => saveField('branches')}>СОХРАНИТЬ</button>
                                <button class="btn-cancel-mini" on:click={() => { editMode.branches = false; syncTemp(); }}>ОТМЕНА</button>
                            </div>
                        {:else}
                            <div class="branch-view-row" on:click={() => editMode.branches = true}>
                                <div class="chips-list">
                                    {#if tempValues.branchIds.length > 0}
                                        {#each allBranches.filter(b => tempValues.branchIds.includes(b.id)) as b}
                                            <span class="chip-static">{b.name}</span>
                                        {/each}
                                    {:else}
                                        <span class="no-branches">Не привязан к филиалам</span>
                                    {/if}
                                </div>
                                <span>✎</span>
                            </div>
                        {/if}
                    </div>

                    <div class="info-tile">
                        <label>Должность / Специализация</label>
                        {#if editMode.specialty}
                            <div class="edit-input-group">
                                <input type="text" bind:value={tempValues.specialty} autofocus />
                                <button class="btn-tick" on:click={() => saveField('specialty')}>✓</button>
                            </div>
                        {:else}
                            <p on:click={() => editMode.specialty = true}>
                                {staff.specialty || 'Не указана'} <span>✎</span>
                            </p>
                        {/if}
                    </div>

                    <div class="info-tile">
                        <label>Контактный телефон</label>
                        {#if editMode.phone}
                            <div class="edit-input-group">
                                <input type="tel" bind:value={tempValues.phone} autofocus />
                                <button class="btn-tick" on:click={() => saveField('phone')}>✓</button>
                            </div>
                        {:else}
                            <p on:click={() => editMode.phone = true}>
                                {staff.phone ? phoneUtils.format(staff.phone) : 'Не указан'} <span>✎</span>
                            </p>
                        {/if}
                    </div>

                    <div class="info-tile">
                        <label>Уровень доступа (Роль)</label>
                        {#if editMode.role}
                            <div class="edit-input-group">
                                <select bind:value={tempValues.role}>
                                    <option value="ADMIN">ADMIN</option>
                                    <option value="MANAGER">MANAGER</option>
                                    <option value="EMPLOYEE">EMPLOYEE</option>
                                </select>
                                <button class="btn-tick" on:click={() => saveField('role')}>✓</button>
                            </div>
                        {:else}
                            <p on:click={() => editMode.role = true}>
                                <span class="badge-role">{staff.role}</span> <span>✎</span>
                            </p>
                        {/if}
                    </div>
                </div>
            {/if}
        </div>
    </div>
</div>

<style>
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal-content { background: #f8fafc; width: 100%; max-width: 460px; border-radius: 32px; overflow: hidden; box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.4); max-height: 90vh; overflow-y: auto; }

    .modal-header { padding: 20px 24px; display: flex; justify-content: space-between; align-items: center; background: white; border-bottom: 1px solid #f1f5f9; position: sticky; top: 0; z-index: 10; }
    .modal-header h2 { margin: 0; font-size: 17px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }
    .btn-close { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; font-weight: 800; cursor: pointer; color: #94a3b8; }

    .modal-body { padding: 24px; }

    .hero-section { text-align: center; margin-bottom: 28px; }
    .avatar-box { width: 84px; height: 84px; background: var(--primary-gradient); color: white; border-radius: 28px; display: flex; align-items: center; justify-content: center; font-size: 36px; font-weight: 900; margin: 0 auto 16px; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); overflow: hidden; }
    .avatar-image { width: 100%; height: 100%; object-fit: cover; }

    h3 { margin: 0; font-size: 22px; font-weight: 800; color: #0f172a; cursor: pointer; }
    h3 span { color: var(--primary-color); opacity: 0.4; font-size: 16px; margin-left: 4px; }

    .status-toggle-wrap { margin-top: 16px; display: flex; justify-content: center; }
    .toggle-pill { display: flex; align-items: center; gap: 8px; padding: 6px 16px 6px 8px; border-radius: 20px; border: 1.5px solid #e2e8f0; background: white; cursor: pointer; transition: all 0.3s; }
    .toggle-pill .dot { width: 12px; height: 12px; border-radius: 50%; background: #94a3b8; }
    .toggle-pill .label { font-size: 10px; font-weight: 900; color: #94a3b8; letter-spacing: 0.5px; }

    .toggle-pill.active { border-color: #10b981; background: #f0fdf4; }
    .toggle-pill.active .dot { background: #10b981; box-shadow: 0 0 8px #10b981; }
    .toggle-pill.active .label { color: #166534; }

    .details-list { display: grid; gap: 12px; }
    .info-tile { background: white; padding: 16px; border-radius: 20px; border: 1px solid #f1f5f9; cursor: pointer; transition: transform 0.1s; }
    .info-tile:active { transform: scale(0.99); }

    label { display: block; font-size: 10px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }
    p { margin: 0; font-size: 15px; font-weight: 600; color: #1e293b; display: flex; align-items: center; justify-content: space-between; }
    p span { color: var(--primary-color); opacity: 0.4; }

    .branch-view-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
    .chips-list { display: flex; flex-wrap: wrap; gap: 6px; }
    .chip-static { background: #f1f5f9; color: #475569; font-size: 12px; font-weight: 700; padding: 4px 10px; border-radius: 8px; }
    .no-branches { color: #94a3b8; font-style: italic; font-size: 14px; }

    .branch-selector-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
    .branch-chip { border: 1.5px solid #e2e8f0; background: white; color: #64748b; padding: 8px 14px; border-radius: 12px; font-size: 13px; font-weight: 700; cursor: pointer; transition: 0.2s; }
    .branch-chip.selected { border-color: var(--primary-color); background: #eff6ff; color: var(--primary-color); }

    .edit-actions-row { display: flex; gap: 8px; margin-top: 8px; }
    .btn-save-mini { flex: 1; background: var(--primary-color); color: white; border: none; padding: 8px; border-radius: 10px; font-weight: 800; font-size: 11px; cursor: pointer; }
    .btn-cancel-mini { background: #f1f5f9; color: #64748b; border: none; padding: 8px 16px; border-radius: 10px; font-weight: 700; font-size: 11px; cursor: pointer; }

    .badge-role { background: #eff6ff; color: var(--primary-color); padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 800; }

    .edit-input-group { display: flex; gap: 8px; width: 100%; }
    input, select { flex: 1; padding: 10px 14px; border: 2.5px solid var(--primary-color); border-radius: 12px; font-size: 15px; outline: none; background: white; color: #0f172a; font-weight: 600; }

    .btn-tick { background: #10b981; color: white; border: none; width: 40px; height: 40px; border-radius: 12px; font-weight: bold; cursor: pointer; }
    .btn-cross { background: #f1f5f9; color: #64748b; border: none; width: 40px; height: 40px; border-radius: 12px; cursor: pointer; }

    .spinner { width: 24px; height: 24px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { display: flex; justify-content: center; padding: 40px; }
</style>
