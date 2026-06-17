import { writable } from 'svelte/store';
import { adminService } from '$lib/services/adminService.js';

// Состояние всего дашборда
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
export const activeTab = writable('management');
export const selectedDate = writable(new Date());

// ✅ Инициализация из localStorage для сохранения выбора между сессиями
const storedBranchId = typeof window !== 'undefined' ? localStorage.getItem('activeBranchId') : null;
export const activeBranchId = writable(storedBranchId);

// ✅ Подписка на изменения для автосохранения
if (typeof window !== 'undefined') {
    activeBranchId.subscribe(id => {
        if (id) localStorage.setItem('activeBranchId', id);
        else localStorage.removeItem('activeBranchId');
    });
}

export const refreshTrigger = writable(0);

// ✅ Умное определение мобильного устройства с поддержкой SSR и ресайза
function createMobileStore() {
    const { subscribe, set } = writable(false);

    if (typeof window !== 'undefined') {
        const check = () => set(window.innerWidth < 768);
        check();
        window.addEventListener('resize', check);
    }

    return { subscribe };
}

export const isMobile = createMobileStore();
