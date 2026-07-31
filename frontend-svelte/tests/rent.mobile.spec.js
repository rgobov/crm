import { test, expect } from '@playwright/test';

// Мобильный RENT-сценарий (проект "Mobile Chrome", viewport Pixel 5).
// Запускается отдельно: npx playwright test --project="Mobile Chrome"

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

async function createBranch(request, token, name) {
    const res = await request.post('/api/admin/branches', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data: { name, address: 'Тест', timezone: 'Europe/Moscow', niche: 'RENT' }
    });
    expect(res.status(), `Создание RENT-филиала: ${res.status()}`).toBe(200);
    return (await res.json()).id;
}

async function createResource(request, token, branchId, name) {
    const res = await request.post('/api/admin/resources', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data: { name, description: 'Объект аренды', branchId }
    });
    expect(res.status(), `Создание ресурса: ${res.status()}`).toBe(200);
    return (await res.json()).id;
}

async function createAppointment(request, token, data) {
    const res = await request.post('/api/admin/appointments', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data
    });
    expect(res.status(), `Создание аренды: ${res.status()}`).toBe(200);
}

async function loginAdminInBrowser(page) {
    await page.goto('/');
    await page.waitForSelector('#email');
    await page.evaluate(() => {
        const el = document.getElementById('email');
        const pw = document.getElementById('password');
        el.value = 'admin@test.com';
        pw.value = 'password';
        el.dispatchEvent(new Event('input', { bubbles: true }));
        pw.dispatchEvent(new Event('input', { bubbles: true }));
    });
    await page.getByRole('button', { name: 'Войти' }).click();
    await page.waitForURL('**/admin');
}

test.describe('RENT: таймлайн (мобильный)', () => {

    test('выбор RENT-филиала, запись на таймлайне и сохранение через «ОБНОВИТЬ»', async ({ page, request }) => {
        const token = await loginAsAdmin(request);
        const branchName = uid('rent-branch');
        const branchId = await createBranch(request, token, branchName);
        const resourceName = uid('rent-res');
        const resourceId = await createResource(request, token, branchId, resourceName);

        const todayMsk = new Date().toLocaleDateString('sv-SE', { timeZone: 'Europe/Moscow' });
        await createAppointment(request, token, {
            startTime: `${todayMsk}T10:00:00+03:00`,
            durationInMinutes: 60,
            service: 'Аренда бокса',
            clientName: 'Мобильная Аренда',
            clientPhone: '79990000007',
            resourceId,
            branchId
        });

        await loginAdminInBrowser(page);

        // Мобильная оболочка: «Ещё» → выбор филиала (карточка, а не <select>)
        const bottomNav = page.locator('.bottom-nav');
        await bottomNav.getByRole('button', { name: 'Ещё' }).click();
        await page.locator('.branch-card', { hasText: branchName }).click();
        await bottomNav.getByRole('button', { name: 'Таймлайн' }).click();

        // Колонка ресурса (объекта аренды) видна на таймлайне
        await expect(page.locator('.staff-cell .n', { hasText: resourceName }).first()).toBeVisible();
        // Блок записи виден
        const block = page.locator('.appt-box', { hasText: 'Мобильная Аренда' }).first();
        await expect(block).toBeVisible();

        // Регрессия: открыть запись → «Изменить всё» → «ОБНОВИТЬ» без изменений → запись остаётся
        await block.click();
        await page.getByRole('button', { name: 'Изменить всё' }).click();
        await page.getByRole('button', { name: 'ОБНОВИТЬ' }).click();

        await expect(page.locator('.appt-box', { hasText: 'Мобильная Аренда' }).first()).toBeVisible();
    });
});
