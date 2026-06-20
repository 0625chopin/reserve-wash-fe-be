<script setup lang="ts">
// BO 관리자 매출(S8)·후기 확인(S6) 대시보드 — ADMIN 전용.
import { ref, watch } from 'vue'
import { getApprovedStores } from '~/services/storeService'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

// BE 응답 DTO와 일치
interface SalesResponse {
  storeId: string
  total: number
}
interface AdminReview {
  id: string
  reservationId: string
  userId: string
  storeId: string
  managerId: string | null
  rating: number
  text: string
  createdAt: string
}

const stores = getApprovedStores()
const storeId = ref('')
const sales = ref<SalesResponse | null>(null)
const reviews = ref<AdminReview[]>([])

function base() {
  return useRuntimeConfig().public.apiBase
}

async function load() {
  if (!storeId.value) {
    sales.value = null
    reviews.value = []
    return
  }
  const { $apiFetch } = useNuxtApp()
  try {
    sales.value = await $apiFetch<SalesResponse>(`${base()}/admin/stores/${storeId.value}/sales`)
  } catch {
    sales.value = null
  }
  try {
    reviews.value = await $apiFetch<AdminReview[]>(`${base()}/admin/stores/${storeId.value}/reviews`)
  } catch {
    reviews.value = []
  }
}

watch(storeId, load)

function won(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}
</script>

<template>
  <section data-testid="page-admin-sales" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매출 · 후기</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매장을 선택하면 세차완료 매출 합계와 후기를 확인할 수 있어요.
      </p>
    </header>

    <div class="card p-6 sm:p-8">
      <label class="field-label" for="sales-store">매장</label>
      <select id="sales-store" v-model="storeId" data-testid="sales-store" class="bo-input">
        <option value="" disabled>선택</option>
        <option v-for="s in stores" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>

      <!-- 매출 총액(S8) -->
      <div v-if="sales" class="mt-6 border-t border-[--color-line-soft] pt-5">
        <span class="field-label">세차완료 매출 합계</span>
        <p data-testid="admin-sales-total" class="text-2xl font-bold text-[--color-content-strong]">
          {{ won(sales.total) }}
        </p>
      </div>
    </div>

    <!-- 후기 목록(S6) -->
    <div v-if="storeId" class="mt-6">
      <h2 class="mb-3 text-lg font-bold">후기</h2>
      <ul v-if="reviews.length" class="space-y-3">
        <li
          v-for="r in reviews"
          :key="r.id"
          :data-testid="`admin-review-row-${r.id}`"
          class="card p-4"
        >
          <p class="font-semibold text-[--color-brand-accent]">★ {{ r.rating }} / 5</p>
          <p v-if="r.text" class="mt-1 text-sm text-[--color-content-strong]">{{ r.text }}</p>
          <p class="mt-1 text-xs text-[--color-content-muted]">{{ r.createdAt }}</p>
        </li>
      </ul>
      <p v-else data-testid="admin-reviews-empty" class="text-sm text-[--color-content-muted]">
        아직 후기가 없습니다.
      </p>
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
