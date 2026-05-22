import { test, expect } from '@playwright/test';

// Тестовые данные
const testUsers = {
    admin: {
        email: 'admin@test.com',
        password: 'password',
        role: 'ADMIN',
        expectedRoutes: ['/admin', '/admin/dashboard', '/admin/staff', '/admin/services']
    },
    manager: {
        email: 'manager@test.com',
        password: 'password',
        role: 'MANAGER',
        expectedRoutes: ['/manager', '/manager/wappi', '/manager/contacts']
    },
    employee: {
        email: 'employee@test.com',
        password: 'password',
        role: 'EMPLOYEE',
        expectedRoutes: ['/employee', '/employee/schedule', '/employee/shifts']
    }
};

// Настройки для всех тестов
test.beforeEach(async ({ page }) => {
    // Очищаем localStorage перед каждым тестом
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
});

// ========== АВТОРИЗАЦИЯ И РОЛИ ==========

test.describe('Аутентификация и проверка ролей', () => {
    
    test('ADMIN может войти и получить доступ к админским функциям', async ({ page }) => {
        const user = testUsers.admin;
        
        // Логинимся как админ
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', user.email);
        await page.fill('[data-testid=password-input]', user.password);
        await page.click('[data-testid=login-button]');
        
        // Проверяем редирект на дашборд админа
        await expect(page).toHaveURL('/admin/dashboard');
        
        // Проверяем наличие админских элементов
        await expect(page.locator('[data-testid=admin-dashboard]')).toBeVisible();
        await expect(page.locator('[data-testid=admin-nav]')).toBeVisible();
        
        // Проверяем доступ к админским маршрутам
        for (const route of user.expectedRoutes) {
            await page.goto(route);
            await expect(page.locator('[data-testid=loading]')).not.toBeVisible();
            await expect(page.locator('body')).not.toContainText('Доступ запрещен');
        }
    });

    test('MANAGER может войти и получить доступ к функциям менеджера', async ({ page }) => {
        const user = testUsers.manager;
        
        // Логинимся как менеджер
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', user.email);
        await page.fill('[data-testid=password-input]', user.password);
        await page.click('[data-testid=login-button]');
        
        // Проверяем редирект на страницу менеджера
        await expect(page).toHaveURL('/manager');
        
        // Проверяем наличие элементов менеджера
        await expect(page.locator('[data-testid=manager-dashboard]')).toBeVisible();
        
        // Проверяем доступ к маршрутам менеджера
        for (const route of user.expectedRoutes) {
            await page.goto(route);
            await expect(page.locator('[data-testid=loading]')).not.toBeVisible();
            await expect(page.locator('body')).not.toContainText('Доступ запрещен');
        }
    });

    test('EMPLOYEE может войти и получить доступ к функциям сотрудника', async ({ page }) => {
        const user = testUsers.employee;
        
        // Логинимся как сотрудник
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', user.email);
        await page.fill('[data-testid=password-input]', user.password);
        await page.click('[data-testid=login-button]');
        
        // Проверяем редирект на страницу сотрудника
        await expect(page).toHaveURL('/employee');
        
        // Проверяем наличие элементов сотрудника
        await expect(page.locator('[data-testid=employee-dashboard]')).toBeVisible();
        
        // Проверяем доступ к маршрутам сотрудника
        for (const route of user.expectedRoutes) {
            await page.goto(route);
            await expect(page.locator('[data-testid=loading]')).not.toBeVisible();
            await expect(page.locator('body')).not.toContainText('Доступ запрещен');
        }
    });
});

// ========== ЗАПРЕЩЕННЫЙ ДОСТУП ==========

test.describe('Запрещенный доступ между ролями', () => {
    
    test('MANAGER не может получить доступ к админским функциям', async ({ page }) => {
        const user = testUsers.manager;
        
        // Логинимся как менеджер
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', user.email);
        await page.fill('[data-testid=password-input]', user.password);
        await page.click('[data-testid=login-button]');
        
        // Пытаемся получить доступ к админским маршрутам
        const adminRoutes = ['/admin/dashboard', '/admin/staff', '/admin/services'];
        
        for (const route of adminRoutes) {
            await page.goto(route);
            await expect(page.locator('body')).toContainText('Доступ запрещен');
            await expect(page).toHaveURL('/manager'); // Редирект на страницу менеджера
        }
    });

    test('EMPLOYEE не может получить доступ к функциям менеджера', async ({ page }) => {
        const user = testUsers.employee;
        
        // Логинимся как сотрудник
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', user.email);
        await page.fill('[data-testid=password-input]', user.password);
        await page.click('[data-testid=login-button]');
        
        // Пытаемся получить доступ к маршрутам менеджера
        const managerRoutes = ['/manager', '/manager/wappi', '/manager/contacts'];
        
        for (const route of managerRoutes) {
            await page.goto(route);
            await expect(page.locator('body')).toContainText('Доступ запрещен');
            await expect(page).toHaveURL('/employee'); // Редирект на страницу сотрудника
        }
    });

    test('EMPLOYEE не может получить доступ к админским функциям', async ({ page }) => {
        const user = testUsers.employee;
        
        // Логинимся как сотрудник
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', user.email);
        await page.fill('[data-testid=password-input]', user.password);
        await page.click('[data-testid=login-button]');
        
        // Пытаемся получить доступ к админским маршрутам
        const adminRoutes = ['/admin/dashboard', '/admin/staff', '/admin/services'];
        
        for (const route of adminRoutes) {
            await page.goto(route);
            await expect(page.locator('body')).toContainText('Доступ запрещен');
            await expect(page).toHaveURL('/employee'); // Редирект на страницу сотрудника
        }
    });
});

// ========== API ЗАПРОСЫ ==========

