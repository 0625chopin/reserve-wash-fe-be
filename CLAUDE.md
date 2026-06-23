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

자동차 **세차 예약 서비스**(MVP). 단일 저장소에 **프론트엔드(`app/`, 루트)와 백엔드(`backend/`)가 공존**한다.

- **프론트엔드**: **Nuxt 4**(Vue 3 `<script setup>` Composition API + Vite + Nitro). 파일 기반 라우팅, 상태 관리 Pinia(`@pinia/nuxt`), TypeScript, 기본 SSR. 스타일은 Tailwind CSS v4(`@tailwindcss/vite`). 루트 `package.json`이 프론트엔드 빌드.
- **백엔드**: **Spring Boot 3.3.5 / Java 21 / Gradle**. 영속 계층은 **MyBatis**(매퍼 인터페이스 + XML SQL, **JPA/Hibernate 미사용**), DB는 **H2 in-memory**(MySQL 호환 모드, 재기동 시 휘발). 인증은 **Spring Security + JWT**(HS256). `backend/` 하위가 별도 Gradle 프로젝트.
- **데이터 진화 단계**: 1차(`ROADMAP_1.md`)는 FO 플로우를 프론트 더미·in-memory로 구현, 2차(`ROADMAP_2.md`)는 그 더미를 Spring Boot REST API + DB로 교체하고 **BO(매니저/매장관리자/관리자) 화면**을 추가한다. 현재 2차 진행 중 — 더미 데이터(`app/data/`)는 카탈로그 일부만 남고 대부분 백엔드로 이관됨.

> 역할은 4종: **USER**(고객) · **MANAGER**(매장 매니저) · **STORE_ADMIN**(매장관리자) · **ADMIN**(전체 관리자). FE `app/types/enums.ts`의 `UserRole`과 BE `domain/enums/UserRole.java`가 **글자까지 일치**해야 한다.

## 주요 명령어

### 프론트엔드 (루트에서 실행)

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

E2E 테스트는 **Playwright**로 수행 (`playwright.config.ts`, 테스트는 `e2e/`, baseURL `http://localhost:3000`, dev 서버 자동 기동). 셀렉터는 `data-testid` 기반. 단위 테스트 러너(vitest)는 미설치.

### 백엔드 (`backend/`에서 실행)

```sh
./gradlew bootRun        # 개발 서버 기동 → http://localhost:8080 (Windows는 gradlew.bat)
./gradlew build          # 프로덕션 빌드(테스트 포함)
./gradlew compileJava    # 메인 소스 컴파일 (빠른 오류 점검)
./gradlew test           # JUnit 5 단위/통합 테스트
./gradlew test --tests 'com.carwash.SomeTest'  # 단일 테스트 클래스 실행
./gradlew clean          # 빌드 산출물 정리
```

