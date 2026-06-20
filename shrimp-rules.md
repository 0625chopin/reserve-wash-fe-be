# 개발 가이드라인 (AI Agent 전용)

> 본 문서는 코딩 AI Agent의 작업 수행 규칙이다. 일반 개발 지식이 아닌 **본 프로젝트 고유 규칙**만 정의한다. 모든 코드 생성·수정 시 아래 규칙을 강제 적용하라.

## 프로젝트 개요

- **모노레포**다. 1차 **Nuxt 4 FE**(저장소 루트 `app/`)와 2차 **Spring Boot BE**(`backend/`)를 **하나의 git 저장소**에서 관리하며, 런타임은 포트로 분리한다: **FE :3000 / BE :8080**.
- **FE 스택**(1차, 구현 완료): **Nuxt 4.x**, **Vue 3 (`<script setup>` Composition API)**, **Pinia 3.x (`@pinia/nuxt` 모듈)**.
  - 라우팅: **Nuxt 파일 기반 라우팅** (`app/pages/`). vue-router를 수동 설정하지 않는다(Nuxt 내장).
  - 언어: **TypeScript** (`typescript ~6.0.0`), 빌드 도구: **Nuxt(Vite 내장)**, 타입 검사: **`nuxt typecheck`(내부 vue-tsc)**.
  - 스타일링: **Tailwind CSS v4** (`@tailwindcss/vite` 플러그인을 `nuxt.config.ts`의 `vite.plugins`에 등록, `app/assets/main.css`에서 `@import 'tailwindcss'`). `tailwind.config.js`는 없다(v4는 CSS-first 설정).
  - 렌더링: **기본 SSR**. 클라이언트 전용 로직은 `import.meta.client` 가드 또는 `useCookie` 등을 사용하라.
  - E2E 테스트: **Playwright 설치됨**(`playwright.config.ts`, 테스트는 `e2e/`, baseURL `http://localhost:3000`). 단위 테스트 러너(vitest)는 미설치.
- **BE 스택**(2차, `docs/roadmaps/ROADMAP_2.md` 정본 — **`backend/`는 아직 미생성**, Phase 0에서 신설): **Java 21 (LTS)** + **Spring Boot 3.x** + **Gradle Wrapper**. **DB 접근은 MyBatis**(`mybatis-spring-boot-starter`, 매퍼 인터페이스 + XML SQL) — **JPA/Hibernate 미사용**(require_v1.md v1.5 확정). 데이터는 2단계 **H2 in-memory**(`schema.sql`로 스키마 생성, 휘발성) → 3단계 **MySQL 8.x + Flyway**(영속화). 통합테스트는 `@SpringBootTest`(`backend/src/test/...`).
- **additive(증분) 철학(2차 최우선)**: 1차 FE 자산(화면·스토어·E2E)을 **삭제·구조 변경하지 마라**. 백엔드 연동은 **`app/services/*` 내부 구현만** 더미 import → `$fetch`/`useFetch` API 호출로 교체한다. 컴포넌트 마크업·스토어 구조 변경은 **0**을 목표로 하라.

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
| `app/services/` *(계약은 `README.md`)* | 데이터 접근 추상화 계층 | `priceService.ts`·`storeService.ts`·`reservationService.ts`. **명시 import 대상**. 계약: `app/services/README.md`. **2차 교체 지점**: 내부만 `$fetch`로 교체(시그니처 유지) |
| `backend/` *(2차·미생성)* | Spring Boot BE — **독립 Gradle 프로젝트** | 루트 `package.json`에 Java 빌드를 끼워넣지 마라. FE는 루트 `npm`, BE는 `cd backend && ./gradlew`로 빌드 경계를 분리하라. `backend/build/`·`backend/.gradle/`·`*.class`·IDE 산출물(`.idea/`·`*.iml`)을 루트 `.gitignore`에 추가하라 |

### Spring Boot + MyBatis 패키지 구조 (`backend/src/main/java/com/carwash/`, 2차)

