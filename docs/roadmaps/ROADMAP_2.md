# 자동차 세차 예약 서비스 (MVP) 개발 로드맵 — 2차 백엔드 진화(Spring Boot) + BO 전체

> **문서 버전**: v2.2 (require_v1.md **v1.7** 정합 — 4역할·가입 2단계 승인·휴가/반차 1단계 승인·역할별 BO 페이지 분리)
> **작성일**: 2026-06-20 (최종 수정: 2026-06-21)
> **작성자**: PM/PL
>
> **🔄 v2.2 변경(require v1.7 정합 — 결재 단계 수 재정립)**: require_v1.md가 **v1.7**로 갱신되어, v1.6의 "3역할 단순화·승인 제거"가 **번복**되고 역할이 **4역할(`USER`/`MANAGER`/`STORE_ADMIN`/`ADMIN`)** 로 세분화, **매장매니저관리자(`STORE_ADMIN`)** 가 부활했습니다. 본 로드맵을 v1.7에 정합하도록 다음을 정정합니다:
> - **가입 승인 = 2단계**(매장매니저관리자 1차 M7 → 관리자 2차 S3, `ACTIVE` 전 로그인 불가, 상태 `PENDING_APPROVAL_L1 → L2`). Phase 3 가입 상태머신·Phase 7 가입 승인 흐름에 반영.
> - **휴가/반차(매니저 휴무) 승인 = 1단계**(매장매니저관리자 `STORE_ADMIN`가 `SUBMITTED → APPROVED`로 **종결**, **관리자 개입 없음**). **기존 Phase 7의 "2단계 결재(최고매니저 → 관리자, `APPROVED_L1 → APPROVED_L2 → CONFIRMED`)"는 v1.7과 충돌하므로 1단계로 전면 정정**합니다(require v1.7 §8.2·§8.3). ⚠️ 가입 승인(2단계)과 휴가/반차 승인(1단계)의 **단계 수가 다름**에 유의.
> - **프로세스 코드 재정의(require v1.7 §11.1)**: M6=휴가/반차 신청, M7=일반매장매니저 가입 1차 승인(매장매니저관리자), **M8=휴가/반차 승인(신설, 매장매니저관리자 1단계 종결)**, S3=매니저 가입 2차 최종 승인(관리자), **S9=매장별 매니저 근무상태 확인(신설)**. 구 "M6=예약승인" 표기 제거.
> - **역할별 BO 페이지 4그룹 분리**(require v1.7 §12.4): 일반매장매니저/매장매니저관리자/관리자별 화면 경로를 Phase 6~8에 반영.
>
> ✅ **구현 현황(2026-06-21 v1.7 재정합 완료)**: 기존 BE 구현의 v1.6/구 v2.1 가정(휴무 2단계 결재 `APPROVED_L1→L2`)을 **v1.7로 재정합 완료**했습니다 — 휴가/반차 1단계 collapse(`DayoffApprovalStatus`, STORE_ADMIN 종결), 가입 2단계 승인(M7→S3, `UserApprovalStatus`, `ACTIVE`만 로그인), 역할별 BO 페이지 4그룹 분리(§12.4, `/store-admin/*`·`/admin/manager-approvals` 신설), 로그인/AppNav 역할 정합. BE `./gradlew build`·FE `type-check`·`lint`·`test:e2e`(33건) 전건 통과(Phase 7 구현 메모 참조).
>
> **🔧 v2.1 변경(DB 접근 기술 확정)**: require_v1.md **v1.5** 결정에 따라 백엔드 영속 계층을 **MyBatis**(`mybatis-spring-boot-starter`, 매퍼 인터페이스 + XML SQL)로 확정한다. **JPA/Hibernate는 사용하지 않는다.** 이에 따라 본 로드맵의 패키지 구조(`entity/`→`domain/`, `repository/`(JpaRepository)→`mapper/`(`@Mapper`+XML)), 동시성 락(`@Version`→version 컬럼 비교 UPDATE, `@Lock(PESSIMISTIC_WRITE)`→매퍼 `SELECT ... FOR UPDATE` SQL), 스키마 관리(`ddl-auto`→`schema.sql`/Flyway 직접 관리)를 MyBatis 기준으로 기술한다.
> **대상 독자**: 주니어 ~ 시니어 **백엔드·풀스택** 개발자
> **연계 문서**: [`docs/require_v1.md`](../require_v1.md) (요구사항 정의서 **v1.7** 기준), [`docs/예약_규칙_명세_v1.md`](../예약_규칙_명세_v1.md) (예약 규칙 보강 명세 — 8장 미해결 질문 Q1~Q8), [`docs/roadmaps/ROADMAP_1.md`](./ROADMAP_1.md) (1차 FO + 프론트 더미)
> **범위**: 데이터 진화 **2단계(Spring Boot 인메모리 더미)** + **3단계(MySQL)** + **BO 전체(일반매장매니저·매장매니저관리자·관리자 프로세스 M3~M8·S3~S9, 가입 2단계 승인·휴가/반차 1단계 승인 워크플로우, SMTP/알림 인프라)** 집중 상세화. 1차(FO + 프론트 더미)는 [ROADMAP_1.md](./ROADMAP_1.md)에서 완료된 자산으로 전제한다.
>
> **🔄 v2.0 신설 배경**: 1차([ROADMAP_1.md](./ROADMAP_1.md), Nuxt 4 FO + 프론트엔드 더미 데이터)가 Phase 8(E2E 마무리)까지 완료되었습니다. 이제 **실제 백엔드(Spring Boot)** 를 도입하여 동시성·인증·영속화를 서버/DB 레벨로 끌어올리고, 1차에서 **문서화만** 했던 **BO(매니저·관리자) 기능 전체**(require 2.2)를 구현합니다.
>
> **🧩 additive(증분) 철학**: 1차 자산(Nuxt 4 화면·스토어·Pinia 상태·E2E 시나리오)을 **삭제하지 않고 유지**합니다. 변경은 **서비스 추상화 계층(`app/services/*`) 내부**에 한정합니다 — 1차에서 더미 데이터를 직접 반환하던 `services/`를 **`$fetch`/`useFetch` 기반 API 클라이언트**로 교체하면, 컴포넌트·스토어·페이지는 거의 무수정으로 동작합니다(ROADMAP_1 2.1·5.1 "교체 비용 최소화" 설계 의도의 회수 지점). 동시성 검증 위치는 ROADMAP_1 4.4의 `// TODO(2단계)`/`// TODO(3단계)` 주석을 **그대로 이어받아** 클라이언트 → 서버 → DB로 이동합니다.
>
> **⚠️ require 12장 스택 주의**: require_v1.md 12.2절(백엔드)은 "Java(LTS) + Spring Boot(최신 무료 버전), 데이터 진화 2단계에서 도입"으로 확정되어 있습니다. 본 로드맵의 BE 스택은 require 12.2를 정본으로 따릅니다.

---

## 0. 로드맵 사용 가이드

### 0.1 이 문서를 읽는 법

이 문서는 **위에서 아래로 순서대로** 읽고 따라오면 2차(백엔드 진화 + BO)가 완성되도록 설계되었습니다. 1차를 마치고 백엔드에 처음 합류한 주니어 개발자라면:

1. **0장(사용 가이드) → 1장(2차 환경 셋업) → 2장(아키텍처/컨벤션)** 을 먼저 읽고 손으로 따라 하세요. 여기까지가 "2차 출발선"입니다(FE :3000 + BE :8080 동시 기동).
2. **3장 Phase 0 → Phase 10** 을 순서대로 진행하세요. 각 Phase는 **선행 Phase가 끝나야** 시작할 수 있습니다(의존성 명시). 특히 **Phase 0(명세 확정 Q1~Q8)** 의 결정이 Phase 1 데이터 모델 개정에 직접 반영되므로 건너뛰지 마세요.
3. 막히는 동시성/락 로직은 **4장(동시성 처리 구현 가이드 2·3단계)** 를, MySQL 이행은 **5장**, 테스트는 **6장(Spring 통합테스트 + Playwright 회귀)** 을 펼쳐 보세요.
4. PR 올리기 전에 **7장 PR 셀프 체크리스트**(Java + TS)를 반드시 확인하세요.

### 0.2 require_v1.md 와의 연계 (섹션 매핑 안내)

각 Phase 머리말에 `> require_v1.md 참조: N장` 형식으로 근거 섹션을, 프로세스 코드(`M#`/`S#`)를 함께 표기했습니다. 구현 중 "왜 이렇게 해야 하지?"가 생기면 해당 요구사항 섹션을 펼쳐 보세요. 전체 추적 매핑은 **8장 부록**에 정리되어 있습니다.

> 💡 **BO 범위 근거**: 1차에서 "2차 과제(문서화만)"로 분류했던 BO 기능(require 2.2)을 본 로드맵에서 구현합니다. *(require v1.7 프로세스)* BO M3~M8(대행·세차완료 보조·취소 보조·**휴가/반차 신청(M6)**·**가입 1차 승인(M7)**·**휴가/반차 승인(M8)**)은 require **11.1·3.2**, BO S3~S9(**가입 2차 최종 승인(S3)**·예약상태 확인(S4)·후기 확인(S6)·매출(S8)·**매니저 근무상태 확인(S9)**)은 require **11.1·3.2**가 출처입니다. **가입 승인은 2단계(M7→S3), 휴가/반차 승인은 1단계(M8)** 로 단계 수가 다릅니다(require §4.4·§8.3).

### 0.3 주니어 진행 원칙 (3원칙)

- **작은 단위 PR**: 한 PR = 한 Phase의 일부 태스크. 백엔드는 "도메인/스키마 정의", "매퍼(Mapper+XML)", "서비스", "컨트롤러"처럼 계층 단위로 쪼개서 올리세요.
- **Phase별 DoD 확인**: 각 Phase 끝의 **완료기준(DoD)** 을 모두 만족해야 다음 Phase로 넘어갑니다. 체크박스를 직접 체크하며 진행하세요.
- **셀프 체크리스트 활용**: BE는 `./gradlew build` → `./gradlew test`(통합테스트 포함), FE는 `npm run lint` → `npm run type-check` → `npm run test:e2e`(회귀) 흐름을 매번 거치세요. 7장 체크리스트가 가이드입니다.

### 0.4 전체 마일스톤 한눈에 보기

| Phase | 제목 | 핵심 산출물 | require 참조 | 공수(일) | 누적(일) |
|:---:|---|---|:---:|:---:|:---:|
| **0** | 명세 확정(Q1~Q8) & 2차 환경 셋업 | 차종↔베이 매핑 확정표, Spring Boot 부트스트랩(H2), FE↔BE 계약 | 명세 8장, 12장 | 2.0 | 2.0 |
| **1** | 백엔드 도메인 모델 & DB 스키마 | 도메인 POJO 10종 + 매퍼·`schema.sql`, 슬롯 `UNIQUE`, 시드 데이터 | 5·10장 | 3.0 | 5.0 |
| **2** | 서비스 추상화 교체(FE↔BE 연동) | `app/services/*`를 `$fetch` API 호출로, DTO 계약 | 12.3 | 3.0 | 8.0 |
| **3** | 인증/인가(실제 토큰) | JWT·역할 가드·이메일 인증(SMTP 연계) | 4장·3.2 | 3.5 | 11.5 |
| **4** | 예약 API + 동시성 2단계 | 슬롯 `UNIQUE` + 낙관적(version 컬럼)/비관적(`FOR UPDATE`) 락, 충돌 409 | 6·7장 / FW5 | 3.5 | 15.0 |
| **5** | 예약 상태 전이 API(완료/취소/승인) | M4·M5·M6 서버화, 상태 가드 | 11.3 / FW6·FW7·M6 | 2.5 | 17.5 |
| **6** | BO — 예약 대행 + 매장 관리 | M3 대행예약, S4 예약자관리·S5 사용자관리 | 3.2·11.1 / M3·S4·S5 | 3.5 | 21.0 |
| **7** | 휴가/반차 1단계 승인 + 가입 2단계 승인 + 매장휴일 | 휴가/반차 1단계(`SUBMITTED→APPROVED/REJECTED`, M6→M8 매장매니저관리자 종결) + 가입 2단계(M7→S3, `PENDING_APPROVAL_L1→L2`) + 매장휴일 | 4·8장 / M6·M7·M8·S3 | 3.5 | 24.5 |
| **8** | 후기/평점 API + BO 확인·매출 | S6 후기확인, S8 매출집계 | 9장·11.1 / S6·S8 | 2.5 | 27.0 |
| **9** | 알림 — SMTP 인프라 + 정책 | 발송 인프라, 메일/푸시 정책 | 13.2 항목 6·7 | 2.5 | 29.5 |
| **10** | 데이터 3단계 — MySQL 이행 | 트랜잭션 + 유니크 인덱스, Flyway 마이그레이션, 운영 영속화 | 7·12장 | 3.5 | 33.0 |

> **총 예상 공수: 약 33일** (주니어 기준 넉넉하게 산정. 리뷰·수정 버퍼 포함). 2단계(Spring Boot 인메모리/H2)는 Phase 0~9, 3단계(MySQL)는 Phase 10에서 다룹니다.
>
> 🔄 **Phase 0의 특별한 위치**: Phase 0은 단순 환경 셋업이 아니라 **명세 미해결 질문 Q1~Q8(예약_규칙_명세_v1.md 8장)을 의사결정으로 확정하는 게이트**입니다. 차종↔베이 매핑 확정 결과가 Phase 1 도메인(`Bay.size`, `Price`)·스키마와 Phase 4 베이 노출 로직에 직접 반영되므로, **Phase 0 결정표가 잠기기 전까지 Phase 1을 시작하지 마세요**.
>
> 🧩 **additive 영향(Phase 2)**: 1차 화면/스토어는 유지되며, `app/services/*`(예: `reservationService.ts`·`storeService.ts`·`priceService.ts`)의 **내부 구현만** 더미 import → `$fetch` API 호출로 교체됩니다. ROADMAP_1 2.1의 "`services/`를 지금 만드는 이유"가 본 Phase에서 회수됩니다.

---

## 1. 2차 개발 환경 셋업 (Phase 0 일부)

> **공수: (Phase 0에 포함)** · **require_v1.md 참조: 12장**

### 1.1 사전 요구사항 (BE 추가분)

1차 FE 요구사항(Node.js `^22.18.0 || >=24.12.0`, npm, Git)에 더해 백엔드 도구를 추가합니다.

| 항목 | 요구 버전 | 확인 명령 |
|---|---|---|
| JDK | **Java 21 (LTS)** (require 12.2 "Java LTS") | `java -version` |
| Gradle | Wrapper 사용(프로젝트 동봉) | `./gradlew --version` |
| Spring Boot | **3.x (최신 무료 버전)** (require 12.2) | `build.gradle` 의존성 |
| H2 Database | in-memory (2단계 더미) | (Gradle 의존성) |
| MySQL | 8.x (3단계, Phase 10) | `mysql --version` |

> ⚠️ **주의**: require 12.2는 "Java(LTS) — 최신 무료 버전 / Spring Boot — 최신 무료 버전"으로 명시했습니다. 본 로드맵은 **Java 21 + Spring Boot 3.x**를 기준으로 작성하되, 팀 환경에 맞춰 LTS 범위 내에서 조정하세요. 빌드 도구는 **Gradle(권장)** 을 기준으로 하며, Maven 사용 시 명령만 치환하면 됩니다.

### 1.2 프로젝트 레이아웃 (모노레포 — `backend/` 추가, **확정**)

> 📌 **저장소 구조 결정(확정)**: 2차는 **모노레포** 방식으로, **현재 Nuxt 저장소(`reserve-wash-fe-be/`) 루트에 `backend/` 디렉터리를 추가**하여 Spring Boot BE를 둔다. 별도 저장소로 분리하지 않는다. FE와 BE는 **하나의 git 저장소**에서 관리되며, 런타임은 **포트로 분리**(FE :3000 / BE :8080)하여 동시 기동한다.

**모노레포를 택한 이유 (CoT)**:

- **버전 정합성**: FE의 `app/services/*` 시그니처와 BE API 계약(DTO)이 **같은 커밋·같은 PR**에서 함께 바뀌어야 한다(2장 DTO 계약). 단일 저장소면 FE↔BE 변경을 원자적으로 묶을 수 있다.
- **추적성**: ROADMAP_1·2, require 정본, E2E 시나리오가 모두 한 저장소에 있어 1↔2차 연계 추적이 끊기지 않는다.
- **점진 교체**: additive 철학상 FE는 유지하고 `app/services/*` 내부만 교체하므로, FE와 BE를 같은 워킹트리에서 나란히 두고 개발하는 편이 마찰이 적다.

