/**
 * Утилиты для работы с временем на таймлайне
 */

export const timeUtils = {
    /**
     * Преобразует время в пиксели от начала шкалы
     */
    getTimeOffset(timeStr, startHour, hourHeight) {
        const date = new Date(timeStr);
        const hours = date.getHours();
        const minutes = date.getMinutes();
        // ФИКС: Исправлен регистр переменной hourHeight
        return ((hours - startHour) * 60 + minutes) * (hourHeight / 60);
    },

    /**
     * Форматирует время для отображения (HH:mm)
     */
    formatTime(date) {
        return new Date(date).toLocaleTimeString('ru', { hour: '2-digit', minute: '2-digit' });
    },

    /**
     * Проверяет, является ли слот перерывом
     */
    getSlotStatus(staff, hour, minute) {
        if (!staff) return 'WORK';
        const slotTime = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00`;

        if (staff.breakStartTime && staff.breakEndTime) {
            if (slotTime >= staff.breakStartTime && slotTime < staff.breakEndTime) return 'BREAK';
        }

        if (staff.isDayOff || !staff.workStartTime || !staff.workEndTime) return 'OFF';
        if (slotTime < staff.workStartTime || slotTime >= staff.workEndTime) return 'OFF';

        return 'WORK';
    }
};
