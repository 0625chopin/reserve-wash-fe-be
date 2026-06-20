---
name: roadmap-docs-conventions
description: 세차예약 ROADMAP 문서(ROADMAP_1 FE 1차 / ROADMAP_2 BE 2차) 형식·구조·범위 관례
metadata:
  type: project
---

세차 예약 서비스 로드맵을 `docs/roadmaps/`에 작성. ROADMAP_1(1차 FO+더미), ROADMAP_2(2차 BE 진화+BO).

**Why:** require_v1.md(정본)·예약_규칙_명세_v1.md(보강)을 개발 착수 가능한 단계별 로드맵으로 변환. 주니어가 위→아래로 따라오게 CoT(목표→태스크→파일→예시→DoD).

**How to apply:** 후속 ROADMAP 작성 시 아래 형식을 **그대로 미러링**.
- 머리말: `>` 인용 블록. 문서버전/작성일/작성자(PM/PL)/대상독자/연계문서(상대경로). `docs/roadmaps/`에 위치하므로 require·명세는 `../`, 다른 ROADMAP은 `./`.
- 장 골격: 0장 사용가이드(0.4 마일스톤 6컬럼 표: Phase·제목·핵심산출물·require참조·공수(일)·누적(일)) → 1장 환경셋업 → 2장 아키텍처/컨벤션 → 3장 Phase별 본문 → 4장 동시성 → 5장 데이터단계 → 6장 테스트 → 7장 컨벤션&PR체크리스트 → 8장 부록(require 추적 매핑 4컬럼: Phase·기능·require절·프로세스코드 M/S).
- Phase 공통 포맷: `> **공수: N일** · **선행조건: ...** · **require_v1.md 참조: ...**` 머리말 + #### 목표 / #### 태스크 체크리스트(`- [ ]`) / #### 생성·수정 파일 / #### 구현 예시(코드펜스) / #### 완료기준(DoD `- [ ]`) / (필요시) #### 구현 메모(📌).
- 이모지 박스 관례: ⚠️주의 💡팁 📌메모 🔄변경/배경 🔎참조.
- 코드 예시: Java/Spring은 표준 컨벤션(JPA·Lombok·@Version·@Transactional·@Lock PESSIMISTIC_WRITE·record DTO), FE TS는 oxfmt(**세미콜론 없음·작은따옴표**), 주석 전부 한국어. 실제 컴파일 가능 수준.
- 문서 끝에 마무리 `>` 인용 블록.

**ROADMAP_2(v2.0) 확정 방향:**
- BE 스택 = Spring Boot 3.x + Java 21(require 12.2). 2단계 H2 in-memory(ddl create-drop, 휘발) → 3단계 MySQL(Flyway, ddl validate, 영속).
- additive 철학: 1차 Nuxt 자산 유지, `app/services/*` 내부만 더미→$fetch/useFetch 교체. ROADMAP_1 4.4 `// TODO(2/3단계)` 주석이 실제 코드 되는 흐름.
- 범위 = 데이터 2·3단계 + BO 전체(M3~M7·S3~S8, 결재 워크플로우 SUBMITTED→APPROVED_L1→APPROVED_L2→CONFIRMED/REJECTED, SMTP/알림).
- Phase 0~10. Phase 0이 게이트: 명세 Q1~Q8을 결정표(질문/결정안/영향파일 3컬럼)로 확정 → Phase 1 데이터모델 반영. 결정표 권고안은 1차 구현 보존 방향(누적수용 유지, 가격표 차종기준 무변경).
- 동시성: 슬롯 UNIQUE(store,bay,date,time)=최종방어선. 낙관(@Version)/비관(FOR UPDATE) 락 선택, 충돌→409→1차 useToast 재선택 재사용.
- 테스트: @SpringBootTest 멀티스레드 동시성("한 건만 성공") + 1차 Playwright E2E 회귀 유지(H2/MySQL 양쪽 재실행).

**주의:** require_v1.md·예약_규칙_명세_v1.md·ROADMAP_1·기존 코드는 읽기전용. 신규 ROADMAP만 작성. [[carwash-reqdoc-conventions]] [[reservation-rules-spec]]
