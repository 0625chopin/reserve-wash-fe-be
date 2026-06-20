import type { SlotStatus } from '~/types/enums'

// 예약/슬롯 데이터 접근 추상화 (단방향 의존: services → data·types, README 계약)
// 상태 변이(hold/confirm/release)는 reservation 스토어가 소유하고, 본 service는
// (1) 1단계 더미 선점유 시드 (2) 2·3단계 서버 검증 교체 지점만 담당한다.

// 슬롯 식별 키 = (매장, 베이, 날짜, 30분 시간단위) — UNIQUE (require 5.2)
function slotKey(storeId: string, bayId: string, date: string, timeSlot: string): string {
  return `${storeId}|${bayId}|${date}|${timeSlot}`
}

// 1단계 더미 선점유 시드 — 백엔드 없이 '이미 예약된 슬롯'을 결정적으로 재현한다.
// E2E의 동시 예약 충돌·베이 점유 비활성 시나리오를 고정값으로 단정하기 위함.
// 강남점(store1) A1 베이의 2026-06-25 10:00 슬롯은 이미 예약(RESERVED)된 상태로 시드한다.
// TODO(2단계): 이 시드 조회를 $fetch GET /slots 서버 응답으로 교체(시그니처 유지)
// TODO(3단계): MySQL 슬롯 테이블 + 유니크 인덱스 조회로 교체
const seededSlotStatus: Record<string, SlotStatus> = {
  [slotKey('store1', 'store1-A1', '2026-06-25', '10:00')]: 'RESERVED',
}

// 슬롯의 더미 선점유 상태 조회 — 시드에 없으면 AVAILABLE (require 7.1 1단계)
// 호출부(reservation 스토어)는 이 시그니처에만 의존한다.
export function getSeededSlotStatus(
  storeId: string,
  bayId: string,
  date: string,
  timeSlot: string,
): SlotStatus {
  return seededSlotStatus[slotKey(storeId, bayId, date, timeSlot)] ?? 'AVAILABLE'
}