**디렉터리 레이아웃**:

```
reserve-wash-fe-be/            # 단일 git 저장소(모노레포 루트)
├─ app/                        # 1차 Nuxt 4 FE (유지, services 내부만 $fetch로 교체)
├─ nuxt.config.ts              # FE 설정 (runtimeConfig·devProxy는 2장)
├─ package.json                # FE 빌드/스크립트 (그대로)
├─ server/                     # (선택) Nitro BFF — 프록시·BFF가 필요할 때만
├─ e2e/                        # 1차 Playwright E2E (유지·회귀)
├─ docs/ …                     # 정본·로드맵 (유지)
└─ backend/                    # 2차 Spring Boot BE (신규) — 독립 Gradle 프로젝트
   ├─ build.gradle             # BE 의존성 (Nuxt package.json과 무관·분리)
   ├─ settings.gradle          # rootProject.name = 'backend'
   ├─ gradlew / gradlew.bat    # Gradle Wrapper (BE 전용)
   └─ src/
      ├─ main/java/com/carwash/…   # controller·service·repository·entity·dto (2장)
      ├─ main/resources/application.yml
      └─ test/java/com/carwash/…   # @SpringBootTest 통합테스트 (6장)
```

> 💡 **빌드 경계 분리**: `backend/`는 **자체 Gradle Wrapper를 가진 독립 프로젝트**다. FE는 루트에서 `npm`, BE는 `backend/`에서 `./gradlew`로 빌드하여 도구 체인이 섞이지 않는다(루트 `package.json`에 Java 빌드를 끼워넣지 않는다). 필요 시 루트에 `npm run be:dev` 같은 편의 스크립트로 `cd backend && ./gradlew bootRun`을 감쌀 수 있다(선택).

> ⚠️ **`.gitignore` 보강**: `backend/build/`, `backend/.gradle/`, `*.class`, IDE 산출물(`.idea/`·`*.iml`)을 루트 `.gitignore`에 추가하라(BE 빌드 산출물이 커밋되지 않도록). 1차 FE의 `node_modules/`·`.nuxt/`·`.output/` 무시 규칙은 유지한다.

### 1.3 동시 기동 (FE :3000 ↔ BE :8080)

| 대상 | 명령 | 포트 | 비고 |
|---|---|---|---|
| FE (Nuxt) | `npm run dev` | **3000** | 1차와 동일(ROADMAP_1 1.3) |
| BE (Spring) | `cd backend && ./gradlew bootRun` | **8080** | H2 in-memory(2단계) |

> 💡 **CORS / 프록시**: FE(:3000)가 BE(:8080)를 `$fetch`로 호출할 때 브라우저 CORS가 발생합니다. **개발 단계 2가지 선택지**가 있습니다 — ① BE에 `@CrossOrigin` 또는 `WebMvcConfigurer`로 `http://localhost:3000` 허용, ② Nuxt `nuxt.config.ts`의 `nitro.devProxy` 또는 `routeRules`로 `/api/**`를 `:8080`으로 프록시. **권장**: 운영 일관성을 위해 ②(프록시)로 동일 출처처럼 호출하고, `$fetch`의 `baseURL`을 `runtimeConfig`로 주입하세요(2장 참조).

### 1.4 `application.yml` (2단계 H2 기준 예시)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:carwash;DB_CLOSE_DELAY=-1;MODE=MySQL   # in-memory, MySQL 호환 모드
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always                 # 기동 시 schema.sql·data.sql 자동 실행
      schema-locations: classpath:db/schema.sql   # 스키마 DDL(슬롯 UNIQUE 포함) 직접 관리
      data-locations: classpath:db/data.sql       # 시드 데이터
  h2:
    console:
      enabled: true                # http://localhost:8080/h2-console 로 확인
