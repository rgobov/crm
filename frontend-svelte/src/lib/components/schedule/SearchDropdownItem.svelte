<script>
    import { createEventDispatcher } from 'svelte';
    import { fade } from 'svelte/transition';

    export let title = "";
    export let subtitle = "";
    export let icon = ""; // Текст для аватара или иконка
    export let type = "client"; // 'client' или 'service'

    const dispatch = createEventDispatcher();
</script>

<button class="dropdown-item" on:click={() => dispatch('select')} in:fade={{duration: 150}}>
    <div class="item-visual" class:is-service={type === 'service'}>
        {#if type === 'service'}
            <span class="icon">⭐</span>
        {:else}
            <span class="avatar">{icon || title.charAt(0).toUpperCase()}</span>
        {/if}
    </div>

    <div class="item-info">
        <span class="item-title">{title}</span>
        {#if subtitle}
            <span class="item-subtitle">{subtitle}</span>
        {/if}
    </div>

    <div class="item-action">
        <span>+</span>
    </div>
</button>

<style>
    .dropdown-item {
        width: 100%;
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 10px 14px;
        border: none;
        background: white;
        text-align: left;
        cursor: pointer;
        border-radius: 14px;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        margin-bottom: 4px;
    }

    .dropdown-item:hover {
        background: #f0f9ff;
        transform: translateX(4px);
    }

    .dropdown-item:active {
        transform: scale(0.98);
    }

    .item-visual {
        width: 40px;
        height: 40px;
        background: #eff6ff;
        color: #0ea5e9;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
    }

    .item-visual.is-service {
        background: #fff7ed;
        color: #f59e0b;
    }

    .avatar {
        font-weight: 900;
        font-size: 16px;
    }

    .icon {
        font-size: 18px;
    }

    .item-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
    }

    .item-title {
        font-size: 14px;
        font-weight: 750;
        color: #0f172a;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .item-subtitle {
        font-size: 11px;
        font-weight: 600;
        color: #94a3b8;
    }

    .item-action {
        width: 24px;
        height: 24px;
        background: #f1f5f9;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #cbd5e1;
        font-weight: 800;
        font-size: 16px;
        transition: all 0.2s;
    }

    .dropdown-item:hover .item-action {
        background: #0ea5e9;
        color: white;
        transform: rotate(90deg);
    }
</style>
