<script>
    import { onMount, createEventDispatcher } from 'svelte';

    export let selectedDate = new Date();
    const dispatch = createEventDispatcher();

    let scrollContainer;
    let days = [];
    const weekNames = ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];

    $: {
        const tempDays = [];
        const start = new Date(selectedDate);
        start.setDate(start.getDate() - 50);

        for (let i = 0; i < 100; i++) {
            const date = new Date(start);
            date.setDate(start.getDate() + i);
            tempDays.push(date);
        }
        days = tempDays;
    }

    onMount(() => {
        scrollToSelected();
    });

    function scrollToSelected() {
        if (!scrollContainer) return;
        const selectedEl = scrollContainer.querySelector('.is-selected');
        if (selectedEl) {
            const containerWidth = scrollContainer.offsetWidth;
            const elementOffset = selectedEl.offsetLeft;
            const elementWidth = selectedEl.offsetWidth;
            scrollContainer.scrollTo({
                left: elementOffset - (containerWidth / 2) + (elementWidth / 2),
                behavior: 'smooth'
            });
        }
    }

    function isSameDay(d1, d2) {
        return d1.getFullYear() === d2.getFullYear() &&
               d1.getMonth() === d2.getMonth() &&
               d1.getDate() === d2.getDate();
    }

    function isToday(date) {
        const today = new Date();
        return isSameDay(date, today);
    }

    function selectDate(date) {
        dispatch('dateSelected', { date });
        scrollToSelected();
    }
</script>

<div class="date-picker-wrapper" bind:this={scrollContainer}>
    <div class="days-strip">
        {#each days as date}
            {@const isWeekend = date.getDay() === 0 || date.getDay() === 6}
            {@const selected = isSameDay(date, selectedDate)}
            {@const today = isToday(date)}

            <button
                class="day-btn"
                class:is-selected={selected}
                class:is-today={today}
                on:click={() => selectDate(date)}
            >
                <span class="week-name" class:weekend={isWeekend && !selected}>
                    {weekNames[date.getDay()]}
                </span>
                <span class="day-num">{date.getDate()}</span>
                {#if today && !selected}
                    <span class="today-dot"></span>
                {/if}
            </button>
        {/each}
    </div>
</div>

<style>
    .date-picker-wrapper {
        width: 100%;
        overflow-x: auto;
        background: #eee8d5; /* Solarized Base2 */
        scrollbar-width: none;
        -ms-overflow-style: none;
        border-bottom: 1px solid #ddd6c1;
    }
    .date-picker-wrapper::-webkit-scrollbar { display: none; }

    .days-strip {
        display: flex;
        padding: 6px 8px; /* Уменьшены отступы */
        gap: 5px;
    }

    .day-btn {
        min-width: 42px; /* Уменьшено с 48px */
        height: 48px;    /* Уменьшено с 58px - пропорция золотого сечения к таймлайну */
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: #fdf6e3;
        border: 1px solid #ddd6c1;
        border-radius: 10px;
        cursor: pointer;
        position: relative;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .day-btn.is-selected {
        background: #268bd2;
        border-color: #268bd2;
        box-shadow: 0 3px 8px rgba(38, 139, 210, 0.2);
        transform: translateY(-1px);
    }

    .day-btn.is-selected .week-name,
    .day-btn.is-selected .day-num {
        color: white !important;
    }

    .week-name {
        font-size: 8px; /* Уменьшено с 9px */
        font-weight: 850;
        text-transform: uppercase;
        color: #93a1a1;
        margin-bottom: 1px;
    }
    .week-name.weekend { color: #dc322f; opacity: 0.9; }

    .day-num {
        font-size: 13px; /* Уменьшено с 15px */
        font-weight: 900;
        color: #073642;
    }

    .is-today:not(.is-selected) {
        border: 1.2px solid #268bd2;
        background: #eee8d5;
    }

    .today-dot {
        position: absolute;
        bottom: 4px;
        width: 3px;
        height: 3px;
        background: #268bd2;
        border-radius: 50%;
    }
</style>
