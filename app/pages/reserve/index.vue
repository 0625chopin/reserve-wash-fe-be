<script setup lang="ts">
// 예약 위저드 1페이지 — 차종 → 매장 → 매니저 → 서비스 (가격 자동 표시) → 다음
//   차종을 먼저 고르면, 그 차종을 수용하는 베이가 있는 매장만 노출한다(특대형은 XLARGE 베이 보유 매장만).
import { computed } from 'vue'
import {
  getCarTypes,
  getManagersByStore,
  getServiceTypes,
  getStoresForCar,
} from '~/services/storeService'

definePageMeta({ middleware: ['auth', 'reservation-fresh-entry'] })

// 진행 상태는 위저드 스토어가 소유(페이지 간 유지)
const draft = useReservationDraftStore()

// 카탈로그 (services 경유)
const carTypeOptions = getCarTypes()
const serviceTypeOptions = getServiceTypes()

// 선택 차종을 수용하는 매장만 노출 (특대형 → XLARGE 베이 보유 매장만)
const storesForCar = computed(() => (draft.carType ? getStoresForCar(draft.carType) : []))
// 선택 매장의 매니저만 노출 (require 6.3)
const storeManagers = computed(() => (draft.storeId ? getManagersByStore(draft.storeId) : []))

function onNext() {
  if (draft.canProceedToSlot) navigateTo('/reserve/slot')
}
</script>

<template>
  <section data-testid="page-reserve" class="mx-auto max-w-2xl">
    <!-- 페이지 헤더 -->
    <header class="mb-4">
      <span class="badge-accent mb-2">세차 예약 · 1/3</span>
      <h1 class="text-2xl font-bold">무엇을 예약할까요?</h1>
      <p class="mt-1 text-sm text-[--color-content-muted]">
        차종 · 매장 · 매니저 · 서비스를 선택하면 다음 단계로 넘어가요.
      </p>
    </header>

    <!-- 단계형 폼 카드 -->
    <div class="card space-y-3 p-5 sm:p-6">
      <!-- 차종 (최상단) -->
      <div>
        <span class="field-label">차종 선택</span>
        <SearchableSelect
          v-model="draft.carType"
          testid="cartype-select"
          :options="carTypeOptions"
          label-key="name"
          value-key="code"
          placeholder="차종 검색 (예: 소형)"
        />
      </div>

      <!-- 매장 (차종 선택 시 노출) — 그 차종을 수용하는 매장만 -->
      <div v-if="draft.carType">
        <span class="field-label">매장 선택</span>
        <SearchableSelect
          v-model="draft.storeId"
          testid="store-select"
          :options="storesForCar"
          label-key="name"
          value-key="id"
          placeholder="매장 검색"
        />
        <p
          v-if="storesForCar.length === 0"
          data-testid="store-empty-for-car"
          class="mt-2 text-xs font-medium text-[--color-brand-accent]"
        >
          선택하신 차종을 수용할 수 있는 매장이 없어요. 다른 차종을 선택해 주세요.
        </p>
      </div>

      <!-- 매니저 (매장 선택 시 노출) -->
      <div v-if="draft.storeId">
        <span class="field-label">매니저 선택</span>
        <SearchableSelect
          v-model="draft.managerId"
          testid="manager-select"
          :options="storeManagers"
          label-key="name"
          value-key="id"
          placeholder="매니저 검색"
        />
      </div>

      <!-- 서비스 (매장 선택 시 노출) — 차종 × 서비스로 가격 산출 (require 10.2/10.3) -->
      <div v-if="draft.storeId">
        <span class="field-label">서비스 선택</span>
        <SearchableSelect
          v-model="draft.serviceType"
          testid="service-select"
          :options="serviceTypeOptions"
          label-key="name"
          value-key="code"
          placeholder="서비스 검색 (예: 외부세차)"
        />
        <!-- 가격 자동 계산 + 현장결제 안내 (require 6.4) -->
        <div
          v-if="draft.price !== null"
          data-testid="price-display"
          class="mt-3 flex items-center justify-between rounded-xl border border-[--color-line-soft] bg-[--color-surface-1] px-4 py-3"
        >
          <span class="text-sm text-[--color-content-muted]">예상 금액</span>
          <span class="text-right">
            <strong class="text-lg font-bold text-[--color-content-strong]">{{
              draft.priceLabel
            }}</strong>
            <span class="block text-xs text-[--color-content-muted]">
              현장결제만 가능 (예약 시 결제 없음)
            </span>
          </span>
        </div>
      </div>

      <!-- 하단: 다음 -->
      <div class="border-t border-[--color-line-soft] pt-6">
        <button
          data-testid="reserve-next"
          type="button"
          class="btn btn-primary w-full"
          :disabled="!draft.canProceedToSlot"
          @click="onNext"
        >
          다음 · 날짜·시간·베이 선택
        </button>
        <p
          v-if="!draft.canProceedToSlot"
          class="mt-2 text-center text-xs text-[--color-content-muted]"
        >
          차종 · 매장 · 매니저 · 서비스를 모두 선택해 주세요.
        </p>
      </div>
    </div>
  </section>
</template>
