<script>
    import { activeTab, activeBranchId } from '$lib/stores/dashboardStore.js';
    import ManagementTab from '$lib/components/admin/ManagementTab.svelte';
    import CalendarTab from '$lib/components/admin/CalendarTab.svelte';

    const tabs = {
        management: ManagementTab,
        calendar: CalendarTab,
        timeline: CalendarTab
    };

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
