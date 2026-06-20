<script setup lang="ts">
// BO 매장매니저관리자 휴가/반차 승인(M8) — 1단계 승인 종결 (require v1.7 §8.2·§8.3). STORE_ADMIN 전용.
import { onMounted, ref } from 'vue'
import type { DayoffType } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['STORE_ADMIN'] })

// BE DayoffApprovalResponse와 일치
interface DayoffApproval {
  id: number
  managerId: string
  date: string
  type: DayoffType
  status: string
}

const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '상신',
  APPROVED: '승인(확정)',
  REJECTED: '반려',
}

const rows = ref<DayoffApproval[]>([])
const message = ref('')

function base() {
  return useRuntimeConfig().public.apiBase
}

// 휴가/반차 결재함 전체 로드(검토 목록)
async function load() {
  const { $apiFetch } = useNuxtApp()
  try {
    rows.value = await $apiFetch<DayoffApproval[]>(`${base()}/store-admin/dayoffs`)
  } catch {
    rows.value = []
  }
}

// 1단계 승인(M8, SUBMITTED→APPROVED 종결) / 반려
async function patch(url: string) {
  const { $apiFetch } = useNuxtApp()
  try {
    await $apiFetch(url, { method: 'PATCH' })
    message.value = '처리되었습니다.'
    await load()
  } catch {
    // 불가 전이(409) 등 — 목록 갱신으로 현재 상태 반영
    message.value = '처리에 실패했습니다.'
    await load()
  }
}

onMounted(load)
</script>

<template>
  <section data-testid="page-store-admin-dayoff-approvals" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 매장매니저관리자</span>
      <h1 class="text-3xl font-bold">휴가/반차 승인</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        일반매장매니저의 휴가/반차 신청을 1단계로 승인(확정)·반려합니다. 관리자 개입 없이 여기서 종결됩니다.
      </p>
    </header>

    <ul v-if="rows.length" class="space-y-3">
      <li
        v-for="r in rows"
        :key="r.id"
        :data-testid="`dayoff-approval-row-${r.id}`"
        class="card flex items-center justify-between gap-3 p-4"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">{{ r.managerId }} · {{ r.date }}</p>
          <p class="text-sm text-[--color-content-muted]">{{ r.type }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span :data-testid="`dayoff-approval-status-${r.id}`" class="text-xs font-medium">
            {{ STATUS_LABEL[r.status] ?? r.status }}
          </span>
          <button
            v-if="r.status === 'SUBMITTED'"
            :data-testid="`dayoff-approve-${r.id}`"
            type="button"
            class="btn btn-primary"
            @click="patch(`${base()}/store-admin/dayoffs/${r.id}/approve`)"
          >
            승인(확정)
          </button>
          <button
            v-if="r.status === 'SUBMITTED'"
            :data-testid="`dayoff-reject-${r.id}`"
            type="button"
            class="btn btn-ghost"
            @click="patch(`${base()}/store-admin/dayoffs/${r.id}/reject`)"
          >
            반려
          </button>
        </div>
      </li>
    </ul>
    <p v-else class="text-sm text-[--color-content-muted]">휴가/반차 신청이 없습니다.</p>

    <p v-if="message" data-testid="dayoff-approval-message" class="mt-4 text-sm text-[--color-brand-accent]">
      {{ message }}
    </p>
  </section>
</template>
