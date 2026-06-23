# start-kit2 — 자동차 세차 예약 서비스 (MVP)

자동차 **세차 예약 서비스** MVP. 단일 저장소에 **프론트엔드(`app/`, 루트)와 백엔드(`backend/`)가 공존**하는 풀스택 구성이다.

- **프론트엔드**: **Nuxt 4**(Vue 3 `<script setup>` + Vite + Nitro), 파일 기반 라우팅, 상태 관리 **Pinia**, 기본 **SSR**, 스타일 **Tailwind CSS v4**
- **백엔드**: **Spring Boot 3.3.5 / Java 21 / Gradle**, 영속 계층 **MyBatis**(JPA 미사용), DB **H2 in-memory**(재기동 시 휘발), 인증 **Spring Security + JWT**(HS256)
- 코드 스타일(FE): **oxc 체인**(oxlint + oxfmt — 세미콜론 없음·작은따옴표) + ESLint(2차)

> **데이터 진화 단계**: 1차(`docs/roadmaps/ROADMAP_1.md`)는 FO 플로우를 프론트 더미·in-memory로 구현했고, 2차(`docs/roadmaps/ROADMAP_2.md`)는 그 더미를 **Spring Boot REST API + DB로 교체**하고 **BO(매니저/매장관리자/관리자) 화면**을 추가한다. **현재 2차 진행 중** — 더미 데이터(`app/data/`)는 카탈로그 일부만 남고 대부분 백엔드로 이관되었다.

## 역할 (4종)

| 역할 | 설명 | 진입 |
|---|---|---|
| `USER` | 고객 — 예약·후기 | `/login` |
| `MANAGER` | 매장 매니저 — 본인 예약·휴무 신청 | `/manager/login` |
| `STORE_ADMIN` | 매장관리자 — 매니저 가입·휴무 1차 승인 | `/manager/login` |
| `ADMIN` | 전체 관리자 — 매니저 가입 2차 승인·매출·전체 관리 | `/admin/login` |

> FE `app/types/enums.ts`의 `UserRole`과 BE `domain/enums/UserRole.java`는 **글자까지 일치**해야 한다.

## 사전 요구

| 항목 | 요구 버전 |
|---|---|
| Node.js | `^22.18.0 \|\| >=24.12.0` (`package.json` `engines`) |
| JDK | **21** (백엔드 Gradle 툴체인) |

## 로컬 풀스택 구동

백엔드와 프론트엔드를 **각각 띄운다**(FE가 `apiBase`로 BE를 호출).

```sh
# 1) 백엔드 (backend/ 에서) — http://localhost:8080
cd backend && ./gradlew bootRun        # Windows: gradlew.bat bootRun

# 2) 프론트엔드 (루트에서) — http://localhost:3000
npm install                            # postinstall 로 nuxt prepare 자동 실행
npm run dev
```

- FE는 `runtimeConfig.public.apiBase`(기본 `http://localhost:8080/api`, `NUXT_PUBLIC_API_BASE`로 override)로 BE를 호출한다.
- 보호 API는 `auth-fetch` 플러그인의 `$apiFetch`가 `access_token` 쿠키를 읽어 `Authorization: Bearer`를 자동 주입한다.
- **메일 발송(SMTP)**: `backend/.env`(템플릿 `backend/.env.example`)로 자격증명을 주입한다. `.env`는 **상대경로 로딩**이라 BE를 반드시 `backend/`에서 실행해야 반영된다. 미설정 시 기본값은 로컬 메일 캐처(MailHog/Mailpit `localhost:1025`). 상세는 [`backend/README.md`](backend/README.md).

## 주요 명령어 (프론트엔드 — 루트)

| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버(HMR) — **http://localhost:3000** |
| `npm run build` / `npm run generate` | 프로덕션 빌드(`.output`) / 정적 사이트 생성(SSG) |
| `npm run preview` | 빌드 결과물 로컬 미리보기 |
| `npm run type-check` | 타입 검사(`nuxt typecheck`, 내부 vue-tsc) |
| `npm run lint` | oxlint → eslint 순차(둘 다 `--fix`) |
| `npm run format` | oxfmt로 `app/` 포맷팅 |
| `npm run test:e2e` / `:ui` | Playwright E2E (dev 서버 자동 기동, 포트 3000) / UI 모드 |

백엔드 명령(`./gradlew bootRun`·`build`·`test` 등)은 [`backend/README.md`](backend/README.md) 참고.

## 시드 계정

DB 시드(`backend/src/main/resources/db/data.sql`)로 주입되며, **비밀번호는 모두 `password`**(BCrypt 해시 저장).

