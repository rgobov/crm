import { writable } from 'svelte/store';

// Поисковый запрос
export const staffSearchQuery = writable('');

// Кэш списка сотрудников, чтобы не показывать пустой экран при переходе
export const cachedStaff = writable([]);
export const staffMetadata = writable({ totalElements: 0, totalPages: 0 });
