import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),
	kit: {
		adapter: adapter(),
		// Разрешаем работу через прокси Easypanel (исправляет многие ошибки 403 и SIGTERM)
		csrf: {
			checkOrigin: false,
		}
	}
};

export default config;