| 이메일 | 이름 | 역할 | 상태 |
|---|---|---|---|
| `user@test.com` | 홍길동 | USER | ACTIVE |
| `user2@test.com` | 김고객 | USER | ACTIVE |
| `manager@test.com` | 김매니저 | MANAGER | ACTIVE (store1) |
| `storeadmin@test.com` | 매장관리자 | STORE_ADMIN | ACTIVE (store1) |
| `admin@test.com` | 관리자 | ADMIN | ACTIVE |
| `pending1@test.com` · `pending2@test.com` | 신입/대기매니저 | MANAGER | PENDING_APPROVAL_L1 (승인 대기) |

> H2는 in-memory라 재기동 시 시드 상태로 초기화된다.

## 주요 화면 흐름

**FO (고객)**

1. **로그인/회원가입** `/login` · `/signup` — 가입은 **이메일 인증**(6자리 코드, 유효 3분). USER는 인증 즉시 자동 로그인, MANAGER는 승인 대기.
2. **예약 위저드(3단계)** `/reserve`(매장→매니저→차종→서비스, 가격 자동) → `/reserve/slot`(날짜·시간→베이 그리드, 점유 베이 비활성) → `/reserve/done`(완료 요약)
3. **예약 목록** `/reservations` — 상태 뱃지, 세차완료/취소(취소 시 슬롯 release), 완료 예약은 후기 작성 진입
4. **후기 작성** `/review/:reservationId` — 세차완료·본인 예약만, 평점(1~5)+텍스트 → 매장/매니저 평균 반영

**BO (운영)** — `role-guard`로 역할 인가

- **매니저** `/manager/*` — 본인 예약(`reserve`·`reservations`), 휴무 신청(`dayoffs`)
- **매장관리자** `/store-admin/*` — 매니저 가입 1차 승인(`manager-signups`), 휴무 1차 승인(`dayoff-approvals`)
- **관리자** `/admin/*` — 매니저 등록(`managers`)·가입 2차 승인(`manager-approvals`)·휴무 결재(`approvals`)·매출(`sales`)·매장별 예약/사용자(`stores/[id]/...`)

## 테스트

```sh
npm run test:e2e        # 전체 E2E (e2e/*.spec.ts), baseURL http://localhost:3000
npm run test:e2e:ui     # UI 모드
```

- 셀렉터는 `data-testid` 기반. `playwright.config.ts`는 `reuseExistingServer:true` — 이미 떠 있는 :3000·:8080을 재사용한다(슬롯은 서버 전역 1회 자원이라 재실행 시 충돌하면 BE 클린 재기동).
- 백엔드 테스트는 JUnit 5(`cd backend && ./gradlew test`).

## 문서

| 문서 | 내용 |
|---|---|
| [`docs/require_v1.md`](docs/require_v1.md) | 요구사항 정의서(정본) |
| [`docs/roadmaps/ROADMAP_1.md`](docs/roadmaps/ROADMAP_1.md) | 로드맵 1차(FO + 프론트 더미) |
| [`docs/roadmaps/ROADMAP_2.md`](docs/roadmaps/ROADMAP_2.md) | 로드맵 2차(Spring Boot 백엔드 진화 + BO) |
| [`docs/예약_규칙_명세_v1.md`](docs/예약_규칙_명세_v1.md) | 예약 순서·차종↔베이 규칙 명세 |
| [`CLAUDE.md`](CLAUDE.md) | 아키텍처·린트 파이프라인·작업 규칙(상세) |
| [`backend/README.md`](backend/README.md) | 백엔드 명령어·API·SMTP·IDE 설정(IntelliJ + Lombok) |

## 디렉터리 개요

```
app/                          프론트엔드 (Nuxt, srcDir)
├── pages/        파일 기반 라우트 (login·signup·reserve/·reservations·review/ + admin/·manager/·store-admin/)
├── components/   재사용 컴포넌트 (자동 임포트, icons/ 포함)
├── stores/       Pinia setup 스토어 (auth·reservation·reservationDraft·review)
├── middleware/   라우트 가드 (auth·guest·role-guard·reservation-*·review-guard)
├── services/     데이터 접근 추상화 (storeService·priceService·reservationService·catalogCache)
├── plugins/      catalog(카탈로그 하이드레이트)·auth-fetch($apiFetch 제공)
├── composables/  재사용 로직 (useSlots·useToast·useAuthRedirect)
├── data/         더미/정적 카탈로그
└── types/        도메인 타입 (enums·domain)

backend/                      백엔드 (Spring Boot, 별도 Gradle 프로젝트)
└── src/main/java/com/carwash/  controller → service → mapper 레이어드 (상세: backend/README.md)
```

## IDE 설정 (권장)

- **FE**: [VS Code](https://code.visualstudio.com/) + [Vue (Official) / Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)(Vetur 비활성), [oxc](https://marketplace.visualstudio.com/items?itemName=oxc.oxc-vscode) 확장(저장 시 자동 포맷, `.vscode/settings.json`)
- **BE**: IntelliJ IDEA + Lombok 플러그인·어노테이션 처리 활성 — 상세 [`backend/README.md`](backend/README.md)
