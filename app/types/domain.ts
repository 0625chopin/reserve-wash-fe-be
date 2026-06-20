// 도메인 엔티티 인터페이스 (require_v1.md 5.4 표 기준)
import type {
  BaySize,
  CarType,
  DayoffType,
  ReservationStatus,
  ServiceType,
  SlotStatus,
  UserRole,
} from '~/types/enums'

// 사용자 (require 3.1)
export interface User {
  id: string
  email: string
  name: string
  role: UserRole
}

// 매장 (require 5.1)
export interface Store {
  id: string
  name: string
  bayCount: number // 동일 시간대 최대 수용 = 베이 수 N (require 5.2)
  approved: boolean // 가입/승인된 매장만 노출 (require 6.1)
}

// 베이 — 매장 내 개별 세차 라인 (require 5.1)
export interface Bay {
  id: string
  storeId: string
  code: string // 'A1' ~ 'AN'
  size: BaySize // 수용 가능한 차 크기 등급 (이 등급 이하의 차를 수용)
}

// 매니저 휴무 = (날짜, 휴무 유형) — 전일 또는 교대조 단위 부분 휴무 (require 5.4, 5.5)
export interface ManagerDayoff {
  date: string // 'YYYY-MM-DD'
  type: DayoffType // FULL_DAY=전일 / SHIFT_1~3=교대조 단위
}

// 매니저 (require 3.1, 6.1)
export interface Manager {
  id: string
  storeId: string
  name: string
  isStoreAdmin: boolean // 매장 최고권한 매니저 여부
  dayoffs: ManagerDayoff[] // 휴무(전일/교대조) — Phase 5 슬롯 비활성에 사용 (require 5.5, 6.1)
}

// 슬롯 = (매장, 베이, 날짜, 30분 시간단위), UNIQUE (require 5.2)
export interface Slot {
  storeId: string
  bayId: string
  date: string // 'YYYY-MM-DD'
  timeSlot: string // 'HH:mm' (30분 단위 시작 시각)
  status: SlotStatus
}

// 가격 — 차종 × 서비스 단가 (require 10.3)
export interface Price {
  carType: CarType
  serviceType: ServiceType
  amount: number // 원
}

// 예약 (require 6장, 11.3)
export interface Reservation {
  id: string
  serverId?: string // BE가 부여한 예약 id — 상태 전이 PATCH 호출용(표시 id와 분리)
  userId: string
  storeId: string
  bayId: string
  managerId: string | null // 매니저 대행이 아니면 null
  date: string // 'YYYY-MM-DD'
  timeSlot: string // 'HH:mm'
  carType: CarType
  serviceType: ServiceType
  amount: number
  status: ReservationStatus
}

// 후기/평점 — 예약(세차) 완료 사용자만 작성 (require 9.1)
export interface Review {
  id: string
  reservationId: string
  userId: string
  storeId: string
  managerId: string | null
  rating: number // 1 ~ 5 정수 (require 9.1)
  text: string
  createdAt: string // ISO 문자열
}
