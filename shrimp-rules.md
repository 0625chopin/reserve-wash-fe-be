# 개발 가이드라인 (AI Agent 전용)

> 본 문서는 코딩 AI Agent의 작업 수행 규칙이다. 일반 개발 지식이 아닌 **본 프로젝트 고유 규칙**만 정의한다. 모든 코드 생성·수정 시 아래 규칙을 강제 적용하라.

## 프로젝트 개요

- **Nuxt 4** 기반 프론트엔드 프로젝트다. (내부적으로 Vue 3 `<script setup>` Composition API + Vite + Nitro 서버)
- 런타임 스택: **Nuxt 4.x**, **Vue 3 (`<script setup>` Composition API)**, **Pinia 3.x (`@pinia/nuxt` 모듈)**.
- 라우팅: **Nuxt 파일 기반 라우팅** (`app/pages/`). vue-router를 수동 설정하지 않는다(Nuxt 내장).
- 언어: **TypeScript** (`typescript ~6.0.0`), 빌드 도구: **Nuxt(Vite 내장)**, 타입 검사: **`nuxt typecheck`(내부 vue-tsc)**.
- 스타일링: **Tailwind CSS v4** (`@tailwindcss/vite` 플러그인을 `nuxt.config.ts`의 `vite.plugins`에 등록, `app/assets/main.css`에서 `@import 'tailwindcss'`). `tailwind.config.js`는 없다(v4는 CSS-first 설정).
- 렌더링: **기본 SSR**. 클라이언트 전용 로직은 `import.meta.client` 가드 또는 `useCookie` 등을 사용하라.
- E2E 테스트: **Playwright 설치됨**(`playwright.config.ts`, 테스트는 `e2e/`, baseURL `http://localhost:3000`). 단위 테스트 러너(vitest)는 미설치.

## 프로젝트 아키텍처

| 경로 | 역할 | 수정 규칙 |
|------|------|-----------|
| `nuxt.config.ts` | Nuxt 설정(모듈/CSS/별칭 등). 진입점 부트스트랩은 Nuxt가 담당 | 전역 모듈·플러그인 등록은 여기 또는 `app/plugins/` |
| `app/app.vue` | 루트 컴포넌트(`<NuxtPage />` 포함) | 전역 레이아웃 골격 |
| `app/pages/` | **파일 기반 라우트** 페이지 (`index.vue`=`/`, `about.vue`=`/about`) | 신규 페이지 추가 = 라우트 추가(별도 등록 불필요) |
| `app/stores/` | Pinia 스토어(자동 임포트) | 신규 스토어 파일을 이 디렉터리에 생성 |
| `app/components/` | 재사용 컴포넌트(자동 임포트, `icons/` 하위 포함) | 재사용 단위만 배치 |
| `app/composables/` | 재사용 로직(자동 임포트) | Composition 함수 배치 |
| `app/middleware/` | 라우트 미들웨어(`defineNuxtRouteMiddleware`). 4개 구현: `auth.ts`(인증)·`reservation-fresh-entry.ts`(`/reserve` 진입 시 draft 초기화)·`reservation-wizard-guard.ts`(위저드 단계 순서)·`review-guard.ts`(후기 자격) | `definePageMeta({ middleware: '...' })`로 적용. **미들웨어 키는 파일명 kebab-case**. 신규 가드는 이 디렉터리에 추가 |
| `app/layouts/` | 공통 레이아웃(자동 임포트). `default.vue` 활성(`app.vue`가 `<NuxtLayout>` opt-in) | `default.vue` 수정 또는 신규 레이아웃 추가 |
| `app/assets/` | `base.css`·`main.css`(Tailwind 진입 + `@theme` 토큰 + `@layer components` 공통 클래스) | `nuxt.config.ts`의 `css`로 등록. **색상/공통 컴포넌트 스타일은 여기서 토큰화** |
| `app/types/` | 도메인 TS 타입/인터페이스/유니언 | `enums.ts`, `domain.ts`. **명시 import 대상**(`import type`) |
| `app/data/` | 더미 데이터(매장/베이/가격/매니저/차종/서비스/사용자) | `prices.ts`·`stores.ts`·`managers.ts`·`carTypes.ts`·`serviceTypes.ts`·`users.ts`. **명시 import 대상** |
| `app/services/` *(계약은 `README.md`)* | 데이터 접근 추상화 계층 | `priceService.ts`·`storeService.ts`·`reservationService.ts`. **명시 import 대상**. 계약: `app/services/README.md` |

