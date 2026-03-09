import { readable, writable } from 'svelte/store';

export const staffSearchQuery = writable('');
export const cachedStaff = writable([]);
export const staffMetadata = writable({ totalElements: 0, totalPages: 0, currentPage: 0 });

// ИНСТРУМЕНТ ДИРИЖИРОВАНИЯ
export const isMobile = readable(false, (set) => {
    if (typeof window === 'undefined') return;
    const check = () => set(window.innerWidth < 768);
    check();
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
});

export const isTablet = readable(false, (set) => {
    if (typeof window === 'undefined') return;
    const check = () => set(window.innerWidth >= 768 && window.innerWidth < 1024);
    check();
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
});
