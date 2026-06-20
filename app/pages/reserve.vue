<script setup lang="ts">
// 예약 페이지 (FW3 매장선택 → FW4 매니저선택 → 베이 → 날짜·시간 선택) — 보호 라우트
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { CarType, ServiceType } from '~/types/enums'
import type { Reservation, Slot } from '~/types/domain'
import {
  getApprovedStores,
  getBaysForCar,
  getCarTypes,
  getManager,
  getManagersByStore,
  getServiceTypes,
  isManagerFullDayOff,
  isManagerOffAt,
} from '~/services/storeService'
import { getPrice } from '~/services/priceService'

definePageMeta({ middleware: 'auth' })

// 승인된 매장만 노출 (require 6.1)
const approvedStores = getApprovedStores()
// 차종 카탈로그 (require 10.1)
const carTypeOptions = getCarTypes()
// 서비스 카탈로그 (require 10.2)
const serviceTypeOptions = getServiceTypes()

// 자동 임포트 스토어 — 슬롯 점유(동시성)·인증·토스트
const reservation = useReservationStore()
const auth = useAuthStore()
const { toast, show: showToast } = useToast()

const selectedStoreId = ref<string | null>(null)
const selectedManagerId = ref<string | null>(null)
const selectedCarType = ref<CarType | null>(null)
const selectedServiceType = ref<ServiceType | null>(null)
const selectedBayId = ref<string | null>(null)
const selectedDate = ref<string | null>(null)
const selectedTime = ref<string | null>(null)

// 현재 낙관적 점유(HOLDING) 중인 슬롯 — 선택 변경/확정 시 release 추적용
const heldSlot = ref<Slot | null>(null)

// 선택 매장 (요약 표시용)
const selectedStore = computed(() =>
  selectedStoreId.value ? approvedStores.find((s) => s.id === selectedStoreId.value) : undefined,
)

// 선택 매장의 매니저만 노출 (require 6.3)
const storeManagers = computed(() =>
  selectedStoreId.value ? getManagersByStore(selectedStoreId.value) : [],
)

// 선택 차종 (요약 표시용)
const selectedCarTypeName = computed(
  () => carTypeOptions.find((c) => c.code === selectedCarType.value)?.name ?? '',
)

// 선택 서비스 (요약 표시용)
const selectedServiceTypeName = computed(
  () => serviceTypeOptions.find((s) => s.code === selectedServiceType.value)?.name ?? '',
)

// 차종 × 서비스 단가 자동 계산 (require 6.4, 10.3) — 둘 다 선택돼야 산출
const price = computed(() =>
  selectedCarType.value && selectedServiceType.value
    ? getPrice(selectedCarType.value, selectedServiceType.value)
    : null,
)
// 금액 표기 — SSR/CSR 일관을 위해 ko-KR 로케일 고정
const priceLabel = computed(() =>
  price.value === null ? '' : `${price.value.toLocaleString('ko-KR')}원`,
)

// 선택 매장 + 차 크기에 맞는 베이만 노출 (require 5.2) — 차 크기에 따라 목록이 달라짐
const storeBays = computed(() =>
  selectedStoreId.value && selectedCarType.value
    ? getBaysForCar(selectedStoreId.value, selectedCarType.value)
    : [],
)
const selectedBay = computed(() =>
  selectedBayId.value ? storeBays.value.find((b) => b.id === selectedBayId.value) : undefined,
)

// 선택 매니저와 휴무 (require 6.1)
const selectedManager = computed(() =>
  selectedManagerId.value ? getManager(selectedManagerId.value) : undefined,
)
const selectedManagerDayoffs = computed(() => selectedManager.value?.dayoffs ?? [])

// 점유(HOLDING) 중인 슬롯 해제 — 슬롯 식별값(베이/날짜/시간)이 바뀔 때 호출 (require 7.1)
function releaseHeld() {
  if (heldSlot.value) {
    reservation.releaseSlot(heldSlot.value)
    heldSlot.value = null
  }
}

