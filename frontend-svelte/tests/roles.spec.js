import { test, expect } from '@playwright/test';

// Тестовые данные (создаются TestDataInitializer на тестовом профиле бэкенда)
const testUsers = {
    admin: { email: 'admin@test.com', password: 'password', role: 'ADMIN' },
    manager: { email: 'manager@test.com', password: 'password', role: 'MANAGER' },
    employee: { email: 'employee@test.com', password: 'password', role: 'EMPLOYEE' }
};

async function login(page, email, password) {
    // Страница уже на '/' (см. beforeEach) — дожидаемся формы и заполняем по id.
    // toHaveValue гарантирует, что Svelte bind:value уже обновил переменные до клика.
    await expect(page.locator('#email')).toBeVisible();
    await page.locator('#email').fill(email);
    await page.locator('#password').fill(password);
    await expect(page.locator('#email')).toHaveValue(email);
    await expect(page.locator('#password')).toHaveValue(password);
    await page.getByRole('button', { name: 'Войти' }).click();
}

async function getToken(request, email, password) {
    const res = await request.post('/api/auth/login', {
        data: { email, password }
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    return body.token;
}

test.beforeEach(async ({ page }) => {
    // Чистый SPA: форма логина на '/'
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload(); // перезагрузка с очищенным токеном — гарантированно форма логина
});

// ========== АВТОРИЗАЦИЯ И ПРИЗЕМЛЕНИЕ ПО РОЛЯМ ==========

test.describe('Авторизация и переход по ролям', () => {

    test('ADMIN логинится и попадает на /admin', async ({ page }) => {
        await login(page, testUsers.admin.email, testUsers.admin.password);
        await page.waitForURL('**/admin');
        // Десктопный сайдбар админа виден
        await expect(page.locator('.sidebar-aside')).toBeVisible();
        await expect(page.locator('.nav-menu')).toBeVisible();
    });

    test('MANAGER логинится и попадает на /manager', async ({ page }) => {
        await login(page, testUsers.manager.email, testUsers.manager.password);
        await page.waitForURL('**/manager');
        await expect(page.locator('.manager-shell')).toBeVisible();
    });

    test('EMPLOYEE логинится и попадает на /employee', async ({ page }) => {
        await login(page, testUsers.employee.email, testUsers.employee.password);
        await page.waitForURL('**/employee');
        await expect(page.locator('.employee-dashboard')).toBeVisible();
    });
});

// ========== НЕАВТОРИЗОВАННЫЙ ДОСТУП ==========

test.describe('Неавторизованный доступ', () => {

    test('Без токена на "/" показывается форма логина', async ({ page }) => {
        await page.goto('/');
        await expect(page.locator('#email')).toBeVisible();
        await expect(page.locator('#password')).toBeVisible();
    });

    test('API без токена возвращает 401', async ({ request }) => {
        const res = await request.get('/api/admin/dashboard/stats');
        expect(res.status()).toBe(401);
    });
});

// ========== API-ДОСТУП ПО РОЛЯМ ==========

test.describe('Проверка API запросов для разных ролей', () => {

    test('ADMIN может обращаться к админским и нижестоящим API', async ({ request }) => {
        const token = await getToken(request, testUsers.admin.email, testUsers.admin.password);
        const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

        // dashboard/stats исключён: использует JOIN LATERAL (Postgres-only), на H2 не работает.
        // employee/dashboard/stats исключён: требует staffId, которого у админа нет (403 в контроллере).
        const allowed = [
            '/api/admin/workload?year=2026&month=7',
            '/api/admin/staff',
            '/api/admin/services',
            '/api/manager/workload?year=2026&month=7'
        ];
        for (const url of allowed) {
            const res = await request.get(url, { headers });
            expect(res.status(), `ADMIN должен иметь доступ к ${url}`).toBe(200);
        }
    });

    test('MANAGER имеет доступ только к менеджерским API', async ({ request }) => {
        const token = await getToken(request, testUsers.manager.email, testUsers.manager.password);
        const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

        for (const url of ['/api/manager/workload?year=2026&month=7', '/api/manager/settings/wappi']) {
            const res = await request.get(url, { headers });
            expect(res.status(), `MANAGER должен иметь доступ к ${url}`).toBe(200);
        }

        for (const url of ['/api/admin/dashboard/stats', '/api/employee/dashboard/stats']) {
            const res = await request.get(url, { headers });
            expect(res.status(), `MANAGER НЕ должен иметь доступ к ${url}`).toBe(403);
        }
    });

    test('EMPLOYEE имеет доступ только к employee API', async ({ request }) => {
        const token = await getToken(request, testUsers.employee.email, testUsers.employee.password);
        const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

        for (const url of ['/api/employee/dashboard/stats', '/api/employee/profile']) {
            const res = await request.get(url, { headers });
            expect(res.status(), `EMPLOYEE должен иметь доступ к ${url}`).toBe(200);
        }

        for (const url of ['/api/admin/dashboard/stats', '/api/manager/workload?year=2026&month=7']) {
            const res = await request.get(url, { headers });
            expect(res.status(), `EMPLOYEE НЕ должен иметь доступ к ${url}`).toBe(403);
        }
    });
});

// ========== НАВИГАЦИЯ ==========

test.describe('Навигация', () => {

    test('Админ может перейти на Таймлайн из сайдбара', async ({ page }) => {
        await login(page, testUsers.admin.email, testUsers.admin.password);
        await page.waitForURL('**/admin');
        await page.getByRole('button', { name: 'Таймлайн' }).click();
        await expect(page.locator('.calendar-tab-root')).toBeVisible();
    });
});

// ========== ВЫХОД ==========

test.describe('Выход из системы', () => {

    test('Админ выходит из системы и токен удаляется', async ({ page }) => {
        await login(page, testUsers.admin.email, testUsers.admin.password);
        await page.waitForURL('**/admin');
        await page.locator('.logout-btn-desktop').click();
        await page.waitForURL('**/');
        const token = await page.evaluate(() => localStorage.getItem('token'));
        expect(token).toBeNull();
        await expect(page.locator('#email')).toBeVisible();
    });
});
