import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		host: '0.0.0.0',
		port: 5173,
		strictPort: true,
		proxy: {
			// Прокси для API
			'/api': {
				target: 'http://127.0.0.1:8080',
				changeOrigin: true,
				secure: false,
				timeout: 30000,
				// ФИКС: Не дублируем /api, если бэкенд уже его имеет
				// Если ваш бэкенд принимает /api/..., то оставляем как есть
				// Но добавим логирование для отладки
				configure: (proxy, _options) => {
					proxy.on('error', (err, req, _res) => {
						console.log('proxy error', err);
						console.log('request details:', req.method, req.url, req.headers);
					});
					proxy.on('proxyReq', (proxyReq, req, _res) => {
						console.log('Sending Request to the Target:', req.method, req.url);
						proxyReq.setTimeout(30000);
					});
					proxy.on('proxyRes', (proxyRes, req, _res) => {
						console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
						console.log('Response headers:', proxyRes.headers);
						console.log('Response length:', proxyRes.headers['content-length']);
					});
				}
			},
			// Прокси для WebSocket
			'/ws': {
				target: 'http://127.0.0.1:8080',
				ws: true,
				changeOrigin: true
			}
		}
	},
	build: {
		rollupOptions: {
			output: {
				// Отключаем manualChunks - Vite сам оптимизирует лучше
				manualChunks: undefined
			}
		}
	}
});
