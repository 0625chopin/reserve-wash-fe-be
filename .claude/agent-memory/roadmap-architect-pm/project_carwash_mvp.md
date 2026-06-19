---
name: project-carwash-mvp
description: 자동차 세차 예약 서비스 MVP — 요구사항/로드맵 위치, 범위, 핵심 도메인 규칙
metadata:
  type: project
---

`start-kit2`는 Nuxt 4(Vue 3 `<script setup>` + Vite 내장 + Nitro) 스타터를 토대로 한 **자동차 세차 예약 서비스 MVP** 프로젝트다.

- **요구사항 정의서**: `docs/require_v1.md` (v1.1). 13개 섹션. FW/MW/SM 프로세스 코드로 추적성 확보.
- **개발 로드맵**: `docs/ROADMAP.md` (v1.0, 작성일 2026-06-20). Phase 0~8 구조, 1차 FO + 프론트 더미(데이터 1단계)에 집중. 총 예상 공수 약 19일.

**핵심 도메인 규칙 (require_v1.md 기준)**:
- 슬롯 = (매장, 베이, 날짜, 30분 시간단위), UNIQUE. 동일 시간대 최대 수용 = 베이 수 N (A1~AN).
- 동시성 1단계 = 클라이언트 슬롯 잠금 시뮬레이션 + 낙관적 갱신. 2단계=Spring Boot UNIQUE+락, 3단계=MySQL 트랜잭션+유니크 인덱스.
- FO 플로우: FW2(로그인)→FW3(매장선택)→FW4(매니저선택)→FW5(예약)→FW6(세차완료) / 취소=FW7.
- 가격: 차종 5(LIGHT/SMALL/MID/LARGE/VAN_ETC) × 서비스 4(EXT/INT/FULL/PREMIUM) 매트릭스, 확정 단가(require 10.3).
- 후기: 평점 1~5 정수, 세차완료(COMPLETED) 사용자만 작성.
- 결제: 현장결제만. BO(매니저/관리자)는 2차 과제(문서화만).

**Why**: 데이터 진화 3단계 전략 — 백엔드(Java+Spring Boot) 없이 FE/UX 먼저 검증 후 점진 도입.
**How to apply**: 신규 작업 시 데이터 접근은 `app/services/`로 추상화해 2단계 교체 비용을 낮춘다. `// TODO(2단계)/(3단계)` 주석 위치가 교체 지점.
