import api from '$lib/api.js';

export const managerService = {
    async getWappiSettings() {
        const response = await api.get('/manager/wappi/settings');
        return response.data;
    },

    async updateWappiSettings(settings) {
        await api.put('/manager/wappi/settings', settings);
    },

    async sendTestMessage(phone) {
        await api.post(`/manager/wappi/test?phone=${phone}`);
    }
};
