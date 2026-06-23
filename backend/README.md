# backend — 자동차 세차 예약 서비스 (Spring Boot)

ROADMAP 2차의 **Spring Boot 백엔드**. 프론트(Nuxt)의 더미 데이터를 실제 REST API + DB로 진화시킨다.

- 런타임: **Spring Boot 3.3.5**, **Java 21**(툴체인) 빌드 도구: **Gradle**
- 영속 계층: **MyBatis**(매퍼 인터페이스 + XML SQL) — **JPA/Hibernate 미사용**(`require_v1.md` v1.5)
- DB: **H2 in-memory**(MySQL 호환 모드) — 기동 시 `schema.sql`(DDL) → `data.sql`(시드) 자동 실행, 재기동 시 휘발
- 인증/인가: **Spring Security + JWT**(jjwt, HS256) · BCrypt 비밀번호 해시
- 보일러플레이트 축소: **Lombok**(컴파일 전용 — `@Getter`·`@Builder`·`@NoArgsConstructor` 등)

## 주요 명령어

| 명령 | 설명 |
|---|---|
| `./gradlew bootRun` | 개발 서버 기동 — **http://localhost:8080** |
| `./gradlew build` | 프로덕션 빌드(테스트 포함) |
| `./gradlew compileJava` | 메인 소스 컴파일(오류 점검용) |
| `./gradlew test` | 단위/통합 테스트(JUnit 5) |
| `./gradlew clean` | 빌드 산출물 정리 |

> Windows에서는 `gradlew.bat`을, macOS/Linux에서는 `./gradlew`를 사용한다.