# MyBatis 설정 (JPA/Hibernate 미사용 — require_v1.md v1.5)
mybatis:
  mapper-locations: classpath:mapper/*.xml        # 매퍼 XML 위치
  type-aliases-package: com.carwash.domain        # 도메인 POJO 별칭 패키지
  configuration:
    map-underscore-to-camel-case: true            # snake_case 컬럼 ↔ camelCase 프로퍼티
server:
  port: 8080
```

> ⚠️ **2단계 = 휘발성**: H2 in-memory이므로 **BE 재기동 시 데이터가 초기화**됩니다(require 7.2 "인메모리라 재시작 시 휘발"). **MyBatis는 ORM 자동 DDL이 없으므로** 스키마는 `db/schema.sql`로, 시드는 `db/data.sql`로 기동 시 주입합니다(`spring.sql.init`). 운영 영속화·마이그레이션은 3단계(Phase 10, MySQL + Flyway)에서 다룹니다.

### 1.5 완료기준 (DoD)

- [ ] `java -version`이 Java 21(LTS)을 출력한다
- [ ] `./gradlew bootRun`으로 BE가 :8080에서 기동되고, `http://localhost:8080/h2-console`이 열린다
- [ ] `npm run dev`(FE :3000)와 BE :8080이 **동시에** 기동된다
- [ ] FE에서 BE의 헬스 엔드포인트(예: `GET /api/health`)를 CORS 오류 없이 호출할 수 있다(프록시 또는 CORS 설정 확인)

---

## 2. 아키텍처 & 컨벤션 가이드

### 2.1 FE↔BE 경계 (서비스 추상화 교체)

1차에서 `app/services/*`는 더미 데이터를 직접 import해 반환했습니다. 2차에서는 **같은 함수 시그니처를 유지**하면서 내부 구현만 `$fetch`(서버 REST 호출)로 교체합니다. 이것이 additive 철학의 핵심입니다.

```
[컴포넌트/페이지]  →  [Pinia 스토어]  →  [app/services/*]  →  $fetch  →  [Spring REST :8080]
   (1차 그대로)         (1차 그대로)       (내부만 교체)                    (2차 신규)
```

> 💡 **`$fetch` vs `useFetch`**: 컴포넌트 setup의 **데이터 로드(SSR 친화)** 는 `useFetch`(중복 요청 제거·SSR 페이로드 직렬화), 스토어 액션이나 **사용자 인터랙션(버튼 클릭 → 예약 확정)** 은 `$fetch`를 권장합니다. `baseURL`은 `useRuntimeConfig().public.apiBase`로 주입하세요(1.3 프록시 사용 시 `/api`).

### 2.2 Spring + MyBatis 패키지 구조 (계층형)

```
com.carwash
├─ CarwashApplication.java        # @SpringBootApplication 진입점
├─ config/                        # CORS·Security·Jackson·MyBatis 설정
├─ controller/                    # REST 컨트롤러(@RestController) — DTO만 노출
├─ service/                       # 비즈니스 로직(@Service, @Transactional)
├─ mapper/                        # MyBatis 매퍼 인터페이스(@Mapper) — SQL은 XML
├─ domain/                        # 순수 POJO 도메인 모델 (JPA 애너테이션 없음)
├─ dto/                           # 요청/응답 DTO(record 권장) — 도메인 직접 노출 금지
├─ exception/                     # 도메인 예외 + @RestControllerAdvice
└─ security/                      # JWT 필터·UserDetails(Phase 3)

src/main/resources
├─ mapper/                        # MyBatis 매퍼 XML(*Mapper.xml) — namespace = 매퍼 FQN
└─ db/                            # schema.sql·data.sql(2단계 H2) → db/migration/*(3단계 Flyway)
```

> 📌 **DTO 계약 원칙**: 컨트롤러는 **도메인 객체를 직접 반환하지 않습니다.** 요청은 `XxxRequest`, 응답은 `XxxResponse`(Java `record` 권장)로 받고 내보내며, 도메인↔DTO 변환은 service 또는 정적 팩토리(`XxxResponse.from(domain)`)에서 수행합니다. FE의 `app/types/domain.ts`(1차 정의)와 **필드명을 일치**시켜 무변환 매핑이 되도록 합니다(2단계 교체 비용 최소화).
>
> 📌 **MyBatis 매핑 원칙**: DB 컬럼은 snake_case, 도메인/DTO 프로퍼티는 camelCase로 두고 `map-underscore-to-camel-case: true`로 자동 매핑합니다(컬럼 별칭 남발 금지). 매퍼 인터페이스(`mapper/*Mapper.java`)와 XML(`resources/mapper/*Mapper.xml`)은 1:1로 두고 XML `namespace`를 인터페이스 FQN과 일치시킵니다. **JPA `JpaRepository`를 쓰지 않습니다.**

### 2.3 코딩 컨벤션

| 항목 | 규칙 |
|---|---|
| 응답/주석/커밋/문서 | **한국어** (CLAUDE.md) |
| Java 클래스명 | `PascalCase` (예: `ReservationService`) |
| Java 메서드/필드명 | `camelCase` (예: `confirmReservation`) |
| Java 상수 | `UPPER_SNAKE_CASE` |
| Java 패키지 | 소문자 (예: `com.carwash.service`) |
| Lombok | `@Getter`·`@Builder`·`@NoArgsConstructor` (도메인 setter 지양, MyBatis 매핑용 기본 생성자 보장) |
| 트랜잭션 | 쓰기 메서드에 `@Transactional`, 조회는 `@Transactional(readOnly = true)` (MyBatis-Spring도 Spring 트랜잭션으로 동작) |
| DB 접근 | **MyBatis 매퍼만** 사용. JPA/Hibernate(`@Entity`·`JpaRepository`·`@Version`·`ddl-auto`) 금지 |
| FE TS | **세미콜론 없음** + **작은따옴표** (oxfmt, ROADMAP_1 2.3 그대로) |
| FE 주석 | **한국어** |

> 💡 **도메인 불변성 지향**: `domain/`은 순수 POJO입니다(JPA 애너테이션 없음). setter를 열지 말고, 상태 전이는 의미 있는 도메인 메서드(`reservation.complete()`, `reservation.cancel()`)로 표현하세요. `@Builder`로 생성하되, **MyBatis 결과 매핑을 위해 `@NoArgsConstructor`(또는 매퍼 `<constructor>` 매핑)** 를 보장합니다.

---

## 3. Phase별 개발 로드맵 (2차 백엔드 진화 + BO)

> 각 Phase는 **목표 / 선행조건 / 태스크 체크리스트 / 생성·수정 파일 / 구현 예시 / 완료기준(DoD) / require 참조** 공통 포맷을 따릅니다. (ROADMAP_1과 동일)

---

### Phase 0 — 명세 확정(Q1~Q8) & 2차 환경 셋업

> **공수: 2.0일** · **선행조건: 1차(ROADMAP_1) 완료** · **require_v1.md 참조: 12장 / 예약_규칙_명세_v1.md 8장**

#### 목표
예약_규칙_명세_v1.md 8장의 **미해결 질문 Q1~Q8을 의사결정으로 확정**하고(차종↔베이 매핑이 핵심), 그 결과를 Phase 1 데이터 모델 개정에 반영할 수 있도록 **결정표로 잠근다**. 동시에 Spring Boot(+H2) 부트스트랩과 FE↔BE 계약(헬스/CORS)을 완료한다.

> ⚠️ **이 Phase가 게이트인 이유**: Q1~Q8(특히 차종 5분류 ↔ 베이 4등급 매핑, 특대형 신설의 가격표 영향)이 확정되지 않으면 Phase 1의 `Bay.size` 등급 수·`Price` 매트릭스·`getBaysForCar` 노출 알고리즘을 설계할 수 없습니다. **결정 없이 추측 구현하지 마세요** — 명세 0장 충실성 원칙을 따릅니다.

#### 의사결정 결정표 (Q1~Q8 — 질문 / 결정안 / 영향 파일)

> 🔒 **결정표 잠금(2026-06-21 확정)**: 이해관계자 확정에 따라 본 표를 **정본으로 잠근다**. 아래 Q1·Q5는 **4등급(특대형 신설) 확정값**으로 갱신됐다(나머지 Q2·Q3·Q4·Q6·Q7·Q8은 권고안 그대로 확정). Phase 1은 본 표를 SSOT로 따른다.

| ID | 질문(명세 8장) | 결정안(🔒 확정) | 영향 파일 |
|----|------|------|------|
| **Q1** | 차종 5분류 → 베이 4등급 매핑 | 🔒 **확정 — 4등급 신설**: 경형·소형→소형(`SMALL`), 준중형·중형→중형(`MID`), 대형·SUV→대형(`LARGE`), **승합·기타→특대형(`XLARGE` 신설)**. `BaySize`에 4번째 값 `XLARGE` 추가, `VAN_ETC→XLARGE` 매핑 | `domain/Bay.java`(`size`), `domain/enums/BaySize`(+XLARGE), `BayService`, **FE `app/types/enums.ts`·`app/data/carTypes.ts`(동반 수정)** |
| **Q2** | 크기 등급당 베이 1개 vs 복수 | **복수 허용** — 등급은 `size` 속성, 베이 수 N은 매장별 유지(require 5.2 수용량 보존) | `domain/Bay.java`, 시드 데이터 |
| **Q3** | 베이 노출 1:1 vs 누적 | **1차 누적 로직 유지** — 차 크기 이상을 수용하는 베이 노출(`sizeRank >= min`), 1차 `getBaysForCar` 동작 보존 | `BayService.findBaysForCar()` |
| **Q4** | A/B/C/D 코드 ↔ 기존 매장별 일련번호 마이그레이션 | 베이에 **`size`(등급) 속성 신설**, `code`는 매장 내 식별자로 유지(코드와 등급 분리). 매장이 A~D 전 등급을 보유할 필요 없음 | `domain/Bay.java`, 시드, `dto/BayResponse` |
| **Q5** | 특대형 ↔ 5분류 연결 & 가격표 영향 | 🔒 **확정**: 특대형 등급(`XLARGE`)=`VAN_ETC`(승합·기타) 차종과 연결. **가격표(10.3)는 차종 기준 유지** — 베이 등급 신설은 노출에만 영향, 가격 무변경. 누적 로직(`size>=min`)상 VAN_ETC가 수용 베이 0이 되지 않도록 **시드에 `XLARGE` 베이 ≥1 추가**(없으면 Q8 빈 상태 적용) | `domain/Price.java`(무변경 확인), `db/data.sql`(XLARGE 베이 시드), `BayService` |
| **Q6** | 규칙 1 "매니저→차종" 직렬 엄격성 | **자유 순서 유지**(매장 이후 매니저·차종 자유, 베이만 차종 이후) — 1차 `reserve/index.vue` 동작 보존 | FE `reserve/index.vue`(무변경) |
| **Q7** | 규칙 2를 매니저 대행(6.2)에도 적용 | **적용** — 대행 예약(M3)도 동일 베이 노출 규칙 사용(Phase 6에서 공유) | `BayService`(M3 재사용) |
| **Q8** | 차종 대응 베이가 매장에 없을 때 처리 | 베이 목록 빈 상태 → "해당 차종 수용 베이 없음" 안내 + 예약 차단(409 아님, 선택 단계 비활성) | `BayService`, FE 빈 상태 UI |

> 💡 **권고안의 일관성**: 위 결정안은 명세 8장의 "추측 후보(`?`)"와 정합하되, **1차 구현(누적 수용·매장별 베이 수)을 최대한 보존**하는 방향으로 잡았습니다(additive). 가격표는 차종 기준이므로 베이 등급 신설의 영향을 받지 않습니다(Q5).

> 🔒 **id 정책 결정(2026-06-21 확정 — Phase 1 SSOT)**: **마스터 엔티티(User/Store/Bay/Manager)의 id는 문자열(`VARCHAR`) 무변환**으로 둔다. FE 1차의 `'store1'`·`'store1-A1'`·`'mgr1'`·`'user1'`을 그대로 PK로 시드하여 FE↔BE 무변환 매핑을 보장한다. 즉 **아래 `schema.sql` 예시의 마스터 `BIGINT AUTO_INCREMENT`는 채택하지 않는다**. `Reservation`/`Review`는 앱 레이어가 문자열 id를 부여한다(AUTO_INCREMENT 미사용). `Slot`·`ManagerDayoff`는 FE 타입에 id가 없으므로 내부 surrogate(`Slot.id`=`BIGINT AUTO_INCREMENT`, FE 미노출)만 두고, `Price`는 `(car_type, service_type)` 복합 PK를 쓴다.

> ⚠️ **Q1 4등급 선택의 후속 영향(additive 예외 — 승인됨)**: 특대형(`XLARGE`) 신설로 BE `BaySize`가 4값이 되므로, FE도 `app/types/enums.ts`(`BaySize`+`XLARGE`)·`app/data/carTypes.ts`(`VAN_ETC→XLARGE`)·`app/data/stores.ts`(XLARGE 베이 ≥1)를 **동반 수정**하여 BE와 enum 값집합을 일치시킨다(Phase 1 T7). 1차 E2E는 회귀로 통과 유지.

#### 태스크 체크리스트
- [x] **Q1~Q8 결정표 검토 회의** — 이해관계자와 권고안 확정/수정, 본 표 잠금 (2026-06-21 확정: Q1·Q5 4등급 신설, id 마스터=VARCHAR 무변환)
- [ ] `backend/` Spring Boot 프로젝트 부트스트랩(Gradle, Java 21, Spring Web·**MyBatis(`mybatis-spring-boot-starter`)**·Validation·H2 의존성 — **Data JPA 추가 금지**)
- [ ] `application.yml` 작성(1.4 H2 in-memory 기준)
- [ ] `config/CorsConfig.java` 또는 Nuxt `devProxy`로 FE(:3000)↔BE(:8080) 계약 수립
- [ ] `GET /api/health` 헬스 엔드포인트 — FE에서 호출 성공 확인
- [ ] FE `runtimeConfig.public.apiBase` 추가(`$fetch` baseURL 주입 준비)

#### 생성·수정 파일
`backend/build.gradle`, `backend/settings.gradle`, `backend/src/main/java/com/carwash/CarwashApplication.java`, `backend/src/main/java/com/carwash/config/CorsConfig.java`, `backend/src/main/java/com/carwash/controller/HealthController.java`, `backend/src/main/resources/application.yml`, `nuxt.config.ts`(수정 — `runtimeConfig`·`devProxy`)

#### 구현 예시 — 헬스 컨트롤러 & CORS 설정

`backend/.../controller/HealthController.java`
```java
package com.carwash.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    // FE↔BE 계약 확인용 헬스 엔드포인트
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
```

`backend/.../config/CorsConfig.java`
```java
package com.carwash.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // 개발 단계: FE dev 서버(:3000)의 교차 출처 호출 허용
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowCredentials(true);
    }
}
```

`nuxt.config.ts` (수정 — apiBase 주입, 프록시 선택 시)
```ts
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      // $fetch baseURL — 프록시 사용 시 '/api', 직접 호출 시 'http://localhost:8080/api'
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080/api',
    },
  },
})
```

#### 완료기준 (DoD)
- [ ] **Q1~Q8 결정표가 확정·잠김**(이해관계자 승인) — Phase 1 착수 전제
- [ ] BE가 :8080에서 기동되고 `GET /api/health`가 `{"status":"UP"}`을 반환한다
- [ ] FE(:3000)에서 `$fetch(apiBase + '/health')`가 CORS 오류 없이 성공한다
- [ ] `./gradlew build` 통과

#### 구현 메모 (📌)
- 📌 **결정표는 Phase 1의 SSOT**: Q1~Q8 확정값이 권고안과 다르면 **본 결정표만 갱신**하세요. Phase 1 도메인(`Bay.size`)·스키마·시드·`getBaysForCar`가 결정표를 참조하므로, 코드보다 표가 먼저 확정되어야 합니다.
- 📌 **명세는 읽기 전용**: 예약_규칙_명세_v1.md / require_v1.md는 수정하지 않습니다. 확정 결과는 본 로드맵 결정표와 후속 Phase 코드에만 반영합니다.

---

### Phase 1 — 백엔드 도메인 모델 & DB 스키마

> **공수: 3.0일** · **선행조건: Phase 0(Q1~Q8 확정)** · **require_v1.md 참조: 5장(도메인), 10장(가격)**

#### 목표
require 5장(도메인)·10장(가격)과 Phase 0 결정표를 기준으로 **도메인 POJO 10종 + MyBatis 매퍼**를 정의하고, `db/schema.sql`(DDL)에 슬롯 `UNIQUE(store_id, bay_id, date, time_slot)` 제약(require 5.2·7.3 최종 방어선)을 건다. 기동 시 `db/data.sql`로 시드 데이터를 주입한다. **JPA/Hibernate를 쓰지 않으므로 스키마는 SQL로 직접 관리한다.**

#### 태스크 체크리스트 — ✅ 2026-06-21 완료
- [x] `domain/` POJO 10종 — `User`, `Store`, `Bay`, `Slot`, `Reservation`, `Manager`, `ManagerDayoff`, `StoreHoliday`, `Review`, `Price` (JPA 애너테이션 없음, Lombok)
- [x] `Bay.size`에 **Phase 0 결정(4등급: SMALL/MID/LARGE/XLARGE)** 반영(require 5.4·명세 Q1)
- [x] `ManagerDayoff`에 `DayoffType`(FULL_DAY/SHIFT_1·2·3) enum 반영(require 5.5)
- [x] `db/schema.sql`에 10개 테이블 DDL + `slot` 테이블 `CONSTRAINT uk_slot_store_bay_date_time UNIQUE (store_id, bay_id, date, time_slot)` (require 5.2·7.3)
- [x] `mapper/` MyBatis 매퍼 인터페이스(`@Mapper`) 9종 + `resources/mapper/*Mapper.xml`
- [x] `Price` 매트릭스(차종 5 × 서비스 4 = 20행, require 10.3 확정 단가)를 `db/data.sql`에 시드
- [x] `db/data.sql`로 시드(매장·베이·매니저·휴무·가격·더미 사용자) 주입 + XLARGE 베이(store1-A4) 신설
- [x] FE `app/types/domain.ts`(1차)와 **필드명 일치 확인**(DTO 무변환 매핑 대비) + FE 정합(BaySize 4값)

#### 생성·수정 파일
`domain/User.java`, `domain/Store.java`, `domain/Bay.java`, `domain/Slot.java`, `domain/Reservation.java`, `domain/Manager.java`, `domain/ManagerDayoff.java`, `domain/StoreHoliday.java`, `domain/Review.java`, `domain/Price.java`, 각 `mapper/*Mapper.java` + `resources/mapper/*Mapper.xml`, `enums/` (또는 도메인 내부 enum), `resources/db/schema.sql`(DDL+UNIQUE), `resources/db/data.sql`(시드)

#### 구현 예시 — 스키마 DDL + Slot 도메인 POJO + 매퍼

`resources/db/schema.sql` (슬롯 UNIQUE = 최종 방어선, require 5.2·7.3)
```sql
-- 슬롯 = (매장, 베이, 날짜, 30분 시간단위), 시스템 전체 UNIQUE
CREATE TABLE IF NOT EXISTS slot (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id    BIGINT      NOT NULL,
    bay_id      BIGINT      NOT NULL,
    date        VARCHAR(10) NOT NULL,   -- 'YYYY-MM-DD'
    time_slot   VARCHAR(5)  NOT NULL,   -- 'HH:mm' (30분 단위 시작 시각)
    status      VARCHAR(20) NOT NULL,   -- AVAILABLE / HOLDING / RESERVED / COMPLETED
    version     BIGINT      NOT NULL DEFAULT 0,   -- 낙관적 락용 버전(Phase 4)
    CONSTRAINT uk_slot_store_bay_date_time UNIQUE (store_id, bay_id, date, time_slot)
);
-- price, reservation, manager_dayoff 등 나머지 테이블도 같은 파일에 정의
```

`domain/Slot.java` (순수 POJO — JPA 애너테이션 없음)
```java
package com.carwash.domain;

import lombok.*;

// 슬롯 = (매장, 베이, 날짜, 30분 시간단위) (require 5.2)
// UNIQUE 제약은 schema.sql DDL이 담당(동시성 최종 방어선, require 7.3)
@Getter
@NoArgsConstructor              // MyBatis 결과 매핑용 기본 생성자
public class Slot {

    private Long id;
    private Long storeId;        // 컬럼 store_id ↔ camelCase 자동 매핑
    private Long bayId;
    private String date;         // 'YYYY-MM-DD'
    private String timeSlot;     // 'HH:mm' (30분 단위 시작 시각)
    private SlotStatus status;   // AVAILABLE / HOLDING / RESERVED / COMPLETED
    private Long version;        // 낙관적 락용 버전 (Phase 4에서 활용, require 7.3)

    @Builder
    public Slot(Long storeId, Long bayId, String date, String timeSlot, SlotStatus status) {
        this.storeId = storeId;
        this.bayId = bayId;
        this.date = date;
        this.timeSlot = timeSlot;
        this.status = status;
    }

    // 상태 전이는 도메인 메서드로 표현(setter 미개방)
    public void hold() {
        if (this.status != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("점유 불가 상태: " + this.status);
        }
        this.status = SlotStatus.HOLDING;
    }

    public void reserve() {
        this.status = SlotStatus.RESERVED;
    }

    public void release() {
        this.status = SlotStatus.AVAILABLE;
    }
}
```

`mapper/SlotMapper.java` (MyBatis `@Mapper` 인터페이스)
```java
package com.carwash.mapper;

import com.carwash.domain.Slot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SlotMapper {

    // (매장, 베이, 날짜, 시간)으로 슬롯 단건 조회 — 점유/확정 시 사용
    Slot findByKey(@Param("storeId") Long storeId,
                   @Param("bayId") Long bayId,
                   @Param("date") String date,
                   @Param("timeSlot") String timeSlot);

    // 낙관적 락: 영향 행 수 0이면 충돌(Phase 4)
    int updateStatusWithVersion(@Param("id") Long id,
                                @Param("status") String status,
                                @Param("version") Long version);
}
```

`resources/mapper/SlotMapper.xml` (namespace = 매퍼 FQN)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.carwash.mapper.SlotMapper">

    <!-- map-underscore-to-camel-case=true 이므로 컬럼 별칭 불필요 -->
    <select id="findByKey" resultType="com.carwash.domain.Slot">
        SELECT id, store_id, bay_id, date, time_slot, status, version
        FROM slot
        WHERE store_id = #{storeId} AND bay_id = #{bayId}
          AND date = #{date} AND time_slot = #{timeSlot}
    </select>

    <!-- 낙관적 락: version 일치 시에만 갱신, 영향 행 수로 충돌 판정 -->
    <update id="updateStatusWithVersion">
        UPDATE slot
        SET status = #{status}, version = version + 1
        WHERE id = #{id} AND version = #{version}
    </update>
</mapper>
```

#### 완료기준 (DoD) — ✅ 2026-06-21 충족(Phase 1 완료)
- [x] `./gradlew bootRun` 시 `schema.sql`로 H2에 10개 테이블이 생성된다 (SchemaIntegrityTest: BASE TABLE 10종)
- [x] `slot` 테이블에 `uk_slot_store_bay_date_time` 유니크 제약이 존재한다 (SchemaIntegrityTest: TABLE_CONSTRAINTS UNIQUE 1건)
- [x] `price` 테이블에 20행이 시드되고 require 10.3과 정확히 일치한다 (PriceMapperTest: 20행 + VAN_ETC PREMIUM 55000)
- [x] FE `app/types/domain.ts`의 필드명과 도메인 POJO 필드명이 일치한다(불일치 항목 0건) (대조 완료, Slot.id/version은 내부 surrogate)
- [x] 매퍼 단건 조회/갱신이 동작한다(매퍼 단위 통합테스트) (StoreMapper 필터·SlotMapper 낙관락 영향행수 등 13건)
- [x] `./gradlew build` 통과 (전체 17개 테스트 통과)

#### 구현 메모 (📌)
- 📌 **관계 매핑 깊이**: 1차 FE는 `storeId`/`bayId` 같은 **ID 참조** 기반이었습니다. 2차 도메인 POJO도 ID(Long) 참조로 두고(FE DTO 무변환 매핑 유지), 연관 데이터가 필요하면 **매퍼에서 JOIN 또는 별도 조회**로 조립하세요. MyBatis `<association>`/`<collection>` 중첩 매핑은 꼭 필요한 곳에만 쓰고, 무분별한 중첩 조인으로 N+1을 만들지 마세요(JPA 양방향 연관관계 자체가 없습니다).
- 📌 **enum 매핑**: `SlotStatus`·`CarType` 등은 DB에 문자열(VARCHAR)로 저장합니다. MyBatis 기본 `EnumTypeHandler`가 enum ↔ name 문자열을 매핑하므로 별도 핸들러가 대개 불필요합니다(특수 매핑이 필요하면 `TypeHandler`를 등록).

---

### Phase 2 — 서비스 추상화 교체 (FE↔BE 연동)

> **공수: 3.0일** · **선행조건: Phase 1** · **require_v1.md 참조: 12.3(데이터 진화 2단계 교체 경계)**

#### 목표
1차의 `app/services/*`(더미 직접 반환)를 **`$fetch` 기반 API 클라이언트**로 교체하고, BE에 그에 대응하는 **조회 REST 엔드포인트**(매장·매니저·베이·가격)를 만든다. **컴포넌트/스토어/페이지는 무수정**을 목표로 한다(additive 회수 지점).

> 💡 **Phase 2의 핵심 검증**: "1차 화면이 그대로 동작하는데, 데이터 출처만 더미 → 서버로 바뀌었다"가 성공 기준입니다. ROADMAP_1 5.1의 "준비 작업(서비스 추상화)"이 여기서 결실을 맺습니다.

#### 태스크 체크리스트 — ✅ 2026-06-21 완료
- [x] BE 조회 API: `GET /api/stores`(승인 매장만)·`GET /api/managers`(전체, dayoffs)·`GET /api/bays`(전체)·`GET /api/prices`(전체) — *list-all 채택(캐시 1회 로드용). per-store/`?carType=`는 동기 computed 충돌로 기각*
- [x] `dto/StoreResponse`·`ManagerResponse`(+`DayoffResponse`)·`BayResponse`·`PriceResponse`(record, FE 타입과 필드 일치, `isStoreAdmin` `@JsonProperty` 키 고정)
- [x] FE `app/services/storeService.ts` 내부를 서버 하이드레이트 캐시(`catalogCache`) 동기 읽기로 교체(시그니처 유지)
- [x] FE `app/services/priceService.ts`의 `getPrice`를 캐시(`/api/prices` 로드) 기반으로 교체
- [x] FE 베이 노출(`getBaysForCar`)은 캐시된 bays + `SIZE_RANK` 누적 로직을 **FE 동기 계산 유지**(서버 위임 시 computed 충돌 — 방침 변경)
- [x] **1차 Playwright E2E 회귀 통과 확인**(매장 검색·매니저 휴무·가격 표시가 서버 데이터로도 동일) — BE+FE 동시 기동 25건 통과

#### 생성·수정 파일
`controller/StoreController.java`, `controller/PriceController.java`, `service/StoreService.java`, `service/PriceService.java`, `service/BayService.java`, `dto/StoreResponse.java`, `dto/ManagerResponse.java`, `dto/BayResponse.java`, `dto/PriceResponse.java`, FE `app/services/storeService.ts`(내부 교체), FE `app/services/priceService.ts`(내부 교체)

#### 구현 예시 — 조회 컨트롤러 & FE 서비스 교체

`controller/StoreController.java`
```java
package com.carwash.controller;

import com.carwash.dto.BayResponse;
import com.carwash.dto.ManagerResponse;
import com.carwash.dto.StoreResponse;
import com.carwash.service.BayService;
import com.carwash.service.StoreService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final BayService bayService;

    public StoreController(StoreService storeService, BayService bayService) {
        this.storeService = storeService;
        this.bayService = bayService;
    }

    // 승인된 매장만 노출 (require 6.1)
    @GetMapping
    public List<StoreResponse> stores() {
        return storeService.findApprovedStores();
    }

    @GetMapping("/{id}/managers")
    public List<ManagerResponse> managers(@PathVariable Long id) {
        return storeService.findManagers(id);
    }

    // 차종(차 크기)에 맞는 베이만 노출 (명세 규칙 2, Phase 0 Q3 누적 로직)
    @GetMapping("/{id}/bays")
    public List<BayResponse> bays(@PathVariable Long id, @RequestParam String carType) {
        return bayService.findBaysForCar(id, carType);
    }
}
```

FE `app/services/storeService.ts` (내부만 `$fetch`로 교체 — 시그니처 유지)
```ts
import type { Store, Manager, Bay } from '~/types/domain'
import type { CarType } from '~/types/enums'

// 1차: 더미 직접 반환 → 2차: 서버 호출 (컴포넌트/스토어는 무수정)
function apiBase(): string {
  return useRuntimeConfig().public.apiBase
}

// 승인된 매장만 (require 6.1)
export function getApprovedStores(): Promise<Store[]> {
  return $fetch<Store[]>(`${apiBase()}/stores`)
}

export function getManagers(storeId: string): Promise<Manager[]> {
  return $fetch<Manager[]>(`${apiBase()}/stores/${storeId}/managers`)
}

// 차 크기에 맞는 베이만 (명세 규칙 2 — 서버 위임)
export function getBaysForCar(storeId: string, carType: CarType): Promise<Bay[]> {
  return $fetch<Bay[]>(`${apiBase()}/stores/${storeId}/bays`, { query: { carType } })
}
```

#### 완료기준 (DoD) — ✅ 2026-06-21 충족(Phase 2 완료)
- [x] `GET /api/stores`가 승인 매장만 반환한다(`approved=false` 미노출, require 6.1) (CatalogApiTest: length=2, 판교점 제외)
- [x] FE 예약 1페이지(`/reserve`)가 **서버 데이터**로 매장·매니저·차종·가격을 표시한다(화면 무변경) (catalogCache 서버 하이드레이트, 컴포넌트/스토어 무변경)
- [x] **1차 Playwright E2E 회귀**(매장 검색 필터·가격 자동 표시)가 서버 데이터로도 통과한다 (BE+FE 동시 기동, 25건 통과)
- [x] `./gradlew build`(23건), `npm run type-check`, `npm run test:e2e`(25건) 통과

> 📌 **2차 구현 방침(additive)**: 서비스 동기 소비(computed/setup)를 보존하기 위해 services를 Promise화하지 않고, 부팅 시 카탈로그를 1회 로드하는 **서버 하이드레이트 캐시**(`app/services/catalogCache.ts`, Nuxt `useState`)를 도입해 기존 동기 서비스가 캐시를 읽도록 내부만 교체했다. BE는 list-all 4종(`/api/stores`·`/api/managers`·`/api/bays`·`/api/prices`)으로 구성. 슬롯 라이브 상태(getSeededSlotStatus)는 Phase 4에서 서버화.

#### 구현 메모 (📌)
- 📌 **무수정 목표의 현실**: 더미가 동기 반환이던 함수가 `Promise`로 바뀌므로, 호출부에 `await`가 없던 곳은 최소 수정이 필요할 수 있습니다. 이 정도는 "서비스 계층 추상화의 합리적 비용"으로 간주하고, **컴포넌트 마크업·스토어 구조 변경 0**을 진짜 성공 기준으로 삼으세요.

---

### Phase 3 — 인증/인가 (실제 토큰)

> **공수: 3.5일** · **선행조건: Phase 2** · **require_v1.md 참조: 4장(인증·승인 분리), 3.2(권한 매트릭스)**

#### 목표
1차의 더미 로그인을 **JWT 기반 실제 인증**으로 교체하고, **역할(USER/MANAGER/STORE_ADMIN/ADMIN) 기반 인가 가드**와 **이메일 인증(SMTP 연계, Phase 9 인프라와 결합)** 을 도입한다. require 4.2의 **인증/승인 분리** 원칙을 서버에 반영한다. *(require v1.7)* **오직 `ACTIVE` 상태만 로그인 가능**하며, 매니저 계열(`MANAGER`·`STORE_ADMIN`)은 2단계 가입 승인을 통과해 `ACTIVE`가 되기 전에는 로그인이 거부되어야 한다.

#### 태스크 체크리스트
- [ ] `POST /api/auth/login` — 이메일/비밀번호 검증 → JWT 발급. **`ACTIVE`가 아닌 계정(미인증·승인 대기·반려)은 로그인 거부**(require v1.7 §4.4)
- [ ] `security/JwtAuthenticationFilter` + `SecurityConfig`(역할별 경로 인가)
- [ ] 비밀번호 해시(`BCryptPasswordEncoder`)
- [ ] 회원가입 상태 머신(require v1.7 §4.4): `REQUESTED → EMAIL_VERIFIED → ` **{ `USER`: `ACTIVE` 직행 }** / **{ `MANAGER`·`STORE_ADMIN`: `PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2 → ACTIVE` 2단계 승인 }**, + `REJECTED`. 승인 단계 전이(L1·L2)는 Phase 7 가입 승인 흐름과 연계
- [ ] 이메일 인증 토큰 발급/검증(`POST /api/auth/verify-email`) — 발송은 Phase 9 SMTP에 위임(인터페이스만 선언)
- [ ] FE `app/stores/auth.ts`의 `login`을 `$fetch('/auth/login')` + 토큰 보관(`useCookie`)으로 교체
- [ ] FE 미들웨어 `auth.ts`·역할 가드를 토큰 기반으로 교체(ROADMAP_1 Phase 3 자산 위에 증분)

> ⚠️ **SSR 토큰 보관**: 1차 ROADMAP_1 Phase 3에서 안내했듯 `localStorage`는 SSR에서 접근 불가입니다. JWT는 **`useCookie`(httpOnly 권장)** 로 보관해 SSR 미들웨어 단계에서도 인증 상태를 읽게 하세요.

#### 생성·수정 파일
`controller/AuthController.java`, `service/AuthService.java`, `security/JwtTokenProvider.java`, `security/JwtAuthenticationFilter.java`, `config/SecurityConfig.java`, `dto/LoginRequest.java`, `dto/LoginResponse.java`, FE `app/stores/auth.ts`(교체), FE `app/middleware/auth.ts`(토큰 기반), FE `app/middleware/role-guard.ts`(신규 — BO 진입 대비)

#### 구현 예시 — 로그인 컨트롤러 & 인증/승인 분리

`controller/AuthController.java`
```java
package com.carwash.controller;

import com.carwash.dto.LoginRequest;
import com.carwash.dto.LoginResponse;
import com.carwash.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 이메일/비밀번호 검증 후 JWT 발급 (require 4장)
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    // 이메일 인증 링크 클릭 → EMAIL_VERIFIED 전이 (require 4.4)
    @PostMapping("/verify-email")
    public void verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
    }
}
```

`dto/LoginRequest.java` (record + Bean Validation)
```java
package com.carwash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {}
```

`service/AuthService.java` (인증/승인 분리 — require 4.2)
```java
// 인증(이메일 본인확인)과 승인(매니저 가입 검토)은 별개 (require 4.2)
// USER: EMAIL_VERIFIED → ACTIVE 자동 전이(승인 분기 없음, require v1.7 §4.4)
// MANAGER·STORE_ADMIN: EMAIL_VERIFIED → PENDING_APPROVAL_L1 (2단계 승인 진입, Phase 7 가입 승인 연계)
@Transactional
public void verifyEmail(String token) {
    User user = userRepository.findByEmailVerifyToken(token)
        .orElseThrow(() -> new DomainException("유효하지 않은 인증 토큰입니다."));
    user.markEmailVerified();
    if (user.requiresApproval()) {
        user.toPendingApprovalL1();  // 매니저 계열 → 1차 승인 대기(PENDING_APPROVAL_L1, require v1.7 §4.4)
    } else {
        user.activate();             // USER → 즉시 활성(ACTIVE 직행)
    }
}
```

FE `app/stores/auth.ts` (토큰 기반 교체)
```ts
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '~/types/domain'

export const useAuthStore = defineStore('auth', () => {
  const token = useCookie<string | null>('access_token')
  const currentUser = ref<User | null>(null)
  const isLoggedIn = computed(() => !!token.value)

  // 더미 로그인 → 실제 JWT 발급 (require 4장)
  async function login(email: string, password: string): Promise<boolean> {
    try {
      const res = await $fetch<{ token: string; user: User }>(
        `${useRuntimeConfig().public.apiBase}/auth/login`,
        { method: 'POST', body: { email, password } },
      )
      token.value = res.token
      currentUser.value = res.user
      return true
    } catch {
      return false
    }
  }

  function logout() {
    token.value = null
    currentUser.value = null
  }

  return { currentUser, isLoggedIn, login, logout }
})
```

#### 완료기준 (DoD) — ✅ 2026-06-21 충족(핵심 인증 additive 범위)
- [x] 올바른 계정 로그인 시 JWT가 발급되고 `useCookie('access_token')`에 보관된다 (AuthApiTest 200+token, FE auth.ts 쿠키 저장)
- [x] 토큰 없이 보호 API 호출 시 401, 권한 부족 시 403을 반환한다(역할 인가) (SecurityConfig stateless+JWT 필터; 보호 GET은 Phase 4+라 401 골격 검증, 403 역할은 BO API에서 활성)
- [ ] ~~`USER` 가입 `EMAIL_VERIFIED → ACTIVE` 직행, 매니저 계열 `PENDING_APPROVAL_L1 → L2` 2단계 승인 분기(require v1.7 §4.4)~~ → **이연**(이메일 인증/2단계 승인 상태머신·SMTP는 BO/알림 단계 Phase 7/9). Phase 3은 USER 즉시 가입(서버 영속·BCrypt)만 구현
- [x] **1차 로그인 E2E 회귀**(성공/실패/미인증 가드)가 토큰 기반으로 통과한다 (auth.spec 8건 서버 기반 통과)
- [x] `./gradlew build`(29건), `npm run type-check`, `npm run test:e2e`(25건) 통과

> 📌 **Phase 3 구현 방침**: 사용자 승인에 따라 **핵심 인증 additive**(JWT 로그인 + USER 회원가입 서버 영속 + BCrypt + 역할 인가 골격)만 구현. 이메일 인증 토큰/승인 상태머신/SMTP는 **이연**(Phase 6/7/9). BE에 spring-security + jjwt 추가, `SecurityConfig`(@ConditionalOnWebApplication SERVLET·stateless·CORS 단일화), `users.password_hash`(BCrypt 시드). FE는 `auth.ts`를 `useCookie('access_token')` + `$fetch`로 교체(login/signup async, 마크업 무변경). 슬롯/예약 보호 API는 Phase 4.

#### 구현 메모 (📌)
- 📌 **이메일 발송은 Phase 9 위임**: 본 Phase에서는 인증 토큰 발급·검증·상태 전이까지 구현하고, **실제 메일 발송은 `EmailSender` 인터페이스만 선언**해 Phase 9 SMTP 인프라가 구현하도록 둡니다. 2단계에서는 콘솔 로그 스텁(`LoggingEmailSender`)으로 대체합니다.

---

### Phase 4 — 예약 API + 동시성 2단계 ★핵심

> **공수: 3.5일** · **선행조건: Phase 3** · **require_v1.md 참조: 6장(예약), 7장(동시성 2단계·7.3 락 전략) / FW5**

#### 목표
1차의 클라이언트 슬롯 잠금 시뮬레이션(ROADMAP_1 Phase 5)을 **서버 예약 API + 실제 락**으로 교체한다. 슬롯 `UNIQUE` 제약을 최종 방어선으로 두고, **낙관적 락(version 컬럼 비교 UPDATE)/비관적 락(매퍼 `SELECT ... FOR UPDATE`)** 을 슬롯 경합도에 따라 선택 적용한다. 확정 충돌은 **409 Conflict** 로 응답하고, FE는 1차 "재선택 토스트" UX를 그대로 재사용한다.

> 🔎 동시성 2·3단계의 자세한 설명은 **4장(동시성 처리 구현 가이드)** 를 참조하세요. ROADMAP_1 4.4의 `// TODO(2단계)` 교체 지점이 본 Phase에서 실제 코드가 됩니다.

#### 태스크 체크리스트
- [ ] `POST /api/reservations/hold` — 슬롯 점유(AVAILABLE → HOLDING), 동시 요청 한 건만 성공
- [ ] `POST /api/reservations/confirm` — HOLDING → RESERVED 확정, 슬롯 status 동기화
- [ ] **낙관적 락**: version 컬럼 비교 UPDATE(`updateStatusWithVersion`)의 **영향 행 수 0 → 충돌** 처리(`SlotConflictException` → 409)
- [ ] **비관적 락**: 경합 잦은 슬롯에 매퍼 `SELECT ... FOR UPDATE`(`findForUpdate`) — 트랜잭션 내 조회로 직렬화
- [ ] 슬롯 `UNIQUE` 위반(`DuplicateKeyException`/`DataIntegrityViolationException`) → 409 매핑(최종 방어선, require 7.3)
- [ ] `@RestControllerAdvice`로 409/404/400 일관 응답(`ErrorResponse`)
- [ ] FE `app/services/reservationService.ts`·`reservation` 스토어 `confirmReservation`을 서버 호출로 교체(ROADMAP_1 4.4 TODO 위치)
- [ ] FE 409 응답 시 1차 `useToast` 재선택 토스트 + 그리드 새로고침 재사용

#### 생성·수정 파일
`controller/ReservationController.java`, `service/ReservationService.java`, `mapper/SlotMapper.java` + `resources/mapper/SlotMapper.xml`(비관적 락 `FOR UPDATE` SQL 추가), `mapper/ReservationMapper.java` + XML, `dto/HoldRequest.java`, `dto/ConfirmRequest.java`, `dto/ReservationResponse.java`, `exception/GlobalExceptionHandler.java`, `dto/ErrorResponse.java`, FE `app/services/reservationService.ts`(교체), FE `app/stores/reservation.ts`(confirm 서버 위임)

#### 락 전략 선택 기준 (require 7.3)

| 기법 | 적용 슬롯 | 구현 | 장점 | 단점 |
|---|---|---|---|---|
| 낙관적 락 (version 컬럼) | 일반 슬롯(경합 낮음) | `UPDATE ... WHERE version=?` 영향 행 수 판정 + 재시도 | 락 대기 없음, 처리량 높음 | 충돌 시 재시도/409 처리 필요 |
| 비관적 락 (`FOR UPDATE`) | 인기 슬롯(경합 높음) | 매퍼 XML `SELECT ... FOR UPDATE` | 충돌 자체 차단 | 락 대기/데드락 위험, 처리량 저하 |
| UNIQUE 제약(최종) | 전 슬롯 | `schema.sql`의 `UNIQUE(store,bay,date,time)` | DB가 중복 INSERT 원천 차단 | 위반 → 409 변환·재선택 UX 필수 |

> **권장(require 7.3)**: 슬롯 `UNIQUE`를 **최종 방어선**으로 깔고, 기본은 낙관적 락, 경합이 잦은 인기 슬롯만 비관적 락을 선택 적용합니다.

#### 구현 예시 — 예약 확정 서비스(비관적 락) & 409 매핑

`mapper/SlotMapper.java` + `resources/mapper/SlotMapper.xml` (비관적 락 `FOR UPDATE` 추가)
```java
// SlotMapper.java — 인터페이스에 메서드 추가
Slot findForUpdate(@Param("storeId") Long storeId,
                   @Param("bayId") Long bayId,
                   @Param("date") String date,
                   @Param("timeSlot") String timeSlot);
```
```xml
<!-- SlotMapper.xml — 경합 잦은 슬롯: SELECT ... FOR UPDATE (require 7.3 비관적 락) -->
<select id="findForUpdate" resultType="com.carwash.domain.Slot">
    SELECT id, store_id, bay_id, date, time_slot, status, version
    FROM slot
    WHERE store_id = #{storeId} AND bay_id = #{bayId}
      AND date = #{date} AND time_slot = #{timeSlot}
    FOR UPDATE
</select>
```

`service/ReservationService.java`
```java
package com.carwash.service;

import com.carwash.domain.*;
import com.carwash.dto.*;
import com.carwash.exception.SlotConflictException;
import com.carwash.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final SlotMapper slotMapper;
    private final ReservationMapper reservationMapper;

    public ReservationService(SlotMapper slotMapper,
                              ReservationMapper reservationMapper) {
        this.slotMapper = slotMapper;
        this.reservationMapper = reservationMapper;
    }

    // 확정: 비관적 락(FOR UPDATE)으로 슬롯을 잠그고 RESERVED 전이 (require 7.3)
    // 충돌(이미 점유/예약) 시 SlotConflictException → 409 (1차 재선택 UX 재사용)
    // @Transactional 범위 안에서 FOR UPDATE 락이 유지된다(MyBatis-Spring 트랜잭션)
    @Transactional
    public ReservationResponse confirm(ConfirmRequest req) {
        Slot slot = slotMapper.findForUpdate(
                req.storeId(), req.bayId(), req.date(), req.timeSlot());
        if (slot == null) {
            throw new SlotConflictException("슬롯을 찾을 수 없습니다.");
        }

        if (slot.getStatus() != SlotStatus.HOLDING && slot.getStatus() != SlotStatus.AVAILABLE) {
            // 이미 다른 사용자가 확정함 → 충돌
            throw new SlotConflictException("선택하신 슬롯이 방금 예약되었습니다.");
        }
        slot.reserve();
        // 슬롯 상태 반영(잠근 행이므로 안전). 낙관적 경로라면 영향 행 수 0 → 충돌 처리
        slotMapper.updateStatusWithVersion(slot.getId(), slot.getStatus().name(), slot.getVersion());

        Reservation reservation = Reservation.builder()
            .userId(req.userId())
            .storeId(req.storeId())
            .bayId(req.bayId())
            .managerId(req.managerId())
            .date(req.date())
            .timeSlot(req.timeSlot())
            .carType(req.carType())
            .serviceType(req.serviceType())
            .amount(req.amount())
            .status(ReservationStatus.RESERVED)
            .build();
        reservationMapper.insert(reservation);   // INSERT 시 슬롯 UNIQUE가 최종 방어선
        return ReservationResponse.from(reservation);
    }
}
```

`exception/GlobalExceptionHandler.java` (충돌 → 409)
```java
package com.carwash.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // UNIQUE 제약 위반(최종 방어선) → 409 (require 7.3)
    @ExceptionHandler({SlotConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("SLOT_CONFLICT", "다른 슬롯을 선택해 주세요."));
    }
}
```

FE `app/services/reservationService.ts` (ROADMAP_1 4.4 TODO 교체)
```ts
import type { Reservation } from '~/types/domain'

// 1차 TODO(2단계) 교체: 클라이언트 검증 → 서버 확정 위임
// 409(충돌) 시 false 반환 → 호출부가 1차 재선택 토스트 재사용
export async function confirmReservation(payload: Reservation): Promise<boolean> {
  try {
    await $fetch(`${useRuntimeConfig().public.apiBase}/reservations/confirm`, {
      method: 'POST',
      body: payload,
    })
    return true
  } catch (e: any) {
    if (e?.response?.status === 409) return false // 충돌 → 재선택 유도
    throw e
  }
}
```

#### 완료기준 (DoD) — ✅ 2026-06-21 충족(Phase 4 완료)
- [x] 동일 슬롯에 **동시 확정 요청 시 한 건만 성공**, 나머지는 409를 받는다 (ReservationConcurrencyTest: 16스레드 동시 confirm → success==1, 나머지 충돌)
- [x] 409 응답 시 FE가 1차 재선택 토스트("다른 슬롯을 선택해 주세요.")를 노출하고 그리드를 새로고침한다 (reservation.ts confirmReservation false→slot.vue 기존 토스트, 슬롯 재로드)
- [x] 슬롯 `UNIQUE` 위반이 500이 아닌 **409**로 매핑된다 (GlobalExceptionHandler: DataIntegrityViolation·SlotConflictException→409 SLOT_CONFLICT)
- [x] **1차 예약 위저드 E2E 회귀**(1p→2p→3p)가 서버 예약으로도 통과한다 (reserve.spec 115·188 서버 confirm 통과, 25건)
- [x] `./gradlew build`(34건, 동시성 포함) + `npm run type-check` + `npm run test:e2e`(25건) 통과

> 📌 **2차 구현 방침**: 슬롯 행은 **희소**(점유 시에만 존재). 점유는 **INSERT-on-hold** + `uk_slot` UNIQUE를 동시성 최종 방어선으로(동시 INSERT 1건만 통과→나머지 409). confirm은 **낙관락**(`updateStatusWithVersion` 영향행수 0=충돌) + reservation insert를 한 트랜잭션으로, **비관락**(`SELECT ... FOR UPDATE`)은 시연 경로. 보호 API(`/api/reservations/**` JWT 필수, userId는 토큰 uid). FE(additive): 슬롯 상태는 `GET /api/slots` 배치 하이드레이트(useState 캐시, 동기 getStatus 유지), confirm만 `$apiFetch` 서버 위임(reservation.ts/draft/slot.vue 3곳 async). E2E는 슬롯이 서버 전역 자원이라 spec별 유니크 날짜로 조정.

#### 구현 메모 (📌)
- 📌 **검증 위치 이동**: ROADMAP_1에서 슬롯 충돌은 **클라이언트 Pinia**가 판정했습니다. 이제 **진실은 서버/DB**입니다. FE의 점유 그리드 disabled는 "사전 차단 UX"로 유지하되, **최종 충돌 판정은 서버 409**가 담당합니다. 1차에서 방어 코드로 잔존시킨 `confirm()` 충돌 분기·`useToast`가 본 Phase에서 **실제 경로로 부활**합니다(ROADMAP_1 Phase 8 메모와 정합).

---

### Phase 5 — 예약 상태 전이 API (완료/취소)

> **공수: 2.5일** · **선행조건: Phase 4** · **require_v1.md 참조: 11.3(프로세스) / FW6·FW7·M4·M5** *(v2.2: 예약 승인 단계 없음 — M6은 휴가/반차 신청으로 재정의)*

> ⚠️ **v1.7 정정**: 구 v2.1은 "예약 승인(M6)"을 언급했으나, require **v1.7**에서 **M6은 "휴가/반차 신청"으로 재정의**되었고 별도의 "예약 승인" 프로세스는 존재하지 않는다(MVP는 `confirm` 직후 즉시 `RESERVED`). 따라서 본 Phase에서 "예약 승인" 개념은 도입하지 않으며, 휴가/반차 신청(M6)·승인(M8)은 **Phase 7**에서 다룬다.

#### 목표
1차의 클라이언트 상태 전이(ROADMAP_1 Phase 6)를 **서버 API**로 교체하고, **세차완료(FW6/M4)·예약취소(FW7/M5)** 를 서버화한다. 상태 가드(불가능한 전이 차단)를 서버에서 강제한다. (별도 "예약 승인" 단계는 v1.7에 없으므로 도입하지 않는다.)

#### 태스크 체크리스트
- [x] `PATCH /api/reservations/{id}/complete` — RESERVED → COMPLETED (FW6/M4) *(P5-4, 204·404·409)*
- [x] `PATCH /api/reservations/{id}/cancel` — RESERVED/HOLDING → CANCELED + 슬롯 release (FW7/M5) *(P5-4)*
- [x] **"예약 승인" 단계 미도입 (v1.7 정합)** — require v1.7에 별도 예약 승인 프로세스가 없음. 1차 parity(confirm→즉시 RESERVED, PENDING 상태 미신설)와 정합. ~~구 표기 "예약 승인(M6)"은 폐기(M6은 v1.7에서 휴가/반차 신청으로 재정의)~~
- [x] 상태 전이 가드: 도메인 메서드(`Reservation.complete()`/`cancel()`)에서 불가능 전이 시 `IllegalStateException` → **409 `INVALID_TRANSITION`** *(P5-1·P5-3)*
- [x] 취소 시 슬롯 release(AVAILABLE) — 그리드 재방문 시 반영 *(P5-3, `releaseOrComplete`)*
- [x] FE `app/stores/reservation`(`completeReservation`/`cancelReservation` async PATCH 위임) — `reservations.vue` 마크업은 무변경(additive)

#### 생성·수정 파일 *(실제 구현 반영)*
`controller/ReservationController.java`(PATCH complete/cancel 추가), `service/ReservationService.java`(complete/cancel + loadOwned/releaseOrComplete), `domain/Reservation.java`·`domain/Slot.java`(도메인 전이 메서드), `mapper/ReservationMapper.java` + XML(updateStatus), `exception/GlobalExceptionHandler.java`(IllegalStateException→409), `test/.../ReservationTransitionApiTest.java`(신규 5건), FE `app/types/domain.ts`(`serverId`), FE `app/stores/reservation.ts`(서버 위임) — **`reservations.vue` 마크업 무변경(additive)**

#### 상태 전이 표 (서버 강제 — require 11.3)

| 현재 상태 | 액션(프로세스) | 다음 상태 | 슬롯 처리 | 비고 |
|---|---|---|---|---|
| `HOLDING` | 취소(FW7) | `CANCELED` | `AVAILABLE` release | 확정 전 취소(11.3 c) |
| `RESERVED` | 취소(FW7/M5) | `CANCELED` | `AVAILABLE` release | 확정 후 취소(11.3 b) |
| `RESERVED` | 세차완료(FW6/M4) | `COMPLETED` | `COMPLETED` 고정 | 후기 작성 자격 부여 |
| `COMPLETED` | (불가) | — | — | 전이 차단(400/409) |

#### 구현 예시 — 도메인 전이 메서드 & 가드

`domain/Reservation.java` (전이 메서드)
```java
// 상태 전이는 도메인 메서드로 — 불가능한 전이는 예외 (require 11.3)
public void complete() {
    if (this.status != ReservationStatus.RESERVED) {
        throw new IllegalStateException("세차완료 불가 상태: " + this.status);
    }
    this.status = ReservationStatus.COMPLETED;
}

public void cancel() {
    if (this.status == ReservationStatus.COMPLETED || this.status == ReservationStatus.CANCELED) {
        throw new IllegalStateException("취소 불가 상태: " + this.status);
    }
    this.status = ReservationStatus.CANCELED;
}
```

`service/ReservationService.java` (취소 + 슬롯 release)
```java
// 취소: 상태 전이 + 슬롯을 AVAILABLE로 release (FW7, require 11.3)
@Transactional
public void cancel(Long reservationId) {
    Reservation reservation = reservationMapper.findById(reservationId);
    if (reservation == null) {
        throw new DomainException("예약을 찾을 수 없습니다.");
    }
    reservation.cancel();   // 불가능 전이 시 예외 → 400
    reservationMapper.updateStatus(reservation.getId(), reservation.getStatus().name());

    // 슬롯 다시 AVAILABLE (매퍼로 단건 조회 → release → 상태 UPDATE)
    Slot slot = slotMapper.findByKey(
            reservation.getStoreId(), reservation.getBayId(),
            reservation.getDate(), reservation.getTimeSlot());
    if (slot != null) {
        slot.release();
        slotMapper.updateStatusWithVersion(slot.getId(), slot.getStatus().name(), slot.getVersion());
    }
}
```

#### 완료기준 (DoD)
- [x] RESERVED 예약을 세차완료로 전이할 수 있다(FW6/M4) *(전이 통합테스트 `세차완료…COMPLETED`)*
- [x] 승인 전/후 취소가 모두 동작하고 슬롯이 다시 AVAILABLE이 된다(FW7/M5, require 11.3 b/c) *(`예약취소…AVAILABLE release`)*
- [x] 불가능한 전이(COMPLETED→CANCELED 등)가 **서버에서 차단**된다(409 `INVALID_TRANSITION`) *(`불가능한_전이_COMPLETED_재취소는_409`)*
- [x] **1차 목록·취소·완료 E2E 회귀**가 서버 전이로도 통과한다 *(reservations.spec 완료/취소·release, review.spec complete→후기 — E2E 25건 green)*
- [x] `./gradlew build`(BE 39건 0실패, 전이 5건 포함), `npm run test:e2e`(25건) 통과

#### 구현 메모 (📌)
- 📌 **"예약 승인" 단계 부재 확정(v1.7 정합)**: require v1.7에는 별도의 예약 승인 프로세스가 없으며, MVP는 `confirm`이 곧장 RESERVED를 만든다(ROADMAP_1 Phase 6). 따라서 예약에 `PENDING` 중간 상태를 두지 않는다(reservations.spec 회귀 보존). ~~구 표기 "예약 승인(M6)"은 폐기~~ — v1.7에서 **M6은 휴가/반차 신청**으로 재정의되어 **Phase 7**에서 다룬다(예약 흐름과 무관).
- 📌 **id 분리(serverId)**: BE `confirm`은 서버가 부여한 예약 id(`rsv-<UUID>`)를 `ReservationResponse`로 반환한다. FE는 로컬 표시 id(`rsv-N`, 결정적 시퀀스 — testid용)는 유지하되, 응답 id를 `Reservation.serverId`(옵셔널)로 캡처해 전이 PATCH `…/{serverId}/complete|cancel`에 사용한다. 표시 id와 서버 id를 분리해 1차 E2E testid 정합과 서버 전이를 동시에 만족.
- 📌 **전이 가드 위치**: 불가능 전이 차단은 도메인 메서드(`Reservation.complete()`/`cancel()`)가 `IllegalStateException`을 던지고, `GlobalExceptionHandler`가 이를 **409 `INVALID_TRANSITION`** 으로 매핑. 소유자 불일치/미존재는 서비스에서 `ResponseStatusException(404)`. 취소 시 슬롯은 `releaseOrComplete`가 `findByKey→release→updateStatusWithVersion(AVAILABLE)`로 release.

---

### Phase 6 — BO: 예약 대행 + 매장 관리

> **공수: 3.5일** · **선행조건: Phase 5** · **require_v1.md 참조: 3.2(권한 매트릭스), 11.1(프로세스) / M3·S4·S5**

#### 목표
1차에서 문서화만 했던 BO를 착수한다. **매니저 예약 대행(M3)** — 매니저가 소속 매장 기준으로 사용자 예약을 대행 — 과 **관리자 매장별 예약자 관리(S4)·사용자 관리(S5)** 를 구현한다. 인가는 Phase 3의 역할 가드를 재사용한다.

#### 태스크 체크리스트
- [x] `POST /api/manager/reservations` — 매니저 대행 예약(M3), 소속 매장·본인 휴무 반영(require 6.2) *(P6-3·P6-5)*
- [x] 대행 예약도 동일 동시성 경로(Phase 4 `confirm`) + 동일 베이 노출 규칙(Phase 0 Q7) 재사용 *(proxyReserve가 reservationService.confirm 위임)*
- [x] `GET /api/admin/stores/{id}/reservations` — 매장별 예약자 관리(S4, require 11.1) *(P6-4·P6-5)*
- [x] `GET /api/admin/stores/{id}/users` — 매장별 사용자 관리(S5) *(P6-4·P6-5)*
- [x] 역할 인가: M3=MANAGER/STORE_ADMIN, S4·S5=ADMIN (require 3.2) *(SecurityConfig 경로 기반)*
- [x] FE BO 화면(신규): 매니저 대행 예약(`manager/reserve.vue`), 관리자 예약자/사용자 관리(`admin/stores/[id]/reservations.vue`·`users.vue`)
- [x] FE 라우트 가드 `role-guard` 적용 — 권한 외 접근 차단(`meta.roles`)

#### 생성·수정 파일
`controller/ManagerReservationController.java`, `controller/AdminController.java`, `service/ManagerReservationService.java`, `service/AdminService.java`, `dto/ProxyReservationRequest.java`, `dto/AdminReservationResponse.java`, `dto/AdminUserResponse.java`, FE `app/pages/manager/reserve.vue`(신규), FE `app/pages/admin/stores/[id]/reservations.vue`·`users.vue`(신규), FE `app/middleware/role-guard.ts`(적용)

#### 권한 매트릭스 매핑 (require 3.2)

| 액션 | 프로세스 | 허용 역할 | 엔드포인트 |
|---|---|---|---|
| 예약 대행 | M3 | MANAGER, STORE_ADMIN | `POST /api/manager/reservations` |
| 매장별 예약자 관리 | S4 | ADMIN | `GET /api/admin/stores/{id}/reservations` |
| 매장별 사용자 관리 | S5 | ADMIN | `GET /api/admin/stores/{id}/users` |

#### 구현 예시 — 매니저 대행 예약(M3) & 역할 인가

`controller/ManagerReservationController.java`
```java
package com.carwash.controller;

import com.carwash.dto.ProxyReservationRequest;
import com.carwash.dto.ReservationResponse;
import com.carwash.service.ManagerReservationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/reservations")
public class ManagerReservationController {

    private final ManagerReservationService service;

    public ManagerReservationController(ManagerReservationService service) {
        this.service = service;
    }

    // 매니저 대행 예약 (M3) — 소속 매장·본인 휴무 반영 (require 6.2, 3.2)
    @PreAuthorize("hasAnyRole('MANAGER', 'STORE_ADMIN')")
    @PostMapping
    public ReservationResponse proxyReserve(@Valid @RequestBody ProxyReservationRequest req) {
        return service.proxyReserve(req);  // Phase 4 confirm 경로 재사용(동시성 동일)
    }
}
```

#### 완료기준 (DoD)
- [x] 매니저가 소속 매장 기준으로 사용자 예약을 대행할 수 있고, 본인 휴무 시간대는 차단된다(M3, require 6.2) *(BoApiTest 대행 성공·휴무 400)*
- [x] 대행 예약이 일반 예약과 **동일한 동시성/베이 노출 규칙**을 따른다(Phase 0 Q7) *(confirm 위임 — insertHold·UNIQUE·낙관락 동일)*
- [x] 관리자가 매장별 예약자(S4)·사용자(S5) 목록을 조회할 수 있다(require 11.1) *(BoApiTest S4·S5 200, BO E2E 조회)*
- [x] 권한 외 역할이 BO 엔드포인트 호출 시 403을 받는다(require 3.2) *(USER 대행 403·MANAGER admin 403)*
- [x] `./gradlew build`(BO 8건 포함 전건 green)·`npm run type-check`(0) + BO E2E(대행/조회/role-guard 4건) + 1차 회귀 25건 = **E2E 29건** 통과

#### 구현 메모 (📌)
- 📌 **인가 방식 — 경로 기반 채택**: `@PreAuthorize`(메서드 보안) 대신 `SecurityConfig.authorizeHttpRequests`에서 `/api/manager/**`→`hasAnyRole(MANAGER,STORE_ADMIN)`, `/api/admin/**`→`hasRole(ADMIN)`으로 중앙집중 인가. 기존 permitAll/anyRequest 설정과 단일 소스로 일관(구체 매처를 `anyRequest` 이전 배치). JWT 필터가 이미 `ROLE_`+role 권한을 세팅하므로 `@EnableMethodSecurity` 불필요. 권한 외→403(기본 AccessDeniedHandler), 미인증→401(기존 entryPoint).
- 📌 **대행 고객 식별 — 이메일 기반**: User(로그인 계정)와 Manager(`mgr1`~) 엔티티가 분리돼 있어, `ProxyReservationRequest`는 대행 고객을 **이메일**로 받고 서버가 `userMapper.findByEmail`로 해석(미존재 404). 대행 매니저는 `managerId`로 받아 **소속 매장 일치**(불일치 400)와 **본인 휴무**(서버 SHIFT 매핑, 400)를 검증한 뒤 `reservationService.confirm(고객id, ConfirmRequest{managerId})`로 위임 → Phase 4 동시성 경로 그대로 재사용(충돌 409).
- 📌 **S5 정의**: users 테이블에 store 소속 컬럼이 없으므로 "매장별 사용자"는 **해당 매장에 예약 이력이 있는 고객(distinct, 최초 등장 순)**으로 정의. `userMapper.findAll()` 1회 맵으로 조인(N+1 회피), `passwordHash` 미노출.
- 📌 **휴무 SHIFT 서버 검증**: FE `storeService.isManagerOffAt`와 동일 경계(SHIFT_1 06:00~14:00 / SHIFT_2 14:00~22:00 / SHIFT_3 22:00~06:00, FULL_DAY 전일)를 `ManagerReservationService`에 복제.
- 📌 **additive**: 기존 컴포넌트·페이지·AppNav 무변경, BO는 신규 파일(미들웨어 1·페이지 3·컨트롤러 2·서비스 2·DTO 3·매퍼 메서드 1). BO E2E는 AppNav 링크 없이 직접 `goto`.

---

### Phase 7 — 휴가/반차 1단계 승인 + 가입 2단계 승인 + 매장휴일

> **공수: 3.5일** · **선행조건: Phase 6** · **require_v1.md 참조: 8장(휴가/반차 1단계 결재), 5.5(교대조 휴무), 4.4·11.2(가입 2단계 승인) / M6·M7·M8·S3·8.1**

> ⚠️ **v1.7 정정 (단계 수 분리 — 매우 중요)**: 본 Phase는 require **v1.7**에 맞춰 **두 종류의 승인 워크플로우를 단계 수를 구분**하여 구현한다.
> - **휴가/반차(매니저 휴무) = 1단계 승인**: 일반매장매니저(`MANAGER`)가 신청(M6) → **매장매니저관리자(`STORE_ADMIN`)가 `SUBMITTED → APPROVED`로 종결**(M8). **관리자(`ADMIN`)는 개입하지 않는다**(require §8.2·§8.3).
> - **매니저 가입 = 2단계 승인**: `EMAIL_VERIFIED → ` 매장매니저관리자 1차(M7) → 관리자 2차(S3) → `ACTIVE`(require §4.4·§11.2).
> - 즉 **휴가/반차는 1단계, 가입은 2단계**로 단계 수가 다르다. ~~구 v2.1의 "휴무 2단계 결재(`SUBMITTED→APPROVED_L1→APPROVED_L2→CONFIRMED`, 최고매니저→관리자)"는 v1.7과 충돌하므로 폐기~~.

#### 목표
require v1.7의 **두 승인 워크플로우**를 구현한다.
1. **휴가/반차 1단계 결재 상태머신**(`SUBMITTED → APPROVED / REJECTED`)을 구현하고, 일반매장매니저 신청(M6) → 매장매니저관리자 승인(M8)으로 **종결**한다(관리자 미개입). 휴무 유형(FULL_DAY/SHIFT_n, require 5.5)은 전일/교대조 구분 없이 동일 1단계 흐름을 따른다(require §8.2).
2. **가입 2단계 승인**(M7 매장매니저관리자 1차 → S3 관리자 2차)을 구현하여 매니저 계열 가입자를 `PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2 → ACTIVE`로 전이한다(require §4.4). 두 워크플로우는 "신청 → 검토자 승인" 패턴을 공유하되 **단계 수가 다르다**.
3. **매장 휴일(8.1)**: 등록 주체는 require v1.7에서 추후 확정(관리자 또는 매장매니저관리자 운영 가정, §13.2). 본 로드맵은 매니저 신청 → 관리자 승인의 단일 승인으로 처리한다.

#### 태스크 체크리스트 — ✅ 2026-06-21 완료(v1.7 재정합)
- [x] `ManagerDayoff`에 **휴가/반차 1단계** `DayoffApprovalStatus` enum(`SUBMITTED/APPROVED/REJECTED`, require §8.3) — 구 `APPROVED_L1/L2` 2단계 enum을 1단계로 정정(공용 `ApprovalStatus`는 휴일 전용 잔존)
- [x] `POST /api/manager/dayoffs` — 일반매장매니저 휴가/반차 신청(M6, `SUBMITTED`), 휴무유형 포함(require 5.5)
- [x] `PATCH /api/store-admin/dayoffs/{id}/approve` — **매장매니저관리자 1단계 승인(M8, `SUBMITTED → APPROVED`로 종결)** — 구 `approve-l1`/`approve-l2`를 단일 `approve`로 정정, 관리자 `approve-l2` 제거(StoreAdminDayoffController 신설)
- [x] `PATCH .../reject` + `/resubmit` — 반려(→ `REJECTED`) 후 재신청(→ `SUBMITTED`)
- [x] **가입 2단계 승인(M7→S3)**: `User`에 `UserApprovalStatus(PENDING_APPROVAL_L1/L2/ACTIVE/REJECTED)` + `PATCH /api/store-admin/manager-signups/{id}/approve`(1차, `L1→L2`) + `PATCH /api/admin/manager-approvals/{id}/confirm`(2차, `L2→ACTIVE`) + 반려(`REJECTED`). 로그인은 `ACTIVE`만 허용(403 게이팅)
- [x] 매장 휴일(8.1): 매니저 신청(`POST /api/manager/holidays`) → 관리자 단일 승인(`PATCH /api/admin/holidays/{id}/approve`) — 기존 구현 유지
- [x] `APPROVED`(휴가/반차) 시 슬롯 비활성 반영: FULL_DAY=그날 전체, SHIFT_n=해당 교대 시간대만(require 5.5·6.1) — 카탈로그 노출(`status='APPROVED'`만)→`isManagerOffAt` 게이팅
- [x] 역할 인가: **휴가/반차 승인(M8)=STORE_ADMIN 종결**, **가입 1차(M7)=STORE_ADMIN·2차(S3)=ADMIN** (require 3.2·§4.4·§8.3) — SecurityConfig `/api/store-admin/**`=STORE_ADMIN, `/api/admin/**`=ADMIN

> ✅ **현행 코드 재정합 완료(2026-06-21)**: 기존 Phase 7 BE 구현(휴무 2단계 `approve-l1`/`approve-l2`)을 v1.7 1단계로 재정합 완료. ① `ManagerDayoff` 상태 enum을 `DayoffApprovalStatus(SUBMITTED/APPROVED/REJECTED)`로 축소, ② `approve-l2`(관리자 휴무 승인) 엔드포인트·인가·테스트 제거, ③ `approve-l1`을 `/api/store-admin/dayoffs/{id}/approve`(STORE_ADMIN 종결)로 변경, ④ 카탈로그 노출 필터 `status='CONFIRMED'` → `'APPROVED'`. 가입 2단계 승인(M7→S3)은 신규 도입(역할별 BO 페이지 §12.4 분리: `/store-admin/*`·`/admin/manager-approvals` 신설). 역할별 AppNav 메뉴·로그인 STORE_ADMIN 빠른버튼 정합 완료.

#### 생성·수정 파일
`domain/ManagerDayoff.java`(휴가/반차 1단계 승인상태·휴무유형), `domain/StoreHoliday.java`(승인상태), `domain/User.java`(가입 2단계 승인상태 `PENDING_APPROVAL_L1/L2`), `mapper/ManagerDayoffMapper.java`·`mapper/StoreHolidayMapper.java`·`mapper/UserMapper.java` + XML, `controller/DayoffController.java`, `controller/StoreHolidayController.java`, `controller/ManagerSignupController.java`(가입 1차 승인), `controller/AdminApprovalController.java`(가입 2차 승인), `service/DayoffApprovalService.java`(휴가/반차 1단계), `service/SignupApprovalService.java`(가입 2단계), `dto/DayoffRequest.java`, `dto/ApprovalResponse.java`, FE `app/pages/manager/dayoffs.vue`(휴가/반차 신청 M6)·`app/pages/store-admin/dayoff-approvals.vue`(휴가/반차 승인 M8)·`app/pages/store-admin/manager-signups.vue`(가입 1차 승인 M7)·`app/pages/admin/manager-approvals.vue`(가입 2차 승인 S3)(신규)

#### 휴가/반차 1단계 결재 상태 머신 (require §8.3)

| 상태 | 설명 | 다음 상태 | 전이 주체 |
|---|---|---|---|
| `SUBMITTED` | 휴가/반차 신청 상신(M6) | `APPROVED` / `REJECTED` | 신청자(일반매장매니저) |
| `APPROVED` | 승인/확정 → 휴무 확정·슬롯 반영(§6.1) | — | **매장매니저관리자(`STORE_ADMIN`) — 1단계 종결(M8)** |
| `REJECTED` | 매장매니저관리자 거부 | `SUBMITTED`(재신청) | 매장매니저관리자(`STORE_ADMIN`) |

> ⚠️ **휴가/반차는 1단계**(`SUBMITTED → APPROVED`, 매장매니저관리자 종결). **관리자(`ADMIN`)는 개입하지 않는다**(require §8.2·§8.3). 구 2단계(`APPROVED_L1 → APPROVED_L2 → CONFIRMED`) 모델은 폐기.

#### 가입 2단계 승인 상태 머신 (require §4.4)

| 상태 | 설명 | 다음 상태 | 전이 주체 |
|---|---|---|---|
| `PENDING_APPROVAL_L1` | 1차 승인 대기 | `PENDING_APPROVAL_L2` / `REJECTED` | **매장매니저관리자(`STORE_ADMIN`) — 1차(M7)** |
| `PENDING_APPROVAL_L2` | 2차 승인 대기 | `ACTIVE` / `REJECTED` | **관리자(`ADMIN`) — 2차 최종(S3)** |
| `ACTIVE` | 활성 — 로그인 가능 유일 상태 | — | (시스템) |
| `REJECTED` | 어느 단계든 거부 | `REQUESTED`(재신청) | 검토자(STORE_ADMIN/ADMIN) |

> **매장 휴일(8.1)** 은 매니저 신청 → 관리자 승인의 **단일 승인**으로 처리(등록 주체는 require §13.2에서 추후 확정).

> 📌 **단계 수 비교(중요, require §8.3)**: 휴가/반차 승인 = **1단계**(매장매니저관리자 종결). 가입 승인 = **2단계**(매장매니저관리자 1차 → 관리자 2차). 두 흐름을 혼동하지 않는다.

#### 구현 예시 — 휴가/반차 1단계 승인 서비스 + 가입 2단계 승인

`service/DayoffApprovalService.java` (휴가/반차 1단계 — 매장매니저관리자 종결)
```java
// 휴가/반차 1단계 승인 — 매장매니저관리자(STORE_ADMIN)가 종결 (require §8.2·§8.3)
// 관리자(ADMIN) 개입 없음. APPROVED 시 슬롯 비활성 반영
@Transactional
public void approve(Long dayoffId) {
    ManagerDayoff dayoff = dayoffMapper.findById(dayoffId);
    if (dayoff == null) {
        throw new DomainException("휴가/반차 신청을 찾을 수 없습니다.")
    }
    dayoff.approve();   // SUBMITTED → APPROVED (불가 전이 시 예외)
    dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());

    // APPROVED 시 슬롯 비활성 반영 (require 5.5·6.1)
    // FULL_DAY: 그날 전체 슬롯 / SHIFT_n: 해당 교대 시간대 슬롯만
    slotDeactivationService.deactivate(dayoff.getManagerId(), dayoff.getDate(), dayoff.getType());
}
```

`domain/ManagerDayoff.java` (휴가/반차 1단계 전이 메서드)
```java
// 휴가/반차 결재 전이 — 1단계(매장매니저관리자 종결) (require §8.3)
public void approve() {
    if (this.status != DayoffApprovalStatus.SUBMITTED) {
        throw new IllegalStateException("승인 불가 상태: " + this.status)
    }
    this.status = DayoffApprovalStatus.APPROVED   // 1단계 종결 = 확정
}

public void reject() {
    if (this.status != DayoffApprovalStatus.SUBMITTED) {
        throw new IllegalStateException("반려 불가 상태: " + this.status)
    }
    this.status = DayoffApprovalStatus.REJECTED
}
```

`service/SignupApprovalService.java` (매니저 가입 2단계 승인 — M7→S3)
```java
// 가입 1차 승인(M7) — 매장매니저관리자(STORE_ADMIN) (require §4.4)
@Transactional
public void approveL1(Long userId) {
    User user = userMapper.findById(userId);
    user.approveSignupL1();   // PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2
    userMapper.updateApprovalStatus(user.getId(), user.getApprovalStatus().name());
}

// 가입 2차 최종 승인(S3) — 관리자(ADMIN) → ACTIVE (require §4.4)
@Transactional
public void confirmL2(Long userId) {
    User user = userMapper.findById(userId);
    user.confirmSignupL2();   // PENDING_APPROVAL_L2 → ACTIVE (로그인 가능)
    userMapper.updateApprovalStatus(user.getId(), user.getApprovalStatus().name());
}
```

#### 완료기준 (DoD) — ✅ 2026-06-21 충족(v1.7 재정합 완료)
- [x] 휴가/반차가 **1단계 승인**(매장매니저관리자 `SUBMITTED → APPROVED`)으로 **종결**되며, **관리자 승인 단계가 없다**(require §8.2) — ApprovalApiTest: STORE_ADMIN approve 종결·ADMIN 휴무승인 권한없음 403
- [x] 매니저 가입이 **2단계 승인**(매장매니저관리자 1차 M7 → 관리자 2차 S3)을 거쳐 `ACTIVE`가 되고, `ACTIVE` 전에는 로그인 불가다(require §4.4) — SignupApprovalApiTest + e2e 가입 2단계
- [x] 매장 휴일이 단일 승인(매니저 신청 → 관리자)으로 확정된다(require §8.1) — 기존 구현 회귀 통과
- [x] 휴가/반차 `APPROVED` 시 FULL_DAY는 그날 전체, SHIFT_n은 해당 교대 시간대만 슬롯이 비활성된다(require 5.5·6.1) — ApprovalApiTest 대행 차단/허용
- [x] 어느 단계든 반려(REJECTED) 후 재신청이 가능하다
- [x] 권한 외 승인이 차단된다(휴가/반차 승인=STORE_ADMIN, 가입 1차=STORE_ADMIN·2차=ADMIN) (require 3.2·§4.4·§8.3)
- [x] `./gradlew build` + 두 워크플로우(휴가/반차 1단계·가입 2단계) 통합테스트 통과 + `type-check`·`test:e2e`(33건) 회귀 통과

#### 구현 메모 (📌)
- 📌 **워크플로우 엔진 미사용**: require §8.2대로 별도 엔진 없이 **상태값 변경 방식**으로 구현. 휴가/반차는 `DayoffApprovalStatus`(`SUBMITTED/APPROVED/REJECTED`) + 도메인 전이 메서드(`approve`/`reject`/`resubmit`), 가입은 `ApprovalStatus`(`PENDING_APPROVAL_L1/L2/ACTIVE/REJECTED`) + 경로 기반 역할 인가의 조합.
- 📌 **슬롯 비활성 = 승인된 휴무의 카탈로그 반영**: 희소 슬롯 행을 변형하지 않는다. 휴무는 카탈로그와 **동일 `manager_dayoff` 테이블**을 쓰고, 카탈로그 쿼리는 `status='APPROVED'`만 노출(`ManagerMapper` LEFT JOIN 필터). 승인된 휴무는 `Manager.dayoffs`에 나타나 기존 `isManagerOffAt`(FULL_DAY 전일/SHIFT_n 해당 교대) 게이팅으로 **FE 슬롯/시간 선택 비활성 + 서버 대행 예약 차단(400)** 을 동시에 실현. 기존 시드 휴무는 `status DEFAULT 'APPROVED'`로 회귀 안전.
- 📌 **역할 세분 인가**: 휴가/반차 승인(M8)은 `PATCH /api/store-admin/dayoffs/*/approve` → `hasRole('STORE_ADMIN')` 종결(관리자 매처 없음). 가입 1차(M7)는 `PATCH /api/store-admin/manager-signups/*/approve` → STORE_ADMIN, 가입 2차(S3)는 `PATCH /api/admin/manager-approvals/*/confirm` → ADMIN. STORE_ADMIN 데모용 시드 유저(`storeadmin@test.com`) 추가.
- 📌 ⚠️ **현행 코드 재정합 필요(v1.7 충돌)**: 본 저장소의 기존 Phase 7 BE 구현은 휴무를 **2단계(`approve-l1`/`approve-l2`, `ApprovalStatus.APPROVED_L1→CONFIRMED`)** 로 작성되어 있어 v1.7(휴가/반차 1단계 종결)과 **충돌**한다. 재정합 작업: ① `ManagerDayoff` enum을 `SUBMITTED/APPROVED/REJECTED` 1단계로 축소, ② 관리자 휴무 승인(`approve-l2`) 엔드포인트·인가·테스트 제거, ③ `approve-l1`을 `approve`(STORE_ADMIN 종결)로 변경, ④ 카탈로그 노출 필터 `CONFIRMED → APPROVED`, ⑤ 가입 2단계 승인(M7→S3)을 신규 도입. 위 DoD는 이 재정합 기준으로 갱신(체크 해제 = 재정합 미완료).

---

### Phase 8 — 후기/평점 API + BO 확인·매출

> **공수: 2.5일** · **선행조건: Phase 7** · **require_v1.md 참조: 9장(후기/평점), 11.1(프로세스) / S6·S8**

#### 목표
1차의 후기/평점(ROADMAP_1 Phase 7)을 **서버 API**로 교체하고, BO 측 **관리자 후기 확인(S6)** 과 **매출 집계(S8)** 를 구현한다. 후기 작성 자격(세차완료 사용자만, require 9.1)은 서버에서 검증한다.

#### 태스크 체크리스트
- [x] `POST /api/reviews` — 후기 작성, 작성 자격(COMPLETED + 본인 + 중복 방지) 서버 검증(require 9.1) *(P8-2·P8-3)*
- [x] `GET /api/reviews/stores/{id}/average`·`/managers/{id}/average` — 평균 평점 집계 *(Java 집계, AverageRatingResponse)*
- [x] `GET /api/admin/stores/{id}/reviews` — 관리자 매장별 후기 확인(S6, require 11.1)
- [x] `GET /api/admin/stores/{id}/sales` — 매장별 매출 집계(S8) — COMPLETED 예약 금액 합산 *(findByStore 재사용)*
- [x] FE `review/[reservationId].vue`를 서버 API(POST /reviews, serverId)로 교체 + 서버 매장 평균 표시 — `reservations.vue`는 로컬 미러 유지(additive)
- [x] FE BO 화면(신규): 관리자 후기 확인·매출 대시보드(`admin/sales.vue`)

#### 생성·수정 파일
`controller/ReviewController.java`, `controller/AdminReviewController.java`, `controller/SalesController.java`, `service/ReviewService.java`, `service/SalesService.java`, `dto/ReviewRequest.java`, `dto/AverageRatingResponse.java`, `dto/SalesResponse.java`, FE `app/pages/review/[reservationId].vue`(교체), FE `app/pages/admin/sales.vue`(신규)

#### 구현 예시 — 작성 자격 검증 & 매출 집계

`service/ReviewService.java`
```java
// 후기 작성 자격: 세차완료(COMPLETED) + 본인 예약 + 중복 방지 (require 9.1)
@Transactional
public ReviewResponse addReview(ReviewRequest req, Long currentUserId) {
    Reservation reservation = reservationRepository.findById(req.reservationId())
        .orElseThrow(() -> new DomainException("예약을 찾을 수 없습니다."));

    if (reservation.getStatus() != ReservationStatus.COMPLETED) {
        throw new DomainException("세차완료 예약만 후기를 작성할 수 있습니다.");
    }
    if (!reservation.getUserId().equals(currentUserId)) {
        throw new DomainException("본인 예약만 후기를 작성할 수 있습니다.");
    }
    if (reviewRepository.existsByReservationId(req.reservationId())) {
        throw new DomainException("이미 후기를 작성했습니다.");
    }
    // 평점 범위 1~5 (require 9.1)
    Review review = Review.builder()
        .reservationId(req.reservationId())
        .userId(currentUserId)
        .storeId(reservation.getStoreId())
        .managerId(reservation.getManagerId())
        .rating(req.rating())
        .text(req.text())
        .build();
    return ReviewResponse.from(reviewRepository.save(review));
}
```

`service/SalesService.java` (매출 집계 — S8)
```java
// 매장별 매출 = COMPLETED 예약 금액 합산 (S8, require 11.1)
@Transactional(readOnly = true)
public SalesResponse storeSales(Long storeId) {
    long total = reservationRepository
        .sumAmountByStoreIdAndStatus(storeId, ReservationStatus.COMPLETED);
    return new SalesResponse(storeId, total);
}
```

#### 완료기준 (DoD)
- [x] COMPLETED가 아닌/타인/중복 후기 작성이 **서버에서 차단**된다(require 9.1) *(ReviewApiTest 미완료400·타인404·중복409)*
- [x] 평점(1~5) 범위를 벗어나면 거부된다(Bean Validation) *(`평점_범위_초과는_400`, `@Min(1)@Max(5)`)*
- [x] 매장/매니저 평균 평점이 서버 집계로 표시된다 *(GET .../average + review 페이지 avg-store)*
- [x] 관리자가 매장별 후기(S6)·매출(S8)을 조회할 수 있다(권한 인가) *(ADMIN 200·USER 403, admin/sales.vue)*
- [x] **1차 후기 E2E 회귀**(자격 가드·평점 제출·평균)가 서버 API로도 통과한다 *(review.spec 2건 — 서버 POST 위임 후 로컬 미러 표시)*
- [x] `./gradlew build`(후기/매출 8건 포함 green)·`npm run test:e2e`(31건) 통과

#### 구현 메모 (📌)
- 📌 **후기 식별 = 예약 serverId**: 후기는 BE 예약 id(`serverId`)를 `reservationId`로 POST해 서버가 `reservationMapper.findById`로 자격(COMPLETED·본인=JWT uid·중복=`countByReservationId`)을 검증. 미존재/타인 404, 미완료 400, 중복 409, 평점 범위 400(Bean Validation). FE 표시 id(`rsv-N`)와 분리(Phase 5 `serverId` 재사용).
- 📌 **로컬 미러 유지(additive)**: 서버 POST 성공 시 기존 클라이언트 `reviewStore`에도 add해 1차 표시 로직(`avg-overall`/`my-rating`/`my-review-text`/`reviewed-*`, `review-guard`, `reservations.vue`)을 무변경 유지 → review.spec 회귀 그린. 서버 집계는 매장 평균(`avg-store`)으로 **추가** 표시.
- 📌 **매출 집계(S8) = 기존 매퍼 재사용**: `SalesService`가 `ReservationMapper.findByStore`(Phase 6)에서 COMPLETED 예약 `amount`를 Java로 합산 — 별도 집계 SQL 없이 구현. 후기 평균도 `findByStore`/`findByManager` 목록을 Java 집계.

---

### Phase 9 — 알림: SMTP 인프라 + 정책

> **공수: 2.5일** · **선행조건: Phase 3·7(인증·결재 이벤트 발생 지점)** · **require_v1.md 참조: 13.2 항목 6·7**

#### 목표
require 13.2가 2차 과제로 이관한 **SMTP 발송 인프라 선정(항목 6)** 과 **알림(메일/푸시) 정책 수립(항목 7)** 을 구현한다. Phase 3에서 인터페이스만 선언한 `EmailSender`를 실제 SMTP 구현으로 채우고, 발송 시점(이메일 인증·예약 확정·결재 결과)을 정책으로 정의한다.

#### 태스크 체크리스트
- [ ] `EmailSender` 구현(`JavaMailSender` 기반 SMTP) — Phase 3 스텁 교체
- [ ] `application.yml`에 `spring.mail.*`(host/port/username/password) 설정
- [ ] 발송 정책 정의: ① 이메일 인증 링크(require 4.4), ② 예약 확정 안내, ③ 결재 결과(승인/반려) 통지
- [ ] **비동기 발송**(`@Async`) + 실패 재시도/로깅 — 발송 실패가 본 트랜잭션을 막지 않도록 분리
- [ ] (선택) 푸시 채널 추상화(`NotificationChannel`) — 메일/푸시 정책 확장 포인트
- [ ] 발송 이력 로깅(추적성)

#### 생성·수정 파일
`service/SmtpEmailSender.java`, `service/NotificationService.java`, `config/AsyncConfig.java`, `application.yml`(수정 — `spring.mail`), `domain/NotificationLog.java` + `mapper/NotificationLogMapper.java`(선택)

#### 알림 정책 표 (require 13.2 항목 7)

| 알림 | 트리거(시점) | 채널 | 근거 |
|---|---|---|---|
| 이메일 인증 링크 | 가입(REQUESTED) | 메일 | require 4.4 |
| 예약 확정 안내 | 예약 RESERVED | 메일(추후 푸시) | require 6장 |
| 승인 결과 통지 | 휴가/반차 APPROVED/REJECTED(1단계)·가입 승인 단계 전이(L1→L2→ACTIVE/REJECTED)·휴일 승인 | 메일 | require 4·8장 |

#### 구현 예시 — SMTP 발송 & 비동기

`service/SmtpEmailSender.java`
```java
package com.carwash.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 비동기 발송 — 본 트랜잭션을 막지 않음 (발송 실패가 가입/예약을 롤백시키지 않도록 분리)
    @Async
    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