| 패키지 | 역할 | 규칙 |
|------|------|-----------|
| `controller/` | `@RestController` — REST 진입점 | **DTO만 입출력**하라. 도메인 객체를 직접 반환·수신하지 마라 |
| `service/` | `@Service` 비즈니스 로직 | 쓰기 메서드에 `@Transactional`, 조회에 `@Transactional(readOnly = true)`. 도메인↔DTO 변환 수행 |
| `mapper/` | **MyBatis `@Mapper` 인터페이스** | SQL은 **`resources/mapper/*.xml`** 에 작성(또는 애너테이션 SQL). 경합 슬롯은 `SELECT ... FOR UPDATE` SQL을 매퍼에 직접 작성. **`JpaRepository`를 쓰지 마라** |
| `domain/` | **순수 POJO 도메인 모델** | **JPA 애너테이션(`@Entity`·`@Id`·`@Table`·`@Version`) 금지.** setter 미개방, 상태 전이는 도메인 메서드(`reserve()`·`cancel()`·`complete()`·`approveL1()`)로 표현. MyBatis 결과 매핑용 기본 생성자(또는 `<constructor>` 매핑) 허용 |
| `dto/` | 요청/응답 DTO | **Java `record` 권장**. FE `app/types/domain.ts` 필드명과 **정확히 일치**(무변환 매핑) |
| `config/` | CORS·Security·Jackson·Async·MyBatis 설정 | FE(:3000) 교차 출처 허용은 `CorsConfig`(또는 Nuxt `devProxy`). MyBatis `map-underscore-to-camel-case: true` |
| `security/` | JWT 필터·`UserDetails` (Phase 3) | 인가는 `SecurityConfig`/`@PreAuthorize` |
| `exception/` | 도메인 예외 + `@RestControllerAdvice` | 409/404/400을 `ErrorResponse`로 일관 응답 |
| `resources/mapper/` | **MyBatis 매퍼 XML** | `*Mapper.xml` — `namespace`를 매퍼 인터페이스 FQN과 일치. snake_case 컬럼 ↔ camelCase 프로퍼티 |
| `resources/db/` | 스키마/시드 | 2단계 `schema.sql`·`data.sql`(H2), 3단계 `db/migration/V{n}__*.sql`(Flyway·MySQL). **슬롯 `UNIQUE` 제약을 DDL에 직접 작성** |

- **`~`·`@` 별칭은 모두 `app/`(srcDir)를 가리킨다.** (Nuxt가 자동 제공) 상대경로(`../`)보다 별칭을 우선하라.
- **데이터 접근은 `app/services/`로 감싸라.** 컴포넌트/스토어에서 `app/data/`를 직접 import하지 마라(2단계 백엔드 교체 지점). 단방향 의존 계약은 `app/services/README.md` 참조.
- **1차(ROADMAP_1) Phase 0~8 구현 완료**: `app/types`(`enums`·`domain`)·`app/data`(`prices`·`stores`·`managers`·`carTypes`·`serviceTypes`·`users`)·`app/composables`(`useSlots`·`useToast`)·`app/services`(`priceService`·`storeService`·`reservationService`)가 채워졌고, 인증(`app/stores/auth.ts`·`app/middleware/auth.ts`·`app/pages/login.vue`)·레이아웃 opt-in(`app.vue` → `<NuxtLayout>` → `app/layouts/default.vue`)·공통 네비(`app/components/AppNav.vue`)·**예약 위저드 3분할**(`app/pages/reserve/{index,slot,done}.vue` + `app/stores/reservationDraft.ts` + 위저드/진입 가드)·예약 목록·상태전이(`app/pages/reservations.vue` + `app/stores/reservation.ts`)·후기/평점(`app/pages/review/[reservationId].vue` + `app/stores/review.ts`)·입력 컴포넌트(`WheelPicker.vue`·`SearchableSelect.vue`·`SlotGrid.vue`)·Playwright E2E(`e2e/`)가 모두 구현됐다. 1차 DoD는 `docs/roadmaps/ROADMAP_1.md`, **2차(Spring Boot 백엔드 + BO)는 `docs/roadmaps/ROADMAP_2.md`** 를 정본으로 따르라.
- **미구현(다음 1차 대상) — 일반 회원가입(FW1)**: `/signup`은 ROADMAP_1 **Phase 3.1(v1.5)** 에 정의됐으나 **아직 미구현**이다. 구현 시 즉시 가입(더미) 방식으로 `app/pages/signup.vue`(신규)·`app/middleware/guest.ts`(신규)·`app/stores/auth.ts`의 `signup` 액션(이메일 중복 검사 → in-memory `users` 추가 → 자동 로그인)을 만들고, `app/components/AppNav.vue`·`app/pages/login.vue`에 상호 링크를 추가하라. 이메일 인증/SMTP·매니저/관리자 가입은 2차(`ROADMAP_2`)다.

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