- **`~`·`@` 별칭은 모두 `app/`(srcDir)를 가리킨다.** (Nuxt가 자동 제공) 상대경로(`../`)보다 별칭을 우선하라.
- **데이터 접근은 `app/services/`로 감싸라.** 컴포넌트/스토어에서 `app/data/`를 직접 import하지 마라(2단계 백엔드 교체 지점). 단방향 의존 계약은 `app/services/README.md` 참조.
- **1차(ROADMAP_1) Phase 0~8 구현 완료**: `app/types`(`enums`·`domain`)·`app/data`(`prices`·`stores`·`managers`·`carTypes`·`serviceTypes`·`users`)·`app/composables`(`useSlots`·`useToast`)·`app/services`(`priceService`·`storeService`·`reservationService`)가 채워졌고, 인증(`app/stores/auth.ts`·`app/middleware/auth.ts`·`app/pages/login.vue`)·레이아웃 opt-in(`app.vue` → `<NuxtLayout>` → `app/layouts/default.vue`)·공통 네비(`app/components/AppNav.vue`)·**예약 위저드 3분할**(`app/pages/reserve/{index,slot,done}.vue` + `app/stores/reservationDraft.ts` + 위저드/진입 가드)·예약 목록·상태전이(`app/pages/reservations.vue` + `app/stores/reservation.ts`)·후기/평점(`app/pages/review/[reservationId].vue` + `app/stores/review.ts`)·입력 컴포넌트(`WheelPicker.vue`·`SearchableSelect.vue`·`SlotGrid.vue`)·Playwright E2E(`e2e/`)가 모두 구현됐다. 1차 DoD는 `docs/roadmaps/ROADMAP_1.md`, **2차(Spring Boot 백엔드 + BO)는 `docs/roadmaps/ROADMAP_2.md`** 를 정본으로 따르라.

### 자동 임포트 vs 명시 import (강제 구분)

- **자동 임포트되어 import 구문을 작성하지 마라**: `app/components/`(컴포넌트), `app/stores/`(`useXxxStore`), `app/composables/`, `app/middleware/`, `app/layouts/`, Nuxt 매크로(`definePageMeta`, `defineNuxtRouteMiddleware`, `navigateTo`, `useRoute`, `useCookie`).
- **반드시 명시 import 하라**: `app/types/`(타입), `app/data/`, `app/services/`, 그리고 `vue`/`pinia`의 `ref`·`computed`·`defineStore`.

## 코드 스타일 규칙 (최우선 강제)

- **세미콜론을 절대 붙이지 마라.** `.oxfmtrc.json`의 `"semi": false` 강제.
- **문자열은 작은따옴표(`'`)를 사용하라.** `"singleQuote": true` 강제.
- 들여쓰기·따옴표·세미콜론은 **oxfmt가 최종 결정자**다. 직접 포맷을 추측하지 말고 위 규칙을 따르라.

```ts
// 올바름 (DO)
const name = 'home'
import { useCounterStore } from '~/stores/counter'

// 금지 (DON'T)
const name = "home";
import { useCounterStore } from "../stores/counter";
```

## 기능 구현 규칙

### Pinia 스토어

- **setup 스토어 문법만 사용하라.** `defineStore('id', () => { ... })` 형태로 작성하고, `state`/`getters`/`actions` 옵션 객체 문법을 쓰지 마라.

```ts
// 올바름 (DO) — app/stores/counter.ts 패턴
export const useCounterStore = defineStore('counter', () => {
  const count = ref(0)
  const doubleCount = computed(() => count.value * 2)
  function increment() {
    count.value++
  }
  return { count, doubleCount, increment }
})

// 금지 (DON'T) — 옵션 객체 문법
export const useCounterStore = defineStore('counter', {
  state: () => ({ count: 0 }),
})
```

### Vue 컴포넌트

- 모든 `.vue` 컴포넌트는 **`<script setup lang="ts">`** 로 작성하라. Options API를 쓰지 마라.

### 라우팅 (파일 기반)

- 신규 라우트는 **`app/pages/`에 `.vue` 파일을 생성**하면 자동 등록된다. 라우트 배열 수동 등록·동적 import 작성이 **불필요**하다(Nuxt가 자동 코드 스플리팅).
- 동적 라우트는 대괄호 파일명: `/review/:reservationId` → `app/pages/review/[reservationId].vue`.
- 페이지 내비게이션은 `<NuxtLink>`, 라우트 아웃렛은 `<NuxtPage>`(또는 레이아웃의 `<slot>`)를 사용하라. `<RouterLink>`/`<RouterView>`를 직접 쓰지 마라.
- 라우트 가드는 **`app/middleware/`의 미들웨어**(`defineNuxtRouteMiddleware`)로 구현하고, 페이지에서 `definePageMeta({ middleware: 'auth' })`로 적용하라.

