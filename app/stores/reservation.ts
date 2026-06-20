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
    return (
      slotStatus.value[key] ??
      getSeededSlotStatus(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
    )
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

  // 확정 — 서버(POST /api/reservations/confirm) 위임. UNIQUE 제약 + 낙관락이 동시성 최종 판정.
  // 성공 시 로컬 RESERVED 반영 + 목록 추가, 409 등 충돌이면 false(호출부에서 재선택 토스트).
  // 3단계(MySQL)에서도 서버 트랜잭션 + 유니크 인덱스로 시그니처 동일 유지.
  async function confirmReservation(reservation: Reservation): Promise<boolean> {
    const key = slotKey(
      reservation.storeId,
      reservation.bayId,
      reservation.date,
      reservation.timeSlot,
    )
    const { $apiFetch } = useNuxtApp()
    const base = useRuntimeConfig().public.apiBase
    try {
      // userId는 서버가 JWT(uid)에서 도출하므로 바디에 싣지 않는다
      await $apiFetch(`${base}/reservations/confirm`, {
        method: 'POST',
        body: {
          storeId: reservation.storeId,
          bayId: reservation.bayId,
          date: reservation.date,
          timeSlot: reservation.timeSlot,
          managerId: reservation.managerId,
          carType: reservation.carType,
          serviceType: reservation.serviceType,
          amount: reservation.amount,
        },
      })
      slotStatus.value[key] = 'RESERVED'
      reservations.value.push({ ...reservation, status: 'RESERVED' })
      return true
    } catch {
      // 서버 409(타인이 선점) — 그리드에 점유 반영하고 재선택 유도
      slotStatus.value[key] = 'RESERVED'
      return false
    }
  }

  // 점유(HOLDING) 해제 → AVAILABLE 복귀 (취소/충돌 시)
  function releaseSlot(slot: Slot) {
    const key = slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
    if (slotStatus.value[key] === 'HOLDING') {
      slotStatus.value[key] = 'AVAILABLE'
    }
  }

  // 세차완료 — RESERVED → COMPLETED (FW6, require 11.3). 슬롯도 COMPLETED 고정
  // 불가능 전이(RESERVED 아님)는 차단한다.
  // TODO(2단계): 상태 전이를 서버 검증(reservationService)으로 위임
  function completeReservation(id: string): boolean {
    const target = reservations.value.find((r) => r.id === id)
    if (!target || target.status !== 'RESERVED') return false
    target.status = 'COMPLETED'
    slotStatus.value[slotKey(target.storeId, target.bayId, target.date, target.timeSlot)] =
      'COMPLETED'
    return true
  }

  // 예약 취소 — RESERVED/HOLDING → CANCELED (FW7, require 11.3 b/c). 슬롯은 AVAILABLE로 release
  // 승인 전/후 케이스 모두 동일 처리(MVP는 승인 단계 없음). COMPLETED/CANCELED는 차단한다.
  // TODO(2단계): 상태 전이를 서버 검증(reservationService)으로 위임
  function cancelReservation(id: string): boolean {
    const target = reservations.value.find((r) => r.id === id)
    if (!target || (target.status !== 'RESERVED' && target.status !== 'HOLDING')) return false
    target.status = 'CANCELED'
    // 시드 RESERVED를 덮어쓰도록 런타임 맵에 명시 AVAILABLE 기록
    slotStatus.value[slotKey(target.storeId, target.bayId, target.date, target.timeSlot)] =
      'AVAILABLE'
    return true
  }

  const reservedCount = computed(() => reservations.value.length)

  return {
    slotStatus,
    reservations,
    reservedCount,
    getStatus,
    holdSlot,
    confirmReservation,
    releaseSlot,
    completeReservation,
    cancelReservation,
  }
})
