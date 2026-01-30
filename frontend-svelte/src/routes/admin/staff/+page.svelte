<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { goto } from '$app/navigation';

    let staff = [];
    let isLoading = true;
    let tg = null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin'));
        }
        await loadStaff();
    });

    async function loadStaff() {
        isLoading = true;
        try {
            staff = await staffService.getStaff();
        } catch (e) {
            console.error('Failed to load staff');
        } finally {
            isLoading = false;
        }
    }

    async function handleDelete(id, name) {
        const confirmed = confirm(`Вы уверены, что хотите удалить сотрудника ${name}?`);
        if (confirmed) {
            try {
                await staffService.deleteStaffMember(id);
                if (tg) tg.HapticFeedback.notificationOccurred('success');
                await loadStaff();
            } catch (e) {
                alert('Ошибка при удалении');
            }
        }
    }
</script>

<div class="page">
    <div class="header">
        <h1>Сотрудники</h1>
        <p>Управление вашей командой ({staff.length})</p>
    </div>

    {#if isLoading}
        <div class="center">
            <span class="spinner"></span>
        </div>
    {:else if staff.length === 0}
        <div class="empty-state">
            <div class="icon">👥</div>
            <p>У вас пока нет сотрудников</p>
            <button class="add-btn" on:click={() => goto('/admin/staff/new')}>Добавить первого</button>
        </div>
    {:else}
        <div class="staff-list">
            {#each staff as member}
                <div class="staff-card card">
                    <div class="avatar">{member.name.charAt(0)}</div>
                    <div class="info">
                        <h3>{member.name}</h3>
                        <p>{member.specialty}</p>
                        {#if member.phone}
                            <span class="phone">📱 {member.phone}</span>
                        {/if}
                    </div>
                    <div class="actions">
                        <button class="edit-btn" on:click={() => goto(`/admin/staff/${member.id}`)}>✎</button>
                        <button class="delete-btn" on:click={() => handleDelete(member.id, member.name)}>🗑</button>
                    </div>
                </div>
            {/each}
        </div>

        <button class="fab" on:click={() => goto('/admin/staff/new')}>+</button>
    {/if}
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; }

    .header h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .header p { color: var(--hint-color); margin: 4px 0 24px 0; font-size: 14px; }

    .staff-list { display: grid; gap: 12px; }

    .staff-card {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 16px;
        background: white;
        border-radius: 20px;
        border: 1px solid rgba(0,0,0,0.02);
    }

    .avatar {
        width: 50px;
        height: 50px;
        background: #f1f5f9;
        color: var(--primary-color);
        border-radius: 16px;
        display: flex;
        justify-content: center;
        align-items: center;
        font-weight: 800;
        font-size: 20px;
    }

    .info { flex: 1; }
    .info h3 { margin: 0; font-size: 16px; font-weight: 700; color: #1e293b; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }
    .phone { font-size: 11px; color: var(--primary-color); font-weight: 600; margin-top: 4px; display: block; }

    .actions { display: flex; gap: 8px; }
    .actions button {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        border: none;
        cursor: pointer;
        display: flex;
        justify-content: center;
        align-items: center;
        font-size: 16px;
        transition: transform 0.1s;
    }
    .actions button:active { transform: scale(0.9); }
    .edit-btn { background: #eff6ff; color: var(--primary-color); }
    .delete-btn { background: #fef2f2; color: #ef4444; }

    .fab {
        position: fixed;
        bottom: 90px;
        right: 20px;
        width: 56px;
        height: 56px;
        background: var(--primary-gradient);
        color: white;
        border: none;
        border-radius: 18px;
        font-size: 28px;
        font-weight: 300;
        box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4);
        cursor: pointer;
        z-index: 100;
    }

    .empty-state { text-align: center; padding: 60px 20px; }
    .empty-state .icon { font-size: 64px; margin-bottom: 16px; }
    .add-btn { background: var(--primary-gradient); color: white; border: none; padding: 12px 24px; border-radius: 14px; font-weight: 700; margin-top: 16px; cursor: pointer; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