// 매장이 바뀌면 매니저·베이 초기화 (차종은 매장 무관이라 유지)
watch(selectedStoreId, () => {
  selectedManagerId.value = null
  selectedBayId.value = null
})
// 차종이 바뀌면 베이 목록이 달라지므로 베이 초기화 + 점유 해제
watch(selectedCarType, () => {
  selectedBayId.value = null
  releaseHeld()
})
// 매니저가 바뀌면(휴무 변동) 날짜·시간 초기화 + 점유 해제
watch(selectedManagerId, () => {
  selectedDate.value = null
  selectedTime.value = null
  reserved.value = null
  releaseHeld()
})
// 베이가 바뀌면 확정 요약만 초기화 (점유는 onHoldSlot이 직접 관리)
watch(selectedBayId, () => {
  reserved.value = null
})
// 날짜가 바뀌면 점유 해제 + 기존 선택 시간이 매니저 휴무(교대조)에 걸리면 시간 초기화 (require 5.5/6.1, NFR-1)
watch(selectedDate, () => {
  releaseHeld()
  if (
    selectedManager.value &&
    selectedDate.value &&
    selectedTime.value &&
    isManagerOffAt(selectedManager.value, selectedDate.value, selectedTime.value)
  ) {
    selectedTime.value = null
  }
})
// 시간이 바뀌면 슬롯 식별값이 달라지므로 점유 해제
watch(selectedTime, () => {
  releaseHeld()
})

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

// 오늘부터 21일치 날짜 항목 생성 (require 5.1 전일자 운영)
function buildDateItems() {
  const result: { label: string; value: string }[] = []
  const base = new Date()
  base.setHours(0, 0, 0, 0)
  for (let i = 0; i < 21; i++) {
    const d = new Date(base)
    d.setDate(base.getDate() + i)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    result.push({
      label: `${d.getMonth() + 1}/${d.getDate()} (${WEEKDAYS[d.getDay()]})`,
      value: `${y}-${m}-${day}`,
    })
  }
  return result
}
const baseDates = buildDateItems()

// 전일(FULL_DAY) 휴무 날짜만 날짜 단계에서 disabled (교대조 휴무는 시간 단계에서 처리, require 5.5/6.1)
const dateItems = computed(() =>
  baseDates.map((d) => ({
    ...d,
    disabled: selectedManager.value ? isManagerFullDayOff(selectedManager.value, d.value) : false,
  })),
)

// 30분 단위 시간 슬롯 48개 (require 5.2) — useSlots 자동 임포트
// 매니저 교대조 휴무 시간대 슬롯은 disabled (require 5.5, 6.1)
const timeItems = computed(() =>
  generateTimeSlots().map((t) => ({
    label: t,
    value: t,
    disabled:
      selectedManager.value && selectedDate.value
        ? isManagerOffAt(selectedManager.value, selectedDate.value, t)
        : false,
  })),
)

// 날짜·시간 단계 노출 조건 — 매니저·베이까지 선택 완료
const canPickSlot = computed(() => !!selectedManagerId.value && !!selectedBayId.value)

// 예약 확정 가능 여부 — 매장·매니저·차종·서비스·베이·날짜·시간 모두 선택
const canReserve = computed(
  () =>
    !!selectedStoreId.value &&
    !!selectedManagerId.value &&
    !!selectedCarType.value &&
    !!selectedServiceType.value &&
    !!selectedBayId.value &&
    !!selectedDate.value &&
    !!selectedTime.value,
)

// 예약 확정 결과(요약)
const reserved = ref<{
  store: string
  manager: string
  carType: string
  service: string
  bay: string
  date: string
  time: string
  amount: string
} | null>(null)

// 예약 접수 토스트 — 5초 후 자동 닫힘
let toastTimer: ReturnType<typeof setTimeout> | null = null

// 현재 선택 (베이, 날짜, 시간)으로 슬롯 객체 구성 — status는 스토어가 판정하므로 placeholder
function currentSlot(bayId: string): Slot {
  return {
    storeId: selectedStoreId.value as string,
    bayId,
    date: selectedDate.value as string,
    timeSlot: selectedTime.value as string,
    status: 'AVAILABLE',
  }
}

// 선택한 베이가 현재 (날짜·시간)에 이미 점유(RESERVED/COMPLETED)됐는지 — 사전 재선택 유도용 (require 6.1)
const selectedBayOccupied = computed(() => {
  if (!selectedBayId.value || !selectedDate.value || !selectedTime.value) return false
  const status = reservation.getStatus(currentSlot(selectedBayId.value))
  return status === 'RESERVED' || status === 'COMPLETED'
})

// 미니그리드에서 베이 클릭 → 즉시 HOLDING (낙관적 점유, require 7.1)
// 이미 예약된 베이는 grid가 비활성이라 호출되지 않지만, 방어적으로 충돌 안내
function onHoldSlot(bayId: string) {
  if (!selectedDate.value || !selectedTime.value) return
  // 다른 베이로 바꾸면 이전 점유 해제
  if (heldSlot.value && heldSlot.value.bayId !== bayId) releaseHeld()
  selectedBayId.value = bayId
  const slot = currentSlot(bayId)
  if (reservation.holdSlot(slot)) {
    heldSlot.value = slot
  } else {
    showToast('이미 예약된 베이예요. 다른 베이를 선택해 주세요.')
  }
}

