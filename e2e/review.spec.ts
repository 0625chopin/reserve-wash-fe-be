import { expect, test, type Page } from '@playwright/test'

// Phase 7 — 후기/평점: 작성 자격 가드 + 평점 제출 + 평균 + 목록 진입점 변화
// 예약 id는 fresh 컨텍스트에서 'rsv-1'부터 결정적으로 부여된다.

async function login(page: Page) {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve$/)
}

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

// A1 베이 1건 예약 생성(RESERVED) → /reserve/done
async function createReservation(page: Page, dateVal: string, timeVal: string) {
  await login(page)
  await step1(page)
  await page.getByTestId('date-wheel').locator(`[data-value="${dateVal}"]`).click()
  await page.getByTestId('time-wheel').locator(`[data-value="${timeVal}"]`).click()
  await page.getByTestId(`slot-store1-store1-A1-${timeVal}`).click()
  await page.getByTestId('reserve-submit').click()
  await expect(page).toHaveURL(/\/reserve\/done$/)
}

test('후기 자격 가드: 미완료/미존재 예약 진입 시 /reservations로 리다이렉트된다', async ({ page }) => {
  // 2단계: 슬롯은 서버 전역 1회 예약 자원 → 테스트별 유니크 슬롯 사용(타 spec과 충돌 회피)
  await createReservation(page, '2026-06-29', '11:00')
  // RESERVED(완료 전) 예약 → 후기 진입 차단
  await page.goto('/review/rsv-1')
  await expect(page).toHaveURL(/\/reservations$/)
  // 미존재 예약 → 차단
  await page.goto('/review/rsv-999')
  await expect(page).toHaveURL(/\/reservations$/)
})

test('후기 작성: 세차완료 예약에 평점 제출 → 완료·평균 표시, 목록 진입점이 작성완료로 바뀐다', async ({
  page,
}) => {
  // 2단계: 유니크 슬롯(타 spec과 충돌 회피)
  await createReservation(page, '2026-06-30', '11:00')
  await page.getByTestId('nav-reservations').click()
  await expect(page).toHaveURL(/\/reservations$/)

  // 세차완료 전이 → 후기 작성 링크 노출
  await page.getByTestId('complete-rsv-1').click()
  await page.getByTestId('review-rsv-1').click()
  await expect(page).toHaveURL(/\/review\/rsv-1$/)

  // 별점 5 + 텍스트 → 제출
  await page.getByTestId('star-5').click()
  await page.getByTestId('review-text').fill('아주 깨끗하게 잘 됐어요')
  await page.getByTestId('review-submit').click()

  // 완료 상태 + 통합 평균(5.0) + 내가 작성한 평점·문구 표시
  await expect(page.getByTestId('review-done')).toBeVisible()
  await expect(page.getByTestId('avg-overall')).toContainText('5.0')
  await expect(page.getByTestId('my-rating')).toContainText('5')
  await expect(page.getByTestId('my-review-text')).toContainText('아주 깨끗하게 잘 됐어요')

  // 목록 재방문 → 작성완료 표시, 작성 링크는 사라짐
  await page.getByTestId('nav-reservations').click()
  await expect(page.getByTestId('reviewed-rsv-1')).toBeVisible()
  await expect(page.getByTestId('review-rsv-1')).toHaveCount(0)
})
