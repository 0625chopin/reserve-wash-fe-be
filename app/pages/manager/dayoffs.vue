<script setup lang="ts">
// BO 일반매장매니저 휴가/반차 신청(M6) — require v1.7 §8.2.
//   승인(M8)은 매장매니저관리자 전용 /store-admin/dayoff-approvals 에서 처리(역할별 페이지 분리 §12.4).
//   ※ 매장 휴일 신청은 관리자 '휴일 결재' 메뉴 제거(v1.11)에 따라 승인 경로가 끊겨 본 화면에서 제외(v1.15).
import { computed, onMounted, ref, watch } from 'vue'
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
// 휴가/반차 1단계 결재 상태(require v1.7 §8.3)
const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '상신',
  APPROVED: '승인(확정)',
  REJECTED: '반려',
}

// 매니저 계열은 본인 소속 매장으로 고정(변경 불가) — require v1.7 §12.4
const storeId = ref(auth.currentUser?.storeId ?? '')
const myStore = computed(() => stores.find((s) => s.id === storeId.value) ?? null)
const managerId = ref('')
const dayoffDate = ref('')
const dayoffType = ref<DayoffType | ''>('')
const rows = ref<DayoffApproval[]>([])
const message = ref('')

const managerOptions = computed(() => (storeId.value ? getManagersByStore(storeId.value) : []))

// 날짜 선택을 일반예약(/reserve/slot)과 동일한 휠로 통일 (require v1.14) — 오늘부터 21일 날짜 휠.
const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
function buildDateItems() {
  const result: { label: string; value: string }[] = []
  const base = new Date()
  base.setHours(0, 0, 0, 0)
  for (let i = 0; i < 21; i++) {
    const d = new Date(base)
    d.setDate(base.getDate() + i)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    result.push({
      label: `${d.getMonth() + 1}/${d.getDate()} (${WEEKDAYS[d.getDay()]})`,
      value: `${y}-${m}-${day}`,
    })
  }
  return result
}
const dateItems = buildDateItems()

function base() {
  return useRuntimeConfig().public.apiBase
}

// 선택 매장의 휴가/반차 신청 현황 로드
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

// 매장이 본인 소속으로 고정되어 watch 초기 트리거가 없으므로 진입 시 1회 로드
onMounted(loadDayoffs)

async function submitDayoff() {
  // 미선택 시 조용히 무시되지 않도록 안내(휠 날짜 미선택 등)
  if (!managerId.value || !dayoffType.value || !dayoffDate.value) {
    message.value = '매니저 · 휴무 유형 · 날짜를 모두 선택하세요.'
    return
  }
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

// 반려된 신청 재신청(신청자) — REJECTED → SUBMITTED
async function resubmit(id: number) {
  const { $apiFetch } = useNuxtApp()
  try {
    await $apiFetch(`${base()}/manager/dayoffs/${id}/resubmit`, { method: 'PATCH' })
    await loadDayoffs()
  } catch {
    message.value = '처리에 실패했습니다.'
  }
}
</script>

<template>
  <section data-testid="page-manager-dayoffs" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 신청</span>
      <h1 class="text-3xl font-bold">휴가/반차 신청</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        휴가/반차는 매장매니저관리자 승인(1단계)으로 확정됩니다.
      </p>
    </header>

    <div class="card space-y-5 p-6 sm:p-8">
      <div>
        <label class="field-label" for="dayoff-store">매장 <span class="text-xs text-[--color-content-muted]">(본인 소속 고정)</span></label>
        <select id="dayoff-store" v-model="storeId" data-testid="dayoff-store" class="bo-input" disabled>
          <option v-if="myStore" :value="myStore.id">{{ myStore.name }}</option>
          <option v-else value="">소속 매장 없음</option>
        </select>
      </div>

      <!-- 매니저 휴무 신청 -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
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
      </div>
      <!-- 날짜 — 일반예약(/reserve/slot)과 동일한 휠 선택기 (require v1.14) -->
      <div>
        <span class="field-label">날짜</span>
        <ClientOnly>
          <WheelPicker v-model="dayoffDate" testid="dayoff-date-wheel" :items="dateItems" />
          <template #fallback>
            <div
              class="h-[200px] animate-pulse rounded-xl border border-[--color-line-soft] bg-[--color-surface-1]"
            />
          </template>
        </ClientOnly>
      </div>
      <button
        data-testid="dayoff-submit"
        type="button"
        class="btn btn-primary w-full"
        @click="submitDayoff"
      >
        휴무 신청 상신
      </button>

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