```ts
// app/middleware/auth.ts — 라우트 미들웨어 예시
export default defineNuxtRouteMiddleware((to) => {
  // 인증 가드 로직
})
```

## 스타일링(Tailwind CSS v4) 규칙

- 스타일링은 **Tailwind CSS v4 유틸리티 클래스**를 우선 사용하라. Tailwind 설정은 **CSS-first**다 — `tailwind.config.js`를 생성하지 마라. 토큰은 `app/assets/main.css`의 **`@theme` 블록**에 CSS 변수로 정의한다.
- **색상은 하드코딩하지 마라.** `main.css`의 `@theme`에 정의된 브랜드 토큰을 사용하라: 표면 `--color-surface-base/1/2`, 브랜드 `--color-brand-primary(-strong)`·`--color-brand-accent`, 텍스트 `--color-content(-strong/-muted)`, 보더 `--color-line(-soft)`. 새 색이 필요하면 임의 hex 대신 `@theme`에 토큰을 추가하라.
- **공통 UI는 기존 `@layer components` 클래스를 재사용하라**(중복 정의 금지): `.btn`·`.btn-primary`·`.btn-ghost`, `.input-field`, `.card`, `.field-label`, `.badge-accent`, `.container-app`. 버튼/입력/카드를 만들 때 새 스타일을 인라인으로 짜지 말고 이 클래스를 먼저 적용하라.
- 컴포넌트 국소 스타일이 꼭 필요하면 `.vue`의 `<style scoped>`를 사용하되, 색/간격은 `@theme` 변수(`var(--color-...)`)를 참조하라.

```vue
<!-- 올바름 (DO) — 토큰화된 공통 클래스 재사용 -->
<button class="btn btn-primary">예약</button>
<input class="input-field" />

<!-- 금지 (DON'T) — 색 하드코딩 + 공통 클래스 무시 -->
<button style="background:#38bdf8">예약</button>
```

## 린트/포맷 워크플로우 규칙 (중요)

- 본 프로젝트는 일반 ESLint+Prettier 조합이 **아니다**. **oxc 도구 체인(oxlint+oxfmt)이 1차, ESLint가 2차**다.
- 린트는 반드시 **순차 실행**된다: `npm run lint` → `lint:oxlint`(--fix) → `lint:eslint`(--fix --cache).
- 포맷은 **`npm run format` (oxfmt app/)** 로 수행하라. Prettier CLI를 호출하지 마라.
- `eslint.config.ts`는 flat config다. `eslint-plugin-oxlint`로 oxlint 중복 규칙을 끄고, `eslint-config-prettier`로 포맷 규칙을 끈다. `.nuxt`/`.output`은 ignore 대상이며, `app/pages`·`app/layouts`·`app.vue`·`error.vue`에 한해 `vue/multi-word-component-names` 규칙을 끈다(파일 기반 라우팅의 단어 1개 파일명 허용). **이 비활성화 구조를 깨뜨리지 마라.**

## TypeScript 설정 규칙

- 루트 `tsconfig.json`은 Nuxt가 생성하는 **`./.nuxt/tsconfig.json`을 확장**한다. `.nuxt/`는 `nuxt prepare`(postinstall) 시 자동 생성되므로 직접 수정하지 마라.
- 타입 검사는 **반드시 `npm run type-check` (nuxt typecheck)** 를 사용하라. `tsc` 단독 실행으로 `.vue` 파일을 검사하지 마라.

## 핵심 파일 동시 수정 규칙

- **신규 페이지 추가 시**: `app/pages/`에 `.vue` 파일을 생성하면 라우트가 자동 등록된다(수동 등록 불필요). 보호가 필요하면 `definePageMeta({ middleware: 'auth' })`를 함께 지정하라.
- **전역 모듈/플러그인 추가 시**: 의존성 설치 → `nuxt.config.ts`의 `modules`에 등록(또는 `app/plugins/`에 플러그인 생성)을 함께 수행하라.
- **의존성 설치 후**: `npm run postinstall`(`nuxt prepare`)로 `.nuxt` 타입을 재생성하라.
- **npm 스크립트 추가 시**: `package.json`과 `CLAUDE.md`의 "주요 명령어" 표를 함께 갱신하라.

## 도메인/문서 정합성 규칙 (정본 우선순위)

