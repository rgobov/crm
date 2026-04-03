<script>
    import { isMobile } from '$lib/stores/ui.js';
    import CalendarTabDesktop from './desktop/CalendarTabDesktop.svelte';
    import CalendarTabMobile from './mobile/CalendarTabMobile.svelte';

    // Прокидываем пропсы вниз
    export let forcedDate = null;
</script>

{#if $isMobile}
    <CalendarTabMobile {forcedDate} />
{:else}
    <CalendarTabDesktop {forcedDate} />
{/if}
