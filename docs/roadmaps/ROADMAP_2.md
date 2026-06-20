# 자동차 세차 예약 서비스 (MVP) 개발 로드맵 — 2차 백엔드 진화(Spring Boot) + BO 전체

> **문서 버전**: v2.1 (백엔드 진화 2·3단계 + BO 전체 착수, **DB 접근 MyBatis 확정**)
> **작성일**: 2026-06-20
> **작성자**: PM/PL
>
> **🔧 v2.1 변경(DB 접근 기술 확정)**: require_v1.md **v1.5** 결정에 따라 백엔드 영속 계층을 **MyBatis**(`mybatis-spring-boot-starter`, 매퍼 인터페이스 + XML SQL)로 확정한다. **JPA/Hibernate는 사용하지 않는다.** 이에 따라 본 로드맵의 패키지 구조(`entity/`→`domain/`, `repository/`(JpaRepository)→`mapper/`(`@Mapper`+XML)), 동시성 락(`@Version`→version 컬럼 비교 UPDATE, `@Lock(PESSIMISTIC_WRITE)`→매퍼 `SELECT ... FOR UPDATE` SQL), 스키마 관리(`ddl-auto`→`schema.sql`/Flyway 직접 관리)를 MyBatis 기준으로 기술한다.
> **대상 독자**: 주니어 ~ 시니어 **백엔드·풀스택** 개발자
> **연계 문서**: [`docs/require_v1.md`](../require_v1.md) (요구사항 정의서 **v1.4** 기준), [`docs/예약_규칙_명세_v1.md`](../예약_규칙_명세_v1.md) (예약 규칙 보강 명세 — 8장 미해결 질문 Q1~Q8), [`docs/roadmaps/ROADMAP_1.md`](./ROADMAP_1.md) (1차 FO + 프론트 더미)
> **범위**: 데이터 진화 **2단계(Spring Boot 인메모리 더미)** + **3단계(MySQL)** + **BO 전체(매니저·관리자 프로세스 M3~M7·S3~S8, 결재 워크플로우, SMTP/알림 인프라)** 집중 상세화. 1차(FO + 프론트 더미)는 [ROADMAP_1.md](./ROADMAP_1.md)에서 완료된 자산으로 전제한다.
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

> 💡 **BO 범위 근거**: 1차에서 "2차 과제(문서화만)"로 분류했던 BO 기능(require 2.2)을 본 로드맵에서 구현합니다. BO M3~M7(대행·세차완료·취소·예약승인·매니저가입승인)은 require **11.1·3.2**, BO S3~S8(가입승인·예약자관리·사용자관리·후기확인·회원정보·매출)은 require **11.1·3.2**가 출처입니다.

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
| **7** | 휴일/휴무 결재 워크플로우 | 상태머신(SUBMITTED→APPROVED_L1→APPROVED_L2→CONFIRMED/REJECTED), M7·8.1 2단계 승인 | 8장 / M5·M7·S3 | 3.5 | 24.5 |
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

> 아래 "결정안" 열은 **기본 권고안(default proposal)** 이며, 이해관계자 확정 시 본 표를 정본으로 잠급니다. 확정값이 권고안과 다르면 본 표만 갱신하면 Phase 1이 그대로 따라옵니다.

