<script setup lang="ts">
// BO 관리자 매출(S8) 대시보드 — ADMIN 전용.
//   (v2.4) 전 매장 매출 비중 원차트(상위 5 + ETC) 추가 + 매장 단건 매출 조회 유지.
//   후기는 별도 페이지(/admin/reviews)로 분리됨(v2.4) — 본 화면에는 매출만 잔존.
import { computed, onMounted, ref, watch } from 'vue'
import { getApprovedStores } from '~/services/storeService'
import { buildSalesSlices, type SalesByStoreRow } from '~/composables/useSalesChart'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

// BE 응답 DTO와 일치
interface SalesResponse {
  storeId: string
  total: number
}

const stores = getApprovedStores()
const storeId = ref('')
const sales = ref<SalesResponse | null>(null)

// 전 매장 매출 비중(원차트용)
const byStore = ref<SalesByStoreRow[]>([])
const slices = computed(() => buildSalesSlices(byStore.value))

function base() {
  return useRuntimeConfig().public.apiBase
}

// 매장 단건 매출(S8)
async function loadStoreSales() {
  if (!storeId.value) {
    sales.value = null
    return
  }
  const { $apiFetch } = useNuxtApp()
  try {
    sales.value = await $apiFetch<SalesResponse>(`${base()}/admin/stores/${storeId.value}/sales`)
  } catch {
    sales.value = null
  }
}

// 전 매장 매출 비중(원차트)
async function loadByStore() {
  const { $apiFetch } = useNuxtApp()
  try {
    byStore.value = await $apiFetch<SalesByStoreRow[]>(`${base()}/admin/sales/by-store`)
  } catch {
    byStore.value = []
  }
}

watch(storeId, loadStoreSales)
onMounted(loadByStore)

function won(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}
</script>

<template>
  <section data-testid="page-admin-sales" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매출</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        전 매장 매출 비중과 매장별 세차완료 매출 합계를 확인할 수 있어요.
      </p>
    </header>

    <!-- 전 매장 매출 비중 원차트(v2.4) — 상위 5개 + ETC -->
    <div class="card p-6 sm:p-8">
      <h2 class="mb-5 text-lg font-bold">매장별 매출 비중 (상위 5 + ETC)</h2>
      <SalesPieChart :slices="slices" />
    </div>

    <!-- 매장 단건 매출(S8) -->
    <div class="card mt-6 p-6 sm:p-8">
      <label class="field-label" for="sales-store">매장 매출 조회</label>
      <select id="sales-store" v-model="storeId" data-testid="sales-store" class="bo-input">
        <option value="" disabled>선택</option>
        <option v-for="s in stores" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>

      <div v-if="sales" class="mt-6 border-t border-[--color-line-soft] pt-5">
        <span class="field-label">세차완료 매출 합계</span>
        <p data-testid="admin-sales-total" class="text-2xl font-bold text-[--color-content-strong]">
          {{ won(sales.total) }}
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.bo-input {
  margin-top: 0.375rem;
  width: 100%;
  border-radius: 0.625rem;
  border: 1px solid var(--color-line);
  background-color: var(--color-surface-1);
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-content-strong);
}
.bo-input:focus {
  outline: 2px solid color-mix(in oklab, var(--color-brand-primary) 45%, transparent);
  outline-offset: 1px;
}
</style>
