<script>
    import { onMount } from 'svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import CalendarTab from '$lib/components/admin/CalendarTab.svelte'; // Календарь загрузки
    import ContactsScreen from '$lib/routes/admin/clients/+page.svelte'; // Используем существующий роут как компонент

    // В SvelteKit лучше импортировать компоненты напрямую
    import ClientsList from '$lib/components/contacts/ContactsList.svelte'; // Нам нужно будет вынести список клиентов в компонент

    let activeTab = 'schedule'; // schedule, calendar, clients

    const tabs = [
        { id: 'schedule', title: 'Расписание', icon: '🕒' },
        { id: 'calendar', title: 'Календарь', icon: '📅' },
        { id: 'clients', title: 'Клиенты', icon: '👥' }
    ];
</script>

<div class="manager-layout">
    <main class="content">
        {#if activeTab === 'schedule'}
            <ScheduleScreen />
        {:else if activeTab === 'calendar'}
            <CalendarTab />
        {:else if activeTab === 'clients'}
            <div class="p-20">
                <!-- В будущем вынесем список клиентов в отдельный компонент для переиспользования -->
                <p class="center-text">Раздел клиентов для менеджера в разработке</p>
            </div>
        {/if}
    </main>

    <nav class="bottom-nav">
        {#each tabs as tab}
            <button
                class="nav-item"
                class:active={activeTab === tab.id}
                on:click={() => activeTab = tab.id}
            >
                <span class="icon">{tab.icon}</span>
                <span class="label">{tab.title}</span>
            </button>
        {/each}
    </nav>
</div>

<style>
    .manager-layout {
        display: flex;
        flex-direction: column;
        height: 100vh;
        background: #f8fafc;
    }

    .content {
        flex: 1;
        overflow-y: auto;
        padding-bottom: 80px; /* Отступ для навигации */
    }

    .bottom-nav {
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        background: white;
        display: flex;
        justify-content: space-around;
        padding: 12px 0;
        border-top: 1px solid #f1f5f9;
        z-index: 1000;
        box-shadow: 0 -4px 15px rgba(0,0,0,0.03);
    }

    .nav-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        border: none;
        background: none;
        color: #94a3b8;
        cursor: pointer;
        transition: all 0.2s;
    }

    .nav-item.active {
        color: var(--primary-color);
    }

    .icon { font-size: 20px; }
    .label { font-size: 10px; font-weight: 700; text-transform: uppercase; }

    .center-text { text-align: center; padding: 40px; color: #64748b; }
    .p-20 { padding: 20px; }
</style>
