<script>
    export let y;
    export let timeStr = "";
    export let mode = "line"; // 'label' или 'line'
</script>

{#if y >= 0}
    {#if mode === 'label'}
        <div class="guide-label-wrapper" style="top: {y}px">
            <div class="guide-label">{timeStr}</div>
        </div>
    {:else}
        <div class="guide-line" style="top: {y}px"></div>
    {/if}
{/if}

<style>
    .guide-label-wrapper {
        position: absolute;
        left: 0;
        right: 0;
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 410;
        transform: translateY(-50%);
        pointer-events: none;
    }

    .guide-label {
        background: #3b82f6;
        color: white;
        font-size: 10px;
        font-weight: 900;
        padding: 2px 6px;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.5);
        white-space: nowrap;
    }

    .guide-line {
        position: absolute;
        left: 0;
        right: 0;
        height: 1px;
        border-top: 1.5px dashed rgba(59, 130, 246, 0.4);
        z-index: 110;
        transform: translateY(-50%);
        pointer-events: none;
    }
</style>
