/**
 * Утилиты для работы с временем на таймлайне с учетом часовых поясов филиалов
 */

export const timeUtils = {
    /**
     * Преобразует UTC ISO строку из базы в формат YYYY-MM-DDTHH:mm для инпута
     * строго по часовому поясу филиала
     */
    toBranchLocalISO(isoStr, timezone = 'Europe/Moscow') {
        if (!isoStr) return "";
        const date = new Date(isoStr);

        // sv-SE формат дает YYYY-MM-DD HH:mm, что идеально для datetime-local
        const formatter = new Intl.DateTimeFormat('sv-SE', {
            timeZone: timezone,
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit',
            hour12: false
        });

        return formatter.format(date).replace(' ', 'T');
    },

    /**
     * Преобразует "наивное" время из инпута (без пояса) в UTC ISO строку,
     * считая, что введенное время принадлежит часовому поясу филиала
     */
    fromBranchLocalToUTC(localStr, timezone = 'Europe/Moscow') {
        if (!localStr) return null;
        const date = new Date(localStr); // Это "наивная" дата

        // Магия Intl: определяем, какое сейчас время в филиале относительно UTC
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

    /**
     * Расчет отступа в пикселях для отображения на таймлайне
     */
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
