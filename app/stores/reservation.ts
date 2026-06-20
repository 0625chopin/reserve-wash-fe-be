import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Reservation, Slot } from '~/types/domain'
import type { SlotStatus } from '~/types/enums'
import { getSeededSlotStatus } from '~/services/reservationService'

// 슬롯 식별 키 = (매장, 베이, 날짜, 30분 시간단위) — UNIQUE (require 5.2)
function slotKey(storeId: string, bayId: string, date: string, timeSlot: string): string {
  return `${storeId}|${bayId}|${date}|${timeSlot}`
}

export const useReservationStore = defineStore('reservation', () => {
  // 런타임 슬롯 상태 맵 (1단계 클라이언트 시뮬레이션, require 7.1)
  const slotStatus = ref<Record<string, SlotStatus>>({})
  const reservations = ref<Reservation[]>([])

  // 슬롯 상태 조회 — 런타임 맵 우선, 없으면 service 더미 시드와 합성 (require 7.1)
  // release로 'AVAILABLE'을 명시 기록하면 시드(RESERVED)를 오버라이드한다.
  function getStatus(slot: Slot): SlotStatus {
    const key = slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
    return slotStatus.value[key] ?? getSeededSlotStatus(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
  }

  // 슬롯 선택 → 즉시 HOLDING (낙관적 갱신, require 7.1 1단계)
  function holdSlot(slot: Slot): boolean {
    const current = getStatus(slot)
    // 충돌 감지: 이미 점유/예약/완료 상태면 실패 (베이 점유 선택 불가 — require 6.1)
    if (current === 'RESERVED' || current === 'COMPLETED' || current === 'HOLDING') {
      return false
    }
    slotStatus.value[slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)] = 'HOLDING'
    return true
  }

  // 확정 직전 충돌 재검사 후 RESERVED 전이 (require 7.1 1단계)
  // TODO(2단계): 이 검증을 서버(reservationService)로 위임 — UNIQUE 제약 + 락
  // TODO(3단계): MySQL 트랜잭션 + 유니크 인덱스로 원천 차단
  function confirmReservation(reservation: Reservation): boolean {
    const key = slotKey(
      reservation.storeId,
      reservation.bayId,
      reservation.date,
      reservation.timeSlot,
    )
    if (slotStatus.value[key] !== 'HOLDING') {
      return false // 충돌 — 재선택 유도
    }
    slotStatus.value[key] = 'RESERVED'
    reservations.value.push({ ...reservation, status: 'RESERVED' })
    return true
  }

  // 점유(HOLDING) 해제 → AVAILABLE 복귀 (취소/충돌 시)
  function releaseSlot(slot: Slot) {
    const key = slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
    if (slotStatus.value[key] === 'HOLDING') {
      slotStatus.value[key] = 'AVAILABLE'
    }
  }

  const reservedCount = computed(() => reservations.value.length)

  return { slotStatus, reservations, reservedCount, getStatus, holdSlot, confirmReservation, releaseSlot }
})
