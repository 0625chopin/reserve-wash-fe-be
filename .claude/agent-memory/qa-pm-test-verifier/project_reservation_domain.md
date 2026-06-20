---
name: project-reservation-domain
description: 예약·세차 MVP 1차 범위의 도메인 규칙과 의도된(버그 아님) 동작 정리
metadata:
  type: project
---

자동차 세차 예약 서비스 MVP. 1차는 FO(고객 플로우) + 프론트 더미데이터(in-memory Pinia)만 구현. BO(매니저/관리자)·SMTP·알림·카드결제는 2차/추후 과제(문서화만).

**핵심 도메인 규칙 (정본: docs/require_v1.md, docs/ROADMAP.md)**
- 예약 위저드 3분할: `/reserve`(매장·매니저·차종·서비스+가격) → `/reserve/slot`(날짜·시간·베이그리드) → `/reserve/done`(완료요약).
- 슬롯 = (매장,베이,날짜,30분) UNIQUE. 동시 수용 = 베이 수 N.
- 매니저 휴무: FULL_DAY(전일, 날짜휠 비활성) / SHIFT_1(06~14)·SHIFT_2(14~22)·SHIFT_3(22~06익일)(해당 시간대 슬롯만 비활성). 운영은 24시간 유지.
- 베이 점유(RESERVED/COMPLETED)는 그리드에서 비활성(선택 불가).
- 후기: COMPLETED·본인 예약만 작성, 평점 1~5, 매장/매니저별 평균.
- 결제: 현장결제만.

**의도된 동작(버그로 오인 금지)**
- 새로고침 시 진행 데이터 초기화 → 위저드 가드가 `/reserve`로 되돌림(require 6.5.3, 1단계 의도).
- MVP는 승인(M6) 단계 없음 → confirm 시 곧장 RESERVED. 목록에 HOLDING 레코드 없음. 취소 2케이스(승인 전/후)는 모두 CANCELED+슬롯 release로 수렴(라벨만 더미 구분).
- 그리드 단일화로 점유 베이가 사전 비활성 → confirm() 충돌 분기·bay-occupied-notice·useToast는 2·3단계 서버검증 대비 방어코드로 잔존(현재 UI에서는 충돌 토스트 경로 도달 불가).
- `/reserve` 진입 시 reservation-fresh-entry 미들웨어가 draft.reset() — 이전 버튼 복귀 시에도 매장부터 새로 시작(의도).

**미확정/범위 외 (docs/예약_규칙_명세_v1.md Q1~Q8)**
- 차종↔베이 4등급(A1/B1/C1/D1)·특대형(D1) 매핑은 미해결. 현재는 누적수용(SIZE_RANK >= min) 3등급(SMALL/MID/LARGE) 유지. 임의 구현 금지.
