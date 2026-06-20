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
