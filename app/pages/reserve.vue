<script setup lang="ts">
// 예약 페이지 (FW3 매장선택 → FW4 매니저선택 → 베이 → 날짜·시간 선택) — 보호 라우트
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { CarType } from '~/types/enums'
import {
  getApprovedStores,
  getBaysForCar,
  getCarTypes,
  getManager,
  getManagersByStore,
} from '~/services/storeService'

definePageMeta({ middleware: 'auth' })

// 승인된 매장만 노출 (require 6.1)
const approvedStores = getApprovedStores()
// 차종 카탈로그 (require 10.1)
const carTypeOptions = getCarTypes()

const selectedStoreId = ref<string | null>(null)
const selectedManagerId = ref<string | null>(null)
const selectedCarType = ref<CarType | null>(null)
const selectedBayId = ref<string | null>(null)
const selectedDate = ref<string | null>(null)
const selectedTime = ref<string | null>(null)

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
const selectedManagerDayoffs = computed(() => selectedManager.value?.dayoffDates ?? [])

// 매장이 바뀌면 매니저·베이 초기화 (차종은 매장 무관이라 유지)
watch(selectedStoreId, () => {
  selectedManagerId.value = null
  selectedBayId.value = null
})
// 차종이 바뀌면 베이 목록이 달라지므로 베이 초기화
watch(selectedCarType, () => {
  selectedBayId.value = null
})
// 매니저가 바뀌면(휴무 변동) 날짜·시간 초기화
watch(selectedManagerId, () => {
  selectedDate.value = null
  selectedTime.value = null
  reserved.value = null
})
// 베이가 바뀌면 확정 요약만 초기화
watch(selectedBayId, () => {
  reserved.value = null
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

// 휴무일은 disabled (require 6.1)
const dateItems = computed(() =>
  baseDates.map((d) => ({ ...d, disabled: selectedManagerDayoffs.value.includes(d.value) })),
)

// 30분 단위 시간 슬롯 48개 (require 5.2) — useSlots 자동 임포트
const timeItems = computed(() => generateTimeSlots().map((t) => ({ label: t, value: t })))

// 날짜·시간 단계 노출 조건 — 매니저·베이까지 선택 완료
const canPickSlot = computed(() => !!selectedManagerId.value && !!selectedBayId.value)

// 예약 확정 가능 여부 — 매장·매니저·차종·베이·날짜·시간 모두 선택
const canReserve = computed(
  () =>
    !!selectedStoreId.value &&
    !!selectedManagerId.value &&
    !!selectedCarType.value &&
    !!selectedBayId.value &&
    !!selectedDate.value &&
    !!selectedTime.value,
)

// 예약 확정 결과(요약). 실제 슬롯 잠금·동시성 처리는 Phase 5에서 reservation 스토어로 구현
const reserved = ref<{
  store: string
  manager: string
  carType: string
  bay: string
  date: string
  time: string
} | null>(null)

// 예약 접수 토스트 — 5초 후 자동 닫힘
let toastTimer: ReturnType<typeof setTimeout> | null = null

function onReserve() {
  if (!canReserve.value) return
  reserved.value = {
    store: selectedStore.value?.name ?? '',
    manager: selectedManager.value?.name ?? '',
    carType: selectedCarTypeName.value,
    bay: selectedBay.value?.code ?? '',
    date: selectedDate.value ?? '',
    time: selectedTime.value ?? '',
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

      <!-- 날짜·시간 휠 (매니저·베이 선택 시 노출) -->
      <div v-if="canPickSlot">
        <span class="field-label">날짜 · 시간 선택</span>
        <p v-if="selectedManagerDayoffs.length" class="mb-2 text-xs text-[--color-content-muted]">
          취소선 표시된 휴무일은 선택할 수 없어요.
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
              <dt class="text-[--color-content-muted]">일시</dt>
              <dd class="font-medium text-[--color-content-strong]">
                {{ reserved.date }} {{ reserved.time }}
              </dd>
            </dl>
          </div>
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
</style>
