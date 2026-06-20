<script setup lang="ts">
// BO 관리자 매니저 가입 2차 최종 승인(S3) — require v1.7 §4.4. ADMIN 전용.
//   PENDING_APPROVAL_L2 목록을 최종 확정(→ACTIVE, 로그인 가능)·반려한다. 1차 승인(M7)은 매장매니저관리자가 처리.
import { onMounted, ref } from 'vue'
import type { UserRole } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

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

// 2차 승인 대기 목록(PENDING_APPROVAL_L2)
async function load() {
  const { $apiFetch } = useNuxtApp()
  try {
    rows.value = await $apiFetch<ManagerSignup[]>(`${base()}/admin/manager-approvals`)
  } catch {
    rows.value = []
  }
}

// 2차 최종 확정(L2→ACTIVE) / 반려
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
  <section data-testid="page-admin-manager-approvals" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매니저 가입 2차 최종 승인</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매장매니저관리자가 1차 승인한 가입을 최종 확정하면 계정이 활성화되어 로그인할 수 있습니다.
      </p>
    </header>

    <ul v-if="rows.length" class="space-y-3">
      <li
        v-for="r in rows"
        :key="r.id"
        :data-testid="`manager-approval-row-${r.id}`"
        class="card flex items-center justify-between gap-3 p-4"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">{{ r.name }} · {{ r.email }}</p>
          <p class="text-sm text-[--color-content-muted]">{{ r.role }}</p>
        </div>
        <div class="flex items-center gap-2">
          <button
            :data-testid="`manager-confirm-${r.id}`"
            type="button"
            class="btn btn-primary"
            @click="patch(`${base()}/admin/manager-approvals/${r.id}/confirm`)"
          >
            최종 승인(활성화)
          </button>
          <button
            :data-testid="`manager-reject-${r.id}`"
            type="button"
            class="btn btn-ghost"
            @click="patch(`${base()}/admin/manager-approvals/${r.id}/reject`)"
          >
            반려
          </button>
        </div>
      </li>
    </ul>
    <p v-else data-testid="manager-approval-empty" class="text-sm text-[--color-content-muted]">
      2차 승인 대기 중인 가입 신청이 없습니다.
    </p>

    <p v-if="message" data-testid="manager-approval-message" class="mt-4 text-sm text-[--color-brand-accent]">
      {{ message }}
    </p>
  </section>
</template>
