// Отключаем SSR для всего приложения, превращая его в чистый SPA.
// Это критично для Telegram Mini App, чтобы избежать ошибок "window is not defined".
export const ssr = false;
export const prerender = false;
