import { writable } from 'svelte/store';
import { branchService } from '$lib/services/branchService.js';

function createBranchStore() {
    const { subscribe, set, update } = writable([]);

    return {
        subscribe,
        // Метод для принудительного обновления данных из API
        refresh: async () => {
            try {
                const data = await branchService.getBranches();
                set(data || []);
                console.log('🌳 BranchStore: Data refreshed', data?.length, 'branches found');
            } catch (e) {
                console.error('🌳 BranchStore: Refresh failed', e);
            }
        },
        // Метод для ручного установления данных (если нужно)
        setBranches: (branches) => set(branches)
    };
}

export const branchStore = createBranchStore();