### Java 코드 스타일 (2차 `backend/`)

- 클래스 `PascalCase`(`ReservationService`), 메서드/필드 `camelCase`(`confirmReservation`), 상수 `UPPER_SNAKE_CASE`, 패키지 소문자(`com.carwash.service`).
- Lombok: `domain/` 객체는 `@Getter`·`@Builder` + MyBatis 매핑용 `@NoArgsConstructor`. `@Setter`·`@Data`를 도메인 객체에 쓰지 마라.
- 주석·커밋 메시지는 **한국어**, 클래스/메서드/필드명은 영어(위 FE 규칙과 동일).

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

## 백엔드(Spring Boot) 구현 규칙 (2차 — `docs/roadmaps/ROADMAP_2.md` 정본)

### DB 접근 (MyBatis — require_v1.md v1.5 정본)

- **DB 접근은 MyBatis 매퍼로만 하라.** `spring-boot-starter-data-jpa`·`@Entity`·`JpaRepository`·`@Version`·`ddl-auto`를 도입하지 마라.
- 매퍼는 `mapper/*Mapper.java`(`@Mapper` 인터페이스) + `resources/mapper/*Mapper.xml`(SQL)로 작성하라. XML `namespace`는 인터페이스 FQN과 일치시켜라.
- 컬럼은 snake_case, 도메인/DTO 프로퍼티는 camelCase로 두고 `map-underscore-to-camel-case: true`로 매핑하라(별칭 남발 금지).
- 스키마는 ORM 자동 생성에 의존하지 마라. 2단계는 `schema.sql`/`data.sql`(H2), 3단계는 Flyway 마이그레이션으로 **직접 관리**하라.

### DTO 계약 (FE↔BE 경계)

- **컨트롤러에서 도메인 객체를 직접 반환·수신하지 마라.** 요청은 `XxxRequest`, 응답은 `XxxResponse`(Java `record`)로 받고 내보내라.
- 도메인↔DTO 변환은 service 또는 정적 팩토리(`XxxResponse.from(domain)`)에서 수행하라.
- DTO 필드명을 **FE `app/types/domain.ts`(1차 정의)와 일치**시켜라(2단계 교체 비용 최소화 — 무변환 매핑).

### 도메인 객체 (불변성 지향)

- `domain/`은 **순수 POJO**다. JPA 애너테이션(`@Entity`·`@Id`·`@Table`·`@Version`)을 붙이지 마라.
- setter를 열지 마라. 상태 전이는 의미 있는 도메인 메서드(`slot.hold()`·`reservation.complete()`·`dayoff.approveL1()`)로 표현하라.
- `@Getter`로 읽기만 노출하고 생성은 `@Builder`를 쓰되, **MyBatis 결과 매핑을 위한 기본 생성자(또는 매퍼 `<constructor>` 매핑)** 를 보장하라.
- **불가능한 상태 전이는 도메인 메서드에서 예외**(`IllegalStateException`)를 던지고, `@RestControllerAdvice`가 400/409로 변환하게 하라.

### 동시성 (예약 슬롯 — require 7.3 정본)

- 슬롯 `UNIQUE(store_id, bay_id, date, time_slot)` 제약을 **DDL(`schema.sql`/Flyway)에 직접 작성**하여 **최종 방어선**으로 항상 깔아라.
- 기본은 **낙관적 락**: version 컬럼을 두고 `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?` 의 **영향 행 수가 0이면 충돌**로 판정하라.
- 경합 잦은 인기 슬롯만 **비관적 락**: 매퍼 XML에 `SELECT ... FOR UPDATE` SQL을 직접 작성하라.
- 충돌(`SlotConflictException`·`DuplicateKeyException`/`DataIntegrityViolationException`)은 **500이 아닌 409 Conflict**로 매핑하라. FE는 1차 `useToast` 재선택 토스트를 재사용한다(새 UX를 만들지 마라).

### 인증/인가 (Phase 3)

