const DEFAULT_THRESHOLD = 80;
const AXIS_LOCK_DISTANCE = 8;

/**
 * Закрывает элемент после вертикального свайпа вниз.
 * Жест начинается только после фиксации вертикальной оси, поэтому обычные
 * нажатия и горизонтальные движения не вызывают событие.
 */
export function swipeDown(node, options = {}) {
    let threshold = getThreshold(options.threshold);
    let gesture = null;
    let suppressClickUntil = 0;

    function getThreshold(value) {
        const parsed = Number(value);
        return Number.isFinite(parsed) && parsed > 0 ? parsed : DEFAULT_THRESHOLD;
    }

    function resetGesture() {
        if (gesture?.captured && node.hasPointerCapture?.(gesture.pointerId)) {
            try {
                node.releasePointerCapture(gesture.pointerId);
            } catch (e) {
                // Pointer уже мог быть отменен браузером.
            }
        }
        gesture = null;
    }

    function handlePointerDown(event) {
        if (event.pointerType === 'mouse' && event.button !== 0) return;

        gesture = {
            pointerId: event.pointerId,
            startX: event.clientX,
            startY: event.clientY,
            axis: null,
            captured: false
        };
    }

    function handlePointerMove(event) {
        if (!gesture || event.pointerId !== gesture.pointerId) return;

        const deltaX = event.clientX - gesture.startX;
        const deltaY = event.clientY - gesture.startY;

        if (!gesture.axis) {
            if (Math.max(Math.abs(deltaX), Math.abs(deltaY)) < AXIS_LOCK_DISTANCE) return;

            gesture.axis = Math.abs(deltaY) >= Math.abs(deltaX) ? 'vertical' : 'horizontal';
            if (gesture.axis === 'horizontal') {
                resetGesture();
                return;
            }

            try {
                node.setPointerCapture?.(event.pointerId);
                gesture.captured = true;
            } catch (e) {
                // Захват не поддерживается в конкретном WebView.
            }
        }

        if (gesture.axis === 'vertical' && deltaY > 0) {
            event.preventDefault();
        }
    }

    function handlePointerUp(event) {
        if (!gesture || event.pointerId !== gesture.pointerId) return;

        const deltaX = event.clientX - gesture.startX;
        const deltaY = event.clientY - gesture.startY;
        const isSwipeDown = gesture.axis === 'vertical'
            && deltaY >= threshold
            && deltaY > Math.abs(deltaX);

        resetGesture();

        if (isSwipeDown) {
            suppressClickUntil = Date.now() + 400;
            node.dispatchEvent(new CustomEvent('swipe'));
        }
    }

    function handlePointerCancel() {
        resetGesture();
    }

    function suppressClickAfterSwipe(event) {
        if (Date.now() >= suppressClickUntil) return;
        suppressClickUntil = 0;
        event.preventDefault();
        event.stopPropagation();
    }

    node.addEventListener('pointerdown', handlePointerDown);
    node.addEventListener('pointermove', handlePointerMove);
    node.addEventListener('pointerup', handlePointerUp);
    node.addEventListener('pointercancel', handlePointerCancel);
    node.addEventListener('click', suppressClickAfterSwipe, true);

    const originalTouchAction = node.style.touchAction;
    node.style.touchAction = 'none';

    return {
        update(newOptions = {}) {
            threshold = getThreshold(newOptions.threshold);
        },
        destroy() {
            resetGesture();
            node.removeEventListener('pointerdown', handlePointerDown);
            node.removeEventListener('pointermove', handlePointerMove);
            node.removeEventListener('pointerup', handlePointerUp);
            node.removeEventListener('pointercancel', handlePointerCancel);
            node.removeEventListener('click', suppressClickAfterSwipe, true);
            node.style.touchAction = originalTouchAction;
        }
    };
}