- **도메인 모델·가격·enum의 정본은 `docs/require_v1.md`다.** `app/types/enums.ts`·`app/types/domain.ts`·`app/data/prices.ts`를 작성·수정할 때 값은 require_v1.md의 5장(도메인)·10장(가격 매트릭스)·11장(프로세스 코드 `FW/M/S`)과 **정확히 일치**시켜라. 불일치 시 require_v1.md를 따르라.
- **FE 스택·라우팅·디렉터리·명령어의 정본은 `docs/roadmaps/ROADMAP_1.md`(v1.2)다.** require_v1.md 12장의 일부 스택 표기는 구버전(Vue3+Vite) 기준이므로, 충돌 시 `roadmaps/ROADMAP_1.md`를 따르라.
- **2차(백엔드 진화 + BO) 작업의 정본은 `docs/roadmaps/ROADMAP_2.md`(v2.0)다.** Spring Boot 백엔드·동시성 2·3단계(슬롯 UNIQUE·낙관/비관 락)·BO 프로세스(M3~M7·S3~S8)·휴일/휴무 결재·SMTP/알림·MySQL 이행은 이 문서를 정본으로 따르라. 1차 FO 자산은 그대로 유지하고 `app/services/*` 내부만 `$fetch` API로 교체하는 additive 원칙을 지켜라.
- **예약 화면 동작(순차 선택·차종별 베이 노출·휠 날짜/시간 선택 등)의 정본은 `docs/예약_규칙_명세_v1.md`다.** 예약 위저드(`app/pages/reserve/{index,slot,done}.vue`)·예약 관련 컴포넌트를 수정할 때 이 명세와 일치시켜라.
- 화면/스토어/서비스를 구현하면 해당 `docs/roadmaps/ROADMAP_1.md` Phase의 체크리스트·DoD를 함께 갱신하라.

## 테스트(Playwright) 규칙

- E2E 테스트는 **`e2e/`에 `*.spec.ts`** 로 작성하라. `baseURL`은 `http://localhost:3000`(Vite의 5173 아님)이다.
- 셀렉터는 **`data-testid` + `getByTestId`** 를 우선 사용하라. 텍스트/클래스 기반 셀렉터는 지양하라. 테스트 대상 컴포넌트에 `data-testid`를 부여하라.
- 더미 데이터는 고정값이므로 결정적으로 단정(assert)하라. `Math.random()`·현재 시각 의존을 피하고 고정 날짜를 사용하라.

## 언어/커뮤니케이션 규칙

- 코드 **주석은 한국어**로 작성하라.
- **커밋 메시지는 한국어**로 작성하라.
- 변수명·함수명은 **영어**(코드 표준)로 작성하라.

## AI 의사결정 기준

- **컴포넌트를 어디에 둘지 모호할 때**: 라우트에 직접 매핑되는 페이지면 `app/pages/`, 여러 곳에서 재사용되면 `app/components/`에 배치하라.
- **새 상태가 필요할 때**: 단일 컴포넌트 내부면 `ref`/`reactive`, 여러 컴포넌트 공유면 `app/stores/`에 Pinia setup 스토어를 추가하라.
- **import 경로 선택 시**: `app/` 내부 모듈은 **`~`·`@` 별칭**을 우선 사용하라 (상대경로 `../../` 남용 금지).

## 금지 사항 (DON'T)

- 세미콜론(`;`) 추가 **금지**.
- 큰따옴표(`"`) 문자열 사용 **금지** (JSON 제외).
- Pinia 옵션 객체 문법 **금지** (setup 문법만).
- Vue Options API **금지** (`<script setup>` 만).
- Prettier CLI 직접 호출 **금지** (oxfmt 사용).
- `tsc` 단독 실행으로 타입 검사 **금지** (`nuxt typecheck` 사용).
- `<RouterView>`/`<RouterLink>` 직접 사용 및 vue-router 수동 설정 **금지** (`<NuxtPage>`/`<NuxtLink>`·파일 기반 라우팅 사용).
- 단위 테스트 러너(vitest 등)는 미설치이므로 임의로 설정 파일을 생성하지 **말 것** (사용자 요청 시에만). E2E는 Playwright(`e2e/`)를 사용하라.
- 자동 임포트 대상(components/stores/composables/middleware/layouts)에 명시 import 구문 작성 **금지**.
- SSR 단계에서 `localStorage`/`window`/`document` 직접 접근 **금지** (`import.meta.client` 가드 또는 `onMounted` 사용).
- 컴포넌트/스토어에서 `app/data/` 더미 데이터 직접 import **금지** (`app/services/` 경유).
- 가격/차종/서비스 enum을 `docs/require_v1.md`와 다른 값으로 작성 **금지**.
- 색상 hex 하드코딩 및 `@layer components` 공통 클래스와 중복되는 인라인 스타일 작성 **금지** (`main.css`의 `@theme` 토큰·공통 클래스 사용).
- `tailwind.config.js` 생성 **금지** (Tailwind v4 CSS-first, `@theme`로 설정).
- `node_modules/`, `.nuxt/`, `.output/` 등 빌드 산출물 직접 수정 **금지**.