- **H2 콘솔**: http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:carwash`, user `sa`, 비밀번호 없음). 기동 시 `resources/db/schema.sql`(DDL) → `data.sql`(시드)가 자동 실행되며 재기동 시 휘발된다.
- IntelliJ 공유 Run Configuration: `backend/.run/CarwashApplication.run.xml`.
- 도메인 POJO는 **Lombok**(`@Getter`·`@Builder` 등) 사용 — Gradle 빌드는 자동 처리하나 IntelliJ 에디터는 Lombok 플러그인 + 어노테이션 처리 활성이 필요(상세 `backend/README.md`). DTO는 Java `record`라 Lombok 미적용.

## 린트/포맷 파이프라인 (중요)

이 프로젝트는 일반적인 ESLint + Prettier 조합이 아니라 **oxc 도구 체인(oxlint + oxfmt)을 1차로, ESLint를 2차로** 사용함:

- **oxlint** (`.oxlintrc.json`): `correctness` 카테고리를 error로. 빠른 1차 린트.
- **eslint** (`eslint.config.ts`, flat config): Vue + TypeScript 규칙. `eslint-plugin-oxlint`로 oxlint와 중복되는 규칙을 끄고, `eslint-config-prettier`로 포맷 관련 규칙을 끔. `.nuxt`/`.output`은 ignore하고, `app/pages`·`app/layouts`·`app.vue`·`error.vue`에 한해 `vue/multi-word-component-names` 규칙을 끔(파일 기반 라우팅의 단어 1개 파일명 허용).
- **oxfmt** (`.oxfmtrc.json`): 세미콜론 없음(`semi: false`), 작은따옴표(`singleQuote: true`). 코드 스타일은 이 설정을 따를 것 — 새 코드 작성 시 세미콜론을 붙이지 말 것.
- VS Code는 저장 시 oxc 포매터(`oxc.oxc-vscode`)와 `source.fixAll`을 자동 적용 (`.vscode/settings.json`).

## 프론트엔드 구조 (`app/`)

- `nuxt.config.ts` — Nuxt 설정(모듈/CSS/별칭/runtimeConfig). 진입점 부트스트랩은 Nuxt가 담당하므로 별도 `main.ts`는 없음.
- `app/app.vue` — 루트 컴포넌트(`<NuxtPage />` 포함). 레이아웃은 `app/layouts/default.vue`.
- `app/pages/` — **파일 기반 라우트**. FO: `login`·`signup`·`reserve/`(3단계 위저드: index→slot→done)·`reservations`·`review/[reservationId]`. BO: `admin/`·`manager/`·`store-admin/` 하위(역할별 로그인·승인·관리·매출 등). 동적 라우트는 `[param].vue`.
  - **예약 위저드 1페이지(`reserve/index.vue`) 선택 순서**(v2.4): **차종 → 매장 → 매니저 → 서비스**. 차종을 먼저 골라야 그 차종을 수용하는 베이가 있는 매장만 노출된다(`getStoresForCar`). 특대형(`VAN_ETC`→`XLARGE`)은 XLARGE 베이 보유 매장만 노출. 차종 변경 시 매장·매니저 선택이 cascade 초기화(`reservationDraft` watch).
  - **관리자(ADMIN) BO 신규 화면**(v2.4): `admin/stores/`(목록·삭제 `index`·등록 `new`·수정 `[id]`)·`admin/reviews`(후기, 매출 페이지에서 분리). 매출 페이지 `admin/sales`에 매장별 매출 비중 원차트(상위 5 + ETC) 추가. 관리자 네비(`AppNav`)에 "매장 관리"·"후기" 탭.
- `app/middleware/` — 라우트 가드(`defineNuxtRouteMiddleware`): `auth`(인증)·`guest`(비로그인 전용)·`role-guard`(역할 인가)·`reservation-wizard-guard`·`reservation-fresh-entry`·`review-guard`.
- `app/stores/` — Pinia 스토어(자동 임포트, **setup 문법** `defineStore('id', () => {...})`): `auth`·`reservation`·`reservationDraft`·`review`.
- `app/services/` — **데이터 접근 추상화 계층**(`storeService`·`priceService`·`reservationService`·`catalogCache`·`adminStoreService`). 단방향 의존(services → catalogCache·data·types). 2차에서 더미 직접 import를 서버 하이드레이트 캐시 동기 읽기로 교체하되 **시그니처를 유지**해 컴포넌트·스토어 무변경(additive)을 지향함 — 새 데이터 소스 연동 시 이 패턴을 따를 것.
  - `adminStoreService`(관리자 매장 CRUD `$apiFetch` 래퍼)는 mutation 성공 직후 `catalogCache.reloadCatalog()`(컨텍스트 보존 위해 `nuxtApp.runWithContext`로 감쌈)로 카탈로그를 재하이드레이트한다. `catalogCache`는 `loadCatalog`/`reloadCatalog`가 **배열을 재할당하지 않고 splice로 제자리 교체** → 컴포넌트가 setup에서 캡처한 배열 참조가 반응형으로 갱신되어, 매장/매니저 변경이 **전체 새로고침 없이** 반영된다.
- `app/plugins/` — `catalog`(부팅 시 카탈로그 하이드레이트)·`auth-fetch`(JWT 주입 `$apiFetch` 제공).
- `app/components/` — 재사용 컴포넌트(자동 임포트, `icons/` 하위 포함).
- `app/composables/` — 재사용 로직(자동 임포트, `useSlots`·`useToast` 등).
- `app/data/` — 더미/정적 카탈로그 데이터, `app/types/`(`enums.ts`·`domain.ts`) — 도메인 타입.
- `~`·`@` 별칭은 모두 `app/`(srcDir)를 가리킴 (Nuxt 자동 제공).

## 백엔드 구조 (`backend/src/main/java/com/carwash/`)

표준 레이어드 아키텍처(`controller` → `service` → `mapper`):

- `controller/` — REST 엔드포인트(`/api/**`). 역할/도메인별로 다수 분리(`AuthController`·`ReservationController`·`Admin*Controller`(매장 CRUD `AdminStoreController` 포함)·`Manager*Controller`·`StoreAdmin*Controller`·`SalesController` 등).
- `service/` — 비즈니스 로직(예약 동시성·슬롯 점유·승인 워크플로 등).
- `mapper/` — MyBatis 매퍼 **인터페이스**. SQL은 `resources/mapper/*.xml`에 분리. `map-underscore-to-camel-case=true`로 snake_case 컬럼 ↔ camelCase 매핑.
- `domain/` + `domain/enums/` — 가변 도메인 POJO(Lombok), enum. `dto/` — 요청/응답(Java `record`).
- `config/` — `SecurityConfig`(아래)·`AppBeansConfig`. `security/` — `JwtTokenProvider`·`JwtAuthenticationFilter`.
- `exception/` — `GlobalExceptionHandler`(도메인 예외 → JSON 에러 응답)·`SlotConflictException`.
- `resources/` — `application.yml`·`db/schema.sql`·`db/data.sql`·`mapper/*.xml`.

### 인증·인가 (SecurityConfig)

- **stateless JWT**(세션 없음). `JwtAuthenticationFilter`가 `Authorization: Bearer` 토큰을 매 요청 검증.
- 무인증 허용: `/api/auth/**`·`/api/health`·카탈로그 조회(`/api/stores`·`/api/managers`·`/api/bays`·`/api/prices`·`/api/slots`)·`/h2-console/**`.
- 역할 인가(구체적 매처를 `anyRequest` 앞에 배치): `/api/store-admin/**`→`STORE_ADMIN`, `/api/manager/**`→`MANAGER`·`STORE_ADMIN`, `/api/admin/**`→`ADMIN`, 그 외 인증 필요. 미인증→**401**, 권한부족→403.
- CORS는 SecurityConfig의 `corsConfigurationSource` 단일 소스에서 FE(`http://localhost:3000`)만 허용(credentials 포함).

### 이메일 인증 가입 (create-after-verify)

회원가입은 **인증 성공 시에만 `users`로 승격**하는 2단계 플로우다(미인증 반쪽 계정 방지). 진행 중 가입 정보는 `users`가 아닌 **`verification` 테이블**에 임시 보관된다.

- 흐름: `POST /api/auth/signup/request`(가입정보+6자리 코드 발급·메일 발송, **이메일당 1건** delete→insert) → `POST /api/auth/signup/verify`(코드 일치+미만료+시도 5회 이내 검증) → USER는 토큰 발급(자동 로그인)·MANAGER는 `PENDING_APPROVAL_L1`(승인 대기). 코드는 3분 TTL, `resend`로 갱신.
- `verification` 테이블: `email`(PK) 기준 단일 행. `password_hash`(원문 미보관)·`role`·`name`·`store_id`·`expires_at`(epoch millis)·`attempts` 보관. **`method` 컬럼**(`VerificationMethod`: `EMAIL`/`SNS`)으로 인증 방법을 구분 — 현재는 `EMAIL` 단일이나 방법 확장 대비. 도메인/매퍼/서비스의 Java 식별자는 역사적 이유로 `EmailVerification*`이지만 **물리 테이블명은 `verification`**.
- 개발 백도어: `GET /api/auth/signup/dev-code?email=`로 대기 중인 코드를 조회(메일 없이 E2E 검증). `app.signup.dev-code-peek`(기본 true, 운영 false)로 차단.

### 관리자(ADMIN) BO — 매장 CRUD·매출 비중·후기·매니저 엔티티 (v2.4)

- **매장 CRUD**: `GET/POST/PUT/DELETE /api/admin/stores`(`AdminStoreController`·`AdminStoreService`). `GET`은 승인/미승인 **전체** 반환(FO `GET /api/stores`는 승인 매장만). **🔒 신규 등록 매장 기본 `approved=false`**(미승인 — 베이/매니저 구성 후 별도 토글로 승인). **🔒 삭제 무결성**: 예약·후기·매니저·슬롯 연관 데이터가 있으면 **409 `STORE_HAS_DEPENDENCIES`**(`StoreHasDependenciesException`→`GlobalExceptionHandler`)로 차단(소프트 비활성 미채택). 베이(`bay`)는 매장 구성요소라 매장과 함께 생성/교체/삭제(삭제 의존성 제외). 매장/베이/예약/후기/매니저/슬롯 매퍼에 `countByStore` 추가.
- **매출 비중**: `GET /api/admin/sales/by-store`(`SalesService.salesByStore`) — 매장별 COMPLETED 금액 합산, 내림차순. 상위 5 + ETC·비중(%) 가공은 FE `useSalesChart.buildSalesSlices`가 수행하고 `SalesPieChart.vue`(외부 차트 라이브러리 없이 conic-gradient)로 렌더. 기존 매장 단건 매출 `GET /api/admin/stores/{id}/sales`(S8)는 유지.
- **후기 분리**: 기존 `GET /api/admin/stores/{id}/reviews`(S6) **재사용**, 신규 엔드포인트 없이 화면만 `admin/sales`→`admin/reviews`로 이전.
- **매니저 엔티티 자동 생성**: "매니저 등록"(`POST /api/admin/managers`)은 `users`(로그인 계정)만 만든다. 예약 매니저 드롭다운은 **`manager` 테이블**(`GET /api/managers`)에서 오므로, **최종 승인**(`SignupApprovalService.confirmL2`, `PENDING_APPROVAL_L2`→`ACTIVE`) 시 `role=MANAGER`이면 `manager` 엔티티를 생성하고 `users.manager_id`로 연결한다(`ManagerMapper.insert`·`UserMapper.updateManagerId`). `STORE_ADMIN`·소속 매장(`store_id`) 없음·이미 연결됨은 미생성. FE 가입 최종 승인 화면은 승인 후 `reloadCatalog()`로 드롭다운에 즉시 반영.

### 알림·SMTP (Phase 9)

`NotificationService`가 발송 정책(수신자·본문·이력)을 결정하고, 실제 dispatch만 `EmailSender`(`SmtpEmailSender`, `@Async("mailTaskExecutor")` — `AsyncConfig`의 전용 풀)에 위임한다.

- **수신자는 호출자 트랜잭션 내에서 동기로 해석**(비동기 스레드의 커밋 전 재조회 회피), 발송은 비동기라 **실패해도 도메인 트랜잭션(가입/예약/결재)에 전파되지 않는다**. 발송 예외는 메일 스레드에 격리되어 로그로만 남는다.
- 발송 이력은 `notification_log`에 기록: 수신자 있으면 `QUEUED`, 없으면 `SKIPPED`. **발송 후 SENT/FAILED 갱신은 없음** — 실제 전송 성패는 메일 스레드 예외 유무로 판단.
- SMTP 설정은 `application.yml`의 `spring.mail`이 `${MAIL_*}` 자리표시자를 읽고, 값은 **`backend/.env`**(properties 형식, `spring.config.import: optional:file:.env[.properties]`)에서 주입. `.env`는 **상대경로**라 BE를 반드시 `backend/`에서 실행해야 로딩된다(`.gitignore` 제외, 템플릿은 `.env.example`). 기본값은 외부 의존 없는 로컬 캐처(MailHog/Mailpit `localhost:1025`), 실 발송은 Gmail SMTP(587·STARTTLS·앱 비밀번호).

## FE ↔ BE 연동

- FE는 `runtimeConfig.public.apiBase`(기본 `http://localhost:8080/api`, 환경변수 `NUXT_PUBLIC_API_BASE`로 override)로 백엔드를 호출한다.
- 보호 API는 `auth-fetch` 플러그인이 제공하는 `useNuxtApp().$apiFetch`로 호출 — `access_token` 쿠키를 읽어 `Authorization: Bearer`를 자동 주입한다. 무인증 호출(login/signup·카탈로그 조회)은 일반 `$fetch` 사용.
- 로컬 풀스택 구동: 백엔드 `./gradlew bootRun`(:8080) + 프론트 `npm run dev`(:3000)를 동시에 띄운다.

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
