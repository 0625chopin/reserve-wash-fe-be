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
  // 서비스 선택 → 가격 자동 표시 (require 6.4)
  await page.getByTestId('service-select-input').click()
  await page.getByTestId('service-select-option').filter({ hasText: '외부세차' }).click()
  await expect(page.getByTestId('price-display')).toContainText('원')
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

// Phase 5 — 슬롯 그리드 + 동시성(낙관적 갱신/충돌) 시나리오

// 강남점 → 이매니저(휴무 없음) → 소형 → A1 → 외부세차 까지 공통 선택
async function selectBase(page: Page) {
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  await page.getByTestId('manager-select-option').filter({ hasText: '이매니저' }).click()
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: '소형' }).click()
  await page.getByTestId('bay-select-input').click()
  await page.getByTestId('bay-select-option').filter({ hasText: 'A1' }).click()
  await page.getByTestId('service-select-input').click()
  await page.getByTestId('service-select-option').filter({ hasText: '외부세차' }).click()
}

test('미니그리드 슬롯 클릭 시 HOLDING 표시 후 가격 확인하고 예약이 확정된다', async ({ page }) => {
  await login(page)
  await selectBase(page)
  // 소형 × 외부세차 = 12,000원 (require 10.3)
  await expect(page.getByTestId('price-display')).toContainText('12,000')

  // 비어 있는 슬롯(2026-06-26 11:00)으로 날짜·시간 고정
  await page.getByTestId('date-wheel').locator('[data-value="2026-06-26"]').click()
  await page.getByTestId('time-wheel').locator('[data-value="11:00"]').click()

  // 미니그리드에서 A1 클릭 → 즉시 HOLDING 시각화(낙관적 갱신)
  const cell = page.getByTestId('slot-store1-store1-A1-11:00')
  await expect(cell).toBeVisible()
  await cell.click()
  await expect(cell).toHaveAttribute('data-status', 'HOLDING')

  // 확정 → 성공 토스트(예약 요약)
  await page.getByTestId('reserve-submit').click()
  const result = page.getByTestId('reserve-result')
  await expect(result).toContainText('외부세차')
  await expect(result).toContainText('현장결제')
})

test('이미 예약된(시드) 슬롯 확정 시도 시 충돌 토스트로 재선택을 유도한다', async ({ page }) => {
  await login(page)
  await selectBase(page)

  // 시드로 RESERVED 고정된 슬롯(2026-06-25 10:00, store1-A1) 선택
  await page.getByTestId('date-wheel').locator('[data-value="2026-06-25"]').click()
  await page.getByTestId('time-wheel').locator('[data-value="10:00"]').click()

  // 미니그리드에서 A1은 예약됨(RESERVED)으로 비활성 — 선택 불가
  const cell = page.getByTestId('slot-store1-store1-A1-10:00')
  await expect(cell).toBeVisible()
  await expect(cell).toHaveAttribute('data-status', 'RESERVED')
  await expect(cell).toBeDisabled()

  // 확정 시도 → 충돌 토스트 + 예약 미생성
  await page.getByTestId('reserve-submit').click()
  await expect(page.getByTestId('toast')).toContainText('다른 슬롯을 선택')
  await expect(page.getByTestId('reserve-result')).toHaveCount(0)
})

// 시나리오 C — 매니저 교대조 휴무 시간대 슬롯만 비활성 (require 5.5/6.1)
test('교대조(오전조) 휴무일에는 해당 시간대 슬롯만 비활성되고 다른 시간대는 선택 가능하다', async ({
  page,
}) => {
  await login(page)
  // 김매니저(강남점): 2026-06-23 오전조(SHIFT_1 06:00~14:00) 휴무
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  await page.getByTestId('manager-select-option').filter({ hasText: '김매니저' }).click()
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: '소형' }).click()
  await page.getByTestId('bay-select-input').click()
  await page.getByTestId('bay-select-option').filter({ hasText: 'A1' }).click()

  // 오전조 휴무일(2026-06-23) 선택 — 전일 휴무가 아니므로 날짜 자체는 선택 가능
  await page.getByTestId('date-wheel').locator('[data-value="2026-06-23"]').click()
  // 오전조(06:00~13:30) 시간대는 비활성(취소선), 14:00부터는 선택 가능
  await expect(page.getByTestId('time-wheel').locator('[data-value="08:00"]')).toHaveClass(
    /disabled/,
  )
  await expect(page.getByTestId('time-wheel').locator('[data-value="14:00"]')).not.toHaveClass(
    /disabled/,
  )
})

// 시나리오 D — 점유 베이 사전 안내 + 같은 시간대 다른 베이 선택 가능 (require 6.1)
test('점유된 베이 선택 시 안내가 뜨고 같은 시간대 다른 베이는 선택 가능하다', async ({ page }) => {
  await login(page)
  await selectBase(page) // 강남점→이매니저→소형→A1→외부세차

  // 시드로 A1이 RESERVED인 슬롯(2026-06-25 10:00)
  await page.getByTestId('date-wheel').locator('[data-value="2026-06-25"]').click()
  await page.getByTestId('time-wheel').locator('[data-value="10:00"]').click()

  // 선택한 A1이 점유됨 → 사전 재선택 안내 노출
  await expect(page.getByTestId('bay-occupied-notice')).toBeVisible()

  // 같은 시간대 A2는 선택 가능 → 클릭 시 HOLDING, 안내 사라짐
  const a2 = page.getByTestId('slot-store1-store1-A2-10:00')
  await expect(a2).toHaveAttribute('data-status', 'AVAILABLE')
  await a2.click()
  await expect(a2).toHaveAttribute('data-status', 'HOLDING')
  await expect(page.getByTestId('bay-occupied-notice')).toHaveCount(0)
})
