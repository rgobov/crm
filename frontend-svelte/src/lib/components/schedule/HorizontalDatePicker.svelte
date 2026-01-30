<script>
    import { onMount, createEventDispatcher, afterUpdate } from 'svelte';

    export let selectedDate = new Date();
    const dispatch = createEventDispatcher();

    let scrollContainer;
    let days = [];
    const weekNames = ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];

    // Генерируем 100 дней (50 до и 50 после выбранной даты)
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

    // Центрируем выбранную дату после каждого обновления
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
        background: white;
        border-top: 1px solid #f1f5f9;
        scrollbar-width: none; /* Скрываем скроллбар */
        -ms-overflow-style: none;
    }
    .date-picker-wrapper::-webkit-scrollbar { display: none; }

    .days-strip {
        display: flex;
        padding: 12px 10px;
        gap: 8px;
    }

    .day-btn {
        min-width: 60px;
        height: 74px;
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: #f8fafc;
        border: 1px solid #f1f5f9;
        border-radius: 16px;
        cursor: pointer;
        position: relative;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .day-btn.is-selected {
        background: var(--primary-gradient);
        border-color: var(--primary-color);
        box-shadow: 0 8px 15px rgba(56, 151, 240, 0.3);
        transform: translateY(-2px);
    }

    .day-btn.is-selected .week-name,
    .day-btn.is-selected .day-num {
        color: white !important;
    }

    .week-name {
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        color: #94a3b8;
        margin-bottom: 4px;
    }
    .week-name.weekend { color: #ef4444; opacity: 0.8; }

    .day-num {
        font-size: 18px;
        font-weight: 800;
        color: #1e293b;
    }

    .is-today:not(.is-selected) {
        border: 1.5px solid var(--primary-color);
        background: #eff6ff;
    }

    .today-dot {
        position: absolute;
        bottom: 8px;
        width: 4px;
        height: 4px;
        background: var(--primary-color);
        border-radius: 50%;
    }
</style>
