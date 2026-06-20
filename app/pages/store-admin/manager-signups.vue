<script setup lang="ts">
// BO 매장매니저관리자 매니저 가입 1차 승인(M7) — require v1.7 §4.4. STORE_ADMIN 전용.
//   PENDING_APPROVAL_L1 목록을 승인(→L2)·반려한다. 2차 최종 승인(S3)은 관리자가 처리.
import { onMounted, ref } from 'vue'
import type { UserRole } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['STORE_ADMIN'] })

// BE ManagerSignupResponse와 일치
interface ManagerSignup {
  id: string
  email: string
  name: string
  role: UserRole
  approvalStatus: string
}

const rows = ref<ManagerSignup[]>([])
const message = ref('')

function base() {
  return useRuntimeConfig().public.apiBase
}

// 1차 승인 대기 목록(PENDING_APPROVAL_L1)
async function load() {
  const { $apiFetch } = useNuxtApp()
  try {
    rows.value = await $apiFetch<ManagerSignup[]>(`${base()}/store-admin/manager-signups`)
  } catch {
    rows.value = []
  }
}

// 1차 승인(L1→L2) / 반려
async function patch(url: string) {
  const { $apiFetch } = useNuxtApp()
  try {
    await $apiFetch(url, { method: 'PATCH' })
    message.value = '처리되었습니다.'
    await load()
  } catch {
    message.value = '처리에 실패했습니다.'
    await load()
  }
}

onMounted(load)
</script>

<template>
  <section data-testid="page-store-admin-manager-signups" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 매장매니저관리자</span>
      <h1 class="text-3xl font-bold">매니저 가입 1차 승인</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매니저 가입 신청을 1차 승인하면 관리자 2차 최종 승인 단계로 넘어갑니다(가입은 2단계 승인).
      </p>
    </header>

    <ul v-if="rows.length" class="space-y-3">
      <li
        v-for="r in rows"
        :key="r.id"
        :data-testid="`signup-row-${r.id}`"
        class="card flex items-center justify-between gap-3 p-4"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">{{ r.name }} · {{ r.email }}</p>
          <p class="text-sm text-[--color-content-muted]">{{ r.role }}</p>
        </div>
        <div class="flex items-center gap-2">
          <button
            :data-testid="`signup-approve-${r.id}`"
            type="button"
            class="btn btn-primary"
            @click="patch(`${base()}/store-admin/manager-signups/${r.id}/approve`)"
          >
            1차 승인
          </button>
          <button
            :data-testid="`signup-reject-${r.id}`"
            type="button"
            class="btn btn-ghost"
            @click="patch(`${base()}/store-admin/manager-signups/${r.id}/reject`)"
          >
            반려
          </button>
        </div>
      </li>
    </ul>
    <p v-else data-testid="signup-empty" class="text-sm text-[--color-content-muted]">
      1차 승인 대기 중인 가입 신청이 없습니다.
    </p>

    <p v-if="message" data-testid="signup-message" class="mt-4 text-sm text-[--color-brand-accent]">
      {{ message }}
    </p>
  </section>
</template>
