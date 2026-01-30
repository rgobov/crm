import { writable } from 'svelte/store';

// ГАРАНТИРУЕМ, что начальное значение - пустая строка, а не undefined
export const staffSearchQuery = writable('');

export const cachedStaff = writable([]);
export const staffMetadata = writable({ totalElements: 0, totalPages: 0, currentPage: 0 });
