---
name: design-tokens
description: 세차 예약 서비스의 확립된 디자인 토큰(프리미엄 다크+일렉트릭)과 공통 컴포넌트 클래스 정의 위치
metadata:
  type: project
---

세차 예약 서비스의 디자인 시스템은 **프리미엄 다크 + 일렉트릭**(디테일링샵 무드)으로 확립됨. Tailwind v4 사용.

**Why:** 화면이 밋밋해 전 화면을 일관된 디자인 시스템으로 재디자인. 사용자가 다크+일렉트릭 방향을 직접 선택.

**How to apply:** 새 화면/컴포넌트도 아래 토큰·공통 클래스를 재사용해 일관성 유지. 색을 하드코딩하지 말 것.

## 토큰 정의 위치: `app/assets/main.css` 의 `@theme { }` 블록
- 표면(배경): `--color-surface-base #0b1120`(네이비, body 배경), `--color-surface-1 #0f172a`(slate-900, 카드), `--color-surface-2 #1e293b`(slate-800, 강조 표면/드롭다운)
- 브랜드: `--color-brand-primary #38bdf8`(sky-400), `--color-brand-primary-strong #0ea5e9`(sky-500, hover), `--color-brand-accent #a3e635`(lime-400)
- 텍스트: `--color-content #e2e8f0`(slate-200, 본문), `--color-content-strong #f8fafc`(제목), `--color-content-muted #94a3b8`(slate-400, 보조)
- 보더: `--color-line #334155`(slate-700), `--color-line-soft #1e293b`(slate-800)
- 모션: `--ease-out-soft cubic-bezier(0.22,1,0.36,1)`, 전환 150~250ms

## 공통 컴포넌트 클래스: `app/assets/main.css` 의 `@layer components { }`
- `.container-app` — 최대폭 72rem + 좌우 1.5rem 패딩(본문 컨테이너)
- `.btn` + `.btn-primary`(스카이 채움, 텍스트 sky-950) / `.btn-ghost`(외곽선). hover/focus-visible(2중 링)/active(translateY)/disabled 정의됨
- `.input-field` — 입력 공통(표면1 배경, 포커스 시 스카이 보더+25% 글로우 링). `.has-icon`이면 좌측 패딩
- `.card` — surface-1 + 라운드 1rem + 그림자
- `.field-label`, `.badge-accent`(라임 칩)

## 전역: `app/assets/base.css`
- body 네이비 배경 + 미묘한 라디얼 글로우(상단 스카이/하단 라임), `background-attachment: fixed`
- h1~h3 slate-50 + letter-spacing -0.02em, a 태그 sky-400/hover sky-300
- ⚠️ main.css의 `@import` 순서(tailwindcss → base.css) 유지 필수

## 레이아웃/네비
- `app/layouts/default.vue`: 스티키 헤더 + `.container-app` 본문(`py-10 sm:py-14`)
- `app/components/AppNav.vue`: 워드마크 `WASH.`(라임 점 + 스카이 마침표), `.nav-link` 클래스(hover/router-link-active=스카이). data-testid 보존됨
- `app/components/SearchableSelect.vue`: 좌측 돋보기 아이콘 입력 + 드롭다운(surface-2). 옵션 `.active`(키보드 하이라이트)=스카이 표면+좌측 라임 바, hover=스카이14%. testid 패턴 유지

## 빈 상태(empty state) 패턴
reservations/review 페이지는 `.card` 안에 아이콘 칩 + 제목 + 보조문구 + (선택)CTA 버튼 구조로 통일.
