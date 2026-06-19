---
name: project-frontend-stack
description: start-kit2 프론트 스택/명령어/컨벤션 — Nuxt 4(Vue3 setup+Vite 내장), oxfmt 세미콜론 없음, Playwright 미설치
metadata:
  type: project
---

start-kit2 프론트엔드 스택 사실(2026-06 기준):
- Nuxt 4(`^4.4.8`, 내부적으로 Vue 3 `<script setup>` + Vite + Nitro), Pinia(`@pinia/nuxt ^0.11.3`, pinia ^3, setup 스토어 문법), Nuxt 파일 기반 라우팅(vue-router 수동 설정 없음), vue ^3.5. 백엔드 없음.
- 디렉터리: 루트 `nuxt.config.ts`; 앱 코드는 `app/`(app.vue, pages/, components/, stores/, composables/, layouts/, middleware/, plugins/, assets/). 로드맵 신규 폴더는 `app/types/`, `app/data/`, `app/services/`.
- 참조 패턴: 동적 라우트 → `app/pages/.../[param].vue`; 라우트 가드 → `app/middleware/`의 `defineNuxtRouteMiddleware`; Pinia setup 스토어 → `app/stores/`(자동 임포트).
- 경로 별칭 `~`/`@` → `app/`(srcDir).
- SSR: Nuxt 기본 SSR 활성. 클라이언트 전용 코드는 `import.meta.client`/`useCookie` 고려.
- 코드 스타일: oxfmt(**세미콜론 없음**, **작은따옴표**) + oxlint(1차) + ESLint(2차). 신규 코드 세미콜론 미사용 필수.
- 명령어: `npm run dev`(`nuxt dev`, **포트 3000**), `npm run build`(`nuxt build`), `npm run generate`(`nuxt generate`), `npm run preview`(`nuxt preview`), `npm run type-check`(`nuxt typecheck`, 내부 vue-tsc), `npm run lint`(oxlint→eslint), `npm run format`(`oxfmt app/`). `postinstall`=`nuxt prepare`.
- Node engines: `^22.18.0 || >=24.12.0`.
- **테스트 러너(vitest/cypress/playwright) 미설치** — Playwright E2E 도입 시 `npm init playwright@latest` + `playwright.config.ts`(baseURL http://localhost:3000) + `test:e2e` 스크립트 추가 필요.

**How to apply**: 예시 코드/스니펫 작성 시 oxfmt 스타일 준수, `~`/`@` 별칭 사용, 라우트는 Nuxt 파일 기반(동적 라우트 `[param].vue`, 가드는 `app/middleware/`), 스토어는 setup 문법. 관련 로드맵은 [[project-carwash-mvp]].