// 예약 식별자 — 세션 내 증가 시퀀스(현재시각·랜덤 의존 회피로 결정적)
let reservationSeq = 0
function nextReservationId(): string {
  reservationSeq += 1
  return `rsv-${reservationSeq}`
}

function onReserve() {
  if (!canReserve.value) return
  const slot = currentSlot(selectedBayId.value as string)
  // 그리드 미클릭 등으로 아직 점유 전이면 확정 직전 낙관적 점유 시도
  if (reservation.getStatus(slot) !== 'HOLDING') {
    if (!reservation.holdSlot(slot)) {
      showToast('선택하신 슬롯이 방금 예약되었습니다. 다른 슬롯을 선택해 주세요.')
      return
    }
    heldSlot.value = slot
  }
  const newReservation: Reservation = {
    id: nextReservationId(),
    userId: auth.currentUser?.id ?? '',
    storeId: selectedStoreId.value as string,
    bayId: selectedBayId.value as string,
    managerId: selectedManagerId.value,
    date: selectedDate.value as string,
    timeSlot: selectedTime.value as string,
    carType: selectedCarType.value as CarType,
    serviceType: selectedServiceType.value as ServiceType,
    amount: price.value ?? 0,
    status: 'RESERVED',
  }
  // 확정 직전 충돌 재검사 (require 7.1) — 실패 시 재선택 유도 토스트
  if (!reservation.confirmReservation(newReservation)) {
    showToast('선택하신 슬롯이 방금 예약되었습니다. 다른 슬롯을 선택해 주세요.')
    return
  }
  heldSlot.value = null
  reserved.value = {
    store: selectedStore.value?.name ?? '',
    manager: selectedManager.value?.name ?? '',
    carType: selectedCarTypeName.value,
    service: selectedServiceTypeName.value,
    bay: selectedBay.value?.code ?? '',
    date: selectedDate.value ?? '',
    time: selectedTime.value ?? '',
    amount: priceLabel.value,
  }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    reserved.value = null
  }, 5000)
}

onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<template>
  <section data-testid="page-reserve" class="mx-auto max-w-2xl">
    <!-- 페이지 헤더 -->
    <header class="mb-8">
      <span class="badge-accent mb-3">세차 예약</span>
      <h1 class="text-3xl font-bold">예약하기</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매장 → 매니저 → 차종 · 베이 → 날짜·시간 순으로 선택하면 예약할 수 있어요.
      </p>
    </header>

    <!-- 단계형 폼 카드 -->
    <div class="card space-y-12 p-6 sm:p-8 sm:py-10">
      <!-- 매장 -->
      <div>
        <span class="field-label">매장 선택</span>
        <SearchableSelect
          v-model="selectedStoreId"
          testid="store-select"
          :options="approvedStores"
          label-key="name"
          value-key="id"
          placeholder="매장 검색"
        />
      </div>

      <!-- 매니저 (매장 선택 시 노출) -->
      <div v-if="selectedStoreId">
        <span class="field-label">매니저 선택</span>
        <SearchableSelect
          v-model="selectedManagerId"
          testid="manager-select"
          :options="storeManagers"
          label-key="name"
          value-key="id"
          placeholder="매니저 검색"
        />
      </div>

      <!-- 차종 (매장 선택 시 노출) — 차 크기에 따라 베이 목록이 달라짐 -->
      <div v-if="selectedStoreId">
        <span class="field-label">차종 선택</span>
        <SearchableSelect
          v-model="selectedCarType"
          testid="cartype-select"
          :options="carTypeOptions"
          label-key="name"
          value-key="code"
          placeholder="차종 검색 (예: 소형)"
        />
      </div>

      <!-- 베이 (차종 선택 시 노출, 차 크기에 맞는 베이만) -->
      <div v-if="selectedStoreId && selectedCarType">
        <span class="field-label">베이 선택</span>
        <SearchableSelect
          v-model="selectedBayId"
          testid="bay-select"
          :options="storeBays"
          label-key="code"
          value-key="id"
          placeholder="베이 검색 (예: A1)"
        />
      </div>

      <!-- 서비스 (차종 선택 시 노출) — 차종 × 서비스로 가격 산출 (require 10.2/10.3) -->
      <div v-if="selectedStoreId && selectedCarType">
        <span class="field-label">서비스 선택</span>
        <SearchableSelect
          v-model="selectedServiceType"
          testid="service-select"
          :options="serviceTypeOptions"
          label-key="name"
          value-key="code"
          placeholder="서비스 검색 (예: 외부세차)"
        />
        <!-- 가격 자동 계산 + 현장결제 안내 (require 6.4) -->
        <div
          v-if="price !== null"
          data-testid="price-display"
          class="mt-3 flex items-center justify-between rounded-xl border border-[--color-line-soft] bg-[--color-surface-1] px-4 py-3"
        >
          <span class="text-sm text-[--color-content-muted]">예상 금액</span>
          <span class="text-right">
            <strong class="text-lg font-bold text-[--color-content-strong]">{{ priceLabel }}</strong>
            <span class="block text-xs text-[--color-content-muted]">
              현장결제만 가능 (예약 시 결제 없음)
            </span>
          </span>
        </div>
      </div>

      <!-- 날짜·시간 휠 (매니저·베이 선택 시 노출) -->
      <div v-if="canPickSlot">
        <span class="field-label">날짜 · 시간 선택</span>
        <p v-if="selectedManagerDayoffs.length" class="mb-2 text-xs text-[--color-content-muted]">
          취소선 표시된 휴무(전일·교대조) 시간대는 선택할 수 없어요.
        </p>
        <!-- 휠 선택기는 클라이언트 전용(스크롤·날짜 계산) -->
        <ClientOnly>
          <div class="grid grid-cols-2 gap-3">
            <WheelPicker v-model="selectedDate" testid="date-wheel" :items="dateItems" />
            <WheelPicker v-model="selectedTime" testid="time-wheel" :items="timeItems" />
          </div>
          <template #fallback>
            <div
              class="h-[200px] animate-pulse rounded-xl border border-[--color-line-soft] bg-[--color-surface-1]"
            />
          </template>
        </ClientOnly>

        <!-- 선택 시간대 베이 점유 미니그리드 (require 5.2/6.1/7장) — 예약된 베이는 선택 불가 -->
        <ClientOnly>
          <div v-if="selectedDate && selectedTime" class="mt-4">
            <span class="field-label">{{ selectedDate }} {{ selectedTime }} 베이 현황</span>
            <p class="mb-2 text-xs text-[--color-content-muted]">
              예약된 베이는 선택할 수 없어요. 원하는 베이를 눌러 선택하세요.
            </p>
            <SlotGrid
              :store-id="selectedStoreId as string"
              :date="selectedDate"
              :time-slot="selectedTime"
              :bays="storeBays"
              @hold="onHoldSlot"
            />
            <!-- 선택한 베이가 이 시간대에 점유됐을 때 사전 재선택 유도 (require 6.1) -->
            <p
              v-if="selectedBayOccupied"
              data-testid="bay-occupied-notice"
              class="mt-2 text-xs font-medium text-[--color-brand-accent]"
            >
              선택하신 베이는 이 시간대에 이미 예약되어 있어요. 위 그리드에서 다른 베이를 선택해 주세요.
            </p>
          </div>
        </ClientOnly>
      </div>

      <!-- 하단: 예약하기 -->
      <div class="border-t border-[--color-line-soft] pt-6">
        <button
          data-testid="reserve-submit"
          type="button"
          class="btn btn-primary w-full"
          :disabled="!canReserve"
          @click="onReserve"
        >
          예약하기
        </button>
        <p v-if="!canReserve" class="mt-2 text-center text-xs text-[--color-content-muted]">
          매장 · 매니저 · 차종 · 베이 · 날짜 · 시간을 모두 선택해 주세요.
        </p>
      </div>
    </div>

    <!-- 예약 접수 토스트 — 화면 중앙, 5초 후 자동 닫힘 -->
    <!-- 배경 폼(라임 선택 표시)과 토스트가 섞이지 않도록 딤+블러 scrim으로 배경을 가라앉힘 -->
    <Teleport to="body">
      <Transition name="toast">
        <div
          v-if="reserved"
          class="toast-scrim pointer-events-none fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div
            data-testid="reserve-result"
            role="status"
            aria-live="polite"
            class="toast-card pointer-events-auto w-full max-w-sm overflow-hidden rounded-2xl"
          >
            <!-- 헤더: 라임 채움 배지로 성공 상태를 면(面)으로 강조 (텍스트 라임 대신 고대비 흰색) -->
            <div class="toast-header flex items-center gap-3 px-5 py-4">
              <span class="toast-check" aria-hidden="true">✓</span>
              <span class="text-base font-semibold text-[--color-content-strong]">
                예약이 접수되었어요
              </span>
            </div>
            <!-- 요약: 라벨(muted) + 값(strong) 위계로 또렷하게 -->
            <dl class="toast-summary grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 px-5 py-4 text-sm">
              <dt class="text-[--color-content-muted]">매장</dt>
              <dd class="font-medium text-[--color-content-strong]">{{ reserved.store }}</dd>
              <dt class="text-[--color-content-muted]">매니저</dt>
              <dd class="font-medium text-[--color-content-strong]">{{ reserved.manager }}</dd>
              <dt class="text-[--color-content-muted]">차종 · 베이</dt>
              <dd class="font-medium text-[--color-content-strong]">
                {{ reserved.carType }} · {{ reserved.bay }}
              </dd>
              <dt class="text-[--color-content-muted]">서비스</dt>
              <dd class="font-medium text-[--color-content-strong]">{{ reserved.service }}</dd>
              <dt class="text-[--color-content-muted]">일시</dt>
              <dd class="font-medium text-[--color-content-strong]">
                {{ reserved.date }} {{ reserved.time }}
              </dd>
              <dt class="text-[--color-content-muted]">금액</dt>
              <dd class="font-medium text-[--color-content-strong]">
                {{ reserved.amount }} · 현장결제
              </dd>
            </dl>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 충돌/안내 토스트 — 하단 중앙, useToast(클라이언트 타이머)로 자동 닫힘 -->
    <Teleport to="body">
      <Transition name="conflict">
        <div
          v-if="toast.visible"
          data-testid="toast"
          role="alert"
          aria-live="assertive"
          class="conflict-toast fixed inset-x-0 bottom-6 z-50 mx-auto w-fit max-w-[90%] rounded-xl px-4 py-3 text-sm"
        >
          {{ toast.message }}
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
/* scrim — 배경 폼을 딤+블러로 가라앉혀 토스트가 또렷이 떠오르게 함.
   5초 자동 닫힘이라 클릭 차단은 불필요 → pointer-events는 래퍼 none 유지(상위 클래스에서) */