- 비밀번호는 `BCryptPasswordEncoder`로 해시하라.
- 역할 인가(`@PreAuthorize`/`SecurityConfig`)는 **require 3.2 권한 매트릭스와 정확히 일치**시켜라(예: 예약 대행 M3=`MANAGER`/`STORE_ADMIN`, 매장별 관리 S4·S5=`ADMIN`).
- 인증(이메일 본인확인)과 승인(역할/매장가입 검토)을 분리하라(require 4.2): 일반 사용자 `EMAIL_VERIFIED → ACTIVE`, 매니저/매장가입 `EMAIL_VERIFIED → PENDING_APPROVAL`.
- FE는 JWT를 **`useCookie`로 보관**하라(SSR 미들웨어에서 읽기 위함). `localStorage`에 토큰을 저장하지 마라.

### 결재 워크플로우 (Phase 7)

- 워크플로우 엔진을 도입하지 마라. **상태 enum + 도메인 전이 메서드 + 역할 인가** 조합으로 구현하라(require 8.2).
- 매니저 휴무: `SUBMITTED → APPROVED_L1`(STORE_ADMIN) `→ APPROVED_L2/CONFIRMED`(ADMIN). 매장 휴일: 단일 승인(매니저 신청 → ADMIN).
- 단계 건너뛰기는 전이 메서드에서 차단하라. CONFIRMED 시 슬롯 비활성: `FULL_DAY`=그날 전체, `SHIFT_n`=해당 교대 시간대만.

### 알림 (Phase 9)

- 이메일 발송은 `EmailSender` 인터페이스로 추상화하라. 2단계는 콘솔 로그 스텁(`LoggingEmailSender`), 실제 SMTP는 `JavaMailSender` 구현(Phase 9).
- 발송은 **`@Async` 비동기**로 분리하여, 발송 실패가 본 트랜잭션(가입/예약/결재)을 롤백시키지 않게 하라.

### MySQL 이행 (Phase 10)

- MyBatis는 ORM 자동 DDL이 없으므로 스키마는 **항상 직접 관리**한다. 3단계는 **Flyway가 스키마 단일 소유자(SSOT)** 다 — `db/migration/V{n}__{설명}.sql`로 마이그레이션하라(2단계 `schema.sql`을 Flyway로 이관).
- 슬롯 `UNIQUE(store_id, bay_id, date, time_slot)`를 DB 유니크 인덱스로 확정하라.
- MyBatis 매퍼 SQL이 H2(2단계)·MySQL(3단계) 양쪽 방언에서 동작하는지 확인하라(특히 `FOR UPDATE`·예약어·날짜 함수).

### 백엔드 빌드·기동 명령

- 빌드/테스트: `cd backend && ./gradlew build`(컴파일+테스트) → `./gradlew test`(**동시성 통합테스트 포함**).
- 기동: `cd backend && ./gradlew bootRun`(:8080). FE는 루트 `npm run dev`(:3000)와 **동시 기동**.

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

- **신규 페이지 추가 시**: `app/pages/`에 `.vue` 파일을 생성하면 라우트가 자동 등록된다(수동 등록 불필요). 보호가 필요하면 `definePageMeta({ middleware: 'auth' })`를 함께 지정하라. **로그인 상태를 차단해야 하는 게스트 전용 페이지(로그인·회원가입)는 `guest` 미들웨어**(로그인 상태면 `/reserve`로 리다이렉트 — `auth`의 반대)로 보호하라(`guest.ts` 미존재 시 ROADMAP_1 Phase 3.1 패턴으로 신설).
- **전역 모듈/플러그인 추가 시**: 의존성 설치 → `nuxt.config.ts`의 `modules`에 등록(또는 `app/plugins/`에 플러그인 생성)을 함께 수행하라.
- **의존성 설치 후**: `npm run postinstall`(`nuxt prepare`)로 `.nuxt` 타입을 재생성하라.
- **npm 스크립트 추가 시**: `package.json`과 `CLAUDE.md`의 "주요 명령어" 표를 함께 갱신하라.
- **BE API 추가·변경 시(2차)**: BE `dto/*`(필드명)와 FE `app/services/*`(`$fetch` 시그니처)·`app/types/domain.ts`를 **같은 PR에서 함께** 수정하라(모노레포 원자 변경). 컨트롤러 추가 시 `service`/`mapper`(+ `resources/mapper/*.xml`)/`dto`/`exception` 계층을 함께 갖춰라.
- **DB 스키마 변경 시(2차)**: `resources/db/schema.sql`(2단계 H2)와 Flyway 마이그레이션(3단계 MySQL)을 함께 갱신하고, 영향받는 `mapper/*.xml` SQL·`domain/*`·`dto/*` 필드를 정합시켜라.
- **BO 신규 화면 추가 시(Phase 6~8)**: FE `app/pages/`(예: `manager/`·`admin/`) 페이지 + `app/middleware/role-guard.ts`(권한 가드) + BE 인가(`@PreAuthorize`)를 함께 추가하고, `e2e/`에 BO 시나리오를 추가하라.

