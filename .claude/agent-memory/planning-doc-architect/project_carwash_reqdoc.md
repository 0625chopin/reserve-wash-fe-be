---
name: carwash-reqdoc-conventions
description: 자동차 세차 예약 MVP 요구사항 정의서(require_v1)의 구조·표기·확정 항목 관례
metadata:
  type: project
---

자동차 세차 예약 서비스(MVP) 요구사항 정의서를 `docs/require_v1.md`에 작성함 (13개 섹션 구조).

**Why:** 개발자 원시 초안을 기획/이해관계자가 의사결정에 쓸 수 있는 정식 문서로 재편성하기 위함. FO 사용자 플로우 우선 구현, BO는 2차 문서화.

**How to apply:** 후속 버전(v2 등) 작성 시 아래 관례를 유지할 것.
- 프로세스 코드 표기법 `FW#/M#/S#`를 그대로 보존(초안 추적성). 교정 시 매핑 추적표를 함께 둠.
- 초안 오타 교정 확정: STMP→SMTP, FW4(매장예약)→FW5, F5→FW6(세차완료), F6→FW7(예약취소).
- 인증/승인 분리 원칙: "이메일 인증(전원 필수)" vs "승인(매니저 가입=관리자, 사용자 매장가입=매장 최고매니저)". 초안의 "승인 불필요" 문구와 모순되므로 이 분리로 해소함.
- FE 스택은 Nuxt 4(Vue 3 `<script setup>` + Vite 내장 + Nitro, Pinia, 파일 기반 라우팅)로 전환 완료·확정.
- 데이터 진화 3단계(프론트 더미 → Spring Boot 인메모리 → MySQL)에 동시성 처리를 매핑. 최종 방어선은 슬롯 UNIQUE(storeId,bayId,date,timeSlot).
- 표/상태머신 표 적극 사용이 이 프로젝트의 선호 형식.

**미확정(v2에서 확정 대상):** 차종/서비스 분류 최종안, 가격표 실수치, SMTP 인프라, 알림 정책. [[carwash-undecided]]
