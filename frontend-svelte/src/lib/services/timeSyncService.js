import api from '$lib/api.js';

/**
 * Сервис для синхронизации локального времени с серверным
 */
class TimeSyncService {
    constructor() {
        this.serverOffset = 0; // Разница в миллисекундах
        this.isSynced = false;
    }

    /**
     * Выполняет разовую синхронизацию с сервером
     */
    async sync() {
        try {
            const start = Date.now();
            const response = await api.get('/system/time');
            const end = Date.now();

            // Учитываем задержку сети (RTT / 2)
            const networkLatency = (end - start) / 2;
            const serverTime = new Date(response.data.serverTime).getTime();

            this.serverOffset = serverTime + networkLatency - end;
            this.isSynced = true;

            console.log(`[TimeSync] Смещение сервера: ${this.serverOffset}ms`);
        } catch (e) {
            console.error('[TimeSync] Не удалось синхронизировать время', e);
        }
    }

    /**
     * Возвращает текущее точное время (Date объект)
     */
    getNow() {
        return new Date(Date.now() + this.serverOffset);
    }
}

export const timeSyncService = new TimeSyncService();
