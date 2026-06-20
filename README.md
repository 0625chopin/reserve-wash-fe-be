# start-kit2 — 자동차 세차 예약 서비스 (MVP)

**Nuxt 4**(Vue 3 `<script setup>` + Vite + Nitro) 기반 프론트엔드 프로젝트. 데이터 진화 **1단계(프론트 더미 데이터 · in-memory)** 로, 로그인 → 예약(3단계 위저드) → 예약 목록(세차완료/취소) → 후기 작성까지의 FO 플로우를 구현한다.

- 렌더링: 기본 **SSR**, 상태 관리: **Pinia**(`@pinia/nuxt`), 라우팅: **파일 기반**
- 코드 스타일: **oxc 체인**(oxlint + oxfmt, 세미콜론 없음 · 작은따옴표) + ESLint(2차)
- 동시성·슬롯·휴무 규칙은 클라이언트 상태로 시뮬레이션(2·3단계에서 서버/DB 교체 예정)

## 사전 요구

| 항목 | 요구 버전 |
|---|---|
| Node.js | `^22.18.0 \|\| >=24.12.0` (`package.json` `engines`) |
| npm | Node 동봉 버전 |

## 설치

```sh
npm install
```

`postinstall`에서 `nuxt prepare`가 자동 실행되어 `.nuxt` 타입이 생성된다. 자동 실행되지 않으면 `npm run postinstall`을 직접 실행한다.

## 주요 명령어

| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버(HMR) — **http://localhost:3000** |
| `npm run build` | 프로덕션 빌드(`.output`) |
| `npm run generate` | 정적 사이트 생성(SSG) |
| `npm run preview` | 빌드 결과물 로컬 미리보기 |
| `npm run type-check` | 타입 검사(`nuxt typecheck`, 내부 vue-tsc) |
| `npm run lint` | oxlint → eslint 순차(둘 다 `--fix`) |
| `npm run format` | oxfmt로 `app/` 포맷팅 |
| `npm run test:e2e` | Playwright E2E (dev 서버 자동 기동, 포트 3000) |
| `npm run test:e2e:ui` | Playwright UI 모드 |

## 더미 로그인 계정

1단계는 더미 인증으로, **비밀번호는 모두 `password`** 로 통일되어 있다(`app/data/users.ts`).

| 이메일 | 이름 | 역할 |
|---|---|---|
| `user@test.com` | 홍길동 | USER |
| `user2@test.com` | 김고객 | USER |
| `manager@test.com` | 김매니저 | MANAGER |
| `admin@test.com` | 관리자 | ADMIN |

> 1차 MVP는 FO(사용자) 플로우만 구현한다. BO(매니저/관리자) 화면은 2차 과제(문서화만).

## 주요 화면 흐름

1. **로그인** `/login` — 더미 계정으로 로그인(미인증 시 보호 라우트 접근 → `/login` 리다이렉트)
2. **예약 위저드(3단계)**
   - `/reserve` — 매장 → 매니저 → 차종 → 서비스 선택(가격 자동 표시) → "다음"
   - `/reserve/slot` — 날짜·시간 선택 → 그 시간대 **베이 버튼 그리드**(예약된 베이는 비활성) → "예약하기"
   - `/reserve/done` — 예약 완료 요약
3. **예약 목록** `/reservations` — 상태 뱃지, **세차완료/취소**(취소 시 슬롯 release), 완료 예약은 후기 작성 진입
4. **후기 작성** `/review/:reservationId` — 세차완료·본인 예약만, 평점(1~5)+텍스트 → 매장/매니저 평균 표시

> ⚠️ **1단계 in-memory 주의**: 진행 중 예약·슬롯 점유·예약/후기 데이터는 메모리에만 보관되어 **브라우저 새로고침 시 초기화**된다. 위저드 미완료 단계로 직접 URL 진입 시 가드가 `/reserve`로 되돌린다.

## 테스트

```sh
npm run test:e2e        # 전체 E2E (e2e/*.spec.ts)
npm run test:e2e:ui     # UI 모드
```

- 셀렉터는 `data-testid` 기반, 더미 데이터 고정값으로 결정적 단정.
- 시나리오: 로그인/가드 · 매장·매니저 선택 · 예약 위저드 · 베이 점유 비활성 · 매니저 휴무(전일/교대조) · 예약 취소·세차완료 · 후기 작성 자격·평점.

## 문서

| 문서 | 내용 |
|---|---|
| [`docs/require_v1.md`](docs/require_v1.md) | 요구사항 정의서(정본) |
| [`docs/roadmaps/ROADMAP_1.md`](docs/roadmaps/ROADMAP_1.md) | 개발 로드맵 1차(FO + 프론트 더미 · Phase별 · 정본) |
| [`docs/roadmaps/ROADMAP_2.md`](docs/roadmaps/ROADMAP_2.md) | 개발 로드맵 2차(Spring Boot 백엔드 진화 + BO · Phase별) |
| [`docs/예약_규칙_명세_v1.md`](docs/예약_규칙_명세_v1.md) | 예약 순서·차종↔베이 규칙 보강 명세 |
| [`shrimp-rules.md`](shrimp-rules.md) | AI Agent 작업 규칙 |
| [`CLAUDE.md`](CLAUDE.md) | 프로젝트 가이드(언어·스택·린트 파이프라인) |
| [`backend/README.md`](backend/README.md) | 백엔드(Spring Boot) 명령어·**IDE 설정(IntelliJ + Lombok)** |

## 디렉터리 개요

```
app/
├── pages/        파일 기반 라우트 (login, reserve/, reservations, review/[reservationId])
├── components/   재사용 컴포넌트 (SearchableSelect, WheelPicker, SlotGrid, AppNav)
├── stores/       Pinia (auth, reservation, reservationDraft, review)
├── middleware/   라우트 가드 (auth, reservation-wizard-guard, reservation-fresh-entry, review-guard)
├── services/     데이터 접근 추상화 (storeService, priceService, reservationService)
├── composables/  재사용 로직 (useSlots, useToast)
├── data/         더미 데이터 (stores, managers, users, prices, carTypes, serviceTypes)
└── types/        도메인 타입 (enums, domain)
```

## IDE 설정 (권장)

- [VS Code](https://code.visualstudio.com/) + [Vue (Official) / Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (Vetur 비활성화)
- [oxc](https://marketplace.visualstudio.com/items?itemName=oxc.oxc-vscode) 확장 — 저장 시 oxfmt 자동 포맷(`.vscode/settings.json`)
- `.vue` 타입 지원은 `nuxt typecheck`(vue-tsc) 기준 — 에디터에서는 Volar 필요