test.describe('Проверка API запросов для разных ролей', () => {
    
    test('ADMIN может выполнять все типы API запросов', async ({ page, request }) => {
        // Устанавливаем токен админа
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', testUsers.admin.email);
        await page.fill('[data-testid=password-input]', testUsers.admin.password);
        await page.click('[data-testid=login-button]');
        
        // Получаем токен из localStorage
        const token = await page.evaluate(() => localStorage.getItem('token'));
        
        // Проверяем API запросы админа
        const adminEndpoints = [
            { method: 'GET', url: '/api/admin/dashboard/stats' },
            { method: 'GET', url: '/api/admin/staff' },
            { method: 'GET', url: '/api/admin/services' },
            { method: 'GET', url: '/api/manager/workload' }, // Админ может получать доступ к менеджерским эндпоинтам
            { method: 'GET', url: '/api/employee/stats' }    // Админ может получать доступ к employee эндпоинтам
        ];
        
        for (const endpoint of adminEndpoints) {
            const response = await request.fetch(endpoint.url, {
                method: endpoint.method,
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            expect(response.status()).toBe(200);
        }
    });

    test('MANAGER может выполнять только менеджерские API запросы', async ({ page, request }) => {
        // Устанавливаем токен менеджера
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', testUsers.manager.email);
        await page.fill('[data-testid=password-input]', testUsers.manager.password);
        await page.click('[data-testid=login-button]');
        
        // Получаем токен из localStorage
        const token = await page.evaluate(() => localStorage.getItem('token'));
        
        // Проверяем разрешенные API запросы менеджера
        const allowedEndpoints = [
            { method: 'GET', url: '/api/manager/workload' },
            { method: 'GET', url: '/api/manager/wappi/settings' }
        ];
        
        for (const endpoint of allowedEndpoints) {
            const response = await request.fetch(endpoint.url, {
                method: endpoint.method,
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            expect(response.status()).toBe(200);
        }
        
        // Проверяем запрещенные API запросы менеджера
        const forbiddenEndpoints = [
            { method: 'GET', url: '/api/admin/dashboard/stats' },
            { method: 'GET', url: '/api/employee/stats' }
        ];
        
        for (const endpoint of forbiddenEndpoints) {
            const response = await request.fetch(endpoint.url, {
                method: endpoint.method,
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            expect(response.status()).toBe(403);
        }
    });

    test('EMPLOYEE может выполнять только employee API запросы', async ({ page, request }) => {
        // Устанавливаем токен сотрудника
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', testUsers.employee.email);
        await page.fill('[data-testid=password-input]', testUsers.employee.password);
        await page.click('[data-testid=login-button]');
        
        // Получаем токен из localStorage
        const token = await page.evaluate(() => localStorage.getItem('token'));
        
        // Проверяем разрешенные API запросы сотрудника
        const allowedEndpoints = [
            { method: 'GET', url: '/api/employee/stats' },
            { method: 'GET', url: '/api/employee/profile' }
        ];
        
        for (const endpoint of allowedEndpoints) {
            const response = await request.fetch(endpoint.url, {
                method: endpoint.method,
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            expect(response.status()).toBe(200);
        }
        
        // Проверяем запрещенные API запросы сотрудника
        const forbiddenEndpoints = [
            { method: 'GET', url: '/api/admin/dashboard/stats' },
            { method: 'GET', url: '/api/manager/workload' }
        ];
        
        for (const endpoint of forbiddenEndpoints) {
            const response = await request.fetch(endpoint.url, {
                method: endpoint.method,
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            expect(response.status()).toBe(403);
        }
    });
});

// ========== NAVIGATION TESTS ==========

test.describe('Навигация и редиректы', () => {
    
    test('Неавторизованный пользователь перенаправляется на страницу логина', async ({ page }) => {
        const protectedRoutes = ['/admin', '/manager', '/employee'];
        
        for (const route of protectedRoutes) {
            await page.goto(route);
            await expect(page).toHaveURL('/login');
        }
    });

    test('Авторизованный пользователь перенаправляется на свою страницу', async ({ page }) => {
        // Тест для админа
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', testUsers.admin.email);
        await page.fill('[data-testid=password-input]', testUsers.admin.password);
        await page.click('[data-testid=login-button]');
        
        await expect(page).toHaveURL('/admin/dashboard');
        
        // Тест для менеджера
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', testUsers.manager.email);
        await page.fill('[data-testid=password-input]', testUsers.manager.password);
        await page.click('[data-testid=login-button]');
        
        await expect(page).toHaveURL('/manager');
        
        // Тест для сотрудника
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', testUsers.employee.email);
        await page.fill('[data-testid=password-input]', testUsers.employee.password);
        await page.click('[data-testid=login-button]');
        
        await expect(page).toHaveURL('/employee');
    });
});

// ========== LOGOUT TESTS ==========

test.describe('Выход из системы', () => {
    
    test('Пользователь может выйти из системы', async ({ page }) => {
        // Логинимся как админ
        await page.goto('/login');
        await page.fill('[data-testid=email-input]', testUsers.admin.email);
        await page.fill('[data-testid=password-input]', testUsers.admin.password);
        await page.click('[data-testid=login-button]');
        
        // Проверяем что мы залогинены
        await expect(page).toHaveURL('/admin/dashboard');
        
        // Выходим из системы
        await page.click('[data-testid=logout-button]');
        
        // Проверяем что мы вышли и перенаправлены на страницу логина
        await expect(page).toHaveURL('/login');
        
        // Проверяем что токен удален
        const token = await page.evaluate(() => localStorage.getItem('token'));
        expect(token).toBeNull();
        
        // Пытаемся получить доступ к защищенному маршруту
        await page.goto('/admin/dashboard');
        await expect(page).toHaveURL('/login');
    });
});
