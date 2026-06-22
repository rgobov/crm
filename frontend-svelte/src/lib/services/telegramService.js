import api from '../api.js';

function extractErrorMessage(error) {
    if (error.response && error.response.data) {
        const data = error.response.data;
        if (data.detail) return data.detail;
        if (data.message) return data.message;
        return JSON.stringify(data);
    }
    if (error.message) return error.message;
    return 'Unknown error';
}

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
    },
    async sendCode(phoneNumber) {
        try {
            await api.post('/admin/telegram/send-code', { phoneNumber });
        } catch (error) {
            throw new Error(extractErrorMessage(error));
        }
    },
    async signIn(code) {
        try {
            await api.post('/admin/telegram/sign-in', { code });
        } catch (error) {
            throw new Error(extractErrorMessage(error));
        }
    },
    async cancelQrGeneration() {
        await api.post('/admin/telegram/cancel-qr');
    }
};
