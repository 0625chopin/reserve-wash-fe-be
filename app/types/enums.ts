// 도메인 리터럴 유니언 타입 모음 (require_v1.md 정본 기준)

// 사용자 역할 (require 3.1)
export type UserRole = 'USER' | 'MANAGER' | 'STORE_ADMIN' | 'ADMIN'

// 차종 5분류 (require 10.1 확정)
export type CarType = 'LIGHT' | 'SMALL' | 'MID' | 'LARGE' | 'VAN_ETC'

// 서비스 4분류 (require 10.2 확정)
export type ServiceType = 'EXT' | 'INT' | 'FULL' | 'PREMIUM'

// 베이 수용 크기 등급 — 차의 크기(소/중/대)에 따라 예약 가능한 베이가 달라짐
export type BaySize = 'SMALL' | 'MID' | 'LARGE'

// 슬롯 상태 — 동시성 1단계 시뮬레이션의 핵심 (require 7.1)
export type SlotStatus = 'AVAILABLE' | 'HOLDING' | 'RESERVED' | 'COMPLETED'

// 예약 상태 — 상태 전이 (require 11.3)
export type ReservationStatus =
  | 'HOLDING' // 슬롯 점유(확정 전)
  | 'RESERVED' // 예약 확정(승인 후 가정)
  | 'COMPLETED' // 세차 완료
  | 'CANCELED' // 취소됨
