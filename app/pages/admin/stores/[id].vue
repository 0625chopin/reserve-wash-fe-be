<script setup lang="ts">
// BO 관리자 매장 수정(v2.4) — ADMIN 전용. 기존 값 로드 후 이름·승인·베이 구성 갱신.
import { onMounted, ref, watch } from 'vue'
import {
  getAdminStore,
  updateAdminStore,
  type AdminStorePayload,
} from '~/services/adminStoreService'
import type { BaySize } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

const BAY_SIZES: BaySize[] = ['SMALL', 'MID', 'LARGE', 'XLARGE']

const route = useRoute()
const storeId = route.params.id as string

const name = ref('')
const bayCount = ref(1)
const approved = ref(false)
const bays = ref<{ code: string; size: BaySize }[]>([])
const error = ref('')
const loaded = ref(false)

// 베이 수 변경 시 코드(A1~AN) 동기화 — 기존 size 보존(최초 로드 이후에만 동작)
watch(bayCount, (n) => {
  if (!loaded.value) return
  const count = Math.max(0, Math.floor(n))
  bays.value = Array.from({ length: count }, (_, i) => ({
    code: `A${i + 1}`,
    size: bays.value[i]?.size ?? 'SMALL',
  }))
})

async function load() {
  try {
    const s = await getAdminStore(storeId)
    name.value = s.name
    approved.value = s.approved
    bays.value = s.bays.map((b) => ({ code: b.code, size: b.size }))
    bayCount.value = s.bayCount
    loaded.value = true
  } catch {
    error.value = '매장 정보를 불러오지 못했습니다.'
  }
}

async function onSubmit() {
  error.value = ''
  if (!name.value.trim()) {
    error.value = '매장 이름을 입력하세요.'
    return
  }
  const payload: AdminStorePayload = {
    name: name.value.trim(),
    bayCount: bays.value.length,
    approved: approved.value,
    bays: bays.value,
  }
  try {
    await updateAdminStore(storeId, payload)
    await navigateTo('/admin/stores')
  } catch {
    error.value = '매장 수정에 실패했습니다.'
  }
}

onMounted(load)
</script>

<template>
  <section data-testid="page-admin-store-edit" class="mx-auto max-w-md">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매장 수정</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매장 이름·승인 상태·베이 구성을 변경합니다.
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
            <li v-for="b in bays" :key="b.code" class="flex items-center gap-3">
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
          <span class="text-sm text-[--color-content]">승인(체크 시 예약 화면에 노출)</span>
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
            저장
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
