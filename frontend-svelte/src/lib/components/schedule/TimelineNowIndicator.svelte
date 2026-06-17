<script>
    export let nowLinePos;
    export let label = "";
    export let mode = "dot"; // 'dot' или 'line'
</script>

{#if nowLinePos >= 0}
    {#if mode === 'dot'}
        <div class="dot-wrapper" style="top: {nowLinePos}px">
            {#if label}
                <div class="time-label">{label}</div>
            {/if}
            <div class="dot"></div>
        </div>
    {:else}
        <div class="line" style="top: {nowLinePos}px"></div>
    {/if}
{/if}

<style>
    .dot-wrapper {
        position: absolute;
        right: 0;
        width: 12px;
        height: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 450; /* Выше карточек (320), но ниже меню (1000+) */
        transform: translateY(-50%) translateX(50%);
        pointer-events: none;
    }

    .dot {
        width: 12px;
        height: 12px;
        background: #dc322f; /* Solarized Red */
        border-radius: 50%;
        box-shadow: 0 0 15px rgba(220, 50, 47, 0.8);
        animation: pulse-red 2s infinite;
    }

    .time-label {
        position: absolute;
        bottom: 100%;
        background: #dc322f;
        color: white;
        font-size: 10px;
        font-weight: 900;
        padding: 2px 6px;
        border-radius: 6px;
        margin-bottom: 6px;
        white-space: nowrap;
        box-shadow: 0 4px 10px rgba(220, 50, 47, 0.3);
    }

    .line {
        position: absolute;
        left: 0;
        right: 0;
        height: 2px;
        background: #dc322f;
        z-index: 400; /* Выше сетки и карточек, но ниже навигации */
        transform: translateY(-50%);
        pointer-events: none;
        box-shadow: 0 0 8px rgba(220, 50, 47, 0.4);
    }

    @keyframes pulse-red {
        0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(220, 50, 47, 0.7); }
        70% { transform: scale(1.3); box-shadow: 0 0 0 10px rgba(220, 50, 47, 0); }
        100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(220, 50, 47, 0); }
    }
</style>
