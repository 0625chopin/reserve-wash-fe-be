# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어 및 커뮤니케이션 규칙

- 기본 응답 언어: 한국어
- 코드 주석: 한국어로 작성
- 커밋 메시지: 한국어로 작성
- 문서화: 한국어로 작성
- 변수명/함수명: 영어 (코드 표준 준수)
- 문자열 개행
- 수집, 계획, 작업 생성, 구현 등 단계를 최대한 상세히 작성 (CoT - Chain of Thought 방식)

## 프로젝트 개요

**Nuxt 4** 기반 프론트엔드 프로젝트. 내부적으로 Vue 3 (`<script setup>` Composition API) + Vite + Nitro 서버를 사용하며, 라우팅은 Nuxt 파일 기반 라우팅, 상태 관리는 Pinia(`@pinia/nuxt`), 타입스크립트로 작성됨. 기본 렌더링은 SSR.

## 주요 명령어

```sh
npm install          # 의존성 설치 (postinstall 로 nuxt prepare 자동 실행)
npm run dev          # Nuxt 개발 서버 (HMR), 기본 http://localhost:3000
npm run build        # nuxt build (프로덕션 빌드 → .output)
npm run generate     # nuxt generate (정적 사이트 생성)
npm run preview      # nuxt preview (빌드 결과물 로컬 미리보기)
npm run postinstall  # nuxt prepare (.nuxt 타입 생성)
npm run type-check   # nuxt typecheck (내부적으로 vue-tsc)
npm run lint         # lint:oxlint → lint:eslint 순차 실행 (run-s), 둘 다 --fix
npm run format       # oxfmt로 app/ 포맷팅
npm run test:e2e     # Playwright E2E 테스트 (dev 서버 자동 기동)
npm run test:e2e:ui  # Playwright UI 모드
```

E2E 테스트는 **Playwright**로 수행 (`playwright.config.ts`, 테스트는 `e2e/`, baseURL `http://localhost:3000`, dev 서버 자동 기동). 단위 테스트 러너(vitest)는 미설치.

## 린트/포맷 파이프라인 (중요)

이 프로젝트는 일반적인 ESLint + Prettier 조합이 아니라 **oxc 도구 체인(oxlint + oxfmt)을 1차로, ESLint를 2차로** 사용함:

- **oxlint** (`.oxlintrc.json`): `correctness` 카테고리를 error로. 빠른 1차 린트.
- **eslint** (`eslint.config.ts`, flat config): Vue + TypeScript 규칙. `eslint-plugin-oxlint`로 oxlint와 중복되는 규칙을 끄고, `eslint-config-prettier`로 포맷 관련 규칙을 끔. `.nuxt`/`.output`은 ignore하고, `app/pages`·`app/layouts`·`app.vue`·`error.vue`에 한해 `vue/multi-word-component-names` 규칙을 끔(파일 기반 라우팅의 단어 1개 파일명 허용).
- **oxfmt** (`.oxfmtrc.json`): 세미콜론 없음(`semi: false`), 작은따옴표(`singleQuote: true`). 코드 스타일은 이 설정을 따를 것 — 새 코드 작성 시 세미콜론을 붙이지 말 것.
- VS Code는 저장 시 oxc 포매터(`oxc.oxc-vscode`)와 `source.fixAll`을 자동 적용 (`.vscode/settings.json`).

## 구조

- `nuxt.config.ts` — Nuxt 설정(모듈/CSS/별칭 등). 진입점 부트스트랩은 Nuxt가 담당하므로 별도 `main.ts`는 없음.
- `app/app.vue` — 루트 컴포넌트(`<NuxtPage />` 포함).
- `app/pages/` — **파일 기반 라우트** 페이지(`index.vue`=`/`, `about.vue`=`/about`). 자동 코드 스플리팅. 동적 라우트는 `[param].vue` 형식.
- `app/middleware/` — 라우트 미들웨어(`defineNuxtRouteMiddleware`). 인증 가드 등.
- `app/stores/` — Pinia 스토어(자동 임포트). setup 스토어 문법(`defineStore('id', () => {...})`) 사용.
- `app/components/` — 재사용 컴포넌트(자동 임포트, `icons/` 하위 포함).
- `app/composables/` — 재사용 로직(자동 임포트).
- `~`·`@` 별칭은 모두 `app/`(srcDir)를 가리킴 (Nuxt 자동 제공).

## TypeScript 설정

- 루트 `tsconfig.json`은 Nuxt가 생성하는 `./.nuxt/tsconfig.json`을 확장. `.nuxt/`는 `nuxt prepare`(postinstall) 시 자동 생성됨.
- 타입 검사는 `tsc` 단독이 아닌 `nuxt typecheck`(내부 vue-tsc)로 수행됨 — 타입 검사는 반드시 `npm run type-check`를 사용.

## Shrimp Task Manager 서버 선택 규칙 (개발 차수별)

task 관리를 위해 Shrimp Task Manager MCP 서버를 사용할 때, **개발 차수에 따라 서버를 구분**한다.

- **차수 판정**: 현재 작업이 정본으로 따르는 로드맵이 `docs/roadmaps/ROADMAP_{n}.md`이면 **n차 개발**로 간주한다. (예: `ROADMAP_1.md` → 1차, `ROADMAP_2.md` → 2차)
- **서버 매핑**:
  - **1차** → `shrimp-task-manager` (접미사 없음) → 도구 `mcp__shrimp-task-manager__*`
  - **n차(n≥2)** → `shrimp-task-manager-phase{n}` → 도구 `mcp__shrimp-task-manager-phase{n}__*`
  - 예: 2차(ROADMAP_2.md) → `shrimp-task-manager-phase2`(`mcp__shrimp-task-manager-phase2__*`), 3차 → `shrimp-task-manager-phase3`
- **저장소 분리**: 각 서버는 독립된 `DATA_DIR`을 가진다 — 1차 `shrimp_data/`, n차 `shrimp_data_phase{n}/`. 차수가 섞이지 않도록 **반드시 해당 차수의 서버 도구만 호출**한다(1차 작업에 `-phase2` 도구를, 2차 작업에 접미사 없는 도구를 쓰지 마라).
- **신규 차수 시작 시**: `.mcp.json`에 `shrimp-task-manager-phase{n}` 서버를 추가하고 `DATA_DIR`을 `shrimp_data_phase{n}`으로 지정해야 도구가 노출된다. 미등록 시 해당 차수 도구를 호출할 수 없다(현재 `.mcp.json`에는 1차·2차만 등록됨).
