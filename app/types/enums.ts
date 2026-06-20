// 도메인 리터럴 유니언 타입 모음 (require_v1.md 정본 기준)

// 사용자 역할 (require 3.1)
export type UserRole = 'USER' | 'MANAGER' | 'STORE_ADMIN' | 'ADMIN'

// 차종 5분류 (require 10.1 확정)
export type CarType = 'LIGHT' | 'SMALL' | 'MID' | 'LARGE' | 'VAN_ETC'

// 서비스 4분류 (require 10.2 확정)
export type ServiceType = 'EXT' | 'INT' | 'FULL' | 'PREMIUM'

// 베이 수용 크기 등급 — 차의 크기(소/중/대/특대)에 따라 예약 가능한 베이가 달라짐
// 2차 Phase 0 Q1 확정(2026-06-21): 특대형(XLARGE) 신설 — 승합·기타(VAN_ETC) 수용. BE BaySize와 값집합 일치.
export type BaySize = 'SMALL' | 'MID' | 'LARGE' | 'XLARGE'

// 슬롯 상태 — 동시성 1단계 시뮬레이션의 핵심 (require 7.1)
export type SlotStatus = 'AVAILABLE' | 'HOLDING' | 'RESERVED' | 'COMPLETED'

// 예약 상태 — 상태 전이 (require 11.3)
export type ReservationStatus =
  | 'HOLDING' // 슬롯 점유(확정 전)
  | 'RESERVED' // 예약 확정(승인 후 가정)
  | 'COMPLETED' // 세차 완료
  | 'CANCELED' // 취소됨

// 매니저 휴무 유형 — 운영은 24시간 유지, 근무는 3교대(require 5.5)
// 'FULL_DAY'=전일 휴무, SHIFT_1~3=교대조 단위 부분 휴무
//   SHIFT_1(오전조) 06:00~14:00 / SHIFT_2(오후조) 14:00~22:00 / SHIFT_3(야간조) 22:00~06:00(익일)
// 기존 '오전/오후 반차' 개념을 3교대 기반 부분 휴무로 일반화한 것이다.
export type DayoffType = 'FULL_DAY' | 'SHIFT_1' | 'SHIFT_2' | 'SHIFT_3'
