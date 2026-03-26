<script>
    import { isMobile } from '$lib/stores/ui.js';
    import ManagementTabDesktop from './desktop/ManagementTabDesktop.svelte';
    import ManagementTabMobile from './mobile/ManagementTabMobile.svelte';

    // Прокидываем пропсы вниз
    export let forcedDate = null;
    export let branchId = null;
</script>

{#if $isMobile}
    <ManagementTabMobile {forcedDate} {branchId} />
{:else}
    <ManagementTabDesktop {forcedDate} {branchId} />
{/if}