```

#### 완료기준 (DoD)
- [ ] SMTP 설정으로 실제(또는 테스트용 메일 캐처) 메일이 발송된다
- [ ] 이메일 인증·예약 확정·결재 결과 시점에 정책대로 알림이 발송된다(정책 표 3건)
- [ ] 발송이 **비동기**로 처리되어 발송 실패가 본 트랜잭션(가입/예약/결재)을 롤백시키지 않는다
- [ ] 발송 이력이 로깅된다(추적성)
- [ ] `./gradlew build` 통과

#### 구현 메모 (📌)
- 📌 **개발 환경 메일 캐처**: 실제 SMTP 대신 MailHog/Mailpit 같은 로컬 메일 캐처로 발송을 검증하면 외부 의존 없이 테스트할 수 있습니다. `application.yml`의 `spring.mail.host`만 로컬 캐처로 바꾸면 됩니다.

---

### Phase 10 — 데이터 3단계: MySQL 이행

> **공수: 3.5일** · **선행조건: Phase 1~9(2단계 H2 완료)** · **require_v1.md 참조: 7장(동시성 3단계), 12장(데이터 진화)**

#### 목표
2단계 H2(in-memory, 휘발성)를 **MySQL(운영 영속화)** 로 이행한다. 트랜잭션 + **유니크 인덱스**로 중복 예약을 DB 레벨에서 원천 차단하고(require 7·12.3), **Flyway**로 스키마 마이그레이션을 관리한다. **MyBatis는 ORM 자동 DDL이 없으므로** 2단계 `schema.sql`을 Flyway 마이그레이션으로 이관한다. 충돌 시 409 → 1·2단계의 재선택 UX를 그대로 재사용한다.

#### 태스크 체크리스트
- [ ] `application.yml`(또는 `application-prod.yml`)에 MySQL 데이터소스 설정
- [ ] 2단계 `spring.sql.init`(schema.sql/data.sql 자동 실행)을 **운영 프로파일에서 비활성**하고 스키마 소유권을 Flyway로 이전
- [ ] Flyway 도입 — `db/migration/V1__init_schema.sql`(2단계 `schema.sql`의 DDL + 유니크 인덱스 이관)
- [ ] 슬롯 `UNIQUE(store_id, bay_id, date, time_slot)` 를 **DB 유니크 인덱스**로 확정(최종 방어선)
- [ ] MyBatis 매퍼 SQL이 MySQL 방언에서도 동일 동작하는지 확인(`FOR UPDATE`·예약어·날짜 함수)
- [ ] 트랜잭션 격리/락 동작이 MySQL에서도 동일한지 동시성 통합테스트 재실행(6장)
- [ ] 운영 프로파일 분리(`spring.profiles.active`), 시드/마이그레이션 전략 정리
- [ ] (선택) 인증/진행상태 영속화 — JWT·Redis 등 운영 인프라 검토(require 12.3 교체 경계)

#### 생성·수정 파일
`backend/src/main/resources/application-prod.yml`, `backend/src/main/resources/db/migration/V1__init_schema.sql`, `backend/build.gradle`(MySQL 드라이버·Flyway 의존성), `application.yml`(운영 프로파일에서 `spring.sql.init` 비활성)

#### 구현 예시 — Flyway 마이그레이션(유니크 인덱스) & MySQL 설정

`db/migration/V1__init_schema.sql` (슬롯 유니크 인덱스 = 최종 방어선)
```sql
-- 슬롯: (매장, 베이, 날짜, 시간) 유니크 인덱스로 중복 INSERT 원천 차단 (require 7·12.3)
CREATE TABLE slot (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id    BIGINT NOT NULL,
    bay_id      BIGINT NOT NULL,
    date        VARCHAR(10) NOT NULL,
    time_slot   VARCHAR(5)  NOT NULL,
    status      VARCHAR(20) NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_slot_store_bay_date_time UNIQUE (store_id, bay_id, date, time_slot)
);
-- price, reservation, manager_dayoff 등 나머지 테이블도 동일 파일에 정의
```

`application-prod.yml` (MySQL + Flyway가 스키마 소유)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/carwash?serverTimezone=Asia/Seoul
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never            # 운영: schema.sql 자동 실행 비활성 (스키마 소유권은 Flyway)
  flyway:
    enabled: true
    baseline-on-migrate: true
# MyBatis 매퍼 설정은 공통 application.yml 그대로 사용 (JPA 없음)
```

