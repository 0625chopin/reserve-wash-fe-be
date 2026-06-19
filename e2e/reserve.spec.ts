import { expect, test, type Page } from '@playwright/test'

// Phase 4 매장/매니저 선택 + 검색 UX — 승인 매장 필터/검색/매장별 매니저 한정

// 로그인 후 /reserve 진입 (하이드레이션 대기)
async function login(page: Page) {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve/)
}

test('승인된 매장만 노출되고 미승인 매장(판교점)은 제외된다', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').click()
  const options = page.getByTestId('store-select-options')
  await expect(options).toContainText('강남점')
  await expect(options).toContainText('홍대점')
  await expect(options).not.toContainText('판교점')
})

test('매장 검색어 입력 시 목록이 실시간 필터링된다', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').fill('강남')
  const options = page.getByTestId('store-select-options')
  await expect(options).toContainText('강남점')
  await expect(options).not.toContainText('홍대점')
})

test('매장 선택 후 해당 매장의 매니저만 노출된다', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  // 매니저 select 노출
  await expect(page.getByTestId('manager-select-input')).toBeVisible()
  await page.getByTestId('manager-select-input').click()
  const managerOptions = page.getByTestId('manager-select-options')
  // store1 매니저(김매니저/이매니저)만, 다른 매장 매니저(박매니저=store2)는 미노출
  await expect(managerOptions).toContainText('김매니저')
  await expect(managerOptions).toContainText('이매니저')
  await expect(managerOptions).not.toContainText('박매니저')
})
