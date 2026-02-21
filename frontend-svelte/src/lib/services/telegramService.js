import api from '../api.js';

export const telegramService = {
    async getStatus() {
        // Запрашиваем статус. Бэкенд теперь может вернуть {status: "FLOOD_WAIT_238", ...}
        const res = await api.get('/api/admin/telegram/status');
        return res.data;
    },
    async connect() {
        await api.post('/api/admin/telegram/connect');
    },
    async disconnect() {
        await api.post('/api/admin/telegram/disconnect');
    }
};