#### 완료기준 (DoD)
- [ ] MySQL로 기동되고, BE 재기동 후에도 **데이터가 유지**된다(영속화, 2단계 휘발성 해소)
- [ ] Flyway 마이그레이션이 적용되어 스키마가 생성되고, 운영에서 `spring.sql.init`이 비활성이다
- [ ] 슬롯 DB **유니크 인덱스**로 동시 중복 INSERT가 원천 차단되고, 충돌은 409로 응답된다(require 7·12.3)
- [ ] **동시성 통합테스트가 MySQL에서도 통과**(6장) — 한 건만 성공
- [ ] **1차 전체 Playwright E2E 회귀**가 MySQL 백엔드로도 통과한다
- [ ] `./gradlew build` 통과

#### 구현 메모 (📌)
- 📌 **`schema.sql` → Flyway 전환의 의미**: 2단계(H2)는 `spring.sql.init`이 `schema.sql`을 기동 시 실행했지만, 3단계(운영)는 **Flyway가 스키마의 단일 소유자(SSOT)** 입니다. MyBatis는 ORM 자동 DDL이 없으므로 두 단계 모두 스키마를 SQL로 직접 관리하며, 운영에서는 버전 관리되는 Flyway 마이그레이션만 적용해 의도치 않은 스키마 변경을 막습니다.

