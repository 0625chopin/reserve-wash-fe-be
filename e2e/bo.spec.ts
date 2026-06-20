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
// 하이드레이션 레이스 회피: select 먼저 → 텍스트 입력 → 이메일을 마지막에 채우고 값/활성 확인 후 제출
async function proxyReserve(page: Page, date: string, time: string) {
  await page.goto('/manager/reserve')
  await expect(page.getByTestId('page-manager-reserve')).toBeVisible()
  // 매장은 본인 소속(강남점)으로 고정·disabled — 매니저 옵션이 채워질 때까지 대기
  await expect(page.getByTestId('proxy-manager')).toContainText('김매니저')
  await page.getByTestId('proxy-manager').selectOption({ label: '김매니저' })
  await page.getByTestId('proxy-cartype').selectOption({ label: '소형' })
  await page.getByTestId('proxy-service').selectOption({ label: '외부세차' })
  await page.getByTestId('proxy-bay').selectOption({ label: 'A2' })
  await page.getByTestId('proxy-date').fill(date)
  await page.getByTestId('proxy-time').fill(time)
  await page.getByTestId('proxy-customer-email').fill('user@test.com')
  await expect(page.getByTestId('proxy-customer-email')).toHaveValue('user@test.com')
  await expect(page.getByTestId('proxy-submit')).toBeEnabled()
  await page.getByTestId('proxy-submit').click()
}

test('매니저 대행: 소속 매장 고객 예약을 대행하면 완료 메시지가 표시된다', async ({ page }) => {
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-10-01', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')
})

test('역할 가드: USER가 매니저 대행 페이지에 진입하면 /reserve로 리다이렉트된다', async ({ page }) => {
  await loginAs(page, 'user@test.com')
  await page.goto('/manager/reserve')
  await expect(page).toHaveURL(/\/reserve$/)
})

test('역할 가드: USER가 관리자 페이지에 진입하면 /reserve로 리다이렉트된다', async ({ page }) => {
  await loginAs(page, 'user@test.com')
  await page.goto('/admin/stores/store1/reservations')
  await expect(page).toHaveURL(/\/reserve$/)
})

test('관리자: 매장별 예약자(S4)·사용자(S5) 목록을 조회한다', async ({ page }) => {
  // 조회 대상 보장 — 대행 예약 1건 생성(고유 슬롯)
  await loginAs(page, 'manager@test.com')
  await proxyReserve(page, '2026-10-05', '09:00')
  await expect(page.getByTestId('proxy-result')).toContainText('완료')

  // 관리자로 전환해 매장 예약자/사용자 조회
  await loginAs(page, 'admin@test.com')
  await page.goto('/admin/stores/store1/reservations')
  await expect(page.getByTestId('page-admin-reservations')).toBeVisible()
  await expect(page.getByText('user@test.com').first()).toBeVisible()

  await page.goto('/admin/stores/store1/users')
  await expect(page.getByTestId('page-admin-users')).toBeVisible()
  await expect(page.getByText('user@test.com').first()).toBeVisible()
})
