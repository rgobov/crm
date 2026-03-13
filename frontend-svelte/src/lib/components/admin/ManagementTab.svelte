<script>
    import { isMobile } from '$lib/stores/ui.js';
    import ManagementTabDesktop from './desktop/ManagementTabDesktop.svelte';
    import ManagementTabMobile from './mobile/ManagementTabMobile.svelte';
</script>

{#if $isMobile}
    <ManagementTabMobile />
{:else}
    <ManagementTabDesktop />
{/if}
