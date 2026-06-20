---
name: project-carwash-mvp
description: 자동차 세차 예약 서비스 MVP — 요구사항/로드맵 위치, 범위, 핵심 도메인 규칙, 역할/승인 단계 수
metadata:
  type: project
---

`reserve-wash-fe-be`는 Nuxt 4 스타터 기반 **자동차 세차 예약 서비스 MVP**다(2차에서 모노레포 `backend/` Spring Boot + MyBatis 추가).

- **요구사항 정의서(정본)**: `docs/require_v1.md`. **현재 v1.7**(644줄). 버전 이력 표가 §1.2에 있고, deprecated 서술을 취소선/`(deprecated — vX)`로 보존하므로 **항상 최신 버전 번호를 먼저 확인**할 것(과거에 v1.6으로 오인된 적 있음).
- **로드맵**: `docs/roadmaps/ROADMAP_1.md`(1차 FO+프론트더미, 현재 v1.6), `docs/roadmaps/ROADMAP_2.md`(2차 BE진화+BO, 현재 v2.2). CLAUDE.md 규칙: 1차=ROADMAP_1=shrimp-task-manager, 2차=ROADMAP_2=shrimp-task-manager-phase2.

**역할/승인 (require v1.7 — 매우 중요, 버전 따라 번복됨)**:
- 역할 **4종**: `USER`/`MANAGER`(일반매장매니저)/`STORE_ADMIN`(매장매니저관리자)/`ADMIN`(관리자). v1.6에서 STORE_ADMIN 제거→3역할 단순화했다가 **v1.7에서 4역할로 번복·부활**.
- **가입 승인 = 2단계**: 매니저 계열은 `EMAIL_VERIFIED→PENDING_APPROVAL_L1(매장매니저관리자 1차 M7)→PENDING_APPROVAL_L2(관리자 2차 S3)→ACTIVE`. `ACTIVE` 전 로그인 불가. USER는 `EMAIL_VERIFIED→ACTIVE` 직행(승인 없음).
- **휴가/반차 승인 = 1단계**: `SUBMITTED→APPROVED/REJECTED`, 매장매니저관리자(STORE_ADMIN) 종결(M6 신청→M8 승인). **관리자 개입 없음**. ⚠️ 가입(2단계)과 휴가/반차(1단계)의 **단계 수가 다름** — 혼동 금지.
- 프로세스 코드(v1.7): M6=휴가/반차 신청, M7=가입 1차 승인, M8=휴가/반차 승인, S3=가입 2차 승인, S4=예약상태, S6=후기, S8=매출, S9=매니저 근무상태(신설). 구 "M6=예약승인" 폐기.
- FE 페이지 역할별 4그룹 분리(require §12.4): `/manager/*`, `/store-admin/*`, `/admin/*`, USER FO.

**핵심 도메인 규칙**:
- 슬롯=(매장,베이,날짜,30분), UNIQUE. 동일시간대 최대수용=베이 수 N(A1~AN).
- 동시성 3단계: 1=클라이언트 잠금 시뮬+낙관적 갱신, 2=Spring Boot UNIQUE+락(version 컬럼 낙관락/`FOR UPDATE` 비관락), 3=MySQL 트랜잭션+유니크 인덱스. DB 접근=**MyBatis 확정(JPA 미사용, v1.5)**.
- 가격: 차종 5(LIGHT/SMALL/MID/LARGE/VAN_ETC)×서비스 4(EXT/INT/FULL/PREMIUM) 매트릭스(require §10.3 확정 단가).
- 매니저 3교대(SHIFT_1 06~14/SHIFT_2 14~22/SHIFT_3 22~06)·휴무유형 FULL_DAY/SHIFT_n(§5.5).
- 예약 3단계 위저드 `/reserve`→`/reserve/slot`→`/reserve/done`, 진행데이터 Pinia(1단계 in-memory).
- 후기: 평점 1~5, COMPLETED 사용자만.

**⚠️ 코드-요구 불일치(2026-06 시점)**: BE Phase 7 구현이 휴무를 **2단계 결재(approve-l1/l2, APPROVED_L1→CONFIRMED)** 로 작성돼 v1.7(휴가/반차 1단계 종결)과 충돌. ROADMAP_2 Phase 7 구현 메모에 재정합 과제 명시(enum 1단계 축소, approve-l2 제거, 카탈로그 필터 CONFIRMED→APPROVED, 가입 2단계 신규 도입).

**How to apply**: 데이터 접근은 `app/services/`로 추상화(2단계 $fetch 교체 지점). 신규 작업 시 승인 워크플로우는 반드시 단계 수(가입 2 / 휴가 1) 구분. 관련 [[project-frontend-stack]].
