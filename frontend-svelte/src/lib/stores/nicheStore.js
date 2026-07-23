import { derived } from 'svelte/store';
import { activeBranchId } from './dashboardStore.js';
import { branchStore } from './branchStore.js';
import { getNicheConfig } from '$lib/config/nicheConfig.js';

export const activeNiche = derived(
    [activeBranchId, branchStore],
    ([$activeBranchId, $branches]) => {
        const branch = ($branches || []).find(b => b.id === $activeBranchId);
        return branch?.niche || 'AUTO';
    }
);

export const nicheSettings = derived(
    activeNiche,
    ($niche) => getNicheConfig($niche)
);