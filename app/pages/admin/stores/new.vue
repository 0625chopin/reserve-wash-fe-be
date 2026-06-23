<script setup lang="ts">
// BO 관리자 매장 등록(v2.4) — ADMIN 전용. 🔒 approved 기본 false(미승인)로 생성.
import { ref, watch } from 'vue'
import { createAdminStore, type AdminStorePayload } from '~/services/adminStoreService'
import type { BaySize } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

const BAY_SIZES: BaySize[] = ['SMALL', 'MID', 'LARGE', 'XLARGE']

const name = ref('')
const bayCount = ref(1)
const approved = ref(false) // 🔒 기본 미승인
const bays = ref<{ code: string; size: BaySize }[]>([{ code: 'A1', size: 'SMALL' }])
const error = ref('')

// 베이 수 변경 시 코드(A1~AN)를 동기화 — 기존 size는 보존
watch(bayCount, (n) => {
  const count = Math.max(0, Math.floor(n))
  bays.value = Array.from({ length: count }, (_, i) => ({
    code: `A${i + 1}`,
    size: bays.value[i]?.size ?? 'SMALL',
  }))
})

async function onSubmit() {
  error.value = ''
  if (!name.value.trim()) {
    error.value = '매장 이름을 입력하세요.'
    return
  }
  if (bays.value.length === 0) {
    error.value = '베이를 1개 이상 구성하세요.'
    return
  }
  const payload: AdminStorePayload = {
    name: name.value.trim(),
    bayCount: bays.value.length,
    approved: approved.value,
    bays: bays.value,
  }
  try {
    await createAdminStore(payload)
    await navigateTo('/admin/stores')
  } catch {
    error.value = '매장 등록에 실패했습니다.'
  }
}
</script>

<template>
  <section data-testid="page-admin-store-new" class="mx-auto max-w-md">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매장 등록</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        신규 매장은 <strong>미승인</strong>으로 생성됩니다. 베이 구성 후 승인하면 예약 화면에 노출됩니다.
      </p>
    </header>

    <div class="card p-6 sm:p-8">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <div>
          <label for="admin-store-name" class="field-label">매장 이름</label>
          <input
            id="admin-store-name"
            v-model="name"
            data-testid="admin-store-name"
            type="text"
            placeholder="강남점"
            class="admin-store-input"
          />
        </div>

        <div>
          <label for="admin-store-baycount" class="field-label">베이 수</label>
          <input
            id="admin-store-baycount"
            v-model.number="bayCount"
            data-testid="admin-store-baycount"
            type="number"
            min="1"
            class="admin-store-input"
          />
        </div>

        <div>
          <span class="field-label">베이 구성</span>
          <ul class="mt-2 space-y-2">
            <li
              v-for="b in bays"
              :key="b.code"
              class="flex items-center gap-3"
            >
              <span class="w-10 font-mono text-sm text-[--color-content-muted]">{{ b.code }}</span>
              <select
                v-model="b.size"
                :data-testid="`admin-store-bay-size-${b.code}`"
                class="admin-store-input !mt-0"
              >
                <option v-for="sz in BAY_SIZES" :key="sz" :value="sz">{{ sz }}</option>
              </select>
            </li>
          </ul>
        </div>

        <label class="flex items-center gap-2">
          <input
            v-model="approved"
            data-testid="admin-store-approved"
            type="checkbox"
            class="h-4 w-4"
          />
          <span class="text-sm text-[--color-content]">즉시 승인(체크 시 예약 화면에 노출)</span>
        </label>

        <p
          v-if="error"
          data-testid="admin-store-error"
          class="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300"
        >
          {{ error }}
        </p>

        <div class="flex items-center justify-end gap-2">
          <NuxtLink to="/admin/stores" class="btn btn-ghost">취소</NuxtLink>
          <button data-testid="admin-store-submit" type="submit" class="btn btn-primary">
            매장 등록
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.admin-store-input {
  margin-top: 0.375rem;
  width: 100%;
  border-radius: 0.625rem;
  border: 1px solid var(--color-line);
  background-color: var(--color-surface-1);
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-content-strong);
}
.admin-store-input:focus {
  outline: 2px solid color-mix(in oklab, var(--color-brand-primary) 45%, transparent);
  outline-offset: 1px;
}
.admin-store-input option {
  background-color: var(--color-surface-1);
  color: var(--color-content-strong);
}
</style>
