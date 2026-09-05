<script>
    export let agreed = false;
    export let disabled = false;
</script>

<div class="consent-wrapper">
    <label class="consent-label">
        <input
            type="checkbox"
            bind:checked={agreed}
            disabled={disabled}
        />
        <span class="consent-text">
            Я даю согласие на обработку моих персональных данных в соответствии с
            <a href="/privacy">
                Политикой обработки персональных данных
            </a>
            <span class="mirror">(зеркало: <a href="https://999crm.ru/privacy-policy/" target="_blank" rel="noopener noreferrer">999crm.ru</a>)</span>
        </span>
    </label>
</div>

<style>
    .consent-wrapper {
        margin-top: 8px;
        margin-bottom: 16px;
        text-align: left;
    }

    .consent-label {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        cursor: pointer;
        font-size: 13px;
        color: #475569;
        line-height: 1.5;
    }

    .consent-label input[type="checkbox"] {
        width: 18px;
        height: 18px;
        margin-top: 2px;
        flex-shrink: 0;
        cursor: pointer;
        accent-color: #2563eb;
    }

    .consent-text {
        user-select: none;
    }

    .consent-text a {
        color: #2563eb;
        font-weight: 600;
        text-decoration: underline;
    }

    .consent-text a:hover {
        color: #1d4ed8;
    }

    .mirror {
        font-size: 12px;
        color: #94a3b8;
    }
</style>
