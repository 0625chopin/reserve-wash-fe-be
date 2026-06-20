import { expect, test } from '@playwright/test'

// require v1.9 — 로그인 페이지 역할군별 3분리(일반사용자/매니저/관리자) + 매니저 회원가입(승인 대기)

test('매니저 로그인 페이지: 빠른 로그인(매니저) → /manager/reserve로 이동', async ({ page }) => {
  await page.context().clearCookies()
  await page.goto('/manager/login', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-manager-login')).toBeVisible()
  await page.getByTestId('quick-login-manager').click()
  await expect(page).toHaveURL(/\/manager\/reserve$/)
})

test('관리자 로그인 페이지: 빠른 로그인(관리자) → /admin/manager-approvals로 이동', async ({ page }) => {
  await page.context().clearCookies()
  await page.goto('/admin/login', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-admin-login')).toBeVisible()
  await page.getByTestId('quick-login-admin').click()
  await expect(page).toHaveURL(/\/admin\/manager-approvals$/)
})

test('일반 로그인 페이지에서 매니저/관리자 로그인으로 이동할 수 있다', async ({ page }) => {
  await page.context().clearCookies()
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('link-manager-login').click()
  await expect(page).toHaveURL(/\/manager\/login$/)
  await page.getByTestId('link-admin-login').click()
  await expect(page).toHaveURL(/\/admin\/login$/)
})

test('매니저 회원가입: 소속 매장 선택 후 신청하면 승인 대기 안내가 표시된다', async ({ page }) => {
  await page.context().clearCookies()
  await page.goto('/manager/signup', { waitUntil: 'networkidle' })
  await expect(page.getByTestId('page-manager-signup')).toBeVisible()
  // 고유 이메일(타 실행과 충돌 회피)
  const email = `newmgr_${Date.now()}@test.com`
  await page.getByTestId('signup-name').fill('신규매니저')
  await page.getByTestId('signup-store').selectOption({ label: '강남점' })
  await page.getByTestId('signup-email').fill(email)
  await page.getByTestId('signup-password').fill('password')
  await page.getByTestId('signup-password-confirm').fill('password')
  await page.getByTestId('signup-submit').click()
  // 자동 로그인 없이 승인 대기 안내
  await expect(page.getByTestId('manager-signup-done')).toBeVisible()

  // 승인 전(PENDING)이라 매니저 로그인 시도는 실패한다
  await page.goto('/manager/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page.getByTestId('login-error')).toBeVisible()
  await expect(page).toHaveURL(/\/manager\/login/)
})
