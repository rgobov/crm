import api from '../api.js';

export const telegramService = {
    async getStatus() {
        // Путь должен быть /api/admin/telegram/status
        const res = await api.get('/api/admin/telegram/status');
        return res.data;
    },
    async connect() {
        // Путь должен быть /api/admin/telegram/connect
        await api.post('/api/admin/telegram/connect');
    },
    async disconnect() {
        // Путь должен быть /api/admin/telegram/disconnect
        await api.post('/api/admin/telegram/disconnect');
    }
};