.toast-scrim {
  background-color: color-mix(in oklab, var(--color-surface-base) 64%, transparent);
  backdrop-filter: blur(4px);
}

/* 카드 — surface-1 본문 + 라임 보더로 성공 상태 테두리, 깊은 그림자로 입체감 */
.toast-card {
  background-color: var(--color-surface-1);
  border: 1px solid color-mix(in oklab, var(--color-brand-accent) 45%, var(--color-line));
  box-shadow:
    0 1px 2px rgb(0 0 0 / 0.4),
    0 24px 48px -16px rgb(0 0 0 / 0.7);
}

/* 헤더 — 라임을 면(面)으로 채워 성공을 한눈에. 배경 라임 텍스트와 동색 충돌 회피 */
.toast-header {
  background-color: color-mix(in oklab, var(--color-brand-accent) 14%, transparent);
  border-bottom: 1px solid color-mix(in oklab, var(--color-brand-accent) 28%, transparent);
}

/* 토스트 체크 아이콘 — 라임 채움 배지(고대비 다크 글리프) */
.toast-check {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 9999px;
  background-color: var(--color-brand-accent);
  color: var(--color-surface-base);
  font-size: 0.9375rem;
  font-weight: 800;
}

/* 등장/퇴장 트랜지션 — scrim 페이드 + 카드가 가운데서 살짝 떠오름 */
.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.25s var(--ease-out-soft),
    backdrop-filter 0.25s var(--ease-out-soft);
}
.toast-enter-active .toast-card,
.toast-leave-active .toast-card {
  transition: transform 0.25s var(--ease-out-soft);
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
}
.toast-enter-from .toast-card,
.toast-leave-to .toast-card {
  transform: translateY(12px) scale(0.97);
}

/* 충돌/안내 토스트 — surface-2 + 라인 보더, 고대비 텍스트 (하드코딩 hex 금지) */
.conflict-toast {
  background-color: var(--color-surface-2);
  border: 1px solid var(--color-line);
  color: var(--color-content-strong);
  box-shadow: 0 12px 32px -12px rgb(0 0 0 / 0.6);
}

/* 등장/퇴장 — 아래에서 살짝 떠오르며 페이드 */
.conflict-enter-active,
.conflict-leave-active {
  transition:
    opacity 0.2s var(--ease-out-soft),
    transform 0.2s var(--ease-out-soft);
}
.conflict-enter-from,
.conflict-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
