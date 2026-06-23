<script setup lang="ts">
// BO 관리자 매장 관리(v2.4) — 매장 목록·등록 진입·삭제. ADMIN 전용.
//   🔒 삭제: 연관 데이터(예약/후기/매니저/슬롯) 있으면 서버 409(STORE_HAS_DEPENDENCIES) → 안내 메시지.
import { onMounted, ref } from 'vue'
import { listAdminStores, deleteAdminStore, type AdminStore } from '~/services/adminStoreService'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

const stores = ref<AdminStore[]>([])
const message = ref('')
const error = ref('')

async function load() {
  try {
    stores.value = await listAdminStores()
  } catch {
    stores.value = []
    error.value = '매장 목록을 불러오지 못했습니다.'
  }
}

async function onDelete(store: AdminStore) {
  message.value = ''
  error.value = ''
  if (!confirm(`'${store.name}' 매장을 삭제할까요?`)) return
  try {
    await deleteAdminStore(store.id)
    message.value = `'${store.name}' 매장을 삭제했습니다.`
    await load()
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    if (status === 409) {
      error.value = '연관 데이터가 있어 삭제할 수 없습니다.'
    } else {
      error.value = '삭제에 실패했습니다.'
    }
  }
}

onMounted(load)
</script>

<template>
  <section data-testid="page-admin-stores" class="mx-auto max-w-3xl">
    <header class="mb-8 flex items-start justify-between gap-4">
      <div>
        <span class="badge-accent mb-3">백오피스 · 관리자</span>
        <h1 class="text-3xl font-bold">매장 관리</h1>
        <p class="mt-2 text-sm text-[--color-content-muted]">
          매장을 등록·수정·삭제합니다. 신규 매장은 <strong>미승인</strong>으로 생성되며, 승인해야 예약 화면에 노출됩니다.
        </p>
      </div>
      <NuxtLink
        data-testid="admin-store-new"
        to="/admin/stores/new"
        class="btn btn-primary shrink-0 !px-4 !py-2 !text-sm"
      >
        매장 등록
      </NuxtLink>
    </header>

    <ul v-if="stores.length" class="space-y-3">
      <li
        v-for="s in stores"
        :key="s.id"
        :data-testid="`admin-store-row-${s.id}`"
        class="card flex items-center justify-between gap-3 p-4"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">
            {{ s.name }}
            <span
              :data-testid="`admin-store-approved-${s.id}`"
              class="ml-2 rounded-full px-2 py-0.5 text-xs font-medium"
              :class="
                s.approved
                  ? 'bg-emerald-500/15 text-emerald-300'
                  : 'bg-amber-500/15 text-amber-300'
              "
            >
              {{ s.approved ? '승인' : '미승인' }}
            </span>
          </p>
          <p class="text-sm text-[--color-content-muted]">
            베이 {{ s.bayCount }}개 · {{ s.bays.map((b) => `${b.code}(${b.size})`).join(', ') || '-' }}
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <NuxtLink
            :data-testid="`admin-store-edit-${s.id}`"
            :to="`/admin/stores/${s.id}`"
            class="btn btn-ghost !px-3 !py-1.5 !text-sm"
          >
            수정
          </NuxtLink>
          <button
            :data-testid="`admin-store-delete-${s.id}`"
            type="button"
            class="btn btn-ghost !px-3 !py-1.5 !text-sm text-red-300"
            @click="onDelete(s)"
          >
            삭제
          </button>
        </div>
      </li>
    </ul>
    <p v-else data-testid="admin-stores-empty" class="text-sm text-[--color-content-muted]">
      등록된 매장이 없습니다.
    </p>

    <p
      v-if="message"
      data-testid="admin-stores-message"
      class="mt-4 text-sm text-[--color-brand-accent]"
    >
      {{ message }}
    </p>
    <p
      v-if="error"
      data-testid="admin-stores-error"
      class="mt-4 text-sm text-red-300"
    >
      {{ error }}
    </p>
  </section>
</template>
