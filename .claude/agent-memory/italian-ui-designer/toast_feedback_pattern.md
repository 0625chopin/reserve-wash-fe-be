---
name: toast-feedback-pattern
description: 세차 예약 서비스의 성공 토스트/피드백 오버레이 디자인 패턴(scrim+블러, 라임 면 헤더, 라벨/값 요약)
metadata:
  type: project
---

성공 피드백 토스트는 `app/pages/reserve.vue`의 `<Teleport to="body">` 영역에 확립됨. 다른 화면에 피드백 오버레이를 추가할 때 이 패턴을 재사용.

**Why:** 토스트가 라임 텍스트/보더인데 배경 폼의 라임 선택 표시와 동색으로 섞여 안 보였음. scrim 부재로 배경이 그대로 비침.

**How to apply:** 다크 테마에서 액센트(라임) 색을 토스트 "텍스트"로 쓰면 배경 액센트와 충돌하므로, 액센트는 "면(面)"으로(채운 헤더/배지) 쓰고 텍스트는 고대비 중립색(content-strong)으로.

## 핵심 패턴
- **scrim 래퍼**: `fixed inset-0` 풀스크린 + `.toast-scrim`(배경 `surface-base 64%` + `backdrop-filter: blur(4px)`)로 배경 폼을 가라앉힘. 래퍼는 `pointer-events-none`(자동 닫힘이라 클릭 차단 불필요), 카드만 `pointer-events-auto`.
- **카드**: `.toast-card` = surface-1 본문 + 라임 45% 보더 + 깊은 그림자(`0 24px 48px -16px`). `max-w-sm`.
- **헤더**: `.toast-header` = 라임 14% 채움 면 + 라임 28% 하단 보더 + 라임 채움 체크 배지(`.toast-check`: 라임 배경 + surface-base 글리프). 제목은 content-strong(흰색).
- **요약**: `<dl>` `grid-cols-[auto_1fr]`로 라벨(content-muted)/값(content-strong, font-medium) 위계.
- **트랜지션** `name="toast"`: scrim opacity+blur 페이드, 카드는 `translateY(12px) scale(0.97)`에서 떠오름. 0.25s `--ease-out-soft`.

## E2E 보존 제약 (reserve 화면 한정)
- `data-testid="reserve-result"`를 카드 루트에 유지. 그 안에 요약 텍스트('강남점'·'이매니저' 포함)를 렌더해야 `e2e/reserve.spec.ts`(97행)가 통과.
- script의 `onReserve`/`toastTimer`(5초 자동 닫힘)/`onBeforeUnmount`는 건드리지 말 것 — 마크업/스타일만 개선.
- E2E는 총 13개, 전부 통과 기준. 관련: [[design-tokens]]
