<script>
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import ManagementTab from '$lib/components/admin/ManagementTab.svelte';
    import CalendarTab from '$lib/components/admin/CalendarTab.svelte';

    const tabs = {
        management: ManagementTab,
        calendar: CalendarTab,
        timeline: CalendarTab
    };

    // Входящие пропсы от Layout (если нужно прокинуть дату)
    export let forcedDate = null;
</script>

<div class="tab-viewport">
    <svelte:component
        this={tabs[$activeTab]}
        {forcedDate}
    />
</div>

<style>
    .tab-viewport { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
</style>
