/**
 * Утилиты для работы с временем на таймлайне с учетом часовых поясов филиалов
 */

export const timeUtils = {
    // Вспомогательная функция: перевод HH:mm:ss в минуты от начала дня
    toMinutes(timeStr) {
        if (!timeStr) return -1;
        const [h, m] = timeStr.split(':').map(Number);
        return h * 60 + m;
    },

    toBranchLocalISO(isoStr, timezone = 'Europe/Moscow') {
        if (!isoStr) return "";
        const date = new Date(isoStr);
        const formatter = new Intl.DateTimeFormat('sv-SE', {
            timeZone: timezone,
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit',
            hour12: false
        });
        return formatter.format(date).replace(' ', 'T');
    },

    toBranchLocalDateStr(date, timezone = 'Europe/Moscow') {
        if (!date) return '';
        const formatter = new Intl.DateTimeFormat('sv-SE', {
            timeZone: timezone,
            year: 'numeric', month: '2-digit', day: '2-digit'
        });
        return formatter.format(new Date(date));
    },

    fromBranchLocalToUTC(localStr, timezone = 'Europe/Moscow') {
        if (!localStr) return null;
        const date = new Date(localStr);
        const formatter = new Intl.DateTimeFormat('en-US', {
            timeZone: timezone,
            year: 'numeric', month: 'numeric', day: 'numeric',
            hour: 'numeric', minute: 'numeric', second: 'numeric',
            hour12: false
        });
        const parts = formatter.formatToParts(date);
        const map = Object.fromEntries(parts.map(p => [p.type, p.value]));
        const branchDate = new Date(`${map.year}-${map.month.padStart(2,'0')}-${map.day.padStart(2,'0')}T${map.hour.padStart(2,'0')}:${map.minute.padStart(2,'0')}`);
        const diff = branchDate.getTime() - date.getTime();
        return new Date(date.getTime() - diff).toISOString();
    },

    getTimeOffset(timeStr, startHour, hourHeight, timezone = 'Europe/Moscow') {
        const date = new Date(timeStr);
        const formatter = new Intl.DateTimeFormat('en-GB', {
            timeZone: timezone, hour: 'numeric', minute: 'numeric', hour12: false
        });
        const parts = formatter.formatToParts(date);
        const hours = parseInt(parts.find(p => p.type === 'hour').value);
        const minutes = parseInt(parts.find(p => p.type === 'minute').value);
        return ((hours - startHour) * 60 + minutes) * (hourHeight / 60);
    },

    formatTime(dateStr, timezone = 'Europe/Moscow') {
        return new Date(dateStr).toLocaleTimeString('ru-RU', {
            timeZone: timezone, hour: '2-digit', minute: '2-digit'
        });
    },

    // НОВОЕ: Возвращает время окончания в формате HH:mm
    getEndTime(startTimeStr, durationMinutes, timezone = 'Europe/Moscow') {
        const date = new Date(startTimeStr);
        const endDate = new Date(date.getTime() + durationMinutes * 60000);
        return endDate.toLocaleTimeString('ru-RU', {
            timeZone: timezone, hour: '2-digit', minute: '2-digit'
        });
    },

    /**
     * ОПРЕДЕЛЕНИЕ СТАТУСА СЛОТА (УЛУЧШЕНО)
     * Используем сравнение минут вместо строк для 100% точности
     */
    getSlotStatus(staff, hour, minute) {
        if (!staff) return 'WORK';
        if (staff.dayOff) return 'OFF';

        const slotMin = hour * 60 + minute;

        const workStartMin = this.toMinutes(staff.workStartTime);
        const workEndMin = this.toMinutes(staff.workEndTime);
        const breakStartMin = this.toMinutes(staff.breakStartTime);
        const breakEndMin = this.toMinutes(staff.breakEndTime);

        // 1. Проверка перерыва
        if (breakStartMin !== -1 && breakEndMin !== -1) {
            if (slotMin >= breakStartMin && slotMin < breakEndMin) return 'BREAK';
        }

        // 2. Проверка рабочего времени
        if (workStartMin === -1 || workEndMin === -1) return 'OFF';

        // Обработка перехода через полночь (если End < Start)
        if (workEndMin < workStartMin) {
            if (slotMin >= workStartMin || slotMin < workEndMin) return 'WORK';
        } else {
            if (slotMin >= workStartMin && slotMin < workEndMin) return 'WORK';
        }

        return 'OFF';
    }
};
