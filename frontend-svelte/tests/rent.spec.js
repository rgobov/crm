import { test, expect } from '@playwright/test';

// ========== ХЕЛПЕРЫ ==========

let counter = 0;
function uid(prefix) {
    counter += 1;
    return `${prefix}-${Date.now()}-${counter}`;
}

async function loginAsAdmin(request) {
    const res = await request.post('/api/auth/login', {
        data: { email: 'admin@test.com', password: 'password' }
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    return body.token;
}

// Логин в браузере. toHaveValue гарантирует, что Svelte bind:value обновил переменные до клика
// (иначе гоночная ситуация: клик «Войти» с пустым полем).
async function loginAdminInBrowser(page) {
    await page.goto('/');
    await page.locator('#email').fill('admin@test.com');
    await page.locator('#password').fill('password');
    await expect(page.locator('#email')).toHaveValue('admin@test.com');
    await expect(page.locator('#password')).toHaveValue('password');
    await page.getByRole('button', { name: 'Войти' }).click();
    await page.waitForURL('**/admin');
}

async function createBranch(request, token, name) {
    const res = await request.post('/api/admin/branches', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data: { name, address: 'Тест', timezone: 'Europe/Moscow', niche: 'RENT' }
    });
    expect(res.status(), `Создание RENT-филиала: ${res.status()}`).toBe(200);
    const body = await res.json();
    return body.id;
}

async function createResource(request, token, branchId, name) {
    const res = await request.post('/api/admin/resources', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data: { name, description: 'Объект аренды', branchId }
    });
    expect(res.status(), `Создание ресурса: ${res.status()}`).toBe(200);
    const body = await res.json();
    return body.id;
}

async function createAppointment(request, token, data) {
    return request.post('/api/admin/appointments', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data
    });
}

// ========== API: МНОГОДНЕВНАЯ АРЕНДА ==========

