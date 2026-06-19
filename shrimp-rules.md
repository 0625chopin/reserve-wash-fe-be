# 개발 가이드라인 (AI Agent 전용)

> 본 문서는 코딩 AI Agent의 작업 수행 규칙이다. 일반 개발 지식이 아닌 **본 프로젝트 고유 규칙**만 정의한다. 모든 코드 생성·수정 시 아래 규칙을 강제 적용하라.

## 프로젝트 개요

- **Nuxt 4** 기반 프론트엔드 프로젝트다. (내부적으로 Vue 3 `<script setup>` Composition API + Vite + Nitro 서버)
- 런타임 스택: **Nuxt 4.x**, **Vue 3 (`<script setup>` Composition API)**, **Pinia 3.x (`@pinia/nuxt` 모듈)**.
- 라우팅: **Nuxt 파일 기반 라우팅** (`app/pages/`). vue-router를 수동 설정하지 않는다(Nuxt 내장).
- 언어: **TypeScript** (`typescript ~6.0.0`), 빌드 도구: **Nuxt(Vite 내장)**, 타입 검사: **`nuxt typecheck`(내부 vue-tsc)**.
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
| `app/middleware/` | 라우트 미들웨어(`defineNuxtRouteMiddleware`) | 인증 가드 등 |
| `app/assets/` | `base.css`·`main.css` 등 스타일 자원 | `nuxt.config.ts`의 `css`로 등록 |

- **`~`·`@` 별칭은 모두 `app/`(srcDir)를 가리킨다.** (Nuxt가 자동 제공)

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
- `node_modules/`, `.nuxt/`, `.output/` 등 빌드 산출물 직접 수정 **금지**.
