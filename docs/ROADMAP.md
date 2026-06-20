# 자동차 세차 예약 서비스 (MVP) 개발 로드맵 — 1차 FO + 프론트 더미 데이터

> **문서 버전**: v1.3 (예약 규칙 2건 반영 — 매니저 3교대 부분 휴무 / 베이 슬롯 점유 선택불가)
> **작성일**: 2026-06-20
> **작성자**: PM/PL
> **대상 독자**: 주니어 ~ 시니어 프론트엔드 개발자
> **연계 문서**: [`docs/require_v1.md`](./require_v1.md) (요구사항 정의서 v1.1 → **v1.3**)
> **범위**: 데이터 진화 **1단계(프론트엔드 더미 데이터)** + **FO 플로우(로그인/예약/취소/후기)** 집중 상세화. 2~3단계(Spring Boot → MySQL)는 마일스톤 개요만 기재.
>
> **🔄 v1.2 변경 요약**: 프론트엔드 스택이 **Vue3+Vite(vue-router 수동 설정)에서 Nuxt 4로 전환 완료**되었습니다. Phase 구성·공수·DoD 골격은 그대로 유지하되, 라우팅(파일 기반), 디렉터리(`app/`), 별칭(`~`/`@`→`app/`), 명령어/포트(3000), 라우트 가드(미들웨어) 등 스택 의존 표현과 코드 예시를 Nuxt 4 기준으로 갱신했습니다.
>
> **🔄 v1.3 변경 요약**: require_v1.md v1.3의 **예약 규칙 2건**을 최소 변경(additive)으로 반영했습니다. ① 매니저 휴무 모델을 전일(`FULL_DAY`)/**3교대 기반 교대조 단위 부분 휴무**(`SHIFT_1`/`SHIFT_2`/`SHIFT_3`)로 확장 — Phase 1 도메인 타입·더미데이터, Phase 5 슬롯 비활성 로직에 반영. ② **베이 슬롯 점유 시 선택 불가**(RESERVED/COMPLETED 베이는 해당 시간대 미니 그리드에서 비활성) — 선택 순서(매장→매니저→차종→베이→날짜/시간)는 유지하는 하이브리드 UI로 Phase 5에 반영. Phase 구성·공수·표 스타일·문서 톤은 그대로 유지했습니다(require 5.5·6.1·7장 연계).

---

## 0. 로드맵 사용 가이드

### 0.1 이 문서를 읽는 법

이 문서는 **위에서 아래로 순서대로** 읽고 따라오면 1차 MVP(FO)가 완성되도록 설계되었습니다. 처음 합류한 주니어 개발자라면:

1. **0장(사용 가이드) → 1장(환경 셋업) → 2장(아키텍처/컨벤션)** 을 먼저 읽고 손으로 따라 하세요. 여기까지가 "출발선"입니다.
2. **3장 Phase 1 → Phase 8** 을 순서대로 진행하세요. 각 Phase는 **선행 Phase가 끝나야** 시작할 수 있습니다(의존성 명시).
3. 막히는 동시성/슬롯 로직은 **4장(동시성 구현 가이드)** 를, 테스트는 **6장(Playwright)** 을 펼쳐 보세요.
4. PR 올리기 전에 **7장 PR 셀프 체크리스트**를 반드시 확인하세요.

### 0.2 require_v1.md 와의 연계 (섹션 매핑 안내)

각 Phase 머리말에 `> require_v1.md 참조: N장` 형식으로 근거 섹션을 표기했습니다. 구현 중 "왜 이렇게 해야 하지?"가 생기면 해당 요구사항 섹션을 펼쳐 보세요. 전체 추적 매핑은 **8장 부록**에 정리되어 있습니다.

### 0.3 주니어 진행 원칙 (3원칙)

- **작은 단위 PR**: 한 PR = 한 Phase의 일부 태스크. 한 번에 1,000줄을 올리지 말고 "타입 정의", "스토어", "컴포넌트"처럼 쪼개서 올리세요.
- **Phase별 DoD 확인**: 각 Phase 끝의 **완료기준(DoD, Definition of Done)** 을 모두 만족해야 다음 Phase로 넘어갑니다. 체크박스를 직접 체크하며 진행하세요.
- **셀프 체크리스트 활용**: 코드 작성 → `npm run lint` → `npm run type-check`(내부적으로 `nuxt typecheck`) → (해당 시) `npm run test:e2e` 흐름을 매번 거치세요. 7장 체크리스트가 가이드입니다.

### 0.4 전체 마일스톤 한눈에 보기

| Phase | 제목 | 핵심 산출물 | require 참조 | 공수(일) | 누적(일) |
|:---:|---|---|:---:|:---:|:---:|
| **0** | 개발 환경 셋업 | dev 서버 기동(:3000), 명령어 숙지 | 12장 | 0.5 | 0.5 |
| **1** | 도메인 타입 & 더미 데이터 레이어 | `app/types/`, `app/data/`, 슬롯·가격 유틸 (Manager 휴무 모델 교대조 구조화) | 5·10장 | 2.0 | 2.5 |
| **2** | 라우팅 & 레이아웃 골격 | 파일 기반 라우트 4종, 레이아웃, 미들웨어 가드 스텁 | 12.4 | 1.5 | 4.0 |
| **3** | 인증 / 로그인 (FW2) | `auth` 스토어, 로그인 폼, 가드 연동 | 4장 | 2.0 | 6.0 |
| **4** | 매장/매니저 선택 + 검색 UX (FW3/FW4) | `SearchableSelect`, 필터 검색 | 6.3 | 2.5 | 8.5 |
| **5** | 예약 페이지 & 슬롯 그리드 + 동시성 시뮬레이션 (FW5) | 슬롯 그리드, 낙관적 갱신, 가격 계산 | 5·6·7장 | 4.0 | 12.5 |
| **6** | 예약 확정/취소/세차완료 (FW6/FW7) | 예약 목록, 상태 전이, 취소 2케이스 | 11.3 | 2.5 | 15.0 |
| **7** | 후기/평점 (1차 확장) | 후기 작성, 평점 집계 | 9장 | 2.0 | 17.0 |
| **8** | E2E 테스트 & 마무리 | Playwright 시나리오, 린트/타입 통과 | 6장(본 문서) | 2.0 | 19.0 |
| — | **데이터 2단계** (Spring Boot 더미) | *개요만* — 서비스 추상화, UNIQUE+락 | 7·12장 | (별도) | — |
| — | **데이터 3단계** (MySQL) | *개요만* — 트랜잭션 + 유니크 인덱스 | 7·12장 | (별도) | — |

> **총 예상 공수: 약 19일** (주니어 기준 넉넉하게 산정. 리뷰·수정 버퍼 포함). 2·3단계는 별도 로드맵으로 분리 예정.
>
> 🔄 **v1.3 데이터 레이어 영향(Phase 1)**: 매니저 휴무 모델이 기존 `dayoffDates: string[]`(전일만 표현) → **교대조 정보를 포함한 구조**(`{ date, type: 'FULL_DAY' | 'SHIFT_1' | 'SHIFT_2' | 'SHIFT_3' }[]`)로 확장됩니다. Phase 1의 도메인 타입(`Manager`)·더미 데이터(`app/data/managers.ts`)가 영향받으며, Phase 5의 슬롯 비활성 판정이 이를 소비합니다(require 5.5 참조). 공수·Phase 구성 변동 없음(additive).

---

## 1. 개발 환경 셋업 (Phase 0)

> **공수: 0.5일** · **require_v1.md 참조: 12장**

### 1.1 사전 요구사항

| 항목 | 요구 버전 | 확인 명령 |
|---|---|---|
| Node.js | `^22.18.0 \|\| >=24.12.0` (package.json `engines`) | `node -v` |
| npm | Node 동봉 버전 | `npm -v` |
| Git | 최신 | `git --version` |

> ⚠️ **주의**: Node 버전이 `engines` 범위를 벗어나면 설치/빌드가 실패할 수 있습니다. `nvm`(Windows는 `nvm-windows`) 등으로 버전을 맞추세요.

### 1.2 설치

```sh
npm install
```

### 1.3 핵심 명령어

| 명령 | 설명 | 언제 쓰나 |
|---|---|---|
| `npm run dev` | Nuxt 개발 서버(HMR, Vite 내장) 기동 — **http://localhost:3000** | 개발 중 항상 |
| `npm run build` | `nuxt build` — Nitro 서버 번들 프로덕션 빌드 | 배포 전 |
| `npm run generate` | `nuxt generate` — 정적 사이트(SSG) 산출물 생성 | 정적 배포 시 |
| `npm run preview` | `nuxt preview` — 빌드 결과물 로컬 미리보기 | 빌드 검증 |
| `npm run type-check` | `nuxt typecheck` (vue-tsc 기반 타입 검사) | PR 전 필수 |
| `npm run lint` | oxlint → eslint 순차 실행(둘 다 `--fix`) | PR 전 필수 |
| `npm run format` | oxfmt로 `app/` 포맷팅 | 커밋 전 |

> 💡 **`postinstall`**: `npm install` 시 자동으로 `nuxt prepare`가 실행되어 `.nuxt/` 타입 스텁을 생성합니다. 타입 오류가 갑자기 나면 `npm run postinstall`(= `nuxt prepare`)로 타입을 재생성해 보세요.

### 1.4 VS Code 설정 (권장)

- **확장 설치**: `oxc.oxc-vscode` (oxc 포매터). 프로젝트 `.vscode/settings.json`이 저장 시 자동 포맷(`source.fixAll`)을 적용합니다.
- ⚠️ **코드 스타일 주의**: 이 프로젝트는 **세미콜론 없음(`semi: false`)**, **작은따옴표(`singleQuote: true`)** 입니다. 신규 코드에 세미콜론을 붙이지 마세요. Prettier가 익숙하다면 무심코 세미콜론을 추가하기 쉬우니 주의하세요.

### 1.5 완료기준 (DoD)

- [ ] `node -v`가 `engines` 범위 내 버전을 출력한다
- [ ] `npm install`이 오류 없이 완료된다
- [ ] `npm run dev` 실행 후 브라우저에서 기본 페이지(`http://localhost:3000`)가 보인다 (Nuxt welcome / 기본 `app/pages/index.vue` 렌더)
- [ ] `npm run lint`, `npm run type-check`가 통과한다(기본 템플릿 기준)

---

## 2. 아키텍처 & 컨벤션 가이드

### 2.1 폴더 구조 컨벤션 (Nuxt 4 `app/` 기반)

Nuxt 4는 앱 코드를 루트가 아닌 **`app/` 디렉터리** 아래에 둡니다(루트에는 `nuxt.config.ts`만). 기존 폴더(`pages/`, `components/`, `stores/`)에 더해 아래 신규 폴더를 도입합니다. 페이지·컴포넌트·스토어·컴포저블은 **Nuxt 자동 임포트**가 적용되어 별도 import 구문 없이 사용할 수 있습니다.

| 폴더 | 역할 | 예시 | 자동 임포트 |
|---|---|---|:---:|
| `app/pages/` | 파일 기반 라우트(=URL) 페이지 컴포넌트 | `login.vue`, `reserve.vue` | 라우트 자동 등록 |
| `app/components/` | 재사용 UI 컴포넌트 | `SearchableSelect.vue`, `SlotGrid.vue` | ✅ |
| `app/stores/` | Pinia 상태 스토어 (setup 문법) | `auth.ts`, `reservation.ts` | ✅ (`@pinia/nuxt`) |
| `app/middleware/` | 라우트 미들웨어(가드) | `auth.ts` | ✅ (`definePageMeta` 참조) |
| `app/layouts/` | 공통 레이아웃 | `default.vue` | ✅ |
| **`app/types/`** *(신규)* | 도메인 TS 인터페이스/타입/enum | `domain.ts`, `enums.ts` | (타입은 명시 import) |
| **`app/data/`** *(신규)* | 더미 데이터(매장/베이/가격/매니저) | `stores.ts`, `prices.ts` | (명시 import) |
| **`app/composables/`** *(신규)* | 재사용 로직(Composition 함수) | `useToast.ts`, `useSlots.ts` | ✅ |
| **`app/services/`** *(신규)* | 데이터 접근 추상화 계층(2단계 교체 지점) | `reservationService.ts` | (명시 import) |

> 💡 **`services/`를 지금 만드는 이유**: 1단계에서는 더미 데이터를 직접 import해도 동작하지만, **데이터 접근을 `services/`로 한 번 감싸 두면** 2단계(Spring Boot API)로 넘어갈 때 컴포넌트/스토어를 거의 수정하지 않고 `services/` 내부 구현만 교체하면 됩니다. (4·5장 참조)
>
> 💡 **Nuxt 부트스트랩**: Vue3+Vite 시절의 `src/main.ts`(앱 진입점, `createApp`)는 **없습니다.** Nuxt가 앱 초기화·Pinia/Router 등록을 자동 처리하며, 루트 컴포넌트는 `app/app.vue`입니다. Pinia는 `nuxt.config.ts`의 `modules: ['@pinia/nuxt']`로 등록됩니다.

### 2.2 재사용 패턴 (기존 파일을 인용하세요)

- **파일 기반 라우팅 = 자동 코드 스플리팅**: Nuxt는 `app/pages/` 아래 파일을 라우트로 자동 등록하며, **각 페이지는 기본적으로 라우트 단위로 코드 스플리팅(lazy 로딩)** 됩니다. Vue3+Vite 시절의 `() => import('...')` 동적 import 라우터 설정은 **더 이상 작성하지 않습니다.** 파일을 `app/pages/`에 두기만 하면 됩니다. (예: `app/pages/about.vue` → `/about`)
- **Pinia setup 스토어**: `app/stores/counter.ts`의 `defineStore('counter', () => { ... })` 패턴을 따르세요. `ref`로 state, `computed`로 getter, 일반 함수로 action을 만들고 마지막에 `return`합니다. `app/stores/`에 두면 `useXxxStore`가 자동 임포트됩니다.
- **경로 별칭 `~`/`@`**: Nuxt 4에서 `~`와 `@`는 모두 **`app/`** 를 가리킵니다. 상대 경로(`../../`) 대신 `~/types/domain`(또는 `@/types/domain`)처럼 쓰세요. 본 문서 예시는 `~/` 를 기본으로 사용합니다.

### 2.3 코딩 컨벤션

| 항목 | 규칙 |
|---|---|
| 응답/주석/커밋/문서 | **한국어** |
| 변수명/함수명/타입명 | **영어** (코드 표준) |
| 세미콜론 | **없음** (oxfmt `semi: false`) |
| 따옴표 | **작은따옴표** (`singleQuote: true`) |
| 컴포넌트 작성 | Vue 3 `<script setup lang="ts">` Composition API (Nuxt 내장) |
| 임포트 경로 | `~/...`(또는 `@/...`) 별칭 우선 — 둘 다 `app/` |

---

## 3. Phase별 개발 로드맵 (1차 FO + 프론트 더미)

> 각 Phase는 **목표 / 선행조건 / 태스크 체크리스트 / 생성·수정 파일 / 예상 공수 / 완료기준(DoD) / require 참조** 공통 포맷을 따릅니다.

---

### Phase 1 — 도메인 타입 & 더미 데이터 레이어

> **공수: 2.0일** · **선행조건: Phase 0** · **require_v1.md 참조: 5장(도메인), 10장(가격)**

#### 목표
앱 전체가 공유할 **타입 시스템**과 **더미 데이터**, 그리고 **슬롯 생성/가격 조회 유틸**을 구축한다. 이후 모든 Phase의 기반이 된다.

#### 태스크 체크리스트
- [ ] `app/types/enums.ts` — enum/리터럴 유니언 정의 (역할, 차종, 서비스, 슬롯 상태, 예약 상태, **휴무유형 `DayoffType`**: FULL_DAY/SHIFT_1·2·3 — require 5.5)
- [ ] `app/types/domain.ts` — 엔티티 인터페이스 정의(require 5.4 표 기준): `User`, `Store`, `Bay`, `Slot`, `Reservation`, `Manager`, `Price`, `Review`, **`ManagerDayoff`(휴무 = 날짜 × 휴무유형)**
- [ ] `app/data/stores.ts` — 매장 여러 곳 + 베이 `A1~AN` 더미
- [ ] `app/data/managers.ts` — 매장별 매니저 N명(휴무 포함) 더미 — **전일/교대조 휴무 케이스를 섞어 구성**(`FULL_DAY` 1건 + `SHIFT_1`·`SHIFT_2` 교대조 휴무 케이스 포함, require 5.5)
- [ ] `app/data/prices.ts` — 차종 5 × 서비스 4 가격 매트릭스(require 10.3 확정 단가)
- [ ] `app/data/users.ts` — 로그인용 더미 사용자
- [ ] `app/composables/useSlots.ts` — 날짜별 30분 단위 슬롯 생성 유틸
- [ ] `app/services/priceService.ts` — 차종 × 서비스 단가 조회 유틸
- [ ] 가벼운 콘솔 검증(임시 스크립트 or 페이지)로 슬롯/가격 유틸 동작 확인

#### 생성·수정 파일
`app/types/enums.ts`, `app/types/domain.ts`, `app/data/stores.ts`, `app/data/managers.ts`, `app/data/prices.ts`, `app/data/users.ts`, `app/composables/useSlots.ts`, `app/services/priceService.ts`

#### 구현 예시 (oxfmt 스타일 — 세미콜론 없음, 작은따옴표)

`app/types/enums.ts`
```ts
// 사용자 역할 (require 3장)
export type UserRole = 'USER' | 'MANAGER' | 'STORE_ADMIN' | 'ADMIN'

// 차종 5분류 (require 10.1 확정)
export type CarType = 'LIGHT' | 'SMALL' | 'MID' | 'LARGE' | 'VAN_ETC'

// 서비스 4분류 (require 10.2 확정)
export type ServiceType = 'EXT' | 'INT' | 'FULL' | 'PREMIUM'

// 슬롯 상태 — 동시성 1단계 시뮬레이션의 핵심 (require 7.1)
export type SlotStatus = 'AVAILABLE' | 'HOLDING' | 'RESERVED' | 'COMPLETED'

// 예약 상태 — 상태 전이 (require 11.3)
export type ReservationStatus =
  | 'HOLDING' // 슬롯 점유(확정 전)
  | 'RESERVED' // 예약 확정(승인 후 가정)
  | 'COMPLETED' // 세차 완료
  | 'CANCELED' // 취소됨

// 매니저 휴무 유형 — 전일 또는 3교대 기반 교대조 단위 (require 5.5, v1.3)
// SHIFT_1 06:00~14:00 / SHIFT_2 14:00~22:00 / SHIFT_3 22:00~06:00(익일)
export type DayoffType = 'FULL_DAY' | 'SHIFT_1' | 'SHIFT_2' | 'SHIFT_3'
```

`app/types/domain.ts`
```ts
import type { CarType, DayoffType, ReservationStatus, ServiceType, SlotStatus, UserRole } from '~/types/enums'

export interface User {
  id: string
  email: string
  name: string
  role: UserRole
}

export interface Store {
  id: string
  name: string
  bayCount: number // 동일 시간대 최대 수용 = 베이 수 N (require 5.2)
  approved: boolean // 가입/승인된 매장만 노출 (require 6.1)
}

export interface Bay {
  id: string
  storeId: string
  code: string // 'A1' ~ 'AN'
}

// 매니저 휴무 1건 = (날짜, 휴무유형). 교대조 휴무는 같은 날짜에 복수 지정 가능 (require 5.5)
export interface ManagerDayoff {
  date: string // 'YYYY-MM-DD'
  type: DayoffType // FULL_DAY(전일) | SHIFT_1·2·3(교대조 단위 부분 휴무)
}

export interface Manager {
  id: string
  storeId: string
  name: string
  isStoreAdmin: boolean
  // v1.3: 전일 전용 string[] → 교대조 정보 포함 구조로 확장 (require 5.5, 6.1)
  dayoffs: ManagerDayoff[]
}

// 슬롯 = (매장, 베이, 날짜, 30분 시간단위), UNIQUE (require 5.2)
export interface Slot {
  storeId: string
  bayId: string
  date: string // 'YYYY-MM-DD'
  timeSlot: string // 'HH:mm' (30분 단위 시작 시각)
  status: SlotStatus
}

export interface Price {
  carType: CarType
  serviceType: ServiceType
  amount: number // 원
}

export interface Reservation {
  id: string
  userId: string
  storeId: string
  bayId: string
  managerId: string | null
  date: string
  timeSlot: string
  carType: CarType
  serviceType: ServiceType
  amount: number
  status: ReservationStatus
}

export interface Review {
  id: string
  reservationId: string
  userId: string
  storeId: string
  managerId: string | null
  rating: number // 1 ~ 5 (require 9.1)
  text: string
  createdAt: string
}
```

`app/data/prices.ts` (require 10.3 확정 단가)
```ts
import type { Price } from '~/types/domain'

// 차종 5 × 서비스 4 = 20개 단가 매트릭스 (require 10.3)
export const prices: Price[] = [
  { carType: 'LIGHT', serviceType: 'EXT', amount: 10000 },
  { carType: 'LIGHT', serviceType: 'INT', amount: 10000 },
  { carType: 'LIGHT', serviceType: 'FULL', amount: 18000 },
  { carType: 'LIGHT', serviceType: 'PREMIUM', amount: 28000 },
  { carType: 'SMALL', serviceType: 'EXT', amount: 12000 },
  { carType: 'SMALL', serviceType: 'INT', amount: 12000 },
  { carType: 'SMALL', serviceType: 'FULL', amount: 22000 },
  { carType: 'SMALL', serviceType: 'PREMIUM', amount: 32000 },
  { carType: 'MID', serviceType: 'EXT', amount: 15000 },
  { carType: 'MID', serviceType: 'INT', amount: 15000 },
  { carType: 'MID', serviceType: 'FULL', amount: 27000 },
  { carType: 'MID', serviceType: 'PREMIUM', amount: 38000 },
  { carType: 'LARGE', serviceType: 'EXT', amount: 18000 },
  { carType: 'LARGE', serviceType: 'INT', amount: 18000 },
  { carType: 'LARGE', serviceType: 'FULL', amount: 33000 },
  { carType: 'LARGE', serviceType: 'PREMIUM', amount: 45000 },
  { carType: 'VAN_ETC', serviceType: 'EXT', amount: 22000 },
  { carType: 'VAN_ETC', serviceType: 'INT', amount: 22000 },
  { carType: 'VAN_ETC', serviceType: 'FULL', amount: 40000 },
  { carType: 'VAN_ETC', serviceType: 'PREMIUM', amount: 55000 },
]
```

`app/services/priceService.ts`
```ts
import type { CarType, ServiceType } from '~/types/enums'
import { prices } from '~/data/prices'

// 차종 × 서비스 단가 조회 (require 10.3)
export function getPrice(carType: CarType, serviceType: ServiceType): number {
  const found = prices.find((p) => p.carType === carType && p.serviceType === serviceType)
  if (!found) {
    throw new Error(`단가를 찾을 수 없습니다: ${carType} / ${serviceType}`)
  }
  return found.amount
}
```

`app/composables/useSlots.ts` (30분 단위 슬롯 생성)
```ts
import type { Bay, Slot } from '~/types/domain'

// 하루(00:00 ~ 23:30)를 30분 단위 'HH:mm' 배열로 생성 (require 5.2)
export function generateTimeSlots(): string[] {
  const result: string[] = []
  for (let hour = 0; hour < 24; hour++) {
    for (const minute of [0, 30]) {
      const hh = String(hour).padStart(2, '0')
      const mm = String(minute).padStart(2, '0')
      result.push(`${hh}:${mm}`)
    }
  }
  return result
}

// 특정 매장의 특정 날짜 슬롯 전체 생성 (베이 × 시간단위)
export function generateSlotsForDate(storeId: string, bays: Bay[], date: string): Slot[] {
  const times = generateTimeSlots()
  const slots: Slot[] = []
  for (const bay of bays) {
    for (const timeSlot of times) {
      slots.push({ storeId, bayId: bay.id, date, timeSlot, status: 'AVAILABLE' })
    }
  }
  return slots
}
```

#### 완료기준 (DoD)
- [ ] `npm run type-check` 통과 (타입 정의에 오류 없음)
- [ ] 가격 매트릭스 20개 항목이 require 10.3과 정확히 일치
- [ ] `generateTimeSlots()`가 48개(24h × 2) 항목을 반환
- [ ] `getPrice('MID', 'FULL')`가 `27000`을 반환하는 것을 확인

---

### Phase 2 — 라우팅 & 레이아웃 골격

> **공수: 1.5일** · **선행조건: Phase 1** · **require_v1.md 참조: 12.4(화면 목록)**

#### 목표
1차 FO 화면 4종을 **파일 기반 라우팅(`app/pages/`)** 으로 등록하고, 공통 레이아웃(`app/layouts/default.vue`)/네비게이션과 **미인증 라우트 미들웨어 스텁**(`app/middleware/auth.ts`)을 만든다.

> 💡 **Vue3+Vite와의 차이**: 라우트를 `createRouter`/`routes` 배열로 수동 정의하지 않습니다. `app/pages/`에 파일을 두면 파일 경로가 곧 URL이 되며 자동 코드 스플리팅됩니다. 가드는 `router.beforeEach`가 아니라 **`defineNuxtRouteMiddleware`** 로 작성한 미들웨어를 페이지에서 `definePageMeta({ middleware: 'auth' })`로 연결합니다.

#### 태스크 체크리스트
- [ ] `app/pages/login.vue`, `app/pages/reserve.vue`, `app/pages/reservations.vue`, `app/pages/review/[reservationId].vue` 빈 골격 생성
- [ ] (선택) `app/pages/index.vue`에서 `/reserve`로 리다이렉트(`navigateTo('/reserve')`) 또는 홈 안내
- [ ] `app/components/AppNav.vue` 공통 네비게이션 (로그인/로그아웃/메뉴, `<NuxtLink>` 사용)
- [ ] `app/layouts/default.vue`에 레이아웃(네비 + `<slot />`) 배치, `app/app.vue`는 `<NuxtLayout><NuxtPage /></NuxtLayout>`
- [ ] `app/middleware/auth.ts` 라우트 미들웨어 스텁 작성 + 보호 페이지에서 `definePageMeta({ middleware: 'auth' })` (Phase 3에서 실제 인증과 연결)

#### 생성·수정 파일
`app/pages/login.vue`, `app/pages/reserve.vue`, `app/pages/reservations.vue`, `app/pages/review/[reservationId].vue`, `app/pages/index.vue`(수정), `app/components/AppNav.vue`, `app/layouts/default.vue`, `app/middleware/auth.ts`, `app/app.vue`(수정)

#### 라우트 정의 (브라우저 진입 URL ↔ 파일 경로)

Nuxt 파일 기반 라우팅에서 **파일 경로가 곧 URL**입니다. 동적 세그먼트는 `[param].vue` 표기를 씁니다.

| 화면 | 라우트 URL | 파일 경로 (`app/pages/`) | 가드 |
|---|---|---|---|
| 로그인 (FW2) | `/login` | `login.vue` | — |
| 예약 (FW3~FW5) | `/reserve` | `reserve.vue` | `middleware: 'auth'` |
| 예약 목록/취소/완료 (FW6/FW7) | `/reservations` | `reservations.vue` | `middleware: 'auth'` |
| 후기 작성 | `/review/:reservationId` | `review/[reservationId].vue` | `middleware: 'auth'` |

#### 구현 예시 — `app/app.vue` (레이아웃 + 페이지 슬롯)
```vue
<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
```

#### 구현 예시 — `app/layouts/default.vue` (네비 + 페이지 슬롯)
```vue
<template>
  <div class="app-shell">
    <AppNav />
    <main>
      <slot />
    </main>
  </div>
</template>
```

#### 구현 예시 — `app/middleware/auth.ts` (라우트 미들웨어 가드 스텁)
```ts
// 미인증 시 /login 으로 보내는 라우트 미들웨어 (require 4장)
// Phase 3에서 useAuthStore().isLoggedIn 으로 교체
export default defineNuxtRouteMiddleware((to) => {
  // TODO(Phase 3): const auth = useAuthStore(); const isLoggedIn = auth.isLoggedIn
  const isLoggedIn = false
  if (!isLoggedIn) {
    return navigateTo({ path: '/login', query: { redirect: to.fullPath } })
  }
})
```

#### 구현 예시 — 보호 페이지에서 미들웨어 연결 (`app/pages/reserve.vue`)
```vue
<script setup lang="ts">
definePageMeta({ middleware: 'auth' })
</script>

<template>
  <section>
    <!-- Phase 4·5에서 매장 선택 / 슬롯 그리드 구현 -->
  </section>
</template>
```

> 💡 모든 보호 페이지(`reserve`, `reservations`, `review/[reservationId]`)에 동일하게 `definePageMeta({ middleware: 'auth' })`를 선언합니다. (전역 적용을 원하면 파일명을 `auth.global.ts`로 바꿔 전역 미들웨어로 만들 수 있으나, 1차에서는 페이지별 명시 방식을 권장합니다.)

#### 완료기준 (DoD)
- [ ] 4개 라우트로 직접 이동 시 각 빈 화면이 렌더된다 (파일 기반 라우팅 동작 확인)
- [ ] `middleware: 'auth'` 페이지 접근 시 `/login`으로 리다이렉트된다(미들웨어 스텁 동작)
- [ ] `<NuxtLink>` 네비게이션이 동작하고, `default.vue` 레이아웃이 모든 페이지에 적용된다
- [ ] `npm run type-check`, `npm run lint` 통과

---

### Phase 3 — 인증 / 로그인 (FW2)

> **공수: 2.0일** · **선행조건: Phase 2** · **require_v1.md 참조: 4장(인증)**

#### 목표
더미 로그인 기반 `auth` 스토어를 만들고, 로그인 폼/검증/가드 연동을 완성한다. (이메일 인증·승인 워크플로우는 BO 영역이라 1차 범위 밖 — 더미 사용자로 로그인만 처리)

#### 태스크 체크리스트
- [ ] `app/stores/auth.ts` — setup 스토어(`currentUser`, `isLoggedIn`, `login`, `logout`)
- [ ] `app/pages/login.vue` — 이메일/비밀번호 폼, 필수값/형식 검증, 실패 시 에러 메시지
- [ ] 로그인 성공 시 `redirect` 쿼리 또는 `/reserve`로 이동(`navigateTo`)
- [ ] `app/middleware/auth.ts`를 `useAuthStore().isLoggedIn`으로 교체 (Phase 2 스텁 제거)
- [ ] `AppNav.vue`에 로그인 상태/로그아웃 버튼 반영
- [ ] (선택) 새로고침 유지를 위해 로그인 상태 저장 — ⚠️ **SSR 주의**(아래 박스 참조)

> ⚠️ **SSR 주의 (Nuxt 기본 SSR)**: `localStorage`는 **브라우저에만 존재**하므로 서버 렌더 시점에 접근하면 `ReferenceError`가 납니다. 로그인 상태 복원은 `if (import.meta.client) { ... }` 가드 안에서 수행하거나, 서버/클라이언트 모두에서 안전하게 동작하는 **`useCookie`** 사용을 권장합니다(가드 미들웨어가 SSR 단계에서도 인증 상태를 읽으려면 쿠키 방식이 더 안전).

#### 생성·수정 파일
`app/stores/auth.ts`, `app/pages/login.vue`(구현), `app/middleware/auth.ts`(가드 수정), `app/components/AppNav.vue`(수정)

#### 구현 예시 — auth 스토어 (counter.ts 패턴)
```ts
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '~/types/domain'
import { users } from '~/data/users'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<User | null>(null)
  const isLoggedIn = computed(() => currentUser.value !== null)

  // 더미 로그인 — 이메일/비밀번호 일치 시 성공 (1단계, require 4장)
  function login(email: string, password: string): boolean {
    const found = users.find((u) => u.email === email)
    // 1단계 더미: 비밀번호는 'password'로 통일 가정
    if (found && password === 'password') {
      currentUser.value = found
      return true
    }
    return false
  }

  function logout() {
    currentUser.value = null
  }

  return { currentUser, isLoggedIn, login, logout }
})
```

#### 구현 예시 — `app/middleware/auth.ts` (스토어 연결, Phase 2 스텁 교체)
```ts
// auth 스토어와 연결된 실제 가드 (require 4장)
export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn) {
    return navigateTo({ path: '/login', query: { redirect: to.fullPath } })
  }
})
```

#### 완료기준 (DoD)
- [ ] 올바른 더미 계정으로 로그인 성공 → `/reserve` 이동
- [ ] 잘못된 계정/형식 입력 시 에러 메시지 노출(폼 검증)
- [ ] 미인증 상태로 `/reserve` 접근 시 `/login`으로 리다이렉트
- [ ] 로그아웃 후 보호 라우트 접근 불가
- [ ] `npm run type-check`, `npm run lint` 통과

---

### Phase 4 — 매장/매니저 선택 + 검색 UX (FW3/FW4)

> **공수: 2.5일** · **선행조건: Phase 3** · **require_v1.md 참조: 6.3(검색 UX)**

#### 목표
재사용 가능한 `SearchableSelect`(select + 텍스트 입력 필터 검색)를 만들고, **승인된 매장만** 노출하며 매니저 선택 시 **휴무를 반영**한다.

#### 태스크 체크리스트
- [ ] `app/components/SearchableSelect.vue` — select 박스 + 텍스트 입력 시 실시간 필터링, `v-model` 지원
- [ ] `app/services/storeService.ts` — `approved === true` 매장만 반환, 매장별 매니저 조회
- [ ] `app/pages/reserve.vue`에 매장 선택(FW3) → 매니저 선택(FW4) 연동
- [ ] 매니저 선택 시 해당 매니저 `dayoffs`(전일/교대조 휴무)를 예약 컨텍스트로 전달(Phase 5 슬롯 비활성에 사용 — require 5.5)
- [ ] 키보드 접근성: 화살표 이동/Enter 선택(가능 범위)

#### 생성·수정 파일
`app/components/SearchableSelect.vue`, `app/services/storeService.ts`, `app/pages/reserve.vue`(수정)

#### 구현 예시 — SearchableSelect (핵심 로직 발췌)
```vue
<script setup lang="ts" generic="T">
import { computed, ref } from 'vue'

const props = defineProps<{
  options: T[]
  labelKey: keyof T
  valueKey: keyof T
  placeholder?: string
}>()

const model = defineModel<T[keyof T] | null>()

const keyword = ref('')
const open = ref(false)

// 텍스트 입력 시 필터 검색 (require 6.3)
const filtered = computed(() =>
  props.options.filter((o) =>
    String(o[props.labelKey]).toLowerCase().includes(keyword.value.toLowerCase()),
  ),
)

function select(option: T) {
  model.value = option[props.valueKey]
  keyword.value = String(option[props.labelKey])
  open.value = false
}
</script>

<template>
  <div class="searchable-select">
    <input
      v-model="keyword"
      :placeholder="placeholder"
      data-testid="searchable-input"
      @focus="open = true"
    />
    <ul v-if="open" data-testid="searchable-options">
      <li
        v-for="option in filtered"
        :key="String(option[valueKey])"
        @click="select(option)"
      >
        {{ option[labelKey] }}
      </li>
    </ul>
  </div>
</template>
```

#### 완료기준 (DoD)
- [ ] 매장 select에 텍스트 입력 시 목록이 실시간 필터링된다
- [ ] 승인되지 않은(`approved: false`) 매장은 목록에 나오지 않는다
- [ ] 매장 선택 후 해당 매장의 매니저만 매니저 select에 노출된다
- [ ] `npm run type-check`, `npm run lint` 통과

---

### Phase 5 — 예약 페이지 & 슬롯 그리드 + 동시성 시뮬레이션 (FW5) ★핵심

> **공수: 4.0일** · **선행조건: Phase 4** · **require_v1.md 참조: 5장(슬롯·5.5 교대/휴무), 6장(예약·6.1 베이 점유 선택불가), 7장(동시성 1단계)**

#### 목표
**날짜 선택 → 베이(행) × 30분(열) 슬롯 그리드**를 렌더하고, **클라이언트 슬롯 잠금 시뮬레이션 + 낙관적 갱신**으로 동시 예약 충돌을 시뮬레이션한다. 차종/서비스 선택 시 **가격 자동 계산**하고 **현장결제 안내**를 표시한다.

#### 태스크 체크리스트
- [x] `app/stores/reservation.ts` — 슬롯 맵(status 관리), `holdSlot`, `confirmReservation`, `releaseSlot`
- [x] `app/components/SlotGrid.vue` — 베이 × 시간(선택 시간대) 미니그리드, status별 색상/비활성, 클릭 시 HOLDING
- [x] 날짜 선택 UI (휠 — 오늘부터 21일)
- [x] 매니저 휴무(`dayoffs`) 반영 — **전일/교대조 구분**하여 슬롯 비활성 (require 5.5, 6.1)
  - 전일(`FULL_DAY`): 그날 **전체 슬롯 비활성**
  - 교대조(`SHIFT_1` 06:00~14:00 / `SHIFT_2` 14:00~22:00 / `SHIFT_3` 22:00~06:00): **해당 교대 시간대의 30분 슬롯만 비활성**, 그 외 시간대는 예약 가능 (운영은 24시간 유지)
  - 같은 날짜에 교대조 휴무가 복수 지정될 수 있음(예: `SHIFT_1` + `SHIFT_2`)
- [x] **베이 슬롯 점유 시 선택 불가**: 날짜·시간 선택 후, 그 `(날짜, 시간)`에서 **RESERVED/COMPLETED 상태인 베이를 미니 슬롯 그리드에서 비활성**으로 표시(선택 불가) — require 5.2, 6.1, 7장
  - 📌 **선택 순서는 현행 유지**(매장→매니저→차종→베이→날짜/시간). 베이 점유 충돌은 날짜·시간 확정 후 판정 가능하므로, **미니그리드 `disabled` 방식의 하이브리드 UI**로 점유 베이를 사전 차단한다(require 6.1 결정 사항).
- [x] 동일 시간대 최대 수용 = 베이 수 N 시각화 (require 5.2)
- [x] 차종/서비스 select → `getPrice()` 호출 → 금액 표시 + "현장결제만 가능" 안내 (require 6.4)
- [x] **낙관적 갱신**: 슬롯 클릭 즉시 `HOLDING` 표시 → 확정 직전 충돌 감지 → 충돌 시 토스트로 재선택 유도
- [x] `app/composables/useToast.ts` — 토스트 알림 (`useState` 기반 SSR 안전)
- [x] 확장 포인트(2·3단계 서버 검증 교체 지점)에 `// TODO` 주석 명시 (`reservationService.ts`·`confirmReservation`)

> ⚠️ **SSR 주의**: 1단계 슬롯 잠금은 클라이언트 상태(Pinia)로 시뮬레이션합니다. 토스트 표시·`window`/타이머·`localStorage` 등 **브라우저 전용 로직은 `import.meta.client` 가드** 또는 `onMounted` 안에서 실행하세요(서버 렌더 단계에서 충돌·예외 방지).

#### 생성·수정 파일
`app/stores/reservation.ts`, `app/components/SlotGrid.vue`, `app/composables/useToast.ts`, `app/pages/reserve.vue`(수정), `app/services/reservationService.ts`

#### 구현 예시 — reservation 스토어 (낙관적 갱신 + 충돌 감지)
```ts
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Reservation, Slot } from '~/types/domain'
import type { SlotStatus } from '~/types/enums'

// 슬롯 식별 키 = (매장, 베이, 날짜, 시간) — UNIQUE (require 5.2)
function slotKey(storeId: string, bayId: string, date: string, timeSlot: string): string {
  return `${storeId}|${bayId}|${date}|${timeSlot}`
}

export const useReservationStore = defineStore('reservation', () => {
  // 슬롯 상태 맵 (1단계 클라이언트 시뮬레이션, require 7.1)
  const slotStatus = ref<Record<string, SlotStatus>>({})
  const reservations = ref<Reservation[]>([])

  function getStatus(slot: Slot): SlotStatus {
    return slotStatus.value[slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)] ?? 'AVAILABLE'
  }

  // 슬롯 선택 → 즉시 HOLDING (낙관적 갱신, require 7.1 1단계)
  function holdSlot(slot: Slot): boolean {
    const key = slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
    const current = slotStatus.value[key] ?? 'AVAILABLE'
    // 충돌 감지: 이미 다른 흐름이 점유/예약 중이면 실패
    if (current === 'RESERVED' || current === 'COMPLETED' || current === 'HOLDING') {
      return false
    }
    slotStatus.value[key] = 'HOLDING'
    return true
  }

  // 확정 직전 충돌 재검사 후 RESERVED 전이
  // TODO(2단계): 이 검증을 서버(reservationService)로 위임 — UNIQUE 제약 + 락
  // TODO(3단계): MySQL 트랜잭션 + 유니크 인덱스로 원천 차단
  function confirmReservation(reservation: Reservation): boolean {
    const key = slotKey(reservation.storeId, reservation.bayId, reservation.date, reservation.timeSlot)
    if (slotStatus.value[key] !== 'HOLDING') {
      return false // 충돌 — 재선택 유도
    }
    slotStatus.value[key] = 'RESERVED'
    reservations.value.push({ ...reservation, status: 'RESERVED' })
    return true
  }

  function releaseSlot(slot: Slot) {
    const key = slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
    if (slotStatus.value[key] === 'HOLDING') {
      slotStatus.value[key] = 'AVAILABLE'
    }
  }

  const reservedCount = computed(() => reservations.value.length)

  return { slotStatus, reservations, reservedCount, getStatus, holdSlot, confirmReservation, releaseSlot }
})
```

> 🔎 동시성 흐름의 자세한 설명은 **4장(동시성 처리 구현 가이드)** 를 참조하세요.

#### 완료기준 (DoD)
- [x] 날짜·시간 선택 후 선택 시간대 베이 미니그리드가 렌더된다 (베이 수 = 동일 시간대 최대 수용)
- [x] 슬롯(베이) 클릭 시 즉시 HOLDING(시각적 변화)으로 표시된다
- [x] 매니저 **전일 휴무** 날짜는 날짜 휠에서 비활성으로 표시되어 선택 불가
- [x] 매니저 **교대조 휴무**(SHIFT_1·2·3) 시 해당 교대 시간대의 30분 슬롯만 시간 휠에서 비활성되고, 다른 시간대는 선택 가능하다 (require 5.5, 6.1)
- [x] 날짜·시간 선택 후, 그 시간대에 **예약된(RESERVED/COMPLETED) 베이가 미니 슬롯 그리드에서 비활성**으로 표시되어 선택 불가하다 (require 5.2, 6.1, 7장)
- [x] 차종/서비스 선택 시 금액이 자동 계산되고 "현장결제만" 안내가 보인다
- [x] HOLDING 상태가 아닌 슬롯 확정 시도 시 토스트로 재선택을 유도한다
- [x] `npm run type-check`, `npm run lint` 통과 (+ `npm run test:e2e` reserve 9/9 통과)

#### 구현 메모 (Phase 5 완료 시점)

- **하이브리드 UI 확정**: 날짜/시간은 **휠(WheelPicker)** 로 선택하고, 휠을 대체하지 않는다. 선택한 `(날짜, 시간)`에 한해 **미니 SlotGrid**(베이 N칸)를 추가로 렌더하여 점유 현황 시각화 + 베이 선택/HOLDING을 담당한다. ROADMAP 초안의 "베이 × 30분 전체 그리드"는 미니그리드(선택 시간대 1열)로 구현했다.
- **데이터 접근**: 슬롯 점유 더미 시드는 `app/services/reservationService.ts`(단방향 의존)가 제공하고, 상태 변이(`holdSlot`/`confirmReservation`/`releaseSlot`)는 `reservation` 스토어가 소유한다. 검증 위치를 클라이언트 → 서버 → DB로 옮겨도 UI 무변경(4.4 교체 지점 주석 유지).
- **명세 v1 미해결 질문(Q1~Q8)은 본 Phase 범위 외**: 차종↔베이 4등급/특대형(D1) 매핑·가격 영향 등([`docs/예약_규칙_명세_v1.md`](./예약_규칙_명세_v1.md) 8장)은 확정 전이므로 **임의 구현하지 않는다**. 베이 노출은 현행 `getBaysForCar` **누적 수용 로직을 그대로 유지**하며, 확정 후 별도 태스크로 반영한다.

---

### Phase 6 — 예약 확정/취소/세차완료 상태 (FW6/FW7)

> **공수: 2.5일** · **선행조건: Phase 5** · **require_v1.md 참조: 11.3(프로세스), 8장(상태 머신)**

#### 목표
예약 목록 화면에서 **상태 전이**(HOLDING→RESERVED→COMPLETED, 취소)를 다루고, **취소 2케이스**(승인 전/후 — MVP는 더미 상태값)를 구현한다.

#### 태스크 체크리스트
- [ ] `app/pages/reservations.vue` — 내 예약 목록(상태 뱃지, 금액, 매장/날짜/시간)
- [ ] 세차완료 처리: `RESERVED → COMPLETED` 액션 (1단계 더미; 실제로는 매니저 BO 영역, 시뮬레이션 버튼)
- [ ] 취소 케이스 1 (승인 후 취소): `RESERVED → CANCELED` + 슬롯 release (FW7)
- [ ] 취소 케이스 2 (승인 전 취소): `HOLDING → CANCELED` + 슬롯 release (FW7)
- [ ] 취소/완료 시 reservation 스토어 슬롯 status 동기화 (그리드 재방문 시 반영)
- [ ] 상태 전이 가드: 불가능한 전이(예: COMPLETED→CANCELED) 차단

#### 생성·수정 파일
`app/pages/reservations.vue`(구현), `app/stores/reservation.ts`(액션 추가: `completeReservation`, `cancelReservation`)

#### 상태 전이 표

| 현재 상태 | 액션 | 다음 상태 | 슬롯 처리 | 케이스 |
|---|---|---|---|---|
| `HOLDING` | 취소 | `CANCELED` | `AVAILABLE`로 release | 취소 케이스 2(승인 전) |
| `RESERVED` | 취소 | `CANCELED` | `AVAILABLE`로 release | 취소 케이스 1(승인 후) |
| `RESERVED` | 세차완료 | `COMPLETED` | `COMPLETED` 고정 | FW6 |
| `COMPLETED` | (불가) | — | — | 후기 작성만 가능 |

#### 완료기준 (DoD)
- [ ] 예약 목록에서 각 예약의 상태가 뱃지로 정확히 표시된다
- [ ] RESERVED 예약을 세차완료로 전이할 수 있다
- [ ] 승인 전/후 취소 2케이스가 모두 동작하고 슬롯이 다시 AVAILABLE이 된다
- [ ] 불가능한 상태 전이가 차단된다
- [ ] `npm run type-check`, `npm run lint` 통과

---

### Phase 7 — 후기/평점 (1차 확장)

> **공수: 2.0일** · **선행조건: Phase 6** · **require_v1.md 참조: 9장(후기/평점)**

#### 목표
**세차완료(COMPLETED) 예약만** 후기를 작성할 수 있게 하고, 평점(1~5)과 **매장/매니저별 평균 집계**를 표시한다.

#### 태스크 체크리스트
- [ ] `app/stores/review.ts` — `reviews`, `addReview`, `averageByStore`, `averageByManager`
- [ ] `app/pages/review/[reservationId].vue` — 평점(1~5 별점 or select), 텍스트 입력, 작성 자격 검증
- [ ] 작성 자격 가드: 해당 예약이 `COMPLETED`이고 본인 예약일 때만 작성 (require 9.1)
- [ ] `app/pages/reservations.vue` 완료 예약에 "후기 작성" 링크(`<NuxtLink :to="`/review/${reservationId}`">`)
- [ ] 매장/매니저별 평균 평점 표시(예약 화면 or 별도 영역)

> 💡 **동적 라우트 파라미터 접근**: `app/pages/review/[reservationId].vue`에서 `const route = useRoute(); const reservationId = route.params.reservationId` 로 파라미터를 읽습니다(vue-router의 `useRoute`를 Nuxt가 자동 임포트).

#### 생성·수정 파일
`app/stores/review.ts`, `app/pages/review/[reservationId].vue`(구현), `app/pages/reservations.vue`(수정)

#### 구현 예시 — 평점 집계 (computed)
```ts
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Review } from '~/types/domain'

export const useReviewStore = defineStore('review', () => {
  const reviews = ref<Review[]>([])

  function addReview(review: Review) {
    reviews.value.push(review)
  }

  // 매장별 평균 평점 (require 9.1)
  const averageByStore = computed(() => {
    const map: Record<string, { sum: number; count: number }> = {}
    for (const r of reviews.value) {
      const acc = map[r.storeId] ?? { sum: 0, count: 0 }
      acc.sum += r.rating
      acc.count += 1
      map[r.storeId] = acc
    }
    return Object.fromEntries(
      Object.entries(map).map(([storeId, { sum, count }]) => [storeId, sum / count]),
    )
  })

  return { reviews, addReview, averageByStore }
})
```

#### 완료기준 (DoD)
- [ ] COMPLETED가 아닌 예약은 후기 작성 화면에 진입할 수 없다(가드)
- [ ] 평점 범위(1~5) 외 값은 제출 불가
- [ ] 후기 작성 후 매장/매니저 평균 평점이 갱신된다
- [ ] `npm run type-check`, `npm run lint` 통과

---

### Phase 8 — E2E 테스트 & 마무리

> **공수: 2.0일** · **선행조건: Phase 1~7** · **참조: 본 문서 6장(Playwright)**

#### 목표
Playwright E2E를 설치/설정하고, 핵심 FO 플로우 시나리오를 작성·통과시키며, 린트/타입/포맷을 정리한다.

#### 태스크 체크리스트
- [ ] Playwright 설치 및 `playwright.config.ts` 설정 (6장 참조)
- [ ] `package.json`에 `test:e2e` 스크립트 추가
- [ ] 핵심 시나리오 작성: 로그인, 검색 필터, 슬롯 예약, **동시 충돌 재선택**, 취소, 후기 작성 (6장 체크리스트)
- [ ] 컴포넌트에 `data-testid` 부여(테스트 셀렉터 안정화)
- [ ] `npm run lint`, `npm run type-check`, `npm run format` 전체 통과
- [ ] `README.md`/문서 업데이트(실행 방법, 더미 계정 안내)

#### 완료기준 (DoD)
- [ ] `npm run test:e2e`가 모든 핵심 시나리오를 통과
- [ ] `npm run lint`, `npm run type-check` 통과, `npm run format` 적용됨
- [ ] README에 실행/테스트/더미 계정 안내가 반영됨

---

## 4. 동시성 처리 구현 가이드 (1단계 집중)

> **require_v1.md 참조: 7장**

### 4.1 1단계 핵심 — 클라이언트 슬롯 잠금 시뮬레이션 + 낙관적 갱신

1단계에는 백엔드가 없으므로 **클라이언트 상태(Pinia)** 로 동시성을 시뮬레이션합니다. 핵심은 슬롯 `status` enum의 전이입니다.

```
AVAILABLE ──(슬롯 클릭/낙관적 점유)──▶ HOLDING ──(확정 성공)──▶ RESERVED ──(세차완료)──▶ COMPLETED
    ▲                                    │
    └──────(release/취소/충돌)────────────┘
```

### 4.2 낙관적 갱신 흐름 (Optimistic Update)

| 단계 | 동작 | 코드 위치 |
|---|---|---|
| ① 선택 | 슬롯 클릭 → **즉시** `HOLDING`으로 UI 갱신 (서버 응답 안 기다림) | `reservation.holdSlot()` |
| ② 입력 | 차종/서비스 선택, 가격 확인 | `app/pages/reserve.vue` |
| ③ 확정 직전 | `status === 'HOLDING'` 인지 **재검사**(충돌 감지) | `reservation.confirmReservation()` |
| ④-A 성공 | `HOLDING → RESERVED`, 예약 목록에 추가 | `confirmReservation()` |
| ④-B 충돌 | HOLDING이 아니면 실패 반환 → **토스트로 재선택 유도** + 그리드 새로고침 | `useToast()` + 그리드 재조회 |

### 4.3 충돌 시 UX

- 확정 시점에 슬롯이 이미 `RESERVED`/`HOLDING`이면 토스트: "선택하신 슬롯이 방금 예약되었습니다. 다른 슬롯을 선택해 주세요." 노출.
- 그리드를 즉시 새로고침하여 최신 status를 반영하고, 점유 실패한 슬롯의 HOLDING을 release합니다.

### 4.4 2·3단계로 확장 시 바뀌는 지점 (코드 교체 포인트)

> 현재 코드의 `// TODO(2단계)` / `// TODO(3단계)` 주석 위치가 그대로 교체 대상입니다.

| 단계 | 바뀌는 지점 | 변경 내용 |
|---|---|---|
| **2단계** (Spring Boot 더미) | `confirmReservation()` 내부 검증 | 클라이언트 검증 → `reservationService`를 통한 **서버 호출**로 위임. 서버에서 슬롯 `UNIQUE(storeId, bayId, date, timeSlot)` + 낙관적(`@Version`)/비관적 락 적용 |
| **3단계** (MySQL) | `reservationService` 백엔드 구현 | DB **트랜잭션 + 유니크 인덱스**로 중복 INSERT 원천 차단. 충돌 시 409 → 동일 재선택 UX 재사용 |

> 💡 **핵심 설계 의도**: 컴포넌트와 스토어는 `reservationService`만 호출하도록 두면, 검증 위치가 클라이언트 → 서버 → DB로 이동해도 **UI 코드는 거의 그대로** 유지됩니다.

---

## 5. 데이터 진화 2·3단계 마일스톤 개요 (요약)

> 상세 작업은 **별도 로드맵**으로 분리 예정. 여기서는 방향만 제시합니다.

### 5.1 2단계 — Spring Boot 인메모리 더미

- **준비 작업(1단계에서 미리)**: 더미 데이터 접근부를 모두 `app/services/`(API 클라이언트 형태)로 추상화. 컴포넌트/스토어는 `services`만 의존하게 하여 교체 비용 최소화. (Nuxt에서는 서버 호출을 `$fetch`/`useFetch`로 전환하기 용이)
- **서버 측**: 슬롯 `UNIQUE(storeId, bayId, date, timeSlot)` 제약 + 낙관적 락(`@Version`) 또는 비관적 락(`SELECT ... FOR UPDATE`). 동시 요청 시 한 건만 성공.
- **인증**: 더미 로그인 → 실제 인증 토큰 흐름으로 점진 교체.

### 5.2 3단계 — MySQL

- 트랜잭션 + **유니크 인덱스**로 중복 예약 원천 차단.
- 충돌 시 서버 409 응답 → 1단계에서 만든 "재선택 유도 UX" 그대로 재사용.
- 데이터 영속화, 후기/평점 집계 쿼리 이관.

---

## 6. Playwright E2E 테스트 전략

> ⚠️ **현재 Playwright는 미설치 상태입니다.** 아래 설치 가이드부터 진행하세요.

### 6.1 설치 & 설정

```sh
# 방법 A: 대화형 셋업 (권장)
npm init playwright@latest

# 방법 B: 수동 설치
npm install -D @playwright/test
npx playwright install
```

`playwright.config.ts` (baseURL을 Nuxt dev 서버로, dev 서버 자동 기동)
```ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  // 테스트 시작 시 Nuxt dev 서버 자동 기동 (포트 3000)
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
  },
})
```

> ⚠️ **포트 주의**: Nuxt 기본 dev 포트는 **3000**입니다(Vite의 5173 아님). `webServer.url`과 `use.baseURL`을 모두 3000으로 맞추세요. dev 서버 첫 기동(SSR 빌드 워밍업)이 느릴 수 있으니, 필요 시 `webServer.timeout`을 넉넉히 늘리세요.

`package.json`에 스크립트 추가 (현재 없음 — 추가 필요):
```jsonc
{
  "scripts": {
    "test:e2e": "playwright test",
    "test:e2e:ui": "playwright test --ui"
  }
}
```

### 6.2 시나리오 체크리스트

- [ ] **로그인 성공**: 더미 계정 입력 → `/reserve`로 이동, 네비에 사용자 표시
- [ ] **로그인 실패**: 잘못된 비밀번호 → 에러 메시지 노출, 이동하지 않음
- [ ] **미인증 가드**: 로그아웃 상태로 `/reserve` 진입 → `/login`으로 리다이렉트
- [ ] **매장 검색 필터**: select에 키워드 입력 → 일치 매장만 노출
- [ ] **매니저 검색 + 휴무**: 매장 선택 후 매니저 노출, 휴무일 슬롯 비활성 확인
- [ ] **매니저 전일 휴무**: `FULL_DAY` 휴무 날짜 선택 시 그날 전체 슬롯이 비활성 (require 5.5)
- [ ] **매니저 교대조 휴무**: `SHIFT_1`(06:00~14:00) 휴무 매니저 선택 시 해당 시간대 30분 슬롯만 비활성, 14:30 등 타 시간대는 선택 가능 (require 5.5, 6.1)
- [ ] **슬롯 예약 성공**: 날짜→슬롯 클릭(HOLDING)→차종/서비스 선택(가격 확인)→확정→예약 목록에 RESERVED
- [ ] **베이 슬롯 점유 선택불가**: 날짜·시간 선택 후, 그 시간대에 RESERVED/COMPLETED인 베이가 미니 그리드에서 비활성(disabled)으로 표시되어 클릭 불가 (require 5.2, 6.1, 7장)
- [ ] **점유 베이 충돌 재선택**: 점유된 베이 대신 다른 활성 베이를 선택해 정상 예약되는 흐름 확인 (베이 점유 사전 차단 ↔ 아래 동시성 충돌과 구분)
- [ ] **동시 예약 충돌 → 재선택 유도**: 이미 RESERVED/HOLDING 슬롯 확정 시도 → 토스트 노출, 예약 미생성
- [ ] **예약 취소(승인 전/후)**: 취소 → 상태 CANCELED, 슬롯 다시 AVAILABLE
- [ ] **세차완료 → 후기 작성**: RESERVED→COMPLETED 전이 → 후기 작성(평점 1~5) → 평균 평점 갱신
- [ ] **후기 작성 자격**: COMPLETED 아닌 예약은 후기 화면 진입 차단

### 6.3 테스트 작성 팁

- **셀렉터 컨벤션**: `data-testid` 속성 사용. 텍스트/클래스 기반 셀렉터는 깨지기 쉬우니 지양.
  ```ts
  await page.getByTestId('searchable-input').fill('강남')
  await expect(page.getByTestId('searchable-options')).toContainText('강남점')
  ```
- **결정적(deterministic) 테스트**: 더미 데이터(`app/data/`)는 고정값이므로, 테스트는 특정 매장/슬롯/가격을 단정(assert)할 수 있습니다. 랜덤/현재시간 의존을 피하고 고정 날짜를 사용하세요.

### 6.4 동시 충돌 테스트 예시 (oxfmt 스타일)
```ts
import { expect, test } from '@playwright/test'

test('동시 예약 충돌 시 재선택을 유도한다', async ({ page }) => {
  await page.goto('/login')
  await page.getByTestId('email').fill('user@test.com')
  await page.getByTestId('password').fill('password')
  await page.getByTestId('login-submit').click()

  await page.goto('/reserve')
  // 첫 번째 슬롯 점유 후, 동일 슬롯을 강제로 RESERVED 상태로 만든 시나리오 가정
  const slot = page.getByTestId('slot-store1-bay1-0900')
  await slot.click()
  await page.getByTestId('confirm-reservation').click()
  // 같은 슬롯 재확정 시도 → 충돌 토스트
  await slot.click()
  await page.getByTestId('confirm-reservation').click()
  await expect(page.getByTestId('toast')).toContainText('다른 슬롯을 선택')
})
```

---

## 7. 코딩 컨벤션 & PR 체크리스트

### 7.1 브랜치 전략

| 브랜치 | 용도 |
|---|---|
| `main` | 배포 가능한 안정 상태 |
| `feat/phase{N}-{설명}` | Phase별 기능 개발 (예: `feat/phase5-slot-grid`) |
| `fix/{설명}` | 버그 수정 |

> 작은 단위 PR 원칙: 한 PR = 한 Phase의 일부 태스크. 리뷰하기 쉬운 크기로 쪼개세요.

### 7.2 커밋 메시지 규칙 (한국어)

```
feat(phase5): 슬롯 그리드 낙관적 갱신 구현

- 슬롯 클릭 시 즉시 HOLDING 표시
- 확정 직전 충돌 감지 후 재선택 토스트
```

- 형식: `<타입>(<범위>): <한국어 요약>` (타입: feat/fix/refactor/test/docs/chore)
- 본문은 한국어. 변수/함수/타입명만 영어.

### 7.3 PR 전 셀프 체크리스트

- [ ] `npm run lint` 통과 (oxlint + eslint)
- [ ] `npm run type-check` 통과 (`nuxt typecheck`)
- [ ] `npm run format` 적용 (세미콜론 없음 / 작은따옴표 확인 — `oxfmt app/`)
- [ ] 관련 Playwright E2E 시나리오 통과 (`npm run test:e2e`)
- [ ] 해당 Phase의 **DoD를 모두 충족**
- [ ] 신규 코드에 세미콜론이 없는지, `~/`(또는 `@/`) 별칭을 썼는지 확인
- [ ] 주석/커밋은 한국어, 식별자는 영어

### 7.4 리뷰 포인트

- 타입 안정성(any 남용 금지), 슬롯 status 전이 정확성, 더미 데이터 접근이 `services/`로 추상화되었는지(2단계 대비).

---

## 8. 부록 — require_v1.md 추적 매핑

| ROADMAP Phase | 화면/기능 | require 섹션 | 프로세스 코드 |
|---|---|---|---|
| Phase 1 | 도메인 타입·더미 데이터·가격 | 5장, 10장 | — |
| Phase 2 | 라우팅·레이아웃 골격 | 12.4 | — |
| Phase 3 | 로그인 | 4장 | **FW2** |
| Phase 4 | 매장/매니저 선택 + 검색 | 6.3 | **FW3, FW4** |
| Phase 5 | 예약·슬롯 그리드·동시성 1단계 (매니저 교대/휴무·베이 점유 선택불가 포함) | 5장(5.2·**5.5**)·6장(**6.1**)·7장 | **FW5** |
| Phase 6 | 세차완료/예약취소 | 11.3, 8장 | **FW6, FW7** |
| Phase 7 | 후기/평점 | 9장 | (S6 연계 — BO는 2차) |
| Phase 8 | E2E 테스트 | (본 문서 6장) | — |
| 2·3단계(개요) | 데이터 진화 | 7·12장 | M/S 프로세스(2차) |

> **BO(매니저/관리자) 프로세스(M3~M7, S3~S8)는 require 2.2에 따라 2차 과제(문서화만)** 이며 본 1차 MVP 로드맵 범위 밖입니다.

> ℹ️ **require_v1.md 12장(화면/스택) 참조 시 주의**: require_v1.md 문서의 FE 스택 표기는 작성 당시 Vue3+Vite 기준입니다. **현재 FE 스택은 Nuxt 4로 변경됨(본 로드맵 v1.2)** — 라우팅/디렉터리/명령어는 본 로드맵을 정본(Source of Truth)으로 따르세요.

---

> **문서 끝.** 본 로드맵은 require_v1.md v1.1을 기준으로 작성되어 **v1.3**(매니저 3교대 부분 휴무·베이 슬롯 점유 선택불가 규칙)까지 반영했으며, 1차 FO + 프론트 더미 데이터 단계에 집중합니다.