| ID | 질문(명세 8장) | 결정안(권고 — 확정 필요) | 영향 파일 |
|----|------|------|------|
| **Q1** | 차종 5분류 → 베이 4등급 매핑 | 경형·소형→소형(A), 준중형·중형→중형(B), 대형·SUV→대형(C), 승합·기타→특대형(D) | `domain/Bay.java`(`size`), `domain/Price.java`, `BayService` |
| **Q2** | 크기 등급당 베이 1개 vs 복수 | **복수 허용** — 등급은 `size` 속성, 베이 수 N은 매장별 유지(require 5.2 수용량 보존) | `domain/Bay.java`, 시드 데이터 |
| **Q3** | 베이 노출 1:1 vs 누적 | **1차 누적 로직 유지** — 차 크기 이상을 수용하는 베이 노출(`sizeRank >= min`), 1차 `getBaysForCar` 동작 보존 | `BayService.findBaysForCar()` |
| **Q4** | A/B/C/D 코드 ↔ 기존 매장별 일련번호 마이그레이션 | 베이에 **`size`(등급) 속성 신설**, `code`는 매장 내 식별자로 유지(코드와 등급 분리). 매장이 A~D 전 등급을 보유할 필요 없음 | `domain/Bay.java`, 시드, `dto/BayResponse` |
| **Q5** | 특대형 ↔ 5분류 연결 & 가격표 영향 | 특대형=`VAN_ETC`(승합·기타) 등급으로 연결. **가격표(10.3)는 차종 기준 유지** — 베이 등급 신설은 노출에만 영향, 가격 무변경 | `domain/Price.java`(무변경 확인), `BayService` |
| **Q6** | 규칙 1 "매니저→차종" 직렬 엄격성 | **자유 순서 유지**(매장 이후 매니저·차종 자유, 베이만 차종 이후) — 1차 `reserve/index.vue` 동작 보존 | FE `reserve/index.vue`(무변경) |
| **Q7** | 규칙 2를 매니저 대행(6.2)에도 적용 | **적용** — 대행 예약(M3)도 동일 베이 노출 규칙 사용(Phase 6에서 공유) | `BayService`(M3 재사용) |
| **Q8** | 차종 대응 베이가 매장에 없을 때 처리 | 베이 목록 빈 상태 → "해당 차종 수용 베이 없음" 안내 + 예약 차단(409 아님, 선택 단계 비활성) | `BayService`, FE 빈 상태 UI |

> 💡 **권고안의 일관성**: 위 결정안은 명세 8장의 "추측 후보(`?`)"와 정합하되, **1차 구현(누적 수용·매장별 베이 수)을 최대한 보존**하는 방향으로 잡았습니다(additive). 가격표는 차종 기준이므로 베이 등급 신설의 영향을 받지 않습니다(Q5).

#### 태스크 체크리스트
- [ ] **Q1~Q8 결정표 검토 회의** — 이해관계자와 권고안 확정/수정, 본 표 잠금
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

#### 태스크 체크리스트
- [ ] `domain/` POJO 10종 — `User`, `Store`, `Bay`, `Slot`, `Reservation`, `Manager`, `ManagerDayoff`, `StoreHoliday`, `Review`, `Price` (JPA 애너테이션 없음)
- [ ] `Bay.size`에 **Phase 0 결정(4등급 또는 확정값)** 반영(require 5.4·명세 Q1)
- [ ] `ManagerDayoff`에 `DayoffType`(FULL_DAY/SHIFT_1·2·3) enum 반영(require 5.5)
- [ ] `db/schema.sql`에 10개 테이블 DDL + `slot` 테이블 `CONSTRAINT uk_slot_store_bay_date_time UNIQUE (store_id, bay_id, date, time_slot)` (require 5.2·7.3)
- [ ] `mapper/` MyBatis 매퍼 인터페이스(`@Mapper`) + `resources/mapper/*Mapper.xml`
- [ ] `Price` 매트릭스(차종 5 × 서비스 4 = 20행, require 10.3 확정 단가)를 `db/data.sql`에 시드
- [ ] `db/data.sql`로 시드(매장·베이·매니저·휴무·가격·더미 사용자) 주입
- [ ] FE `app/types/domain.ts`(1차)와 **필드명 일치 확인**(DTO 무변환 매핑 대비)

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

#### 완료기준 (DoD)
- [ ] `./gradlew bootRun` 시 `schema.sql`로 H2에 10개 테이블이 생성된다(H2 콘솔 확인)
- [ ] `slot` 테이블에 `uk_slot_store_bay_date_time` 유니크 제약이 존재한다(H2 콘솔 확인)
- [ ] `price` 테이블에 20행이 시드되고 require 10.3과 정확히 일치한다
- [ ] FE `app/types/domain.ts`의 필드명과 도메인 POJO 필드명이 일치한다(불일치 항목 0건)
- [ ] 매퍼 단건 조회/갱신이 동작한다(매퍼 단위 통합테스트)
- [ ] `./gradlew build` 통과

