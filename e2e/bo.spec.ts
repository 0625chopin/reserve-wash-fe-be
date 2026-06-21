import { expect, test, type Page } from '@playwright/test'

// Phase 6 — BO: 매니저 대행 예약(M3)·관리자 매장 관리(S4·S5)·역할 가드(role-guard)
// 슬롯은 서버 전역 1회 예약 자원 → BO 대행은 타 spec과 겹치지 않는 고유 날짜(2026-10-xx) 사용.

// 쿠키를 비우고 지정 계정으로 로그인 → /reserve
async function loginAs(page: Page, email: string) {
  await page.context().clearCookies()
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  // 로그인 성공 — 역할별 기본 화면으로 이동(role별 경로가 다르므로 로그인 상태로 판정)
  await expect(page.getByTestId('nav-logout')).toBeVisible()
}

// 매니저 대행 폼 채우고 제출 (매장=본인 소속 고정, 김매니저·소형·외부세차·A2)
// 날짜·시간은 일반예약과 동일한 휠에서 선택(data-value 클릭). date/time은 오늘+21일 범위 내여야 한다.
async function proxyReserve(page: Page, date: string, time: string) {
  await page.goto('/manager/reserve', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-manager-reserve')).toBeVisible()
  // 매장은 본인 소속(강남점)으로 고정·disabled — 매니저 옵션이 채워질 때까지 대기
  await expect(page.getByTestId('proxy-manager')).toContainText('김매니저')
  await page.getByTestId('proxy-manager').selectOption({ label: '김매니저' })
  await page.getByTestId('proxy-cartype').selectOption({ label: '소형' })
  await page.getByTestId('proxy-service').selectOption({ label: '외부세차' })
  await page.getByTestId('proxy-bay').selectOption({ label: 'A2' })
  // 날짜·시간 휠에서 선택(클릭 시 해당 항목으로 스크롤·확정)
  await page.getByTestId('proxy-date-wheel').locator(`[data-value="${date}"]`).click()
  await page.getByTestId('proxy-time-wheel').locator(`[data-value="${time}"]`).click()
  await page.getByTestId('proxy-customer-email').fill('user@test.com')
  await expect(page.getByTestId('proxy-customer-email')).toHaveValue('user@test.com')
  await expect(page.getByTestId('proxy-submit')).toBeEnabled()
  await page.getByTestId('proxy-submit').click()
}

test('매니저 대행: 소속 매장 고객 예약을 대행하면 완료 메시지가 표시된다', async ({ page }) => {
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-07-04', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')
})

test('크로스롤: 매니저 대행 예약이 일반사용자 예약 목록에 표시된다 (BUG-1/2)', async ({ page }) => {
  // 매니저가 user@test.com 대상 대행 예약 생성(고유 슬롯, 휠 범위 내)
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-07-06', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')

  // 일반사용자로 로그인 → 예약 목록이 서버에서 하이드레이션되어 대행 예약이 보여야 한다
  // (이전: /reservations가 인메모리만 읽어 대행 예약이 보이지 않던 결함의 회귀 가드)
  await loginAs(page, 'user@test.com')
  await page.goto('/reservations', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-reservations')).toBeVisible()
  await expect(page.getByText('2026-07-06 09:00')).toBeVisible()
})

test('크로스롤: 매니저 담당 예약 목록(담당 탭)에 고객/대행 예약이 표시된다 (v1.10 §6.6)', async ({ page }) => {
  // 매니저(김매니저=mgr1)가 user@test.com 대상 대행 예약 생성(고유 슬롯, 휠 범위 내)
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-07-07', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')

  // 동일 매니저(manager@test.com↔mgr1)로 예약 목록 진입 → '담당 예약' 탭은 managerId(mgr1) 기준 목록
  //   (본인 userId 예약이 아닌, 고객이 지정·대행 등록한 예약이 노출되어야 한다)
  await page.goto('/reservations', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-reservations')).toBeVisible()
  await page.getByTestId('tab-assigned').click()
  await expect(page.getByText('2026-07-07 09:00')).toBeVisible()
})

