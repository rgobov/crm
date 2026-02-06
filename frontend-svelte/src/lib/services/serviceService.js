import api from '$lib/api.js';

export const serviceService = {
    async getServices() {
        const response = await api.get('/services');
        return response.data;
    },

    async addService(data) {
        const response = await api.post('/services', data);
        return response.data; // ВОЗВРАЩАЕМ СОЗДАННУЮ УСЛУГУ
    },

    async updateService(data) {
        const response = await api.post('/services', data);
        return response.data; // ВОЗВРАЩАЕМ ОБНОВЛЕННУЮ УСЛУГУ
    },

    async deleteService(id) {
        await api.delete(`/services/${id}`);
    },

    async getServiceById(id) {
        const services = await this.getServices();
        return services.find(s => s.id === id);
    }
};
