import { expect, test, type Page } from '@playwright/test'

// Phase 7 — 휴무 결재 2단계 승인(매니저 상신 → 최고매니저 1차 → 관리자 2차 확정) + 역할 가드
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

// 결재 신청 페이지에서 강남점을 선택(하이드레이션 워밍업: 첫 상호작용 레이스 회피)
async function openDayoffsForGangnam(page: Page) {
  await page.goto('/manager/dayoffs', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-manager-dayoffs')).toBeVisible()
  await page.getByTestId('dayoff-store').click() // 워밍업
  await page.getByTestId('dayoff-store').selectOption({ label: '강남점' })
  // 매장 선택이 하이드레이션 후에도 유지되어 매니저 옵션이 채워졌는지 확인(게이트)
  await expect(page.getByTestId('dayoff-manager')).toContainText('이매니저')
}

test('휴무 2단계 승인: 매니저 상신 → 최고매니저 1차 → 관리자 2차 확정', async ({ page }) => {
  // 1) 매니저 상신
  await loginAs(page, 'manager@test.com')
  await openDayoffsForGangnam(page)
  await page.getByTestId('dayoff-manager').selectOption({ label: '이매니저' })
  await page.getByTestId('dayoff-type').selectOption({ label: '전일' })
  await page.getByTestId('dayoff-date').fill(DATE)
  await page.getByTestId('dayoff-submit').click()
  const myRow = page.locator('[data-testid^="dayoff-row-"]').filter({ hasText: DATE }).first()
  await expect(myRow).toContainText('상신')

  // 2) 최고매니저(STORE_ADMIN) 1차 승인
  await loginAs(page, 'storeadmin@test.com')
  await openDayoffsForGangnam(page)
  const l1Row = page.locator('[data-testid^="dayoff-row-"]').filter({ hasText: DATE }).first()
  await l1Row.locator('[data-testid^="dayoff-approve-l1-"]').click()
  await expect(l1Row).toContainText('1차 승인')

  // 3) 관리자 2차 승인(확정)
  await loginAs(page, 'admin@test.com')
  await page.goto('/admin/approvals', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-admin-approvals')).toBeVisible()
  const l2Row = page
    .locator('[data-testid^="approval-dayoff-row-"]')
    .filter({ hasText: DATE })
    .first()
  await l2Row.locator('[data-testid^="approve-l2-"]').click()
  await expect(l2Row).toContainText('확정')
})

test('역할 가드: USER가 결재 페이지에 진입하면 /reserve로 리다이렉트된다', async ({ page }) => {
  await loginAs(page, 'user@test.com')
  await page.goto('/manager/dayoffs')
  await expect(page).toHaveURL(/\/reserve$/)
  await page.goto('/admin/approvals')
  await expect(page).toHaveURL(/\/reserve$/)
})
