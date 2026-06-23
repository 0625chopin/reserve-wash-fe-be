<script setup lang="ts">
// BO 관리자 후기 확인(S6) — ADMIN 전용. (v2.4) 매출 페이지에서 분리된 후기 전용 화면.
//   기존 엔드포인트(GET /api/admin/stores/{id}/reviews)를 그대로 재사용한다(신규 API 없음 — 화면 이전).
import { ref, watch } from 'vue'
import { getApprovedStores } from '~/services/storeService'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

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
const reviews = ref<AdminReview[]>([])

function base() {
  return useRuntimeConfig().public.apiBase
}

async function load() {
  if (!storeId.value) {
    reviews.value = []
    return
  }
  const { $apiFetch } = useNuxtApp()
  try {
    reviews.value = await $apiFetch<AdminReview[]>(`${base()}/admin/stores/${storeId.value}/reviews`)
  } catch {
    reviews.value = []
  }
}

watch(storeId, load)
</script>

<template>
  <section data-testid="page-admin-reviews" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">후기</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매장을 선택하면 해당 매장의 고객 후기를 확인할 수 있어요.
      </p>
    </header>

    <div class="card p-6 sm:p-8">
      <label class="field-label" for="reviews-store">매장</label>
      <select id="reviews-store" v-model="storeId" data-testid="reviews-store" class="bo-input">
        <option value="" disabled>선택</option>
        <option v-for="s in stores" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>
    </div>

    <div v-if="storeId" class="mt-6">
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
