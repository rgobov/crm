import api from '$lib/api.js';

export const employeeService = {
    // 1. Личный профиль и график на дату
    async getMyProfile(date = new Date()) {
        const dateStr = date.toISOString().split('T')[0];
        const response = await api.get('/employee/profile', {
            params: { date: dateStr }
        });
        return response.data;
    },

    // 2. Личное расписание (Таймлайн мастера)
    async getMyAppointments(date = new Date()) {
        const dateStr = date.toISOString().split('T')[0];
        const response = await api.get('/employee/appointments', {
            params: { date: dateStr }
        });
        return response.data;
    },

    // 3. Личная загрузка на месяц (для календаря)
    async getMyWorkload(year, month) {
        const response = await api.get('/employee/workload', {
            params: { year, month }
        });
        return response.data;
    },

    // 4. Обновление статуса записи (начал работу, завершил)
    async updateAppointmentStatus(id, status) {
        const response = await api.patch(`/employee/appointments/${id}/status`, { status });
        return response.data;
    }
};
