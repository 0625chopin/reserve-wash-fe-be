import { expect, test } from '@playwright/test'

// 기본 동작 스모크 테스트 — 홈/About 라우팅 확인
test('홈 페이지가 렌더되고 환영 메시지가 보인다', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByText('You did it!')).toBeVisible()
})

test('About 링크로 이동하면 about 페이지가 보인다', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('link', { name: 'About' }).click()
  await expect(page).toHaveURL(/\/about$/)
  await expect(page.getByRole('heading', { name: 'This is an about page' })).toBeVisible()
})
