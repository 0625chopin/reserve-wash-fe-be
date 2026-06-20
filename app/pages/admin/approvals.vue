<script setup lang="ts">
// BO 관리자 결재함 — 휴무 2차 승인/반려 + 휴일 단일 승인/반려 (require 8.2·8.3). ADMIN 전용.
import { onMounted, ref } from 'vue'
import type { DayoffType } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

interface DayoffApproval {
  id: number
  managerId: string
  date: string
  type: DayoffType
  status: string
}
interface HolidayApproval {
  id: number
  storeId: string
  date: string
  status: string
}

const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '상신',
  APPROVED_L1: '1차 승인',
  CONFIRMED: '확정',
  REJECTED: '반려',
}

const dayoffs = ref<DayoffApproval[]>([])
const holidays = ref<HolidayApproval[]>([])

function base() {
  return useRuntimeConfig().public.apiBase
}

async function load() {
  const { $apiFetch } = useNuxtApp()
  try {
    dayoffs.value = await $apiFetch<DayoffApproval[]>(`${base()}/admin/dayoffs`)
  } catch {
    dayoffs.value = []
  }
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
      <h1 class="text-3xl font-bold">결재함</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        휴무 2차 승인(확정)·반려, 매장 휴일 승인·반려를 처리합니다.
      </p>
    </header>

    <!-- 휴무 결재 -->
    <h2 class="mb-3 text-lg font-bold">매니저 휴무</h2>
    <ul v-if="dayoffs.length" class="space-y-3">
      <li
        v-for="d in dayoffs"
        :key="d.id"
        :data-testid="`approval-dayoff-row-${d.id}`"
        class="card flex items-center justify-between gap-3 p-4"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">
            {{ d.managerId }} · {{ d.date }}
          </p>
          <p class="text-sm text-[--color-content-muted]">{{ d.type }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span :data-testid="`approval-dayoff-status-${d.id}`" class="text-xs font-medium">
            {{ STATUS_LABEL[d.status] ?? d.status }}
          </span>
          <button
            v-if="d.status === 'APPROVED_L1'"
            :data-testid="`approve-l2-${d.id}`"
            type="button"
            class="btn btn-primary"
            @click="patch(`${base()}/admin/dayoffs/${d.id}/approve-l2`)"
          >
            2차 승인(확정)
          </button>
          <button
            v-if="d.status === 'SUBMITTED' || d.status === 'APPROVED_L1'"
            :data-testid="`reject-dayoff-${d.id}`"
            type="button"
            class="btn btn-ghost"
            @click="patch(`${base()}/admin/dayoffs/${d.id}/reject`)"
          >
            반려
          </button>
        </div>
      </li>
    </ul>
    <p v-else class="text-sm text-[--color-content-muted]">휴무 신청이 없습니다.</p>

    <!-- 휴일 결재 -->
    <h2 class="mb-3 mt-8 text-lg font-bold">매장 휴일</h2>
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
