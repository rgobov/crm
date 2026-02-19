<script>
    export let y;
    export let timeStr;
    export let timeColWidth;
</script>

<div class="guide-root" style="top: {y}px">
    <!-- БЕЙДЖИК: Липнет к левому краю, всегда в колонке времени -->
    <div class="sticky-label-container" style="width: {timeColWidth}px">
        <div class="label-box">{timeStr}</div>
    </div>
    <!-- ЛИНИЯ: Тянется вправо -->
    <div class="line"></div>
</div>

<style>
    .guide-root {
        position: absolute;
        left: 0;
        right: 0;
        pointer-events: none;
        z-index: 400;
        display: flex;
        align-items: center;
        transform: translateY(-50%);
        height: 1px;
    }

    .sticky-label-container {
        position: sticky;
        left: 0;
        flex-shrink: 0;
        display: flex;
        justify-content: center;
        z-index: 410;
    }

    .label-box {
        background: #3b82f6;
        color: white;
        font-size: 10px;
        font-weight: 900;
        padding: 2px 6px;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.5);
        white-space: nowrap;
    }

    .line {
        flex: 1;
        height: 1px;
        border-top: 1.5px dashed rgba(59, 130, 246, 0.4);
    }
</style>
