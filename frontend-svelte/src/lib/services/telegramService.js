import api from '../api.js';

export const telegramService = {
    async getStatus() {
        // Убрали /api, так как он уже есть в baseURL в api.js
        const res = await api.get('/admin/telegram/status');
        return res.data;
    },
    async connect() {
        await api.post('/admin/telegram/connect');
    },
    async disconnect() {
        await api.post('/admin/telegram/disconnect');
    }
};
