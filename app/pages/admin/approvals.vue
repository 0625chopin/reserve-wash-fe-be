<script setup lang="ts">
// BO 관리자 결재함 — 매장 휴일 단일 승인/반려 (require v1.7 §8.1). ADMIN 전용.
//   ⚠️ 휴가/반차(휴무)는 v1.7에서 1단계(매장매니저관리자 종결)로 정정되어 관리자 개입 없음
//      → /store-admin/dayoff-approvals 에서 처리(역할별 페이지 분리 §12.4).
//   매니저 가입 2차 최종 승인(S3)은 /admin/manager-approvals 에서 처리.
import { onMounted, ref } from 'vue'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

interface HolidayApproval {
  id: number
  storeId: string
  date: string
  status: string
}

const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '상신',
  CONFIRMED: '확정',
  REJECTED: '반려',
}

const holidays = ref<HolidayApproval[]>([])

function base() {
  return useRuntimeConfig().public.apiBase
}

async function load() {
  const { $apiFetch } = useNuxtApp()
  try {
    holidays.value = await $apiFetch<HolidayApproval[]>(`${base()}/admin/holidays`)
  } catch {
    holidays.value = []
  }
}

async function patch(url: string) {
  const { $apiFetch } = useNuxtApp()
  try {
    await $apiFetch(url, { method: 'PATCH' })
    await load()
  } catch {
    // 불가 전이(409) 등 — 목록 갱신으로 현재 상태 반영
    await load()
  }
}

onMounted(load)
</script>

<template>
  <section data-testid="page-admin-approvals" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자 결재함</span>
      <h1 class="text-3xl font-bold">매장 휴일 결재함</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">매장 휴일 신청을 승인·반려합니다.</p>
    </header>

    <!-- 휴일 결재 -->
    <h2 class="mb-3 text-lg font-bold">매장 휴일</h2>
    <ul v-if="holidays.length" class="space-y-3">
      <li
        v-for="h in holidays"
        :key="h.id"
        :data-testid="`approval-holiday-row-${h.id}`"
        class="card flex items-center justify-between gap-3 p-4"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">{{ h.storeId }} · {{ h.date }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span :data-testid="`approval-holiday-status-${h.id}`" class="text-xs font-medium">
            {{ STATUS_LABEL[h.status] ?? h.status }}
          </span>
          <button
            v-if="h.status === 'SUBMITTED'"
            :data-testid="`approve-holiday-${h.id}`"
            type="button"
            class="btn btn-primary"
            @click="patch(`${base()}/admin/holidays/${h.id}/approve`)"
          >
            승인(확정)
          </button>
          <button
            v-if="h.status === 'SUBMITTED'"
            :data-testid="`reject-holiday-${h.id}`"
            type="button"
            class="btn btn-ghost"
            @click="patch(`${base()}/admin/holidays/${h.id}/reject`)"
          >
            반려
          </button>
        </div>
      </li>
    </ul>
    <p v-else class="text-sm text-[--color-content-muted]">휴일 신청이 없습니다.</p>
  </section>
</template>
