import { writable } from 'svelte/store';

export const activeModal = writable(null); // 'telegram', 'templates', 'appointment', 'shift'

export function openModal(name) {
    activeModal.set(name);
}

export function closeModal() {
    activeModal.set(null);
}
