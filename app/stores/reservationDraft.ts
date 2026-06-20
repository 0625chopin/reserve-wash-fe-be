import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import type { CarType, ServiceType } from '~/types/enums'
import type { Reservation, Slot } from '~/types/domain'
import {
  getApprovedStores,
  getBaysForCar,
  getCarTypes,
  getManager,
  getServiceTypes,
  isManagerOffAt,
} from '~/services/storeService'
import { getPrice } from '~/services/priceService'

// 확정 예약 요약 (완료 페이지 표시용)
interface ReservationSummary {
  store: string
  manager: string
  carType: string
  service: string
  bay: string
  date: string
  time: string
  amount: string
}

// 예약 위저드 진행 상태 스토어 — '진행 중 선택'만 소유한다(in-memory, require 6.5.3).
// 슬롯 점유의 진실은 reservation 스토어가 계속 소유한다(역할 분리).
// TODO(2·3단계): 진행 상태 영속화를 서버/Redis/JWT로 교체(이 스토어가 경계, require 12.3)
export const useReservationDraftStore = defineStore('reservationDraft', () => {
  // 슬롯 점유/확정·인증은 기존 스토어 위임 (자동 임포트)
  const reservation = useReservationStore()
  const auth = useAuthStore()

  // 이름 매핑용 카탈로그 (services 경유)
  const approvedStores = getApprovedStores()
  const carTypeOptions = getCarTypes()
  const serviceTypeOptions = getServiceTypes()

  // 진행 선택 상태
  const storeId = ref<string | null>(null)
  const managerId = ref<string | null>(null)
  const carType = ref<CarType | null>(null)
  const serviceType = ref<ServiceType | null>(null)
  const bayId = ref<string | null>(null)
  const date = ref<string | null>(null)
  const time = ref<string | null>(null)

  // 낙관적 점유 추적 + 확정 결과
  const heldSlot = ref<Slot | null>(null)
  const lastReservation = ref<ReservationSummary | null>(null)

  // 파생 — 선택 매니저/베이 목록/가격
  const selectedManager = computed(() =>
    managerId.value ? getManager(managerId.value) : undefined,
  )
  const storeBays = computed(() =>
    storeId.value && carType.value ? getBaysForCar(storeId.value, carType.value) : [],
  )
  const price = computed(() =>
    carType.value && serviceType.value ? getPrice(carType.value, serviceType.value) : null,
  )
  // 금액 표기 — SSR/CSR 일관을 위해 ko-KR 로케일 고정
  const priceLabel = computed(() =>
    price.value === null ? '' : `${price.value.toLocaleString('ko-KR')}원`,
  )

  // 1페이지 게이트 (매장·매니저·차종·서비스)
  const canProceedToSlot = computed(
    () => !!storeId.value && !!managerId.value && !!carType.value && !!serviceType.value,
  )
  // 확정 게이트 (+ 베이·날짜·시간)
  const canConfirm = computed(
    () => canProceedToSlot.value && !!bayId.value && !!date.value && !!time.value,
  )

  // 현재 선택 (베이, 날짜, 시간)으로 슬롯 객체 구성 — status는 스토어가 판정하므로 placeholder
  function currentSlot(useBayId: string): Slot {
    return {
      storeId: storeId.value as string,
      bayId: useBayId,
      date: date.value as string,
      timeSlot: time.value as string,
      status: 'AVAILABLE',
    }
  }

  // 선택한 베이가 현재 (날짜·시간)에 이미 점유(RESERVED/COMPLETED)됐는지 (require 6.1)
  const selectedBayOccupied = computed(() => {
    if (!bayId.value || !date.value || !time.value) return false
    const status = reservation.getStatus(currentSlot(bayId.value))
    return status === 'RESERVED' || status === 'COMPLETED'
  })

  // 점유(HOLDING) 해제 — 슬롯 식별값(베이/날짜/시간)이 바뀔 때 호출 (require 7.1)
  function releaseHeld() {
    if (heldSlot.value) {
      reservation.releaseSlot(heldSlot.value)
      heldSlot.value = null
    }
  }

  // NFR-1 cascade — 선행 단계 변경 시 후속 선택 초기화
  watch(storeId, () => {
    managerId.value = null
    bayId.value = null
  })
  watch(carType, () => {
    bayId.value = null
    releaseHeld()
  })
  watch(managerId, () => {
    date.value = null
    time.value = null
    releaseHeld()
  })
  watch(date, () => {
    releaseHeld()
    // 날짜 변경으로 기존 선택 시간이 매니저 휴무(교대조)에 걸리면 시간 초기화 (require 5.5/6.1)
    if (
      selectedManager.value &&
      date.value &&
      time.value &&
      isManagerOffAt(selectedManager.value, date.value, time.value)
    ) {
      time.value = null
    }
  })
  watch(time, () => {
    releaseHeld()
  })

  // 미니그리드 베이 클릭 → 즉시 HOLDING (낙관적 점유, require 7.1)
  function holdBay(useBayId: string) {
    if (!date.value || !time.value) return
    if (heldSlot.value && heldSlot.value.bayId !== useBayId) releaseHeld()
    bayId.value = useBayId
    const slot = currentSlot(useBayId)
    if (reservation.holdSlot(slot)) {
      heldSlot.value = slot
    }
  }

  // 예약 식별자 — 세션 내 증가 시퀀스(현재시각·랜덤 의존 회피로 결정적)
  let seq = 0
  function nextReservationId(): string {
    seq += 1
    return `rsv-${seq}`
  }

  // 예약 확정 — 미점유면 낙관적 점유 시도 후 서버 확정(reservation.confirmReservation 위임, require 7장)
  // 성공 시 lastReservation 요약을 채우고 true, 충돌이면 false(호출부에서 재선택 토스트)
  async function confirm(): Promise<boolean> {
    if (!canConfirm.value) return false
    const slot = currentSlot(bayId.value as string)
    if (reservation.getStatus(slot) !== 'HOLDING') {
      if (!reservation.holdSlot(slot)) return false
      heldSlot.value = slot
    }
    const newReservation: Reservation = {
      id: nextReservationId(),
      userId: auth.currentUser?.id ?? '',
      storeId: storeId.value as string,
      bayId: bayId.value as string,
      managerId: managerId.value,
      date: date.value as string,
      timeSlot: time.value as string,
      carType: carType.value as CarType,
      serviceType: serviceType.value as ServiceType,
      amount: price.value ?? 0,
      status: 'RESERVED',
    }
    if (!(await reservation.confirmReservation(newReservation))) return false
    heldSlot.value = null
    lastReservation.value = {
      store: approvedStores.find((s) => s.id === storeId.value)?.name ?? '',
      manager: selectedManager.value?.name ?? '',
      carType: carTypeOptions.find((c) => c.code === carType.value)?.name ?? '',
      service: serviceTypeOptions.find((s) => s.code === serviceType.value)?.name ?? '',
      bay: storeBays.value.find((b) => b.id === bayId.value)?.code ?? '',
      date: date.value ?? '',
      time: time.value ?? '',
      amount: priceLabel.value,
    }
    return true
  }

  // 전체 초기화 — 완료 후 새 예약 / 위저드 이탈 시
  function reset() {
    releaseHeld()
    storeId.value = null
    managerId.value = null
    carType.value = null
    serviceType.value = null
    bayId.value = null
    date.value = null
    time.value = null
    lastReservation.value = null
  }

  return {
    storeId,
    managerId,
    carType,
    serviceType,
    bayId,
    date,
    time,
    heldSlot,
    lastReservation,
    selectedManager,
    storeBays,
    price,
    priceLabel,
    canProceedToSlot,
    canConfirm,
    selectedBayOccupied,
    holdBay,
    confirm,
    reset,
    releaseHeld,
  }
})
