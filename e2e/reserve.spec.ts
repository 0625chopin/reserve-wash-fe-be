import { expect, test, type Page } from '@playwright/test'

// 예약 위저드 3페이지 흐름 E2E
// 1p /reserve (매장·매니저·차종·서비스) → 2p /reserve/slot (날짜·시간·베이 그리드) → 3p /reserve/done (완료)

// 로그인 후 /reserve(1페이지) 진입
async function login(page: Page) {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve$/)
}

// 1페이지 완료 — 강남점→매니저→차종→서비스 선택 후 다음(→/reserve/slot)
async function completeStep1(
  page: Page,
  opts: { manager: string; carType: string; service: string },
) {
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  await page.getByTestId('manager-select-option').filter({ hasText: opts.manager }).click()
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: opts.carType }).click()
  await page.getByTestId('service-select-input').click()
  await page.getByTestId('service-select-option').filter({ hasText: opts.service }).click()
  await page.getByTestId('reserve-next').click()
  await expect(page).toHaveURL(/\/reserve\/slot$/)
}

// 2페이지 날짜·시간 휠 고정 선택
async function pickDateTime(page: Page, dateVal: string, timeVal: string) {
  await page.getByTestId('date-wheel').locator(`[data-value="${dateVal}"]`).click()
  await page.getByTestId('time-wheel').locator(`[data-value="${timeVal}"]`).click()
}

// ── 1페이지: 매장/매니저 선택 UX ──

test('1페이지: 승인된 매장만 노출되고 미승인 매장(판교점)은 제외된다', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').click()
  const options = page.getByTestId('store-select-options')
  await expect(options).toContainText('강남점')
  await expect(options).toContainText('홍대점')
  await expect(options).not.toContainText('판교점')
})

test('1페이지: 매장 검색어 입력 시 목록이 실시간 필터링된다', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').fill('강남')
  const options = page.getByTestId('store-select-options')
  await expect(options).toContainText('강남점')
  await expect(options).not.toContainText('홍대점')
})

test('1페이지: 매장 선택 후 해당 매장의 매니저만 노출된다', async ({ page }) => {
  await login(page)
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  const managerOptions = page.getByTestId('manager-select-options')
  await expect(managerOptions).toContainText('김매니저')
  await expect(managerOptions).toContainText('이매니저')
  await expect(managerOptions).not.toContainText('박매니저')
})

// ── 재진입 초기화 / 이전 유지 ──

test('재진입: 네비로 /reserve에 다시 들어오면 진행상태가 초기화되어 매장 선택만 노출된다', async ({
  page,
}) => {
  await login(page)
  // 1페이지 일부 선택 → 매니저 블록 노출
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await expect(page.getByTestId('manager-select-input')).toBeVisible()
  // 다른 화면 갔다가 네비로 /reserve 재진입
  await page.getByTestId('nav-reservations').click()
  await expect(page).toHaveURL(/\/reservations$/)
  await page.getByTestId('nav-reserve').click()
  await expect(page).toHaveURL(/\/reserve$/)
  // 초기화: 매니저/차종/서비스 블록 미노출(매장 선택만)
  await expect(page.getByTestId('store-select-input')).toBeVisible()
  await expect(page.getByTestId('manager-select-input')).toHaveCount(0)
  await expect(page.getByTestId('cartype-select-input')).toHaveCount(0)
})

test('이전: 2페이지에서 이전으로 돌아오면 진행상태가 초기화되어 매장 선택만 노출된다', async ({
  page,
}) => {
  await login(page)
  await completeStep1(page, { manager: '이매니저', carType: '소형', service: '외부세차' })
  await page.getByTestId('reserve-prev').click()
  await expect(page).toHaveURL(/\/reserve$/)
  // 초기화: 매장 선택만 노출(매니저/차종/서비스 블록 미노출)
  await expect(page.getByTestId('store-select-input')).toBeVisible()
  await expect(page.getByTestId('manager-select-input')).toHaveCount(0)
  await expect(page.getByTestId('cartype-select-input')).toHaveCount(0)
  await expect(page.getByTestId('service-select-input')).toHaveCount(0)
})

// ── 위저드 진입 가드 ──

