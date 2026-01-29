import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),
	kit: {
		// Адаптер для запуска в Node.js среде
		adapter: adapter(),
		// Отключаем строгую проверку происхождения для работы за прокси Easypanel
		csrf: {
			checkOrigin: false,
		}
	}
};

export default config;
