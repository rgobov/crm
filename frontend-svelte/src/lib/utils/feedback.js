// Утилита для обратной связи (вибрация + вспышка)
// НЕ меняет бизнес-логику, только добавляет UX эффекты

export class FeedbackUtils {
    // Успешное действие
    static success() {
        this.vibrate();
        this.flash('success');
    }

    // Ошибка
    static error() {
        this.vibrate([100, 50, 100]); // Двойная вибрация
        this.flash('error');
    }

    // Вибрация (работает в PWA и Android)
    static vibrate(pattern = 200) {
        try {
            // PWA Vibration API
            if ('vibrate' in navigator) {
                navigator.vibrate(pattern);
            }
            // Telegram HapticFeedback
            else if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.HapticFeedback) {
                if (pattern === 200 || (Array.isArray(pattern) && pattern.length === 1 && pattern[0] === 200)) {
                    window.Telegram.WebApp.HapticFeedback.notificationOccurred('success');
                } else {
                    window.Telegram.WebApp.HapticFeedback.notificationOccurred('error');
                }
            }
        } catch (e) {
            // Игнорируем ошибки - не критично для бизнес-логики
            console.log('Feedback not available:', e);
        }
    }

    // Вспышка экрана
    static flash(type = 'success') {
        try {
            // Создаем элемент вспышки
            const flash = document.createElement('div');
            flash.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100vw;
                height: 100vh;
                pointer-events: none;
                z-index: 9999;
                opacity: 0;
                transition: opacity 0.3s ease;
            `;

            // Цвет в зависимости от типа
            if (type === 'success') {
                flash.style.backgroundColor = 'rgba(0, 255, 0, 0.2)';
            } else if (type === 'error') {
                flash.style.backgroundColor = 'rgba(255, 0, 0, 0.2)';
            }

            // Добавляем на страницу
            document.body.appendChild(flash);

            // Анимация вспышки
            requestAnimationFrame(() => {
                flash.style.opacity = '1';
                setTimeout(() => {
                    flash.style.opacity = '0';
                    setTimeout(() => {
                        document.body.removeChild(flash);
                    }, 300);
                }, 200);
            });
        } catch (e) {
            // Игнорируем ошибки - не критично для бизнес-логики
            console.log('Flash not available:', e);
        }
    }

    // Уведомление (комбинация)
    static notify(type, message) {
        if (type === 'success') {
            this.success();
        } else if (type === 'error') {
            this.error();
        }
        
        // Можно добавить toast уведомление здесь
        console.log(`${type}: ${message}`);
    }
}
