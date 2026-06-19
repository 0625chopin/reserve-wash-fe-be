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

test('매장을 선택해도 다시 열면 다른 매장이 보인다 (필터 잔류 회귀 방지)', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  // 선택 후 다시 포커스하면 전체 목록이 다시 보여야 한다
  await page.getByTestId('store-select-input').click()
  const options = page.getByTestId('store-select-options')
  await expect(options).toContainText('강남점')
  await expect(options).toContainText('홍대점')
})

test('한 select를 열면 다른 select·바깥 클릭으로 드롭다운이 닫힌다', async ({ page }) => {
  await login(page)
  // 매장 선택 후 매니저 노출
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await expect(page.getByTestId('manager-select-input')).toBeVisible()

  // 매니저 드롭다운 열기 → 매장 select 클릭하면 매니저 목록이 닫힌다
  await page.getByTestId('manager-select-input').click()
  await expect(page.getByTestId('manager-select-options')).toBeVisible()
  await page.getByTestId('store-select-input').click()
  await expect(page.getByTestId('manager-select-options')).toBeHidden()
  await expect(page.getByTestId('store-select-options')).toBeVisible()

  // 바깥(헤더 등)을 클릭하면 열린 드롭다운이 닫힌다
  await page.getByRole('heading', { name: '예약하기' }).click()
  await expect(page.getByTestId('store-select-options')).toBeHidden()
})

test('차종(차 크기)에 따라 베이 목록이 달라진다', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  await page.getByTestId('manager-select-option').filter({ hasText: '이매니저' }).click()

  // 대형·SUV → 강남점의 LARGE 베이(A3)만 노출, A1은 제외
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: '대형' }).click()
  await page.getByTestId('bay-select-input').click()
  const largeBays = page.getByTestId('bay-select-options')
  await expect(largeBays).toContainText('A3')
  await expect(largeBays).not.toContainText('A1')

  // 소형 → 모든 베이(A1 포함) 노출
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: '소형' }).click()
  await page.getByTestId('bay-select-input').click()
  await expect(page.getByTestId('bay-select-options')).toContainText('A1')
})

test('매장·매니저·날짜·시간 선택 후 예약하기로 예약이 접수된다', async ({ page }) => {
  await login(page)
  // 매장 → 매니저(휴무 없는 이매니저) → 차종(소형) → 베이(A1)
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  await page.getByTestId('manager-select-option').filter({ hasText: '이매니저' }).click()
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: '소형' }).click()
  await page.getByTestId('bay-select-input').click()
  await page.getByTestId('bay-select-option').filter({ hasText: 'A1' }).click()
  // 날짜·시간 휠(클라이언트 전용)이 렌더되면 중앙값이 자동 선택됨
  await expect(page.getByTestId('date-wheel')).toBeVisible()
  await expect(page.getByTestId('time-wheel')).toBeVisible()
  const submit = page.getByTestId('reserve-submit')
  await expect(submit).toBeEnabled()
  await submit.click()
  const result = page.getByTestId('reserve-result')
  await expect(result).toContainText('강남점')
  await expect(result).toContainText('이매니저')
})