- **H2 콘솔**: 기동 후 http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:carwash`, user `sa`, 비밀번호 없음)
- IntelliJ 공유 **Run Configuration**: `.run/CarwashApplication.run.xml`(저장소 공유) — IDE에서 바로 `CarwashApplication` 실행 가능
- 단일 테스트 클래스 실행: `./gradlew test --tests 'com.carwash.controller.AuthApiTest'`

## 아키텍처

표준 레이어드(`controller` → `service` → `mapper`). 패키지: `com.carwash`.

| 패키지 | 역할 |
|---|---|
| `controller/` | REST 엔드포인트(`/api/**`) — 역할/도메인별 분리 |
| `service/` | 비즈니스 로직(예약 동시성·슬롯 점유·승인 워크플로·알림 정책) |
| `mapper/` | MyBatis 매퍼 **인터페이스** — SQL은 `resources/mapper/*.xml`. `map-underscore-to-camel-case`로 snake_case ↔ camelCase |
| `domain/` · `domain/enums/` | 가변 도메인 POJO(Lombok), enum |
| `dto/` | 요청/응답 — Java **record**(Lombok 미적용) |
| `config/` · `security/` | `SecurityConfig`·`AppBeansConfig`·`AsyncConfig` / `JwtTokenProvider`·`JwtAuthenticationFilter` |
| `exception/` | `GlobalExceptionHandler`(도메인 예외 → JSON), `SlotConflictException` |

## REST API 개요

`/api` prefix. 무인증 허용을 제외한 나머지는 JWT 필요(미인증 401, 권한부족 403).

| 영역 | 엔드포인트 | 인가 |
|---|---|---|
| 인증 | `POST /auth/login`, `/auth/signup`, `/auth/signup-manager` | permitAll |
| 이메일 인증 가입 | `POST /auth/signup/request`·`/verify`·`/resend`, `GET /auth/signup/dev-code` | permitAll |
| 카탈로그 조회 | `GET /stores`·`/managers`·`/bays`·`/prices`·`/slots` | permitAll |
| 헬스 | `GET /health` | permitAll |
| 예약(고객) | `GET /reservations`, `POST /reservations/hold`·`/confirm`, `PATCH /reservations/{id}/complete`·`/cancel` | 인증 |
| 후기 | `POST /reviews`, `GET /reviews/stores/{id}/average`·`/managers/{id}/average` | 인증 |
| 매니저 | `/manager/reservations`, `/manager/dayoffs`(신청·재신청), `POST /manager/holidays` | `MANAGER`·`STORE_ADMIN` |
| 매장관리자 | `/store-admin/reservations`, `/store-admin/dayoffs`(승인·반려), `/store-admin/manager-signups`(1차 승인) | `STORE_ADMIN` |
| 관리자 | `POST /admin/managers`(등록), `/admin/manager-approvals`(2차 승인), `/admin/holidays`(결재), `/admin/stores/{id}/reservations`·`/users`·`/sales`·`/reviews` | `ADMIN` |

## 인증·인가

- **stateless JWT**(세션 없음). `JwtAuthenticationFilter`가 `Authorization: Bearer` 토큰을 매 요청 검증. 비밀번호는 **BCrypt** 해시.
- 역할 4종: `USER`·`MANAGER`·`STORE_ADMIN`·`ADMIN`(FE `enums.ts`와 글자 일치). 매처는 구체적인 것을 `anyRequest` 앞에 배치.
- CORS는 `SecurityConfig.corsConfigurationSource` 단일 소스에서 FE(`http://localhost:3000`)만 허용(credentials 포함).

### 이메일 인증 가입 (create-after-verify)

가입은 **인증 성공 시에만 `users`로 승격**한다(미인증 반쪽 계정 방지). 진행 중 정보는 **`verification` 테이블**에 임시 보관.

- `signup/request`(가입정보 + 6자리 코드 발급·메일 발송, 이메일당 1건) → `signup/verify`(코드 일치 + 미만료 + 시도 5회 이내) → USER는 토큰 발급(자동 로그인)·MANAGER는 `PENDING_APPROVAL_L1`. 코드 3분 TTL, `resend`로 갱신.
- `verification` 테이블: `email`(PK), `code`, `role`, `name`, `password_hash`(원문 미보관), `store_id`, `expires_at`(epoch millis), `attempts`, **`method`**(`VerificationMethod`: `EMAIL`/`SNS` — 인증 방법 확장 대비, 현재 `EMAIL`). 물리 테이블명은 `verification`이나 Java 식별자는 역사적으로 `EmailVerification*`.
- 개발 백도어: `GET /auth/signup/dev-code?email=`로 대기 코드 조회(메일 없이 E2E). `app.signup.dev-code-peek`(기본 true, 운영 false)로 차단.

### 알림·SMTP (Phase 9)

`NotificationService`(정책: 수신자·본문·이력 결정) → `EmailSender`(`SmtpEmailSender`, `@Async("mailTaskExecutor")` — `AsyncConfig` 전용 풀)에 dispatch 위임.

- 수신자는 **호출자 트랜잭션 내 동기 해석**, 발송은 비동기라 **실패해도 도메인 트랜잭션에 비전파**(예외는 메일 스레드에 격리). 이력은 `notification_log`에 `QUEUED`/`SKIPPED`로 기록(발송 후 SENT/FAILED 갱신 없음).
- 설정: `application.yml`의 `spring.mail`이 `${MAIL_*}`를 읽고, 값은 **`backend/.env`**(`spring.config.import: optional:file:.env[.properties]`)에서 주입. `.env`는 **상대경로**라 BE를 반드시 `backend/`에서 실행해야 로딩됨(`.gitignore` 제외, 템플릿 `.env.example`). 기본값은 외부 의존 없는 로컬 캐처(MailHog/Mailpit `localhost:1025`), 실 발송은 Gmail SMTP(587·STARTTLS·앱 비밀번호).

## DB (H2 in-memory)

기동 시 `resources/db/schema.sql`(DDL) → `data.sql`(시드)가 자동 실행되고 재기동 시 휘발한다. **12개 테이블**:

`users` · `store` · `bay` · `manager` · `manager_dayoff` · `slot` · `price` · `reservation` · `review` · `store_holiday` · `notification_log` · `verification`

- `slot`에 `UNIQUE(store_id, bay_id, date, time_slot)`(`uk_slot_store_bay_date_time`)가 예약 동시성 **최종 방어선**.
- `price`는 차종 5 × 서비스 4 = **20행** 시드.
- 시드 계정(비밀번호 모두 `password`): `user@test.com`(USER)·`manager@test.com`(MANAGER)·`storeadmin@test.com`(STORE_ADMIN)·`admin@test.com`(ADMIN)·`pending1·2@test.com`(승인 대기 MANAGER). 전체는 루트 [`README.md`](../README.md) 참고.

> ⚠ Phase 10(R2P10-2) Flyway 도입 시 `notification_log`·`verification`을 마이그레이션 스크립트에 반드시 포함할 것(스키마 누락 방지).

## IDE 설정 (권장) — IntelliJ IDEA + Lombok

이 프로젝트는 도메인 POJO에 **Lombok**(`@Getter`·`@Builder` 등)을 사용한다. Lombok이 컴파일 시점에 생성하는 게터(`getId()`·`getName()` 등)는 **Gradle 빌드에서는 자동 처리**되지만, **IntelliJ 에디터가 인식하려면 IDE 측 설정이 필요**하다.

> **증상**: `./gradlew build`는 정상(BUILD SUCCESSFUL)인데, IntelliJ 에디터에서만 DTO의 `from()` 팩토리(예: `StoreResponse.from()` 내부의 `s.getId()`)나 도메인 게터 호출부가 **빨간 줄("cannot resolve method getXxx")** 로 표시된다.
> **원인**: 빌드가 아니라 **IDE 에디터**가 Lombok 게터를 못 찾는 것 — 코드 결함이 아니다. 아래 설정으로 해결한다.

순서대로 점검한다(대부분 1번에서 해결).

1. **Lombok 플러그인 활성** ← 가장 흔한 원인
   `Settings(Ctrl+Alt+S) → Plugins → Installed`에서 **Lombok** 검색 → 비활성 상태면 체크 → IDE 재시작.
   (에디터의 빨간 줄/자동완성은 이 플러그인이 담당한다. 최신 IntelliJ에는 번들되어 있으나 꺼져 있을 수 있다.)

2. **어노테이션 처리 활성**
   `Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing` 체크.
   (저장소의 `.idea/compiler.xml`에 이미 `enabled=true`로 커밋되어 있어, Gradle 임포트 시 자동 적용된다.)

3. **빌드/실행을 Gradle에 위임**(일관성 권장)
   `Settings → Build, Execution, Deployment → Build Tools → Gradle`에서
   **"Build and run using"**·**"Run tests using"** 를 모두 **Gradle**로.

4. **Gradle 프로젝트 다시 로드**
   Gradle 도구창(코끼리 아이콘) → **Reload All Gradle Projects**.

5. 그래도 빨간 줄이 남으면 **캐시 무효화 후 재빌드**
   `File → Invalidate Caches… → Invalidate and Restart` → 재기동 후 `Build → Rebuild Project`.

### 참고

- **Java 21 SDK**: `File → Project Structure → Project SDK`를 **21**로 지정(`build.gradle` 툴체인과 일치).
- DTO 계층은 Java **record**라 Lombok을 적용하지 않는다(record가 불변 데이터 캐리어로 동일 역할 수행). Lombok은 가변 도메인 POJO에만 사용한다.
