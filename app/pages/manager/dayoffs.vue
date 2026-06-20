<script setup lang="ts">
// BO 매니저 휴무/휴일 결재 신청 + 1차 승인(STORE_ADMIN) — require 8장.
import { computed, ref, watch } from 'vue'
import { getApprovedStores, getManagersByStore } from '~/services/storeService'
import type { DayoffType } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['MANAGER', 'STORE_ADMIN'] })

const auth = useAuthStore()
const stores = getApprovedStores()

// BE DayoffApprovalResponse와 일치
interface DayoffApproval {
  id: number
  managerId: string
  date: string
  type: DayoffType
  status: string
}

const DAYOFF_TYPES: { code: DayoffType; label: string }[] = [
  { code: 'FULL_DAY', label: '전일' },
  { code: 'SHIFT_1', label: '오전조(06–14시)' },
  { code: 'SHIFT_2', label: '오후조(14–22시)' },
  { code: 'SHIFT_3', label: '야간조(22–06시)' },
]
const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '상신',
  APPROVED_L1: '1차 승인',
  CONFIRMED: '확정',
  REJECTED: '반려',
}

const storeId = ref('')
const managerId = ref('')
const dayoffDate = ref('')
const dayoffType = ref<DayoffType | ''>('')
const holidayDate = ref('')
const rows = ref<DayoffApproval[]>([])
const message = ref('')

const managerOptions = computed(() => (storeId.value ? getManagersByStore(storeId.value) : []))
const isStoreAdmin = computed(() => auth.currentUser?.role === 'STORE_ADMIN')

function base() {
  return useRuntimeConfig().public.apiBase
}

// 선택 매장의 휴무 신청 목록 로드(L1 검토용)
async function loadDayoffs() {
  if (!storeId.value) {
    rows.value = []
    return
  }
  const { $apiFetch } = useNuxtApp()
  try {
    rows.value = await $apiFetch<DayoffApproval[]>(`${base()}/manager/dayoffs`, {
      query: { storeId: storeId.value },
    })
  } catch {
    rows.value = []
  }
}

watch(storeId, () => {
  managerId.value = ''
  loadDayoffs()
})

async function submitDayoff() {
  if (!managerId.value || !dayoffDate.value || !dayoffType.value) return
  const { $apiFetch } = useNuxtApp()
  try {
    await $apiFetch(`${base()}/manager/dayoffs`, {
      method: 'POST',
      body: { managerId: managerId.value, date: dayoffDate.value, type: dayoffType.value },
    })
    message.value = '휴무 신청이 상신되었습니다.'
    dayoffDate.value = ''
    dayoffType.value = ''
    await loadDayoffs()
  } catch {
    message.value = '휴무 신청에 실패했습니다.'
  }
}

async function submitHoliday() {
  if (!storeId.value || !holidayDate.value) return
  const { $apiFetch } = useNuxtApp()
  try {
    await $apiFetch(`${base()}/manager/holidays`, {
      method: 'POST',
      body: { storeId: storeId.value, date: holidayDate.value },
    })
    message.value = '매장 휴일이 상신되었습니다.'
    holidayDate.value = ''
  } catch {
    message.value = '매장 휴일 신청에 실패했습니다.'
  }
}

// 1차 승인(STORE_ADMIN) / 재신청
async function approveL1(id: number) {
  await patch(`${base()}/manager/dayoffs/${id}/approve-l1`)
}
async function resubmit(id: number) {
  await patch(`${base()}/manager/dayoffs/${id}/resubmit`)
}
async function patch(url: string) {
  const { $apiFetch } = useNuxtApp()
  try {
    await $apiFetch(url, { method: 'PATCH' })
    await loadDayoffs()
  } catch {
    message.value = '처리에 실패했습니다.'
  }
}
</script>

<template>
  <section data-testid="page-manager-dayoffs" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 결재</span>
      <h1 class="text-3xl font-bold">휴무 · 휴일 결재 신청</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매니저 휴무는 최고매니저(1차)→관리자(2차) 승인을 거쳐 확정됩니다.
      </p>
    </header>

    <div class="card space-y-5 p-6 sm:p-8">
      <div>
        <label class="field-label" for="dayoff-store">매장</label>
        <select id="dayoff-store" v-model="storeId" data-testid="dayoff-store" class="bo-input">
          <option value="" disabled>선택</option>
          <option v-for="s in stores" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
      </div>

      <!-- 매니저 휴무 신청 -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div>
          <label class="field-label" for="dayoff-manager">매니저</label>
          <select
            id="dayoff-manager"
            v-model="managerId"
            data-testid="dayoff-manager"
            class="bo-input"
          >
            <option value="" disabled>선택</option>
            <option v-for="m in managerOptions" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="dayoff-type">휴무 유형</label>
          <select id="dayoff-type" v-model="dayoffType" data-testid="dayoff-type" class="bo-input">
            <option value="" disabled>선택</option>
            <option v-for="t in DAYOFF_TYPES" :key="t.code" :value="t.code">{{ t.label }}</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="dayoff-date">날짜</label>
          <input
            id="dayoff-date"
            v-model="dayoffDate"
            data-testid="dayoff-date"
            type="text"
            placeholder="YYYY-MM-DD"
            class="bo-input"
          />
        </div>
      </div>
      <button
        data-testid="dayoff-submit"
        type="button"
        class="btn btn-primary w-full"
        @click="submitDayoff"
      >
        휴무 신청 상신
      </button>

      <!-- 매장 휴일 신청 -->
      <div class="flex items-end gap-3 border-t border-[--color-line-soft] pt-5">
        <div class="flex-1">
          <label class="field-label" for="holiday-date">매장 휴일 날짜</label>
          <input
            id="holiday-date"
            v-model="holidayDate"
            data-testid="holiday-date"
            type="text"
            placeholder="YYYY-MM-DD"
            class="bo-input"
          />
        </div>
        <button data-testid="holiday-submit" type="button" class="btn btn-ghost" @click="submitHoliday">
          휴일 상신
        </button>
      </div>

      <p v-if="message" data-testid="dayoff-message" class="text-sm text-[--color-brand-accent]">
        {{ message }}
      </p>
    </div>

    <!-- 신청 목록 -->
    <ul v-if="rows.length" class="mt-6 space-y-3">
      <li
        v-for="r in rows"
        :key="r.id"
        :data-testid="`dayoff-row-${r.id}`"
        class="card flex items-center justify-between gap-3 p-4"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">{{ r.managerId }} · {{ r.date }}</p>
          <p class="text-sm text-[--color-content-muted]">{{ r.type }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span :data-testid="`dayoff-status-${r.id}`" class="text-xs font-medium">
            {{ STATUS_LABEL[r.status] ?? r.status }}
          </span>
          <button
            v-if="isStoreAdmin && r.status === 'SUBMITTED'"
            :data-testid="`dayoff-approve-l1-${r.id}`"
            type="button"
            class="btn btn-ghost"
            @click="approveL1(r.id)"
          >
            1차 승인
          </button>
          <button
            v-if="r.status === 'REJECTED'"
            :data-testid="`dayoff-resubmit-${r.id}`"
            type="button"
            class="btn btn-ghost"
            @click="resubmit(r.id)"
          >
            재신청
          </button>
        </div>
      </li>
    </ul>
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