## 도메인/문서 정합성 규칙 (정본 우선순위)

- **도메인 모델·가격·enum의 정본은 `docs/require_v1.md`다.** `app/types/enums.ts`·`app/types/domain.ts`·`app/data/prices.ts`를 작성·수정할 때 값은 require_v1.md의 5장(도메인)·10장(가격 매트릭스)·11장(프로세스 코드 `FW/M/S`)과 **정확히 일치**시켜라. 불일치 시 require_v1.md를 따르라.
- **FE 스택·라우팅·디렉터리·명령어의 정본은 `docs/roadmaps/ROADMAP_1.md`(v1.2)다.** require_v1.md 12장의 일부 스택 표기는 구버전(Vue3+Vite) 기준이므로, 충돌 시 `roadmaps/ROADMAP_1.md`를 따르라.
- **2차(백엔드 진화 + BO) 작업의 정본은 `docs/roadmaps/ROADMAP_2.md`(v2.0)다.** Spring Boot 백엔드·동시성 2·3단계(슬롯 UNIQUE·낙관/비관 락)·BO 프로세스(M3~M7·S3~S8)·휴일/휴무 결재·SMTP/알림·MySQL 이행은 이 문서를 정본으로 따르라. 1차 FO 자산은 그대로 유지하고 `app/services/*` 내부만 `$fetch` API로 교체하는 additive 원칙을 지켜라.
- **DB 접근 기술의 정본은 `docs/require_v1.md` v1.5(12.2)다 — MyBatis 사용, JPA/Hibernate 미사용.** ROADMAP_2.md의 구현 예시가 JPA로 보이면 require_v1.md v1.5를 우선하라.
- **명세 미해결 질문 Q1~Q8(차종↔베이 매핑 등)은 `ROADMAP_2.md` Phase 0 결정표가 SSOT다.** `domain/Bay.java`(`size`)·`domain/Price.java`·`BayService.findBaysForCar`는 결정표를 참조해 구현하라. 확정값이 권고안과 다르면 **결정표만 갱신**하면 Phase 1이 따라온다. **Phase 0 결정표가 잠기기 전 Phase 1(도메인·스키마)을 시작하지 마라.**
- **BE 도메인 객체/DTO 필드명은 `app/types/domain.ts`(1차)·`docs/require_v1.md`(5장 도메인·10장 가격 20행·11장 프로세스 코드)와 동시에 정합**시켜라. 셋 중 하나를 바꾸면 나머지 정합을 확인하라(무변환 매핑 유지).
- **예약 화면 동작(순차 선택·차종별 베이 노출·휠 날짜/시간 선택 등)의 정본은 `docs/예약_규칙_명세_v1.md`다.** 예약 위저드(`app/pages/reserve/{index,slot,done}.vue`)·예약 관련 컴포넌트를 수정할 때 이 명세와 일치시켜라.
- 화면/스토어/서비스를 구현하면 해당 `docs/roadmaps/ROADMAP_1.md` Phase의 체크리스트·DoD를 함께 갱신하라.

## 테스트(Playwright) 규칙

- E2E 테스트는 **`e2e/`에 `*.spec.ts`** 로 작성하라. `baseURL`은 `http://localhost:3000`(Vite의 5173 아님)이다.
- 셀렉터는 **`data-testid` + `getByTestId`** 를 우선 사용하라. 텍스트/클래스 기반 셀렉터는 지양하라. 테스트 대상 컴포넌트에 `data-testid`를 부여하라.
- 더미 데이터는 고정값이므로 결정적으로 단정(assert)하라. `Math.random()`·현재 시각 의존을 피하고 고정 날짜를 사용하라.
- **1차 Playwright E2E(`e2e/`)는 회귀 스위트로 유지하라.** 데이터 출처가 더미 → 서버로 바뀌어도 화면 동작은 동일해야 하며, Phase 2·3·4·5·8 완료 시 전체 회귀를 통과시켜라.

### Spring 통합테스트 (2차 `backend/src/test/...`)

