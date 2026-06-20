import { expect, test, type Page } from '@playwright/test'

// require v1.7 §8·§4.4 — 휴가/반차 1단계 승인(매장매니저관리자 종결) + 가입 2단계 승인(M7→S3) + PENDING 로그인 차단 + 역할 가드.
// 휴무 신청은 슬롯과 무관하나, 타 spec과 겹치지 않는 고유 날짜(2026-11-10)를 쓴다.

const DATE = '2026-11-10'

async function loginAs(page: Page, email: string) {
  await page.context().clearCookies()
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve$/)
}

// 신청 페이지 진입 — 매장은 본인 소속(강남점)으로 고정·disabled, 매니저 옵션이 채워질 때까지 대기
async function openDayoffsForGangnam(page: Page) {
  await page.goto('/manager/dayoffs', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-manager-dayoffs')).toBeVisible()
  // 매장 고정 후 매니저 옵션이 채워졌는지 확인(게이트)
  await expect(page.getByTestId('dayoff-manager')).toContainText('이매니저')
}

test('휴가/반차 1단계 승인: 매니저 상신 → 매장매니저관리자 승인 종결', async ({ page }) => {
  // 1) 일반매장매니저 상신(M6)
  await loginAs(page, 'manager@test.com')
  await openDayoffsForGangnam(page)
  await page.getByTestId('dayoff-manager').selectOption({ label: '이매니저' })
  await page.getByTestId('dayoff-type').selectOption({ label: '전일' })
  await page.getByTestId('dayoff-date').fill(DATE)
  await page.getByTestId('dayoff-submit').click()
  const myRow = page.locator('[data-testid^="dayoff-row-"]').filter({ hasText: DATE }).first()
  await expect(myRow).toContainText('상신')

  // 2) 매장매니저관리자(STORE_ADMIN) 1단계 승인 종결(M8) — 관리자 개입 없음
  await loginAs(page, 'storeadmin@test.com')
  await page.goto('/store-admin/dayoff-approvals', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-store-admin-dayoff-approvals')).toBeVisible()
  const row = page
    .locator('[data-testid^="dayoff-approval-row-"]')
    .filter({ hasText: DATE })
    .first()
  await row.locator('[data-testid^="dayoff-approve-"]').click()
  await expect(row).toContainText('승인(확정)')
})

test('가입 2단계 승인: 매장매니저관리자 1차 → 관리자 2차 → 매니저 로그인 성공', async ({ page }) => {
  // 1) 매장매니저관리자(STORE_ADMIN) 1차 승인(M7) — PENDING_APPROVAL_L1 → L2
  await loginAs(page, 'storeadmin@test.com')
  await page.goto('/store-admin/manager-signups', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-store-admin-manager-signups')).toBeVisible()
  await page.getByTestId('signup-approve-pending1').click()
  // 승인되면 1차 대기 목록에서 사라진다
  await expect(page.getByTestId('signup-row-pending1')).toHaveCount(0)

  // 2) 관리자(ADMIN) 2차 최종 승인(S3) — L2 → ACTIVE
  await loginAs(page, 'admin@test.com')
  await page.goto('/admin/manager-approvals', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-admin-manager-approvals')).toBeVisible()
  await page.getByTestId('manager-confirm-pending1').click()
  await expect(page.getByTestId('manager-approval-row-pending1')).toHaveCount(0)

  // 3) ACTIVE 전환된 매니저 로그인 성공
  await loginAs(page, 'pending1@test.com')
})

test('PENDING 매니저는 승인 전 로그인할 수 없다', async ({ page }) => {
  await page.context().clearCookies()
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('pending2@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  // 승인 대기(403) → 로그인 실패 에러 노출, /reserve로 이동하지 않음
  await expect(page.getByTestId('login-error')).toBeVisible()
  await expect(page).toHaveURL(/\/login/)
})

test('역할 가드: USER가 BO 결재 페이지에 진입하면 /reserve로 리다이렉트된다', async ({ page }) => {
  await loginAs(page, 'user@test.com')
  await page.goto('/manager/dayoffs')
  await expect(page).toHaveURL(/\/reserve$/)
  await page.goto('/store-admin/dayoff-approvals')
  await expect(page).toHaveURL(/\/reserve$/)
  await page.goto('/store-admin/manager-signups')
  await expect(page).toHaveURL(/\/reserve$/)
  await page.goto('/admin/manager-approvals')
  await expect(page).toHaveURL(/\/reserve$/)
})
