---
name: verification-focus
description: 이 프로젝트에서 반복 검증할 취약 영역과 e2e 셀렉터/결정성 패턴
metadata:
  type: feedback
---

검증 시 우선 확인할 취약 영역과 효과적인 패턴.

**How to apply: 다음 항목은 누락/혼동이 잦으니 검증 때 먼저 본다.**
- 폼 검증: review 작성 시 텍스트는 선택(빈 값 허용), 평점만 필수(1~5). login은 빈값/이메일형식/인증실패 3단계.
- 인증 가드 체이닝: 보호 페이지는 `definePageMeta({ middleware: [...] })`. 위저드는 ['auth','reservation-wizard-guard'], 후기는 ['auth','review-guard'].
- SSR 안전: 휠/그리드 등 클라이언트 전용은 `<ClientOnly>`로 감쌈. auth는 useCookie로 SSR 가드 안전.
- 상태 전이 차단: completeReservation은 RESERVED만, cancelReservation은 RESERVED/HOLDING만 허용(COMPLETED/CANCELED 차단).

**e2e 결정성 패턴(재사용 가능)**
- 더미 시드: store1-A1 / 2026-06-25 10:00 = RESERVED (reservationService.ts). 점유 비활성 테스트에 사용.
- 예약 id: fresh 컨텍스트에서 'rsv-1'부터 결정적(reservationDraft seq). 후기 id 'rv-1'.
- 더미 로그인: user@test.com / password. 매니저 휴무 더미: 김매니저(store1) 2026-06-22 FULL_DAY, 2026-06-23 SHIFT_1. 박매니저(store2) 2026-06-23 SHIFT_2.
- 셀렉터: data-testid 컨벤션. SearchableSelect는 `{testid}-input`/`{testid}-options`/`{testid}-option`. 슬롯셀은 `slot-{storeId}-{bayId}-{timeSlot}`.

**검증 환경 메모**
- Playwright config: reuseExistingServer true, baseURL :3000. 단위테스트(vitest) 미설치.
- 현재 e2e 파일: auth/home/reserve/reservations/review.spec.ts.
