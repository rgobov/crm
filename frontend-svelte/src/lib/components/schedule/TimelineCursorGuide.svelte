<script>
    export let y;
    export let timeStr;
    export let timeColWidth;
</script>

<div class="cursor-guide" style="top: {y}px">
    <!-- ЛИПКИЙ БЕЙДЖИК: Всегда по центру шкалы времени -->
    <div class="sticky-label-wrapper" style="width: {timeColWidth}px">
        <div class="guide-label">{timeStr}</div>
    </div>
    <!-- ЛИНИЯ: Тянется вправо через сетку -->
    <div class="guide-line"></div>
</div>

<style>
    .cursor-guide {
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

    .sticky-label-wrapper {
        position: sticky;
        left: 0;
        flex-shrink: 0;
        height: 20px;
        z-index: 410;
        pointer-events: none;
        display: flex;
        align-items: center;
        justify-content: center; /* Центрируем бейджик внутри ширины шкалы */
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
        /* Убеждаемся, что бейджик не "прыгает" */
        display: inline-flex;
        align-items: center;
        justify-content: center;
    }

    .guide-line {
        flex: 1;
        height: 1px;
        border-top: 1.5px dashed rgba(59, 130, 246, 0.4);
        /* Линия начинается сразу за шкалой времени */
    }
</style>
