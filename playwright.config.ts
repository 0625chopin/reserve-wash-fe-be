import { defineConfig, devices } from '@playwright/test'

// 2차: BE(Spring Boot :8080) 기동 명령 — Windows는 gradlew.bat, 그 외는 ./gradlew
// (JAVA_HOME 또는 PATH의 java 21 필요. H2 in-memory라 별도 DB 불필요)
const backendCommand =
  process.platform === 'win32' ? 'cd backend && .\\gradlew.bat bootRun' : 'cd backend && ./gradlew bootRun'

// Playwright E2E 설정 — FE(Nuxt :3000) + BE(Spring Boot :8080) 동시 기동
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // 테스트 시작 시 FE(Nuxt :3000)와 BE(Spring Boot :8080)를 동시 기동
  //   2차: 서비스가 서버 데이터로 동작하므로 BE도 떠 있어야 회귀가 서버 데이터로 검증된다
  webServer: [
    {
      command: 'npm run dev',
      url: 'http://localhost:3000',
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
    },
    {
      command: backendCommand,
      url: 'http://localhost:8080/api/health',
      reuseExistingServer: !process.env.CI,
      timeout: 180 * 1000,
    },
  ],
})