- 동시성은 `@SpringBootTest` + 멀티스레드(`ExecutorService`/`CountDownLatch`)로 **"동시 N요청 중 정확히 1건만 성공"** 을 검증하라.
- H2(Phase 4)와 MySQL(Phase 10)은 락 동작이 다를 수 있으므로 **동일 통합테스트를 양쪽에서 각각 재실행**하라.
- 시드 데이터(`DataSeeder`)는 고정값이므로 특정 매장/슬롯/가격을 단정하라.

## 언어/커뮤니케이션 규칙

- 코드 **주석은 한국어**로 작성하라.
- **커밋 메시지는 한국어**로 작성하라.
- 변수명·함수명은 **영어**(코드 표준)로 작성하라.

## AI 의사결정 기준

- **컴포넌트를 어디에 둘지 모호할 때**: 라우트에 직접 매핑되는 페이지면 `app/pages/`, 여러 곳에서 재사용되면 `app/components/`에 배치하라.
- **새 상태가 필요할 때**: 단일 컴포넌트 내부면 `ref`/`reactive`, 여러 컴포넌트 공유면 `app/stores/`에 Pinia setup 스토어를 추가하라.
- **import 경로 선택 시**: `app/` 내부 모듈은 **`~`·`@` 별칭**을 우선 사용하라 (상대경로 `../../` 남용 금지).
- **(2차) 슬롯 락 기법 선택 시**: 기본은 낙관적 락(version 컬럼 비교 UPDATE의 영향 행 수로 충돌 판정), 경합이 잦은 인기 슬롯만 비관적 락(매퍼 `SELECT ... FOR UPDATE`). 어느 경우든 슬롯 `UNIQUE`를 최종 방어선으로 항상 깔아라.
- **(2차) 백엔드 로직을 어디 둘지 모호할 때**: 비즈니스 규칙·트랜잭션은 `service/`, SQL/영속화는 `mapper/`(+ `resources/mapper/*.xml`), 도메인 상태 전이는 `domain/` 메서드, HTTP 매핑만 `controller/`에 둬라.
- **(2차) FE 변경이 필요해 보일 때**: 먼저 `app/services/*` 내부 교체로 해결 가능한지 보라. 컴포넌트/스토어 마크업을 바꿔야 한다면 additive 위반 가능성을 의심하고 재검토하라(동기→`Promise` 전환에 따른 `await` 추가 정도만 허용).

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
- (2차) **JPA/Hibernate 사용 금지** — `spring-boot-starter-data-jpa`·`@Entity`·`@Id`·`@Table`·`@Version`·`JpaRepository`·`@Lock`·`ddl-auto` 도입 **금지**. DB 접근은 **MyBatis 매퍼**(require_v1.md v1.5).
- (2차) 1차 FE 자산(컴포넌트·스토어·페이지·E2E) **삭제·구조 변경 금지**. 백엔드 연동은 `app/services/*` 내부 교체로 한정(additive).
- (2차) 컨트롤러에서 **도메인 객체 직접 반환·수신 금지**(`record` DTO 경유). 매퍼에서 과도한 중첩 조인/`<association>` 남용 **금지**(N+1 유발).
- (2차) `domain/` 객체에 `@Setter`/`@Data` 부여 **금지**(도메인 전이 메서드 사용).
- (2차) 슬롯 충돌을 500으로 응답 **금지**(반드시 409 + FE 재선택 토스트 재사용).
- (2차) JWT를 `localStorage`에 저장 **금지**(`useCookie` 사용). 결재에 워크플로우 엔진 도입 **금지**(상태 enum 방식).
- (2차) 스키마를 ORM 자동 생성에 의존 **금지** — `schema.sql`(H2)/Flyway(MySQL)로 직접 관리. 운영에서 임의 스키마 변경 **금지**.
- (2차) 루트 `package.json`에 Java/Gradle 빌드 끼워넣기 **금지**(`backend/`는 독립 Gradle). `backend/build/`·`.gradle/`·`*.class` 커밋 **금지**.
- 색상 hex 하드코딩 및 `@layer components` 공통 클래스와 중복되는 인라인 스타일 작성 **금지** (`main.css`의 `@theme` 토큰·공통 클래스 사용).
- `tailwind.config.js` 생성 **금지** (Tailwind v4 CSS-first, `@theme`로 설정).
- `node_modules/`, `.nuxt/`, `.output/` 등 빌드 산출물 직접 수정 **금지**.
