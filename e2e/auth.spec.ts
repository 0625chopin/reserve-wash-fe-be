import { expect, test } from '@playwright/test'

// Phase 3 인증/로그인 플로우 — 더미 로그인/검증/가드/로그아웃
// 폼 상호작용은 Vue 하이드레이션 이후 동작하므로 networkidle까지 대기한다.

test('올바른 더미 계정으로 로그인하면 /reserve로 이동한다', async ({ page }) => {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve/)
  await expect(page.getByTestId('nav-logout')).toBeVisible()
})

test('잘못된 비밀번호로 로그인하면 에러 메시지가 보이고 /login에 머문다', async ({ page }) => {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('wrong')
  await page.getByTestId('login-submit').click()
  await expect(page.getByTestId('login-error')).toBeVisible()
  await expect(page).toHaveURL(/\/login/)
})

test('로그아웃 후에는 보호 라우트에 접근할 수 없다', async ({ page }) => {
  // 먼저 로그인
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve/)
  // 로그아웃 → /login 이동
  await page.getByTestId('nav-logout').click()
  await expect(page).toHaveURL(/\/login/)
  // 보호 라우트 직접 진입 시 다시 /login으로 리다이렉트
  await page.goto('/reserve')
  await expect(page).toHaveURL(/\/login/)
})