test.describe('RENT: многодневная аренда (API)', () => {

    test('аренда на 48 часов видна на каждом покрытом дне', async ({ request }) => {
        const token = await loginAsAdmin(request);
        const branchId = await createBranch(request, token, uid('rent-branch'));
        const resourceId = await createResource(request, token, branchId, uid('rent-res'));

        // 21.02 09:00+03 на 48ч → покрывает 21, 22 и утро 23 февраля
        const res = await createAppointment(request, token, {
            startTime: '2026-02-21T09:00:00+03:00',
            durationInMinutes: 2880,
            service: 'Аренда бокса',
            clientName: 'Многодневная Аренда',
            clientPhone: '79990000001',
            resourceId,
            branchId
        });
        expect(res.status(), `Создание аренды: ${res.status()}`).toBe(200);
        const created = await res.json();

        const dayRes = async (date) => {
            const r = await request.get(`/api/admin/appointments/day?date=${date}&branchId=${branchId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            expect(r.status()).toBe(200);
            return r.json();
        };

        for (const date of ['2026-02-21', '2026-02-22', '2026-02-23']) {
            const day = await dayRes(date);
            expect(
                day.some(a => a.id === created.id),
                `Запись должна быть видна на ${date}`
            ).toBe(true);
        }
    });

    test('пересечение интервалов одного ресурса отклоняется (409), свободное окно проходит', async ({ request }) => {
        const token = await loginAsAdmin(request);
        const branchId = await createBranch(request, token, uid('rent-branch'));
        const resourceId = await createResource(request, token, branchId, uid('rent-res'));

        // Существующая аренда: 21.02 09:00+03 → 23.02 09:00+03
        const base = await createAppointment(request, token, {
            startTime: '2026-02-21T09:00:00+03:00',
            durationInMinutes: 2880,
            service: 'Аренда бокса',
            clientName: 'Базовая Аренда',
            clientPhone: '79990000002',
            resourceId,
            branchId
        });
        expect(base.status()).toBe(200);

        // Пересечение на второй день (22.02 10:00) → 409 «Ресурс занят»
        const overlap = await createAppointment(request, token, {
            startTime: '2026-02-22T10:00:00+03:00',
            durationInMinutes: 60,
            service: 'Аренда бокса',
            clientName: 'Конфликт',
            clientPhone: '79990000003',
            resourceId,
            branchId
        });
        expect(overlap.status(), 'Пересекающаяся аренда должна отклоняться').toBe(409);

        // Свободное окно после окончания (24.02) → 200
        const free = await createAppointment(request, token, {
            startTime: '2026-02-24T10:00:00+03:00',
            durationInMinutes: 60,
            service: 'Аренда бокса',
            clientName: 'Свободное Окно',
            clientPhone: '79990000004',
            resourceId,
            branchId
        });
        expect(free.status(), 'Свободное окно должно проходить').toBe(200);
    });
});

// ========== UI: ТАЙМЛАЙН АРЕНДЫ ==========

test.describe('RENT: таймлайн (UI)', () => {

    test('админ видит колонку ресурса и запись аренды на таймлайне', async ({ page, request }) => {
        const token = await loginAsAdmin(request);
        const branchId = await createBranch(request, token, uid('rent-branch'));
        const resourceName = uid('rent-res');
        const resourceId = await createResource(request, token, branchId, resourceName);

        // Одиночная аренда на «сегодня» (Europe/Moscow, без DST → +03:00)
        const todayMsk = new Date().toLocaleDateString('sv-SE', { timeZone: 'Europe/Moscow' });
        const res = await createAppointment(request, token, {
            startTime: `${todayMsk}T10:00:00+03:00`,
            durationInMinutes: 60,
            service: 'Аренда бокса',
            clientName: 'Тестовая Аренда',
            clientPhone: '79990000005',
            resourceId,
            branchId
        });
        expect(res.status(), `Создание аренды на сегодня: ${res.status()}`).toBe(200);

        // UI: логин админа, выбор RENT-филиала, открытие таймлайна
        await loginAdminInBrowser(page);

        await page.selectOption('.branch-select', branchId);
        await page.getByRole('button', { name: 'Таймлайн' }).click();

        // Колонка ресурса (объекта аренды) отображается в шапке таймлайна
        await expect(page.locator('.staff-cell .n', { hasText: resourceName }).first()).toBeVisible();
        // Блок записи с именем клиента виден на таймлайне
        await expect(page.locator('.appt-box', { hasText: 'Тестовая Аренда' }).first()).toBeVisible();
    });

    test('после «ОБНОВИТЬ» в редактировании запись остаётся на таймлайне', async ({ page, request }) => {
        const token = await loginAsAdmin(request);
        const branchId = await createBranch(request, token, uid('rent-branch'));
        const resourceName = uid('rent-res');
        const resourceId = await createResource(request, token, branchId, resourceName);

        const todayMsk = new Date().toLocaleDateString('sv-SE', { timeZone: 'Europe/Moscow' });
        const res = await createAppointment(request, token, {
            startTime: `${todayMsk}T10:00:00+03:00`,
            durationInMinutes: 60,
            service: 'Аренда бокса',
            clientName: 'Тестовая Аренда',
            clientPhone: '79990000006',
            resourceId,
            branchId
        });
        expect(res.status(), `Создание аренды на сегодня: ${res.status()}`).toBe(200);

        await loginAdminInBrowser(page);

        await page.selectOption('.branch-select', branchId);
        await page.getByRole('button', { name: 'Таймлайн' }).click();

        const block = page.locator('.appt-box', { hasText: 'Тестовая Аренда' }).first();
        await expect(block).toBeVisible();

        // Открываем детали → «Изменить всё» → «ОБНОВИТЬ» без изменений
        await block.click();
        await page.getByRole('button', { name: 'Изменить всё' }).click();
        await page.getByRole('button', { name: 'ОБНОВИТЬ' }).click();

        // РЕГРЕССИЯ: после сохранения запись должна остаться на таймлайне
        await expect(page.locator('.appt-box', { hasText: 'Тестовая Аренда' }).first()).toBeVisible();
    });
});
