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
            <a href="https://999crm.ru/%D0%BF%D0%BE%D0%BB%D0%B8%D1%82%D0%B8%D0%BA%D0%B0-%D0%BE%D0%B1%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D0%BA%D0%B8-%D0%BF%D0%B5%D1%80%D1%81%D0%BE%D0%BD%D0%B0%D0%BB%D1%8C%D0%BD%D1%8B%D1%85-%D0%B4%D0%B0%D0%BD/" target="_blank" rel="noopener noreferrer">
                Политикой обработки персональных данных
            </a>
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
</style>