---

## 4. 동시성 처리 구현 가이드 (2·3단계 집중)

> **require_v1.md 참조: 7장(특히 7.3 락 전략)**

본 장은 ROADMAP_1 4장(1단계 클라이언트 시뮬레이션)을 이어받아, 검증 위치를 **서버(2단계)·DB(3단계)** 로 옮기는 가이드입니다.

### 4.1 핵심 — UNIQUE 제약을 최종 방어선으로

슬롯 status 전이는 1차와 동일하지만, **진실의 소유자가 클라이언트 Pinia → 서버/DB**로 바뀝니다.

```
AVAILABLE ──(서버 hold)──▶ HOLDING ──(서버 confirm + 락)──▶ RESERVED ──(complete)──▶ COMPLETED
    ▲                                  │
    └────────(release/취소/409 충돌)─────┘
```

> 슬롯 `UNIQUE(store_id, bay_id, date, time_slot)`은 모든 단계의 **최종 방어선**입니다(require 7.3). 락이 뚫려도 DB 유니크가 중복 INSERT를 막습니다.

### 4.2 낙관적 vs 비관적 락 선택 기준

| 기법 | 적합 상황 | 구현 위치 | 충돌 처리 |
|---|---|---|---|
| 낙관적 락 (version 컬럼) | 충돌 빈도 **낮음**(대부분 슬롯) | `UPDATE ... WHERE version=?` 영향 행 수 | 0 행 → 충돌 감지 → 재시도 또는 409 |
| 비관적 락 (`FOR UPDATE`) | 충돌 빈도 **높음**(인기 슬롯) | 매퍼 XML `SELECT ... FOR UPDATE` | 락 대기로 직렬화, 데드락 주의 |
| UNIQUE 인덱스 | 전 슬롯(최종) | DB 제약 | `DataIntegrityViolationException` → 409 |