test('크로스롤: 매장매니저관리자 예약 목록에 매장 전체 예약이 표시된다 (v1.10 §6.6)', async ({ page }) => {
  // 대행 예약 1건 생성(store1, mgr1, 고유 슬롯)
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-07-08', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')

  // 매장매니저관리자(storeadmin@test.com, store1)로 전환 → /reservations는 매장 전체 예약을 노출(storeId 기준)
  await loginAs(page, 'storeadmin@test.com')
  await page.goto('/reservations', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-reservations')).toBeVisible()
  await expect(page.getByText('2026-07-08 09:00')).toBeVisible()
})

test('관리자: 예약 목록은 매장 선택 후 해당 매장 예약만 표시하고, 메뉴에 예약/휴일결재가 없다 (v1.11 §6.6)', async ({ page }) => {
  // 대행 예약 1건 생성(store1, 고유 슬롯)
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-07-09', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')

  // 관리자로 전환 → 예약 목록 진입
  await loginAs(page, 'admin@test.com')
  await page.goto('/reservations', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-reservations')).toBeVisible()

  // 관리자 메뉴: '예약'(부킹)·'휴일 결재' 제거 확인
  await expect(page.getByTestId('nav-reserve')).toHaveCount(0)
  await expect(page.getByTestId('nav-admin-approvals')).toHaveCount(0)

  // 매장 선택 전: 해당 예약 미노출(안내 빈 상태)
  await expect(page.getByText('2026-07-09 09:00')).toHaveCount(0)
  // store1 선택 → 해당 매장 예약만 노출
  await page.getByTestId('admin-store-select').selectOption('store1')
  await expect(page.getByText('2026-07-09 09:00')).toBeVisible()
})

test('관리자: 매니저를 직접 등록(매장관리매니저)하면 가입 최종 승인 목록에 노출된다 (v1.12)', async ({
  page,
}) => {
  await loginAs(page, 'admin@test.com')
  await page.goto('/admin/managers', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-admin-managers')).toBeVisible()
  // 역할(매장관리매니저=STORE_ADMIN) + 소속 매장 선택 후 등록
  await page.getByTestId('admin-mgr-role').selectOption('STORE_ADMIN')
  await page.getByTestId('admin-mgr-store').selectOption('store1')
  await page.getByTestId('admin-mgr-name').fill('직접등록관리매니저')
  await page.getByTestId('admin-mgr-email').fill('direct-sadm@test.com')
  await page.getByTestId('admin-mgr-password').fill('password')
  await page.getByTestId('admin-mgr-submit').click()
  // 2차 최종 승인 안내
  await expect(page.getByTestId('admin-mgr-message')).toContainText('2차 최종 승인')

  // 가입 최종 승인 목록(PENDING_APPROVAL_L2)에 노출 + 행 클릭 시 상세 모달(역할/소속 매장)
  await page.goto('/admin/manager-approvals', { waitUntil: 'networkidle' })
  const row = page
    .locator('[data-testid^="manager-approval-row-"]')
    .filter({ hasText: 'direct-sadm@test.com' })
  await expect(row).toBeVisible()
  await row.click()
  const modal = page.getByTestId('manager-approval-modal')
  await expect(modal).toBeVisible()
  await expect(modal).toContainText('매장관리매니저')
  await expect(modal).toContainText('강남점')
})

test('역할 가드: USER가 매니저 대행 페이지에 진입하면 /reserve로 리다이렉트된다', async ({ page }) => {
  await loginAs(page, 'user@test.com')
  await page.goto('/manager/reserve', { waitUntil: 'networkidle' })
  await expect(page).toHaveURL(/\/reserve$/)
})

test('역할 가드: USER가 관리자 페이지에 진입하면 /reserve로 리다이렉트된다', async ({ page }) => {
  await loginAs(page, 'user@test.com')
  await page.goto('/admin/stores/store1/reservations', { waitUntil: 'networkidle' })
  await expect(page).toHaveURL(/\/reserve$/)
})

test('관리자: 매장별 예약자(S4)·사용자(S5) 목록을 조회한다', async ({ page }) => {
  // 조회 대상 보장 — 대행 예약 1건 생성(고유 슬롯)
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-07-05', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')

  // 관리자로 전환해 매장 예약자/사용자 조회
  await loginAs(page, 'admin@test.com')
  await page.goto('/admin/stores/store1/reservations', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-admin-reservations')).toBeVisible()
  await expect(page.getByText('user@test.com').first()).toBeVisible()

  await page.goto('/admin/stores/store1/users', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-admin-users')).toBeVisible()
  await expect(page.getByText('user@test.com').first()).toBeVisible()
})