#### 구현 메모 (📌)
- 📌 **관계 매핑 깊이**: 1차 FE는 `storeId`/`bayId` 같은 **ID 참조** 기반이었습니다. 2차 도메인 POJO도 ID(Long) 참조로 두고(FE DTO 무변환 매핑 유지), 연관 데이터가 필요하면 **매퍼에서 JOIN 또는 별도 조회**로 조립하세요. MyBatis `<association>`/`<collection>` 중첩 매핑은 꼭 필요한 곳에만 쓰고, 무분별한 중첩 조인으로 N+1을 만들지 마세요(JPA 양방향 연관관계 자체가 없습니다).
- 📌 **enum 매핑**: `SlotStatus`·`CarType` 등은 DB에 문자열(VARCHAR)로 저장합니다. MyBatis 기본 `EnumTypeHandler`가 enum ↔ name 문자열을 매핑하므로 별도 핸들러가 대개 불필요합니다(특수 매핑이 필요하면 `TypeHandler`를 등록).

---

### Phase 2 — 서비스 추상화 교체 (FE↔BE 연동)

> **공수: 3.0일** · **선행조건: Phase 1** · **require_v1.md 참조: 12.3(데이터 진화 2단계 교체 경계)**

#### 목표
1차의 `app/services/*`(더미 직접 반환)를 **`$fetch` 기반 API 클라이언트**로 교체하고, BE에 그에 대응하는 **조회 REST 엔드포인트**(매장·매니저·베이·가격)를 만든다. **컴포넌트/스토어/페이지는 무수정**을 목표로 한다(additive 회수 지점).

> 💡 **Phase 2의 핵심 검증**: "1차 화면이 그대로 동작하는데, 데이터 출처만 더미 → 서버로 바뀌었다"가 성공 기준입니다. ROADMAP_1 5.1의 "준비 작업(서비스 추상화)"이 여기서 결실을 맺습니다.

#### 태스크 체크리스트
- [ ] BE 조회 API: `GET /api/stores`(승인 매장만), `GET /api/stores/{id}/managers`, `GET /api/stores/{id}/bays?carType=`, `GET /api/prices`
- [ ] `dto/StoreResponse`·`ManagerResponse`·`BayResponse`·`PriceResponse`(FE 타입과 필드 일치)
- [ ] FE `app/services/storeService.ts` 내부를 `$fetch(apiBase + '/stores')`로 교체(시그니처 유지)
- [ ] FE `app/services/priceService.ts`의 `getPrice`를 `/api/prices` 조회(또는 캐시) 기반으로 교체
- [ ] FE 베이 노출(`getBaysForCar`)을 `GET /api/stores/{id}/bays?carType=`로 위임(Phase 0 Q3 누적 로직 서버화)
- [ ] **1차 Playwright E2E 회귀 통과 확인**(매장 검색·매니저 휴무·가격 표시가 서버 데이터로도 동일)

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

#### 완료기준 (DoD)
- [ ] `GET /api/stores`가 승인 매장만 반환한다(`approved=false` 미노출, require 6.1)
- [ ] FE 예약 1페이지(`/reserve`)가 **서버 데이터**로 매장·매니저·차종·가격을 표시한다(화면 무변경)
- [ ] **1차 Playwright E2E 회귀**(매장 검색 필터·가격 자동 표시)가 서버 데이터로도 통과한다
- [ ] `./gradlew build`, `npm run type-check`, `npm run test:e2e` 통과

#### 구현 메모 (📌)
- 📌 **무수정 목표의 현실**: 더미가 동기 반환이던 함수가 `Promise`로 바뀌므로, 호출부에 `await`가 없던 곳은 최소 수정이 필요할 수 있습니다. 이 정도는 "서비스 계층 추상화의 합리적 비용"으로 간주하고, **컴포넌트 마크업·스토어 구조 변경 0**을 진짜 성공 기준으로 삼으세요.

---

### Phase 3 — 인증/인가 (실제 토큰)

> **공수: 3.5일** · **선행조건: Phase 2** · **require_v1.md 참조: 4장(인증·승인 분리), 3.2(권한 매트릭스)**

#### 목표
1차의 더미 로그인을 **JWT 기반 실제 인증**으로 교체하고, **역할(USER/MANAGER/STORE_ADMIN/ADMIN) 기반 인가 가드**와 **이메일 인증(SMTP 연계, Phase 9 인프라와 결합)** 을 도입한다. require 4.2의 **인증/승인 분리** 원칙을 서버에 반영한다.

