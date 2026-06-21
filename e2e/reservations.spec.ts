import { expect, test, type Page } from '@playwright/test'

// Phase 6 — 예약 목록 상태 전이(세차완료/취소) + 취소 시 슬롯 release
// 예약 id는 fresh 컨텍스트에서 reservationDraft 시퀀스로 'rsv-1'부터 결정적으로 부여된다.

async function login(page: Page) {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve$/)
}

// 1페이지(강남점→이매니저→소형→외부세차) 선택 후 다음 → /reserve/slot
async function step1(page: Page) {
  await page.getByTestId('store-select-input').click()
  await page.getByTestId('store-select-option').filter({ hasText: '강남점' }).click()
  await page.getByTestId('manager-select-input').click()
  await page.getByTestId('manager-select-option').filter({ hasText: '이매니저' }).click()
  await page.getByTestId('cartype-select-input').click()
  await page.getByTestId('cartype-select-option').filter({ hasText: '소형' }).click()
  await page.getByTestId('service-select-input').click()
  await page.getByTestId('service-select-option').filter({ hasText: '외부세차' }).click()
  await page.getByTestId('reserve-next').click()
  await expect(page).toHaveURL(/\/reserve\/slot$/)
}

// 위저드로 A1 베이 1건 예약 생성 → /reserve/done
async function createReservation(page: Page, dateVal: string, timeVal: string) {
  await login(page)
  await step1(page)
  await page.getByTestId('date-wheel').locator(`[data-value="${dateVal}"]`).click()
  await page.getByTestId('time-wheel').locator(`[data-value="${timeVal}"]`).click()
  const cell = page.getByTestId(`slot-store1-store1-A1-${timeVal}`)
  await expect(cell).toBeVisible()
  await cell.click()
  await page.getByTestId('reserve-submit').click()
  await expect(page).toHaveURL(/\/reserve\/done$/)
}

test('예약 목록: RESERVED로 표시되고 세차완료로 전이된다', async ({ page }) => {
  // 2단계: 슬롯은 서버 전역 1회 예약 자원 → reserve.spec 정상흐름(06-26 11:00)과 겹치지 않는 날짜 사용
  await createReservation(page, '2026-06-28', '11:00')
  await page.getByTestId('nav-reservations').click()
  await expect(page).toHaveURL(/\/reservations$/)

  // 생성된 예약(rsv-1)이 '예약 확정'으로 표시
  await expect(page.getByTestId('status-rsv-1')).toContainText('예약 확정')
  // 세차완료 전이 → '세차 완료', 취소 버튼 사라지고 완료 안내 문구 노출
  await page.getByTestId('complete-rsv-1').click()
  await expect(page.getByTestId('status-rsv-1')).toContainText('세차 완료')
  await expect(page.getByTestId('cancel-rsv-1')).toHaveCount(0)
  await expect(page.getByTestId('completed-rsv-1')).toContainText('세차가 완료')
})

test('서버 하이드레이션: 본인 예약이 하드 새로고침 후에도 목록에 유지된다 (BUG-2)', async ({ page }) => {
  // 위저드로 본인 예약 생성(고유 슬롯) → 목록 진입
  await createReservation(page, '2026-07-03', '13:00')
  await page.getByTestId('nav-reservations').click()
  await expect(page).toHaveURL(/\/reservations$/)
  await expect(page.getByText('2026-07-03 13:00')).toBeVisible()

  // 하드 새로고침 → 인메모리 스토어 초기화에도 서버에서 재로딩되어 카드가 유지되어야 한다
  // (이전: 새로고침/직접 URL 진입 시 본인 예약이 사라지던 결함의 회귀 가드)
  await page.reload({ waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-reservations')).toBeVisible()
  await expect(page.getByText('2026-07-03 13:00')).toBeVisible()
})

test('예약 취소 시 CANCELED로 전이되고 슬롯이 다시 AVAILABLE로 release된다', async ({ page }) => {
  await createReservation(page, '2026-06-27', '09:00')
  await page.getByTestId('nav-reservations').click()
  await expect(page).toHaveURL(/\/reservations$/)

  // 취소 → '취소됨'
  await page.getByTestId('cancel-rsv-1').click()
  await expect(page.getByTestId('status-rsv-1')).toContainText('취소됨')

  // /reserve 재진입 후 동일 슬롯 그리드에서 A1이 다시 AVAILABLE인지 확인(release)
  await page.getByTestId('nav-reserve').click()
  await expect(page).toHaveURL(/\/reserve$/)
  await step1(page)
  await page.getByTestId('date-wheel').locator('[data-value="2026-06-27"]').click()
  await page.getByTestId('time-wheel').locator('[data-value="09:00"]').click()
  await expect(page.getByTestId('slot-store1-store1-A1-09:00')).toHaveAttribute(
    'data-status',
    'AVAILABLE',
  )
})