> **권장(require 7.3)**: 기본은 낙관적 락, 경합이 잦은 인기 슬롯만 비관적 락. 그 아래 항상 UNIQUE 인덱스를 깐다.

### 4.3 충돌 시 UX (1차 토스트 재사용)

- 서버가 충돌을 감지하면 **409 Conflict** + `{"code":"SLOT_CONFLICT"}` 를 응답합니다.
- FE는 409를 받으면 **1차 `useToast` 재선택 토스트**("선택하신 슬롯이 방금 예약되었습니다. 다른 슬롯을 선택해 주세요.")를 노출하고 그리드를 새로고침합니다(ROADMAP_1 4.3 그대로).

### 4.4 ROADMAP_1 4.4 교체 지점 이어받기

> ROADMAP_1 4.4의 `// TODO(2단계)` / `// TODO(3단계)` 주석 위치가 본 로드맵에서 **실제 코드**가 됩니다.

| 단계 | ROADMAP_1 TODO 위치 | 본 로드맵 교체 | Phase |
|---|---|---|---|
| **2단계** | `confirmReservation()` 내부 검증(클라이언트) | `reservationService` → 서버 `POST /reservations/confirm` + 락 | Phase 4 |
| **3단계** | `reservationService` 서버 구현 | MySQL **트랜잭션 + 유니크 인덱스** + 409 | Phase 10 |

