<script>
    import { isMobile } from '$lib/stores/dashboardStore.js';
    import DayTimelineDesktop from './desktop/DayTimelineDesktop.svelte';
    import DayTimelineMobile from './mobile/DayTimelineMobile.svelte';
    import { createEventDispatcher } from 'svelte';

    const dispatch = createEventDispatcher();

    // Прокидываем пропсы
    export let day = new Date();
    export let appointments = [];
    export let columns = [];
    export let columnKey = 'staffMemberId';

    // Универсальный проброс событий (bubbling)
    function forward(event) {
        dispatch(event.type, event.detail);
    }
</script>

{#if $isMobile}
    <!-- МОБИЛЬНАЯ ВЕРСИЯ: 3 колонки, Единый скролл, Snap -->
    <DayTimelineMobile
        {day}
        {appointments}
        {columns}
        {columnKey}
        on:appointmentTap={forward}
        on:emptySlotTap={forward}
        on:staffTap={forward}
        on:resourceTap={forward}
        on:refresh={forward}
    />
{:else}
    <!-- ДЕСКТОПНАЯ ВЕРСИЯ: Изолированная структура -->
    <DayTimelineDesktop
        {day}
        {appointments}
        {columns}
        {columnKey}
        on:appointmentTap={forward}
        on:emptySlotTap={forward}
        on:staffTap={forward}
        on:resourceTap={forward}
        on:refresh={forward}
    />
{/if}
