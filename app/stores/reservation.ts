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
  // 매니저 담당 예약(managerId 기준) — 본인 예약(reservations)과 분리. MANAGER 역할 /reservations '담당' 탭 (require v1.10 §6.6)
  const managerAssigned = ref<Reservation[]>([])
  // 매장 전체 예약(storeId 기준) — STORE_ADMIN 역할 /reservations '매장 전체' 뷰 (require v1.10 §6.6)
  const storeReservations = ref<Reservation[]>([])

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
      // userId는 서버가 JWT(uid)에서 도출하므로 바디에 싣지 않는다.
      // 서버가 부여한 예약 id(res.id)를 serverId로 캡처 — 이후 상태 전이 PATCH에 사용.
      const res = await $apiFetch<{ id: string }>(`${base}/reservations/confirm`, {
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
      reservations.value.push({ ...reservation, status: 'RESERVED', serverId: res.id })
      return true
    } catch {
      // 서버 409(타인이 선점) — 그리드에 점유 반영하고 재선택 유도
      slotStatus.value[key] = 'RESERVED'
      return false
    }
  }

  // 본인 예약 서버 하이드레이션 (require 6.1) — /reservations 진입 시 서버에서 본인 예약을 1회 로드한다.
  // 매니저 대행 예약(userId=고객)도 서버 조회에 포함되고, 하드 새로고침/직접 URL 진입에도 목록이 유지된다.
  // 서버 예약 id를 표시 id·serverId 양쪽에 매핑한다 — 목록 key/후기 라우트는 id, 상태 전이 PATCH·후기 POST는 serverId 사용.
  async function fetchMine(): Promise<void> {
    const { $apiFetch } = useNuxtApp()
    const base = useRuntimeConfig().public.apiBase
    try {
      const rows = await $apiFetch<Reservation[]>(`${base}/reservations`)
      // 세션 내 생성·전이된 로컬 예약은 serverId로 식별해 보존(표시 id 유지)하고,
      // 서버에만 있는 예약(매니저 대행 등)·새로고침 후 전체를 중복 없이 보강(append)한다.
      const known = new Set(
        reservations.value.map((r) => r.serverId).filter((id): id is string => !!id),
      )
      const additions = rows
        .filter((r) => !known.has(r.id))
        .map((r) => ({ ...r, serverId: r.id }))
      reservations.value = [...reservations.value, ...additions]
    } catch {
      // 미인증/네트워크 실패 — 기존 세션 상태를 비우지 않는다(낙관적 보존)
    }
  }

  // 매니저 담당 예약 하이드레이션 (require v1.10 §6.6) — MANAGER가 /reservations 진입 시 본인이 담당(managerId)인 예약을 로드한다.
  //   사용자가 그 매니저를 지정한 예약 + 대행 예약이 모두 포함된다. 본인 예약(fetchMine)과 분리된 목록(managerAssigned)에 저장.
  //   serverId를 표시 id로도 매핑(상태 전이 PATCH는 미사용, 읽기 전용 표시 목적).
  async function fetchManagerAssigned(): Promise<void> {
    const { $apiFetch } = useNuxtApp()
    const base = useRuntimeConfig().public.apiBase
    try {
      const rows = await $apiFetch<Reservation[]>(`${base}/manager/reservations`)
      managerAssigned.value = rows.map((r) => ({ ...r, serverId: r.id }))
    } catch {
      // 미인증/권한 없음/네트워크 실패 — 기존 목록 보존
    }
  }

  // 매장 전체 예약 하이드레이션 (require v1.10 §6.6) — STORE_ADMIN이 /reservations 진입 시 소속 매장의 모든 예약을 로드한다.
  async function fetchStoreReservations(): Promise<void> {
    const { $apiFetch } = useNuxtApp()
    const base = useRuntimeConfig().public.apiBase
    try {
      const rows = await $apiFetch<Reservation[]>(`${base}/store-admin/reservations`)
      storeReservations.value = rows.map((r) => ({ ...r, serverId: r.id }))
    } catch {
      // 미인증/권한 없음/네트워크 실패 — 기존 목록 보존
    }
  }

  // 관리자(ADMIN) 매장별 예약 하이드레이션 (require v1.11 §6.6) — 관리자는 소속 매장이 없어 매장을 선택해 조회한다.
  //   기존 S4 엔드포인트(GET /api/admin/stores/{id}/reservations, ADMIN 인가)를 재사용한다.
  //   응답(AdminReservationResponse)은 Reservation의 상위집합(userName/userEmail 추가)이라 표시에 그대로 사용 가능.
  async function fetchAdminStoreReservations(storeId: string): Promise<void> {
    const { $apiFetch } = useNuxtApp()
    const base = useRuntimeConfig().public.apiBase
    try {
      const rows = await $apiFetch<Reservation[]>(`${base}/admin/stores/${storeId}/reservations`)
      storeReservations.value = rows.map((r) => ({ ...r, serverId: r.id }))
    } catch {
      storeReservations.value = []
    }
  }

  // 점유(HOLDING) 해제 → AVAILABLE 복귀 (취소/충돌 시)
  function releaseSlot(slot: Slot) {
    const key = slotKey(slot.storeId, slot.bayId, slot.date, slot.timeSlot)
    if (slotStatus.value[key] === 'HOLDING') {
      slotStatus.value[key] = 'AVAILABLE'
    }
  }

  // 세차완료 — RESERVED → COMPLETED (FW6, require 11.3). 서버 PATCH가 전이를 강제(불가 전이 409).
  // 성공 시 로컬 상태·슬롯을 COMPLETED로 반영. 서버 실패면 로컬 무변경 + false.
  async function completeReservation(id: string): Promise<boolean> {
    const target = reservations.value.find((r) => r.id === id)
    if (!target || target.status !== 'RESERVED') return false
    const { $apiFetch } = useNuxtApp()
    const base = useRuntimeConfig().public.apiBase
    try {
      await $apiFetch(`${base}/reservations/${target.serverId}/complete`, { method: 'PATCH' })
    } catch {
      return false
    }
    target.status = 'COMPLETED'
    slotStatus.value[slotKey(target.storeId, target.bayId, target.date, target.timeSlot)] =
      'COMPLETED'
    return true
  }

  // 예약 취소 — RESERVED/HOLDING → CANCELED (FW7, require 11.3 b/c). 서버가 슬롯을 AVAILABLE로 release.
  // 승인 전/후 케이스 모두 동일 처리. 성공 시 로컬 상태·슬롯 반영, 서버 실패면 false.
  async function cancelReservation(id: string): Promise<boolean> {
    const target = reservations.value.find((r) => r.id === id)
    if (!target || (target.status !== 'RESERVED' && target.status !== 'HOLDING')) return false
    const { $apiFetch } = useNuxtApp()
    const base = useRuntimeConfig().public.apiBase
    try {
      await $apiFetch(`${base}/reservations/${target.serverId}/cancel`, { method: 'PATCH' })
    } catch {
      return false
    }
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
    managerAssigned,
    storeReservations,
    reservedCount,
    getStatus,
    holdSlot,
    confirmReservation,
    fetchMine,
    fetchManagerAssigned,
    fetchStoreReservations,
    fetchAdminStoreReservations,
    releaseSlot,
    completeReservation,
    cancelReservation,
  }
})
