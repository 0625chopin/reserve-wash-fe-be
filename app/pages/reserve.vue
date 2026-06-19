<script setup lang="ts">
// 예약 페이지 (FW3 매장선택 → FW4 매니저선택) — 보호 라우트
import { computed, ref, watch } from 'vue'
import { getApprovedStores, getManager, getManagersByStore } from '~/services/storeService'

definePageMeta({ middleware: 'auth' })

// 승인된 매장만 노출 (require 6.1)
const approvedStores = getApprovedStores()

const selectedStoreId = ref<string | null>(null)
const selectedManagerId = ref<string | null>(null)

// 선택 매장의 매니저만 노출 (require 6.3)
const storeManagers = computed(() =>
  selectedStoreId.value ? getManagersByStore(selectedStoreId.value) : [],
)

// 매장이 바뀌면 매니저 선택 초기화
watch(selectedStoreId, () => {
  selectedManagerId.value = null
})

// 선택 매니저의 휴무 — Phase 5 슬롯 비활성 컨텍스트 (require 6.1)
const selectedManager = computed(() =>
  selectedManagerId.value ? getManager(selectedManagerId.value) : undefined,
)
const selectedManagerDayoffs = computed(() => selectedManager.value?.dayoffDates ?? [])
</script>

<template>
  <section data-testid="page-reserve" class="mx-auto max-w-2xl">
    <!-- 페이지 헤더 -->
    <header class="mb-8">
      <span class="badge-accent mb-3">세차 예약</span>
      <h1 class="text-3xl font-bold">예약하기</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매장을 먼저 선택하면 해당 매장의 매니저를 고를 수 있어요.
      </p>
    </header>

    <!-- 단계형 폼 카드 -->
    <div class="card space-y-7 p-6 sm:p-8">
      <!-- 1단계: 매장 -->
      <div>
        <span class="field-label">
          <span class="step-num">1</span>
          매장 선택
        </span>
        <SearchableSelect
          v-model="selectedStoreId"
          testid="store-select"
          :options="approvedStores"
          label-key="name"
          value-key="id"
          placeholder="매장 검색"
        />
      </div>

      <!-- 2단계: 매니저 (매장 선택 시 노출) -->
      <div v-if="selectedStoreId">
        <span class="field-label">
          <span class="step-num">2</span>
          매니저 선택
        </span>
        <SearchableSelect
          v-model="selectedManagerId"
          testid="manager-select"
          :options="storeManagers"
          label-key="name"
          value-key="id"
          placeholder="매니저 검색"
        />
      </div>

      <!-- 휴무 안내 -->
      <p
        v-if="selectedManager"
        data-testid="manager-dayoffs"
        class="flex flex-wrap items-center gap-2 rounded-lg border border-[--color-line-soft] bg-[--color-surface-2] px-3.5 py-3 text-sm text-[--color-content]"
      >
        <span class="font-semibold text-[--color-content-muted]">휴무</span>
        <span>{{ selectedManagerDayoffs.join(', ') || '없음' }}</span>
      </p>
    </div>
  </section>
</template>

<style scoped>
/* 단계 번호 칩 — 라벨 앞 작은 인디케이터 */
.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  margin-right: 0.375rem;
  border-radius: 9999px;
  background-color: color-mix(in oklab, var(--color-brand-primary) 20%, transparent);
  color: var(--color-brand-primary);
  font-size: 0.6875rem;
  font-weight: 700;
}
</style>
