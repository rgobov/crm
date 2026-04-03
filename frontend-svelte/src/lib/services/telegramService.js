import api from '../api.js';

export const telegramService = {
    async getStatus() {
        const res = await api.get('/admin/telegram/status');
        return res.data;
    },
    async connect() {
        await api.post('/admin/telegram/connect');
    },
    async disconnect() {
        await api.post('/admin/telegram/disconnect');
    },
    async sendPassword(password) {
        await api.post('/admin/telegram/password', { password });
    }
};
