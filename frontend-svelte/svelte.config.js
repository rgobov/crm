import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),
	kit: {
		// Адаптер для запуска в Node.js среде (идеально для Nixpacks/Easypanel)
		adapter: adapter()
	}
};

export default config;