#### 태스크 체크리스트
- [ ] `POST /api/auth/login` — 이메일/비밀번호 검증 → JWT 발급
- [ ] `security/JwtAuthenticationFilter` + `SecurityConfig`(역할별 경로 인가)
- [ ] 비밀번호 해시(`BCryptPasswordEncoder`)
- [ ] 회원가입 상태 머신(require 4.4): `REQUESTED → EMAIL_VERIFIED → PENDING_APPROVAL/ACTIVE`
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
// 인증(이메일 본인확인)과 승인(역할/매장가입 검토)은 별개 (require 4.2)
// 승인 불필요한 일반 사용자: EMAIL_VERIFIED → ACTIVE 자동 전이 (require 4.4)
// 매니저/매장가입: EMAIL_VERIFIED → PENDING_APPROVAL (Phase 7 결재 연계)
@Transactional
public void verifyEmail(String token) {
    User user = userRepository.findByEmailVerifyToken(token)
        .orElseThrow(() -> new DomainException("유효하지 않은 인증 토큰입니다."));
    user.markEmailVerified();
    if (user.requiresApproval()) {
        user.toPendingApproval();   // 매니저/매장가입 → 승인 대기
    } else {
        user.activate();            // 일반 사용자 → 즉시 활성
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

#### 완료기준 (DoD)
- [ ] 올바른 계정 로그인 시 JWT가 발급되고 `useCookie`에 보관된다
- [ ] 토큰 없이 보호 API 호출 시 401, 권한 부족 시 403을 반환한다(역할 인가)
- [ ] 일반 사용자 가입은 `EMAIL_VERIFIED → ACTIVE`, 매니저/매장가입은 `PENDING_APPROVAL`로 분기된다(require 4.4)
- [ ] **1차 로그인 E2E 회귀**(성공/실패/미인증 가드)가 토큰 기반으로 통과한다
- [ ] `./gradlew build`, `npm run type-check`, `npm run test:e2e` 통과

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

#### 완료기준 (DoD)
- [ ] 동일 슬롯에 **동시 확정 요청 시 한 건만 성공**, 나머지는 409를 받는다(통합테스트 6장 검증)
- [ ] 409 응답 시 FE가 1차 재선택 토스트("다른 슬롯을 선택해 주세요.")를 노출하고 그리드를 새로고침한다
- [ ] 슬롯 `UNIQUE` 위반이 500이 아닌 **409**로 매핑된다(최종 방어선)
- [ ] **1차 예약 위저드 E2E 회귀**(1p→2p→3p)가 서버 예약으로도 통과한다
- [ ] `./gradlew build` + 동시성 통합테스트 통과, `npm run test:e2e` 통과

#### 구현 메모 (📌)
- 📌 **검증 위치 이동**: ROADMAP_1에서 슬롯 충돌은 **클라이언트 Pinia**가 판정했습니다. 이제 **진실은 서버/DB**입니다. FE의 점유 그리드 disabled는 "사전 차단 UX"로 유지하되, **최종 충돌 판정은 서버 409**가 담당합니다. 1차에서 방어 코드로 잔존시킨 `confirm()` 충돌 분기·`useToast`가 본 Phase에서 **실제 경로로 부활**합니다(ROADMAP_1 Phase 8 메모와 정합).

---

### Phase 5 — 예약 상태 전이 API (완료/취소/승인)

> **공수: 2.5일** · **선행조건: Phase 4** · **require_v1.md 참조: 11.3(프로세스) / FW6·FW7·M4·M5·M6**

#### 목표
1차의 클라이언트 상태 전이(ROADMAP_1 Phase 6)를 **서버 API**로 교체하고, **세차완료(FW6/M4)·예약취소(FW7/M5)** 에 더해 1차에 없던 **예약 승인(M6)** 을 도입한다. 상태 가드(불가능한 전이 차단)를 서버에서 강제한다.

#### 태스크 체크리스트
- [ ] `PATCH /api/reservations/{id}/complete` — RESERVED → COMPLETED (FW6/M4)
- [ ] `PATCH /api/reservations/{id}/cancel` — RESERVED/HOLDING → CANCELED + 슬롯 release (FW7/M5)
- [ ] `PATCH /api/reservations/{id}/approve` — (승인 도입 시) 예약 승인 (M6, require 11.3)
- [ ] 상태 전이 가드: 도메인 메서드(`complete()`/`cancel()`)에서 불가능 전이 시 예외 → 409/400
- [ ] 취소 시 슬롯 release(AVAILABLE) — 그리드 재방문 시 반영
- [ ] FE `app/pages/reservations.vue`·`reservation` 스토어 액션을 서버 호출로 교체

#### 생성·수정 파일
`controller/ReservationController.java`(전이 엔드포인트 추가), `service/ReservationService.java`(complete/cancel/approve), `domain/Reservation.java`(도메인 전이 메서드), `mapper/ReservationMapper.java` + XML(상태 UPDATE), FE `app/pages/reservations.vue`(수정), FE `app/stores/reservation.ts`(서버 위임)

#### 상태 전이 표 (서버 강제 — require 11.3)

| 현재 상태 | 액션(프로세스) | 다음 상태 | 슬롯 처리 | 비고 |
|---|---|---|---|---|
| `HOLDING` | 취소(FW7) | `CANCELED` | `AVAILABLE` release | 승인 전 취소(11.3 c) |
| `RESERVED` | 승인(M6) | `RESERVED` | — | 승인 도입 시(require 3.2) |
| `RESERVED` | 취소(FW7/M5) | `CANCELED` | `AVAILABLE` release | 승인 후 취소(11.3 b) |
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
- [ ] RESERVED 예약을 세차완료로 전이할 수 있다(FW6/M4)
- [ ] 승인 전/후 취소가 모두 동작하고 슬롯이 다시 AVAILABLE이 된다(FW7/M5, require 11.3 b/c)
- [ ] 불가능한 전이(COMPLETED→CANCELED 등)가 **서버에서 차단**된다(400/409)
- [ ] **1차 목록·취소·완료 E2E 회귀**가 서버 전이로도 통과한다
- [ ] `./gradlew build`, `npm run test:e2e` 통과

#### 구현 메모 (📌)
- 📌 **승인(M6)의 1차 부재**: 1차 MVP는 승인 단계가 없어 `confirm`이 곧장 RESERVED를 만들었습니다(ROADMAP_1 Phase 6 메모). 2차에서 M6 승인을 **도입할지**는 require 3.2 권한 매트릭스(M6)와 운영 정책에 달려 있습니다. 도입 시 `confirm`은 `PENDING` 같은 중간 상태를 거치도록 확장하고, 미도입 시 본 Phase의 approve 엔드포인트는 BO 대행(Phase 6) 전용으로만 둡니다 — Phase 0 결정에 준하여 확정하세요.

---

### Phase 6 — BO: 예약 대행 + 매장 관리

> **공수: 3.5일** · **선행조건: Phase 5** · **require_v1.md 참조: 3.2(권한 매트릭스), 11.1(프로세스) / M3·S4·S5**

#### 목표
1차에서 문서화만 했던 BO를 착수한다. **매니저 예약 대행(M3)** — 매니저가 소속 매장 기준으로 사용자 예약을 대행 — 과 **관리자 매장별 예약자 관리(S4)·사용자 관리(S5)** 를 구현한다. 인가는 Phase 3의 역할 가드를 재사용한다.

#### 태스크 체크리스트
- [ ] `POST /api/manager/reservations` — 매니저 대행 예약(M3), 소속 매장·본인 휴무 반영(require 6.2)
- [ ] 대행 예약도 동일 동시성 경로(Phase 4 confirm) + 동일 베이 노출 규칙(Phase 0 Q7) 재사용
- [ ] `GET /api/admin/stores/{id}/reservations` — 매장별 예약자 관리(S4, require 11.1)
- [ ] `GET /api/admin/stores/{id}/users` — 매장별 사용자 관리(S5)
- [ ] 역할 인가: M3=MANAGER/STORE_ADMIN, S4·S5=ADMIN (require 3.2)
- [ ] FE BO 화면(신규): 매니저 대행 예약 페이지, 관리자 예약자/사용자 관리 페이지
- [ ] FE 라우트 가드 `role-guard`(Phase 3) 적용 — 권한 외 접근 차단

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
- [ ] 매니저가 소속 매장 기준으로 사용자 예약을 대행할 수 있고, 본인 휴무 시간대는 차단된다(M3, require 6.2)
- [ ] 대행 예약이 일반 예약과 **동일한 동시성/베이 노출 규칙**을 따른다(Phase 0 Q7)
- [ ] 관리자가 매장별 예약자(S4)·사용자(S5) 목록을 조회할 수 있다(require 11.1)
- [ ] 권한 외 역할이 BO 엔드포인트 호출 시 403을 받는다(require 3.2)
- [ ] `./gradlew build`, `npm run type-check` 통과 + BO E2E 시나리오(대행/조회) 통과

---

### Phase 7 — 휴일/휴무 결재 워크플로우

> **공수: 3.5일** · **선행조건: Phase 6** · **require_v1.md 참조: 8장(결재), 5.5(교대조 휴무), 4.3(가입승인) / M5·M7·S3·8.1**

#### 목표
require 8장의 **결재 상태머신**(SUBMITTED → APPROVED_L1 → APPROVED_L2 → CONFIRMED / REJECTED)을 구현하고, **매니저 휴무(M7 흐름의 2단계 승인: 최고매니저 → 관리자)** 와 **매장 휴일(8.1: 매니저 신청 → 관리자 승인)** 을 처리한다. 휴무 유형(FULL_DAY/SHIFT_n, require 5.5)은 전일/교대조 구분 없이 동일 흐름을 따른다(require 8.2). 매니저 가입 승인(M7)·가입 승인(S3)도 동일 승인 패턴을 재사용한다.

#### 태스크 체크리스트
- [ ] `domain/Approval` 또는 도메인별(`ManagerDayoff`·`StoreHoliday`)에 `ApprovalStatus` enum(require 8.3)
- [ ] `POST /api/manager/dayoffs` — 매니저 휴무 결재 상신(SUBMITTED), 휴무유형 포함(require 5.5)
- [ ] `PATCH /api/manager/dayoffs/{id}/approve-l1` — 최고매니저 1차 승인(SUBMITTED → APPROVED_L1)
- [ ] `PATCH /api/admin/dayoffs/{id}/approve-l2` — 관리자 최종 승인(APPROVED_L1 → APPROVED_L2 → CONFIRMED)
- [ ] `PATCH .../reject` — 어느 단계든 반려(→ REJECTED → 재신청)
- [ ] 매장 휴일(8.1): 매니저 신청 → 관리자 단일 승인(L1 생략 또는 L1=관리자, require 8.3 단순화)
- [ ] M7(매니저 가입 승인)·S3(가입 승인) — 동일 승인 패턴 재사용(require 4.3·11.2)
- [ ] CONFIRMED 시 슬롯 비활성 반영: FULL_DAY=그날 전체, SHIFT_n=해당 교대 시간대만(require 5.5·6.1)
- [ ] 역할 인가: L1=STORE_ADMIN, L2=ADMIN (require 3.2·8.3)

#### 생성·수정 파일
`domain/ManagerDayoff.java`(승인상태·휴무유형), `domain/StoreHoliday.java`(승인상태), `mapper/ManagerDayoffMapper.java`·`mapper/StoreHolidayMapper.java` + XML, `controller/DayoffController.java`, `controller/StoreHolidayController.java`, `service/ApprovalService.java`, `dto/DayoffRequest.java`, `dto/ApprovalResponse.java`, FE `app/pages/manager/dayoffs.vue`·`app/pages/admin/approvals.vue`(신규)

#### 결재 상태 머신 (require 8.3)

| 상태 | 설명 | 다음 상태 | 전이 주체 |
|---|---|---|---|
| `SUBMITTED` | 결재 상신 | `APPROVED_L1` / `REJECTED` | 신청자 → 검토자 |
| `APPROVED_L1` | 1차 승인 | `APPROVED_L2` / `REJECTED` | 매장 최고매니저(STORE_ADMIN) |
| `APPROVED_L2` | 2차 승인(확정 직전) | `CONFIRMED` | 관리자(ADMIN) |
| `CONFIRMED` | 휴일/휴무 확정 반영 | — | (시스템) |
| `REJECTED` | 어느 단계든 거부 | `SUBMITTED`(재신청) | 검토자 |

> **매장 휴일(8.1)** 은 매니저 신청 → 관리자 승인의 **단일 승인**으로 단순화(L1 생략). **매니저 휴무**는 위 2단계(최고매니저 → 관리자) 흐름을 따른다(require 8.2·8.3).

#### 구현 예시 — 결재 승인 서비스(2단계 승인 + 휴무 유형)

`service/ApprovalService.java`
```java
// 매니저 휴무 2단계 승인 (require 8.2·8.3)
// L1: 최고매니저, L2: 관리자 → CONFIRMED 시 슬롯 비활성 반영
@Transactional
public void approveL1(Long dayoffId) {
    ManagerDayoff dayoff = dayoffMapper.findById(dayoffId);
    if (dayoff == null) {
        throw new DomainException("휴무 신청을 찾을 수 없습니다.");
    }
    dayoff.approveL1();   // SUBMITTED → APPROVED_L1 (불가 전이 시 예외)
    dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());
}

@Transactional
public void approveL2(Long dayoffId) {
    ManagerDayoff dayoff = dayoffMapper.findById(dayoffId);
    if (dayoff == null) {
        throw new DomainException("휴무 신청을 찾을 수 없습니다.");
    }
    dayoff.approveL2();   // APPROVED_L1 → APPROVED_L2 → CONFIRMED
    dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());

    // CONFIRMED 시 슬롯 비활성 반영 (require 5.5·6.1)
    // FULL_DAY: 그날 전체 슬롯 / SHIFT_n: 해당 교대 시간대 슬롯만
    slotDeactivationService.deactivate(dayoff.getManagerId(), dayoff.getDate(), dayoff.getType());
}
```

`domain/ManagerDayoff.java` (전이 메서드 발췌)
```java
// 결재 전이 — 불가능한 단계 건너뛰기 차단 (require 8.3)
public void approveL1() {
    if (this.status != ApprovalStatus.SUBMITTED) {
        throw new IllegalStateException("1차 승인 불가 상태: " + this.status);
    }
    this.status = ApprovalStatus.APPROVED_L1;
}

public void approveL2() {
    if (this.status != ApprovalStatus.APPROVED_L1) {
        throw new IllegalStateException("2차 승인 불가 상태: " + this.status);
    }
    this.status = ApprovalStatus.CONFIRMED; // L2=확정
}
```

#### 완료기준 (DoD)
- [ ] 매니저 휴무가 **2단계 승인**(최고매니저 → 관리자)을 거쳐 CONFIRMED된다(require 8.2)
- [ ] 매장 휴일이 단일 승인(매니저 신청 → 관리자)으로 CONFIRMED된다(require 8.1)
- [ ] CONFIRMED 시 FULL_DAY는 그날 전체, SHIFT_n은 해당 교대 시간대만 슬롯이 비활성된다(require 5.5·6.1)
- [ ] 어느 단계든 반려(REJECTED) 후 재신청(SUBMITTED)이 가능하다
- [ ] 단계 건너뛰기·권한 외 승인이 차단된다(상태 가드 + 역할 인가)
- [ ] `./gradlew build` + 결재 상태머신 통합테스트 통과

#### 구현 메모 (📌)
- 📌 **워크플로우 엔진 미사용**: require 8.2가 명시했듯 별도 워크플로우 엔진 없이 **상태값 변경 방식**으로 구현합니다. 상태 enum + 도메인 전이 메서드 + 역할 인가의 조합으로 충분합니다.
- 📌 **승인 패턴 재사용**: M7(매니저 가입 승인)·S3(가입 승인)도 본질적으로 같은 "신청 → 검토자 승인" 패턴입니다. `ApprovalService`의 전이 골격을 공유하되, 대상 도메인(User vs Dayoff vs Holiday)과 매퍼·승인 주체만 다르게 주입하세요.

---

### Phase 8 — 후기/평점 API + BO 확인·매출

> **공수: 2.5일** · **선행조건: Phase 7** · **require_v1.md 참조: 9장(후기/평점), 11.1(프로세스) / S6·S8**

#### 목표
1차의 후기/평점(ROADMAP_1 Phase 7)을 **서버 API**로 교체하고, BO 측 **관리자 후기 확인(S6)** 과 **매출 집계(S8)** 를 구현한다. 후기 작성 자격(세차완료 사용자만, require 9.1)은 서버에서 검증한다.

#### 태스크 체크리스트
- [ ] `POST /api/reviews` — 후기 작성, 작성 자격(COMPLETED + 본인 + 중복 방지) 서버 검증(require 9.1)
- [ ] `GET /api/reviews/stores/{id}/average`·`/managers/{id}/average` — 평균 평점 집계
- [ ] `GET /api/admin/stores/{id}/reviews` — 관리자 매장별 후기 확인(S6, require 11.1)
- [ ] `GET /api/admin/stores/{id}/sales` — 매장별 매출 집계(S8) — COMPLETED 예약 금액 합산
- [ ] FE `app/pages/review/[reservationId].vue`·`reservations.vue`를 서버 API로 교체
- [ ] FE BO 화면(신규): 관리자 후기 확인·매출 대시보드

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
- [ ] COMPLETED가 아닌/타인/중복 후기 작성이 **서버에서 차단**된다(require 9.1)
- [ ] 평점(1~5) 범위를 벗어나면 거부된다(Bean Validation)
- [ ] 매장/매니저 평균 평점이 서버 집계로 표시된다
- [ ] 관리자가 매장별 후기(S6)·매출(S8)을 조회할 수 있다(권한 인가)
- [ ] **1차 후기 E2E 회귀**(자격 가드·평점 제출·평균)가 서버 API로도 통과한다
- [ ] `./gradlew build`, `npm run test:e2e` 통과

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
| 결재 결과 통지 | 휴무/휴일 CONFIRMED/REJECTED | 메일 | require 8장 |

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
| Phase 3 | 인증/인가(JWT·이메일 인증·승인 분리) | 4장(4.2·4.4)·3.2 | **FW2, M2, S2 / M7·S3(가입승인 연계)** |
| Phase 4 | 예약 API + 동시성 2단계(낙관/비관 락) | 6장·7장(7.3) | **FW5** |
| Phase 5 | 예약 상태 전이(완료/취소/승인) | 11.3 | **FW6, FW7 / M4, M5, M6** |
| Phase 6 | BO 예약 대행 + 매장 관리 | 3.2·11.1 | **M3 / S4, S5** |
| Phase 7 | 휴일/휴무 결재 워크플로우(2단계 승인) | 8장(8.1·8.2·8.3)·5.5·4.3 | **M5, M7 / S3 / 8.1(매장휴일)** |
| Phase 8 | 후기/평점 API + BO 확인·매출 | 9장·11.1 | **S6, S8** |
| Phase 9 | 알림 — SMTP 인프라 + 정책 | 13.2 항목 6·7 | — |
| Phase 10 | 데이터 3단계 — MySQL 이행 | 7장·12장(12.3) | — |

> **BO 범위 근거**: 1차에서 require 2.2에 따라 "2차 과제(문서화만)"였던 BO 프로세스(M3~M7, S3~S8)를 본 2차 로드맵에서 **실제 구현**합니다. 권한 인가는 require 3.2 매트릭스를 정본으로 강제합니다.

> ℹ️ **require_v1.md 12장(스택) 참조 시 주의**: require 12.1(FE)은 Nuxt 4 확정, 12.2(BE)는 "Java(LTS) + Spring Boot(최신 무료), 2단계에서 도입"입니다. 본 로드맵 BE 스택(Java 21 + Spring Boot 3.x + H2→MySQL)은 12.2를 정본으로 따릅니다.

> ℹ️ **명세 Q1~Q8 참조 시 주의**: 예약_규칙_명세_v1.md 8장의 미해결 질문은 **Phase 0 결정표에서 확정**되며, 그 결과가 Phase 1(도메인·스키마)·Phase 4(베이 노출)에 반영됩니다. 명세 문서 자체는 읽기 전용입니다.

---

> **문서 끝.** 본 로드맵(v2.0)은 require_v1.md v1.4와 예약_규칙_명세_v1.md(Q1~Q8)를 기준으로, 1차([ROADMAP_1.md](./ROADMAP_1.md)) FO + 프론트 더미 자산을 **유지(additive)** 한 채 **백엔드 진화(데이터 2단계 Spring Boot/H2 → 3단계 MySQL)** 와 **BO 전체(매니저·관리자 프로세스·결재 워크플로우·SMTP/알림)** 를 구현하는 단계에 집중합니다. 동시성 검증 위치는 클라이언트 → 서버 → DB로 이동하며, 슬롯 `UNIQUE` 제약이 모든 단계의 최종 방어선입니다.
