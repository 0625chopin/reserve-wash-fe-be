import { expect, test } from '@playwright/test'

// Phase 2 라우팅/레이아웃 스모크 테스트 — 파일 기반 라우트 + 미들웨어 가드 확인

test('로그인 페이지가 렌더된다', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByTestId('page-login')).toBeVisible()
})

test('미인증 상태로 보호 라우트(/reserve) 접근 시 /login으로 리다이렉트된다', async ({ page }) => {
  await page.goto('/reserve')
  await expect(page).toHaveURL(/\/login\?redirect=/)
})

test('공통 네비(AppNav)가 노출되고 로그인 링크로 이동한다', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByTestId('nav-login')).toBeVisible()
  await page.getByTestId('nav-reserve').click()
  // /reserve는 보호 라우트라 미인증 시 /login으로 되돌아온다
  await expect(page).toHaveURL(/\/login/)
})
