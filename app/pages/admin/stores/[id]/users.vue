<script setup lang="ts">
// BO 관리자 — 매장별 사용자 관리 (S5, require 11.1). ADMIN 전용.
// 해당 매장에 예약 이력이 있는 고객을 중복 없이 보여준다.
import { onMounted, ref } from 'vue'
import type { UserRole } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

// BE AdminUserResponse와 일치(무변환)
interface AdminUser {
  id: string
  email: string
  name: string
  role: UserRole
}

const route = useRoute()
const storeId = route.params.id as string
const rows = ref<AdminUser[]>([])
const loaded = ref(false)

onMounted(async () => {
  const { $apiFetch } = useNuxtApp()
  const base = useRuntimeConfig().public.apiBase
  try {
    rows.value = await $apiFetch<AdminUser[]>(`${base}/admin/stores/${storeId}/users`)
  } catch {
    rows.value = []
  } finally {
    loaded.value = true
  }
})
</script>

<template>
  <section data-testid="page-admin-users" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매장 사용자 관리</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매장 {{ storeId }}에 예약 이력이 있는 고객입니다.
      </p>
    </header>

    <ul v-if="rows.length" class="space-y-3">
      <li
        v-for="u in rows"
        :key="u.id"
        :data-testid="`admin-user-row-${u.id}`"
        class="admin-user-row card flex items-center justify-between gap-4 p-4 sm:p-5"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">{{ u.name }}</p>
          <p class="mt-0.5 text-sm text-[--color-content-muted]">{{ u.email }}</p>
        </div>
        <span class="text-xs text-[--color-content-muted]">{{ u.role }}</span>
      </li>
    </ul>

    <div
      v-else-if="loaded"
      data-testid="admin-users-empty"
      class="card px-6 py-12 text-center text-sm text-[--color-content-muted]"
    >
      이 매장에는 아직 예약 고객이 없습니다.
    </div>
  </section>
</template>
