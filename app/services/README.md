# services 계층 규칙

> 본 문서는 `app/services/`의 운영 규칙(정본)이다. 폴더 전체 구조 설명은 중복하지 않으며,
> 아키텍처 표는 [`docs/roadmaps/ROADMAP_1.md`](../../docs/roadmaps/ROADMAP_1.md) 2.1·2.2, 운영 규칙은
> 루트 [`shrimp-rules.md`](../../shrimp-rules.md)를 참조한다.

## 역할

- `app/services/`는 **데이터 접근 추상화 계층**이다.
- 컴포넌트(`app/components/`)·페이지(`app/pages/`)·스토어(`app/stores/`)가 더미 데이터(`app/data/`)나
  외부 API에 **직접 접근하지 못하도록** 한 겹 감싼다.
- 1단계(프론트 더미)에서 2단계(Spring Boot)·3단계(MySQL)로 데이터 계층이 바뀌어도
  **호출부(UI/스토어) 코드를 거의 수정하지 않도록** 경계를 고정하는 것이 목적이다.

## 단방향 의존 계약 (필수)

```
component / page / store  →  services  →  data
```

- ✅ 컴포넌트·스토어는 **반드시 `app/services/`를 경유**하여 데이터를 읽는다.
- ❌ 컴포넌트·스토어에서 `app/data/`를 **직접 import 금지**.
- ❌ `app/services/`가 `app/components/`·`app/stores/`를 역으로 import 금지(단방향 유지).
- ✅ `app/services/`는 `app/data/`와 `app/types/`만 의존한다.

## 작성 규칙

- `export function` 형태로 노출한다(자동 임포트 대상 아님 — 호출부에서 **명시 import**).
- import 경로는 `~/` 별칭을 사용한다. 상대경로 `../` 금지.
- oxfmt 스타일: **세미콜론 없음**, **작은따옴표**.
- 주석·문서는 한국어, 식별자(함수명/변수명/타입명)는 영어.

## 계약 형태 (패턴 예시 — 도메인 본문은 Phase 1에서 구현)

> 아래는 시그니처/의존 방향을 보여주는 **패턴 예시**다. 실제 도메인 타입·더미 데이터·반환 값은
> Phase 1(도메인 타입 & 더미 데이터 레이어)에서 정의한다.

```ts
// app/services/exampleService.ts (패턴 예시)
import type { Example } from '~/types/domain'
import { examples } from '~/data/examples'

// 호출부는 이 시그니처에만 의존한다. 내부 구현(data → $fetch)이 바뀌어도 시그니처는 유지한다.
export function getExample(id: string): Example | undefined {
  return examples.find((e) => e.id === id)
}
```

## 2단계 이후 교체 지점

- **2단계(Spring Boot 인메모리)**: 위 예시의 `import { examples } from '~/data/...'` +
  `find(...)` 부분을 `$fetch`/`useFetch` 서버 호출로 교체한다.
  **함수 시그니처(이름·인자·반환 타입)는 그대로 유지**하여 호출부 무변경을 보장한다.
- **3단계(MySQL)**: 서버 측에서 트랜잭션 + 유니크 인덱스로 무결성을 보장한다.
  프론트 `services` 시그니처는 동일하게 유지하고, 충돌 시 재선택 유도 UX만 재사용한다.
