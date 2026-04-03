/**
 * Svelte action: portal
 *
 * Переносит DOM-узел из текущего места в DOM напрямую в <body>
 * (или в любой другой указанный элемент).
 *
 * ЗАЧЕМ НУЖНО:
 * На iOS (WebKit) свойства `backdrop-filter`, `transform`, `will-change`,
 * `filter`, `opacity < 1` на ЛЮБОМ предке создают новый Stacking Context.
 * Это означает, что z-index внутри такого контекста сравнивается только
 * с соседями по тому же контексту — z-index: 99999 внутри контекста с
 * z-index: 1000 никогда не поднимется выше соседнего контекста с z-index: 1001.
 *
 * В нашем случае:
 *   .mobile-bottom-ui { backdrop-filter: blur(...); z-index: 1000 }
 *   → создаёт Stacking Context
 *   .modal-backdrop { z-index: 3500 }
 *   → рендерится внутри .mobile-content — сестринского элемента к .mobile-bottom-ui
 *   → iOS сравнивает z-index КОНТЕКСТОВ, а не абсолютные значения
 *   → BottomNav всегда выше модалки на iOS
 *
 * РЕШЕНИЕ:
 * Перенести модалку в <body> через portal — она окажется вне всех
 * вложенных Stacking Context'ов и её z-index: 99999 будет работать корректно.
 *
 * ИСПОЛЬЗОВАНИЕ:
 *   <div use:portal>...</div>                    // в body
 *   <div use:portal={'#modals-root'}>...</div>   // в конкретный элемент
 *   <div use:portal={someElement}>...</div>      // в DOM-элемент напрямую
 */

/**
 * @param {HTMLElement} node - элемент, который нужно перенести
 * @param {string | HTMLElement} [target='body'] - CSS-селектор или DOM-элемент назначения
 */
export function portal(node, target = 'body') {
    let targetEl;

    /**
     * Находит или создаёт целевой элемент и переносит туда node
     * @param {string | HTMLElement} newTarget
     */
    function update(newTarget) {
        // Сохраняем ссылку на исходного родителя для корректного unmount
        targetEl =
            typeof newTarget === 'string'
                ? document.querySelector(newTarget)
                : newTarget;

        if (!targetEl) {
            // Фоллбэк: создаём контейнер в body, если селектор не нашёлся
            console.warn(`[portal] Target "${newTarget}" not found, falling back to body`);
            targetEl = document.body;
        }

        // Физически переносим узел в конец target
        targetEl.appendChild(node);
    }

    /**
     * Удаляем узел из DOM при уничтожении компонента
     */
    function destroy() {
        if (node.parentNode) {
            node.parentNode.removeChild(node);
        }
    }

    update(target);

    return { update, destroy };
}