test('가드: 미완료 상태로 2·3페이지 직접 진입 시 /reserve로 리다이렉트된다', async ({ page }) => {
  await login(page)
  await page.goto('/reserve/slot')
  await expect(page).toHaveURL(/\/reserve$/)
  await page.goto('/reserve/done')
  await expect(page).toHaveURL(/\/reserve$/)
})

// ── 정상 예약 흐름 (1→2→3) ──

test('정상 흐름: 1p 선택→2p 날짜·시간·베이→확정→3p 완료', async ({ page }) => {
  await login(page)
  // 1페이지: 가격(소형×외부세차=12,000원) 확인 후 다음
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  await page.getByTestId('manager-select-option').filter({ hasText: '이매니저' }).click()
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: '소형' }).click()
  await page.getByTestId('service-select-input').click()
  await page.getByTestId('service-select-option').filter({ hasText: '외부세차' }).click()
  await expect(page.getByTestId('price-display')).toContainText('12,000')
  await page.getByTestId('reserve-next').click()
  await expect(page).toHaveURL(/\/reserve\/slot$/)

  // 2페이지: 비점유 슬롯(2026-06-26 11:00) → 베이 그리드 A1 클릭 → HOLDING → 예약하기
  await pickDateTime(page, '2026-06-26', '11:00')
  const cell = page.getByTestId('slot-store1-store1-A1-11:00')
  await expect(cell).toBeVisible()
  await cell.click()
  await expect(cell).toHaveAttribute('data-status', 'HOLDING')
  await page.getByTestId('reserve-submit').click()

  // 3페이지: 완료 요약
  await expect(page).toHaveURL(/\/reserve\/done$/)
  const result = page.getByTestId('reserve-result')
  await expect(result).toContainText('외부세차')
  await expect(result).toContainText('현장결제')
})

// ── 차종(차 크기)에 따라 2페이지 베이 그리드 구성이 달라진다 ──

test('2페이지: 대형 차종은 베이 그리드에 LARGE 베이(A3)만 노출, A1은 제외', async ({ page }) => {
  await login(page)
  await completeStep1(page, { manager: '이매니저', carType: '대형', service: '외부세차' })
  await pickDateTime(page, '2026-06-26', '11:00')
  await expect(page.getByTestId('slot-store1-store1-A3-11:00')).toBeVisible()
  await expect(page.getByTestId('slot-store1-store1-A1-11:00')).toHaveCount(0)
})

// ── 교대조 휴무 시간대만 비활성 ──

test('2페이지: 오전조(SHIFT_1) 휴무일은 해당 시간대만 비활성되고 다른 시간대는 선택 가능', async ({
  page,
}) => {
  await login(page)
  // 김매니저: 2026-06-23 오전조(SHIFT_1 06:00~14:00) 휴무
  await completeStep1(page, { manager: '김매니저', carType: '소형', service: '외부세차' })
  await page.getByTestId('date-wheel').locator('[data-value="2026-06-23"]').click()
  await expect(page.getByTestId('time-wheel').locator('[data-value="08:00"]')).toHaveClass(
    /disabled/,
  )
  await expect(page.getByTestId('time-wheel').locator('[data-value="14:00"]')).not.toHaveClass(
    /disabled/,
  )
})

// ── 예약된 베이는 그리드에서 비활성, 같은 시간대 다른 베이는 선택 가능 ──

test('2페이지: 예약된(시드) 베이는 그리드에서 비활성이고 다른 베이로 예약할 수 있다', async ({
  page,
}) => {
  await login(page)
  await completeStep1(page, { manager: '이매니저', carType: '소형', service: '외부세차' })
  // 시드로 A1이 RESERVED인 슬롯(2026-06-25 10:00)
  await pickDateTime(page, '2026-06-25', '10:00')
  const a1 = page.getByTestId('slot-store1-store1-A1-10:00')
  await expect(a1).toHaveAttribute('data-status', 'RESERVED')
  await expect(a1).toBeDisabled()
  // 같은 시간대 A2는 선택 가능 → 확정 → 완료
  const a2 = page.getByTestId('slot-store1-store1-A2-10:00')
  await expect(a2).toHaveAttribute('data-status', 'AVAILABLE')
  await a2.click()
  await expect(a2).toHaveAttribute('data-status', 'HOLDING')
  await page.getByTestId('reserve-submit').click()
  await expect(page).toHaveURL(/\/reserve\/done$/)
})