> 💡 **설계 의도 회수**: 컴포넌트·스토어가 `reservationService`만 호출하도록 둔 1차 설계 덕에, 검증 위치가 클라이언트 → 서버 → DB로 이동해도 **UI 코드는 거의 그대로**입니다(ROADMAP_1 4.4·5.1과 정합).

---

## 5. 데이터 3단계(MySQL) 마일스톤

> 상세 작업은 **Phase 10**에 있습니다. 여기서는 2단계(H2) → 3단계(MySQL) 이행의 핵심 축만 요약합니다.

### 5.1 트랜잭션 + 유니크 인덱스

- 슬롯 `UNIQUE(store_id, bay_id, date, time_slot)` 를 **DB 유니크 인덱스**로 확정 — 중복 INSERT 원천 차단(require 7·12.3).
- 쓰기 경로는 `@Transactional`, 충돌은 409 → 1·2단계 재선택 UX 재사용.

### 5.2 운영 영속화 & 마이그레이션

- H2 in-memory(휘발) → MySQL(영속). 재기동 후 데이터 유지.
- **Flyway**로 스키마 버전 관리(`V1__init_schema.sql` …). 2단계 `schema.sql`을 Flyway로 이관하고 운영에서 `spring.sql.init`을 비활성(MyBatis는 ORM 자동 DDL 없음).
- (선택) 인증/진행상태 영속화: JWT·Redis 등 운영 인프라 검토(require 12.3 교체 경계).

---

## 6. 테스트 전략 (Spring 통합테스트 + Playwright 회귀)

### 6.1 Spring 통합테스트 — 동시 예약 락 검증

2차의 핵심 리스크는 **동시성**입니다. `@SpringBootTest` + 멀티스레드로 "동시 N요청 중 한 건만 성공"을 검증합니다.

```java
import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReservationConcurrencyTest {

    @Autowired ReservationService reservationService;

    // 동일 슬롯에 동시 확정 요청 → 정확히 1건만 성공 (require 7.3)
    @Test
    void 동일_슬롯_동시_확정시_한건만_성공한다() throws InterruptedException {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    reservationService.confirm(sampleConfirmRequest());
                    results.add(true);
                } catch (Exception e) {
                    results.add(false); // 충돌(409)로 실패
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        long success = results.stream().filter(Boolean::booleanValue).count();
        assertThat(success).isEqualTo(1); // 정확히 1건만 성공
    }
}
```

> ⚠️ **H2 vs MySQL 락 차이**: H2(2단계)와 MySQL(3단계)의 락/격리 동작이 미묘하게 다를 수 있습니다. **Phase 4(H2)와 Phase 10(MySQL)에서 동일 통합테스트를 각각 재실행**해 양쪽 모두 "한 건만 성공"을 보장하세요.

### 6.2 Playwright E2E 회귀 유지

> 1차 Playwright E2E(ROADMAP_1 6장, 전체 21/21)는 **삭제하지 않고 회귀 스위트로 유지**합니다. 데이터 출처가 더미 → 서버로 바뀌어도 화면 동작은 동일해야 합니다.

- [ ] 각 Phase(2·3·4·5·8) 완료 시 **1차 E2E 전체 회귀** 실행 — 서버 데이터로도 통과 확인
- [ ] BO 신규 화면(Phase 6·7·8)에 대한 E2E 시나리오 추가(대행 예약·결재 승인·매출 조회)
- [ ] `data-testid` 셀렉터 컨벤션(ROADMAP_1 6.3) 유지

### 6.3 테스트 작성 팁

- **결정적 테스트**: 시드 데이터(`DataSeeder`)는 고정값이므로 특정 매장/슬롯/가격을 단정(assert)할 수 있습니다. 동시성 테스트는 고정 슬롯을 사용하세요.
- **통합테스트 격리**: `@Transactional` 롤백 또는 `@DirtiesContext`로 테스트 간 상태 오염을 막으세요(단, 동시성 테스트는 실제 커밋이 필요하므로 별도 정리 전략 사용).

---

## 7. 코딩 컨벤션 & PR 체크리스트

### 7.1 브랜치 전략

| 브랜치 | 용도 |
|---|---|
| `main` | 배포 가능한 안정 상태 |
| `feat/phase{N}-{설명}` | Phase별 기능 개발 (예: `feat/phase4-reservation-lock`) |
| `fix/{설명}` | 버그 수정 |

> 작은 단위 PR 원칙: 한 PR = 한 Phase의 일부 태스크(도메인·스키마/매퍼(+XML)/서비스/컨트롤러 계층 단위). 리뷰하기 쉬운 크기로 쪼개세요.

### 7.2 커밋 메시지 규칙 (한국어)

```
feat(phase4): 예약 확정 비관적 락 + 충돌 409 매핑

- SELECT ... FOR UPDATE로 슬롯 잠금
- UNIQUE 위반/충돌을 409로 일관 응답
- FE는 1차 재선택 토스트 재사용
```

- 형식: `<타입>(phase{N}): <한국어 요약>` (타입: feat/fix/refactor/test/docs/chore)
- 본문은 한국어. 변수/함수/타입/클래스명만 영어.

### 7.3 PR 전 셀프 체크리스트

**백엔드(Java/Spring/MyBatis)**
- [ ] `./gradlew build` 통과(컴파일 + 테스트)
- [ ] `./gradlew test` 통과 — 특히 **동시성 통합테스트**(6.1)
- [ ] **JPA/Hibernate 의존성·애너테이션이 없는지**(`@Entity`·`JpaRepository`·`@Version`·`ddl-auto` 미사용), DB 접근이 MyBatis 매퍼인지
- [ ] 매퍼 XML `namespace`가 인터페이스 FQN과 일치하고, 컬럼 snake_case ↔ 프로퍼티 camelCase 매핑이 맞는지
- [ ] 컨트롤러가 도메인 객체를 직접 노출하지 않고 **DTO**만 반환(2.2)
- [ ] 쓰기 메서드에 `@Transactional`, 조회에 `@Transactional(readOnly = true)`
- [ ] 예외가 `@RestControllerAdvice`로 일관 응답(409/404/400)되는지
- [ ] 역할 인가(`@PreAuthorize`/SecurityConfig)가 require 3.2 매트릭스와 일치

**프론트엔드(Nuxt/TS)**
- [ ] `npm run lint`(oxlint + eslint), `npm run type-check`(`nuxt typecheck`) 통과
- [ ] `npm run format` 적용(세미콜론 없음 / 작은따옴표 — `oxfmt app/`)
- [ ] **1차 E2E 회귀**(`npm run test:e2e`)가 서버 데이터로도 통과
- [ ] `services/*` 교체가 **컴포넌트/스토어 마크업 변경 없이** 이뤄졌는지(additive)
- [ ] 신규 코드에 세미콜론이 없는지, `~/`(또는 `@/`) 별칭을 썼는지, 주석/커밋이 한국어인지

### 7.4 리뷰 포인트

- **동시성**: 락 전략 선택이 슬롯 경합도에 맞는지, UNIQUE가 최종 방어선으로 깔렸는지, 충돌이 409로 매핑되는지.
- **인가**: BO 엔드포인트가 require 3.2 권한 매트릭스를 정확히 강제하는지.
- **additive**: 1차 화면/스토어/E2E가 무수정으로 유지되는지(`services/` 내부만 교체).
- **DTO 경계**: 도메인 객체 직접 노출·매퍼 중첩 조인 남용으로 인한 N+1이 없는지.

---

## 8. 부록 — require_v1.md 추적 매핑

| ROADMAP Phase | 기능 | require 섹션 | 프로세스 코드 (M/S) |
|---|---|---|---|
| Phase 0 | 명세 확정(Q1~Q8)·BE 부트스트랩·FE↔BE 계약 | 12장 / 명세 8장 | — |
| Phase 1 | 백엔드 도메인 모델·DB 스키마·슬롯 UNIQUE | 5장(5.2·5.5)·10장 | — |
| Phase 2 | 서비스 추상화 교체(FE↔BE 연동) | 12.3 | — |
| Phase 3 | 인증/인가(JWT·이메일 인증·승인 분리, 가입 2단계 상태머신) | 4장(4.2·4.4)·3.2 | **FW2, M2, S2 / M7·S3(가입 2단계 승인 연계)** |
| Phase 4 | 예약 API + 동시성 2단계(낙관/비관 락) | 6장·7장(7.3) | **FW5** |
| Phase 5 | 예약 상태 전이(완료/취소) | 11.3 | **FW6, FW7 / M4, M5** |
| Phase 6 | BO 예약 대행 + 매장 관리 | 3.2·11.1 | **M3 / S4, S5** |
| Phase 7 | 휴가/반차 1단계 승인 + 가입 2단계 승인 + 매장휴일 | 4장(4.4)·8장(8.1·8.2·8.3)·5.5 | **M6·M8(휴가/반차 1단계) / M7·S3(가입 2단계) / 8.1(매장휴일)** |
| Phase 8 | 후기/평점 API + BO 확인·매출 | 9장·11.1 | **S6, S8** |
| Phase 9 | 알림 — SMTP 인프라 + 정책 | 13.2 항목 6·7 | — |
| Phase 10 | 데이터 3단계 — MySQL 이행 | 7장·12장(12.3) | — |

> **BO 범위 근거**: 1차에서 require 2.2에 따라 "2차 과제(문서화만)"였던 BO 프로세스(M3~M8, S3~S9)를 본 2차 로드맵에서 **실제 구현**합니다. *(require v1.7)* BO는 **일반매장매니저(`MANAGER`)·매장매니저관리자(`STORE_ADMIN`)·관리자(`ADMIN`) 3역할별 화면으로 분리**(require §12.4)하며, **가입 승인은 2단계(M7→S3), 휴가/반차 승인은 1단계(M8 매장매니저관리자 종결)** 입니다. 권한 인가는 require 3.2 매트릭스를 정본으로 강제합니다.

> ℹ️ **require_v1.md 12장(스택) 참조 시 주의**: require 12.1(FE)은 Nuxt 4 확정, 12.2(BE)는 "Java(LTS) + Spring Boot(최신 무료), 2단계에서 도입"입니다. 본 로드맵 BE 스택(Java 21 + Spring Boot 3.x + H2→MySQL)은 12.2를 정본으로 따릅니다.

> ⚠️ **v1.7 역할별 BO 페이지(require §12.4) 제안 경로**: ① 일반매장매니저 — `/manager/reserve`(대행)·`/manager/reservations`(취소·완료 보조)·`/manager/dayoffs`(휴가/반차 신청 M6). ② 매장매니저관리자 — ①의 매니저 화면 전부 + `/store-admin/manager-signups`(가입 1차 승인 M7)·`/store-admin/dayoff-approvals`(휴가/반차 승인 M8). ③ 관리자 — `/admin/manager-approvals`(가입 2차 최종 승인 S3)·`/admin/sales`(매출 S8)·`/admin/reservations`(예약상태 S4)·`/admin/manager-status`(매니저 근무상태 S9). 실제 라우트는 코드 작업 시 확정(파일 기반 라우팅).

> ℹ️ **명세 Q1~Q8 참조 시 주의**: 예약_규칙_명세_v1.md 8장의 미해결 질문은 **Phase 0 결정표에서 확정**되며, 그 결과가 Phase 1(도메인·스키마)·Phase 4(베이 노출)에 반영됩니다. 명세 문서 자체는 읽기 전용입니다.

---

> **문서 끝.** 본 로드맵(**v2.2**)은 require_v1.md **v1.7**와 예약_규칙_명세_v1.md(Q1~Q8)를 기준으로, 1차([ROADMAP_1.md](./ROADMAP_1.md)) FO + 프론트 더미 자산을 **유지(additive)** 한 채 **백엔드 진화(데이터 2단계 Spring Boot/H2 → 3단계 MySQL)** 와 **BO 전체(4역할 기반 매니저·관리자 프로세스·승인 워크플로우·SMTP/알림)** 를 구현하는 단계에 집중합니다. v1.7 정합으로 **가입 승인은 2단계(매장매니저관리자 1차 → 관리자 2차), 휴가/반차 승인은 1단계(매장매니저관리자 종결)** 로 단계 수를 구분하며, 역할별 BO 페이지를 4그룹으로 분리합니다(§12.4). 동시성 검증 위치는 클라이언트 → 서버 → DB로 이동하며, 슬롯 `UNIQUE` 제약이 모든 단계의 최종 방어선입니다. ⚠️ 기존 BE 구현의 휴무 2단계 결재는 v1.7(1단계)과 충돌하므로 Phase 7 구현 메모의 재정합 과제를 따릅니다.
