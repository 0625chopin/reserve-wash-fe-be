import { expect, test, type Page } from '@playwright/test'

// Phase 3 인증/로그인 플로우 — 더미 로그인/검증/가드/로그아웃
// 폼 상호작용은 Vue 하이드레이션 이후 동작하므로 networkidle까지 대기한다.

// 이메일 인증 코드 조회 — BE 개발용 백도어(GET /api/auth/signup/dev-code).
//   실제 운영은 메일로만 전달하지만, E2E는 결정적 검증을 위해 dev-code로 코드를 읽는다.
async function fetchSignupCode(page: Page, email: string): Promise<string> {
  const res = await page.request.get(
    `http://localhost:8080/api/auth/signup/dev-code?email=${encodeURIComponent(email)}`,
  )
  expect(res.ok()).toBeTruthy()
  return (await res.json()).code as string
}

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

// Phase 3.1 회원가입(FW1) — 이메일 인증(6자리 코드, 3분)/검증/게스트 가드

test('새 이메일로 회원가입하면 이메일 인증 후 자동 로그인되어 /reserve로 이동한다', async ({ page }) => {
  const email = 'newuser@test.com'
  await page.goto('/signup', { waitUntil: 'networkidle' })
  await page.getByTestId('signup-name').fill('신규회원')
  await page.getByTestId('signup-email').fill(email)
  await page.getByTestId('signup-password').fill('mypw1234')
  await page.getByTestId('signup-password-confirm').fill('mypw1234')
  await page.getByTestId('signup-submit').click()
  // 2단계: 코드 입력 화면 + 3:00 형식(mm:ss) 카운트다운
  await expect(page.getByTestId('signup-code')).toBeVisible()
  await expect(page.getByTestId('signup-countdown')).toHaveText(/^[0-3]:[0-5]\d$/)
  // 인증 코드 입력 → 검증 → 자동 로그인
  const code = await fetchSignupCode(page, email)
  await page.getByTestId('signup-code').fill(code)
  await page.getByTestId('signup-verify').click()
  await expect(page).toHaveURL(/\/reserve/)
  await expect(page.getByTestId('nav-logout')).toBeVisible()
})

test('이미 가입된 이메일로 회원가입하면 에러가 보이고 /signup에 머문다', async ({ page }) => {
  await page.goto('/signup', { waitUntil: 'networkidle' })
  await page.getByTestId('signup-name').fill('중복회원')
  await page.getByTestId('signup-email').fill('user@test.com')
  await page.getByTestId('signup-password').fill('mypw1234')
  await page.getByTestId('signup-password-confirm').fill('mypw1234')
  await page.getByTestId('signup-submit').click()
  await expect(page.getByTestId('signup-error')).toBeVisible()
  await expect(page).toHaveURL(/\/signup/)
})

test('비밀번호와 비밀번호 확인이 다르면 에러가 보인다', async ({ page }) => {
  await page.goto('/signup', { waitUntil: 'networkidle' })
  await page.getByTestId('signup-name').fill('불일치')
  await page.getByTestId('signup-email').fill('mismatch@test.com')
  await page.getByTestId('signup-password').fill('mypw1234')
  await page.getByTestId('signup-password-confirm').fill('different')
  await page.getByTestId('signup-submit').click()
  await expect(page.getByTestId('signup-error')).toBeVisible()
  await expect(page).toHaveURL(/\/signup/)
})

test('회원가입 분기: 매니저 선택 시 소속 매장 입력 후 인증하면 승인 대기 안내가 표시된다 (v1.12)', async ({
  page,
}) => {
  const email = 'branch-mgr@test.com'
  await page.goto('/signup', { waitUntil: 'networkidle' })
  // 매니저 탭 전환 → 소속 매장 입력란 노출
  await page.getByTestId('signup-type-manager').click()
  await expect(page.getByTestId('signup-store')).toBeVisible()
  await page.getByTestId('signup-name').fill('분기매니저')
  await page.getByTestId('signup-store').selectOption({ label: '강남점' })
  await page.getByTestId('signup-email').fill(email)
  await page.getByTestId('signup-password').fill('password')
  await page.getByTestId('signup-password-confirm').fill('password')
  await page.getByTestId('signup-submit').click()
  // 코드 인증 단계 → 검증
  await expect(page.getByTestId('signup-code')).toBeVisible()
  const code = await fetchSignupCode(page, email)
  await page.getByTestId('signup-code').fill(code)
  await page.getByTestId('signup-verify').click()
  // 자동 로그인 없이 승인 대기 안내(매니저 가입은 PENDING_APPROVAL_L1)
  await expect(page.getByTestId('manager-signup-done')).toBeVisible()
})

test('회원가입 분기: 매니저 선택 시 소속 매장 미입력이면 에러가 표시된다 (v1.12)', async ({ page }) => {
  await page.goto('/signup', { waitUntil: 'networkidle' })
  await page.getByTestId('signup-type-manager').click()
  await page.getByTestId('signup-name').fill('매장누락')
  await page.getByTestId('signup-email').fill('no-store-mgr@test.com')
  await page.getByTestId('signup-password').fill('password')
  await page.getByTestId('signup-password-confirm').fill('password')
  await page.getByTestId('signup-submit').click()
  await expect(page.getByTestId('signup-error')).toBeVisible()
  await expect(page).toHaveURL(/\/signup/)
})

test('로그인 상태로 /signup에 진입하면 /reserve로 리다이렉트된다', async ({ page }) => {
  // 먼저 로그인
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByTestId('login-email').fill('user@test.com')
  await page.getByTestId('login-password').fill('password')
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/reserve/)
  // 게스트 가드 — 로그인 상태로 /signup 진입 시 /reserve로
  await page.goto('/signup')
  await expect(page).toHaveURL(/\/reserve/)
})
