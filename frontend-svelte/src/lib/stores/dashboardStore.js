import { writable } from 'svelte/store';
import { adminService } from '$lib/services/adminService.js';

// Состояние всего дашборда (аналог ViewModel во Flutter)
function createDashboardStore() {
    const { subscribe, set, update } = writable({
        stats: {
            totalClients: 0,
            todaysAppointmentsCount: 0,
            totalResources: 0
        },
        isLoading: true,
        lastUpdated: null
    });

    return {
        subscribe,
        async refresh() {
            update(s => ({ ...s, isLoading: true }));
            try {
                const data = await adminService.getDashboardStats();
                set({
                    stats: data,
                    isLoading: false,
                    lastUpdated: new Date()
                });
            } catch (e) {
                console.error('Dashboard refresh failed', e);
                update(s => ({ ...s, isLoading: false }));
            }
        }
    };
}

export const dashboardStore = createDashboardStore();
export const activeTab = writable('management'); // Храним текущую вкладку
