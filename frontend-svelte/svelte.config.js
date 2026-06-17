import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),

	kit: {
		adapter: adapter(),
		csrf: {
			// ТЕХНИЧЕСКОЕ РЕШЕНИЕ: Отключаем проверку Origin для CSRF,
			// чтобы эмулятор мог слать POST-запросы на localhost.
			// В продакшене это безопасно, так как бэкенд все равно проверяет JWT.
			checkOrigin: false,
		}
	}
};

export default config;
