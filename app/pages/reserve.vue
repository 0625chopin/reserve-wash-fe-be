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
  <section data-testid="page-reserve">
    <h1>예약</h1>

    <SearchableSelect
      v-model="selectedStoreId"
      testid="store-select"
      :options="approvedStores"
      label-key="name"
      value-key="id"
      placeholder="매장 검색"
    />

    <SearchableSelect
      v-if="selectedStoreId"
      v-model="selectedManagerId"
      testid="manager-select"
      :options="storeManagers"
      label-key="name"
      value-key="id"
      placeholder="매니저 검색"
    />

    <p v-if="selectedManager" data-testid="manager-dayoffs">
      휴무: {{ selectedManagerDayoffs.join(', ') || '없음' }}
    </p>
  </section>
</template>
