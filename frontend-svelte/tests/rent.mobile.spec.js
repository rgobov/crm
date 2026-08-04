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

async function createBranch(request, token, name, niche = 'RENT') {
    const res = await request.post('/api/admin/branches', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data: { name, address: 'Тест', timezone: 'Europe/Moscow', niche }
    });
    expect(res.status(), `Создание филиала: ${res.status()}`).toBe(200);
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

async function createContact(request, token, name, tags) {
    const res = await request.post('/api/admin/clients', {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data: { name, phones: [`7999${Date.now().toString().slice(-7)}`], tags }
    });
    expect(res.status(), `Создание клиента: ${res.status()}`).toBe(200);
    return (await res.json()).id;
}

async function selectInitialMobileBranch(page, branchName) {
    const requiredDialog = page.getByRole('dialog', { name: 'Выберите филиал' });
    await page.waitForFunction(() => Boolean(
        document.querySelector('.branch-required-dialog') || localStorage.getItem('activeBranchId')
    ));
    if (await requiredDialog.isVisible()) {
        await requiredDialog.getByRole('button', { name: branchName }).click();
    }
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
        await selectInitialMobileBranch(page, branchName);
        await expect.poll(() => page.evaluate(() => localStorage.getItem('activeBranchId'))).toBe(branchId);

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

    test('форма RENT не показывает нишевые теги и ресурсы других филиалов', async ({ page, request }) => {
        const token = await loginAsAdmin(request);
        const rentBranchName = uid('rent-form-branch');
        const otherBranchName = uid('auto-form-branch');
        const rentBranchId = await createBranch(request, token, rentBranchName, 'RENT');
        const otherBranchId = await createBranch(request, token, otherBranchName, 'AUTO');
        const rentResourceName = uid('rent-form-resource');
        const otherResourceName = uid('auto-form-resource');

        await createResource(request, token, rentBranchId, rentResourceName);
        await createResource(request, token, otherBranchId, otherResourceName);

        const contactName = uid('client-with-auto-object');
        await createContact(request, token, contactName, [uid('auto-object')]);

        await loginAdminInBrowser(page);
        await selectInitialMobileBranch(page, rentBranchName);
        await page.getByRole('button', { name: 'Таймлайн' }).click();

        await expect(page.locator('.staff-cell .n', { hasText: rentResourceName }).first()).toBeVisible();

        const freeSlot = page.locator('.slot-btn:not(.is-off):not(.is-break)').first();
        await freeSlot.click();
        await expect(page.getByText('Создание записи')).toBeVisible();

        await expect(page.locator('.reference-card')).toHaveCount(0);
        await expect(page.locator('#ref-tag-id')).toHaveCount(0);
        await expect(page.locator('#comment-id')).toBeVisible();

        const resourceSelect = page.locator('#resource-select-mobile');
        await expect(resourceSelect).toBeVisible();
        const resourceOptions = await resourceSelect.locator('option').allTextContents();
        expect(resourceOptions).toContain(rentResourceName);
        expect(resourceOptions).not.toContain(otherResourceName);

        await page.locator('#client-search-input').fill(contactName);
        await expect(page.locator('.drop .dropdown-action-btn').filter({ hasText: contactName }).last()).toBeVisible();
        await page.locator('.drop .dropdown-action-btn').filter({ hasText: contactName }).last().click();
        await expect(page.locator('.tag-chip')).toHaveCount(0);
    });

    test('карточка филиала доступна над нижней навигацией на коротком экране', async ({ page, request }) => {
        const token = await loginAsAdmin(request);
        const branchName = uid('mobile-branches-page');
        await createBranch(request, token, branchName, 'RENT');

        await loginAdminInBrowser(page);
        await selectInitialMobileBranch(page, branchName);
        await page.goto('/admin/branches');

        const branchCard = page.locator('.branch-card').filter({ hasText: branchName });
        await expect(branchCard).toBeVisible();
        await branchCard.scrollIntoViewIfNeeded();

        const cardBox = await branchCard.boundingBox();
        const bottomUiBox = await page.locator('.mobile-bottom-ui').boundingBox();
        expect(cardBox).not.toBeNull();
        expect(bottomUiBox).not.toBeNull();
        expect(cardBox.y + cardBox.height).toBeLessThanOrEqual(bottomUiBox.y + 1);

        await branchCard.click();
        const modal = page.locator('.modal-card');
        await expect(modal).toBeVisible();
        await expect(modal.locator('.modal-footer')).toBeVisible();

        const footerBox = await modal.locator('.modal-footer').boundingBox();
        expect(footerBox).not.toBeNull();
        expect(footerBox.y + footerBox.height).toBeLessThanOrEqual(page.viewportSize().height);
    });
});
