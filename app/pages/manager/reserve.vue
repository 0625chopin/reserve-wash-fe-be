<script setup lang="ts">
// BO 매니저 대행 예약 (M3, require 6.2) — MANAGER/STORE_ADMIN 전용.
// 카탈로그(매장/매니저/차종/서비스/베이)·가격을 재사용하고, 고객 이메일로 대행 예약을 생성한다.
import { computed, ref, watch } from 'vue'
import {
  getApprovedStores,
  getBaysForCar,
  getCarTypes,
  getManager,
  getManagersByStore,
  getServiceTypes,
  isManagerFullDayOff,
  isManagerOffAt,
} from '~/services/storeService'
import { getPrice } from '~/services/priceService'
import type { CarType, ServiceType } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['MANAGER', 'STORE_ADMIN'] })

const auth = useAuthStore()
const stores = getApprovedStores()
const carTypeOptions = getCarTypes()
const serviceTypeOptions = getServiceTypes()

// 매니저 계열은 본인 소속 매장으로 고정(변경 불가) — require v1.7 §12.4
const storeId = ref(auth.currentUser?.storeId ?? '')
const myStore = computed(() => stores.find((s) => s.id === storeId.value) ?? null)

// 대행 입력 상태
const customerEmail = ref('')
const managerId = ref('')
const carType = ref<CarType | ''>('')
const serviceType = ref<ServiceType | ''>('')
const date = ref('')
const time = ref('')
const bayId = ref('')

// 선행 선택 변경 시 후속 초기화 (cascade)
watch(storeId, () => {
  managerId.value = ''
  bayId.value = ''
})
watch(carType, () => {
  bayId.value = ''
})

const managerOptions = computed(() =>
  storeId.value ? getManagersByStore(storeId.value) : [],
)
const bayOptions = computed(() =>
  storeId.value && carType.value ? getBaysForCar(storeId.value, carType.value) : [],
)
const amount = computed(() =>
  carType.value && serviceType.value ? getPrice(carType.value, serviceType.value) : 0,
)

// 대행자(매니저) 휴무 컨텍스트 — 날짜·시간 휠 비활성 처리에 사용 (require 5.5/6.1)
const selectedManager = computed(() => (managerId.value ? getManager(managerId.value) : undefined))

// 일반예약(reserve/slot)과 동일한 날짜·시간 휠 — 오늘부터 21일, 30분 단위 48슬롯
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
const baseDates = buildDateItems()

// 전일(FULL_DAY) 휴무 날짜는 날짜 휠에서 비활성, 교대조 휴무는 시간 휠에서 처리 (require 5.5/6.1)
const dateItems = computed(() =>
  baseDates.map((d) => ({
    ...d,
    disabled: selectedManager.value ? isManagerFullDayOff(selectedManager.value, d.value) : false,
  })),
)
const timeItems = computed(() =>
  generateTimeSlots().map((t) => ({
    label: t,
    value: t,
    disabled:
      selectedManager.value && date.value
        ? isManagerOffAt(selectedManager.value, date.value, t)
        : false,
  })),
)

const canSubmit = computed(
  () =>
    !!customerEmail.value &&
    !!storeId.value &&
    !!managerId.value &&
    !!carType.value &&
    !!serviceType.value &&
    !!date.value &&
    !!time.value &&
    !!bayId.value,
)

// 제출 결과 — 인라인 메시지(성공/실패)
const result = ref<{ ok: boolean; message: string } | null>(null)
const submitting = ref(false)

async function onSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  result.value = null
  const { $apiFetch } = useNuxtApp()
  const base = useRuntimeConfig().public.apiBase
  try {
    await $apiFetch(`${base}/manager/reservations`, {
      method: 'POST',
      body: {
        customerEmail: customerEmail.value,
        managerId: managerId.value,
        storeId: storeId.value,
        bayId: bayId.value,
        date: date.value,
        timeSlot: time.value,
        carType: carType.value,
        serviceType: serviceType.value,
        amount: amount.value,
      },
    })
    result.value = { ok: true, message: '대행 예약이 완료되었습니다.' }
  } catch {
    // 휴무/타매장/충돌/고객 미존재 등 — 서버 메시지 대신 재시도 유도
    result.value = { ok: false, message: '대행 예약에 실패했습니다. 입력값·휴무·중복을 확인해 주세요.' }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section data-testid="page-manager-reserve" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 매니저</span>
      <h1 class="text-3xl font-bold">예약 대행</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        소속 매장 기준으로 고객 예약을 대행합니다. 본인 휴무 시간대는 예약할 수 없어요.
      </p>
    </header>

    <div class="card space-y-5 p-6 sm:p-8">
      <div>
        <label class="field-label" for="proxy-customer-email">고객 이메일</label>
        <input
          id="proxy-customer-email"
          v-model="customerEmail"
          data-testid="proxy-customer-email"
          type="text"
          placeholder="user@test.com"
          class="bo-input"
        />
      </div>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <label class="field-label" for="proxy-store">매장 <span class="text-xs text-[--color-content-muted]">(본인 소속 고정)</span></label>
          <select
            id="proxy-store"
            v-model="storeId"
            data-testid="proxy-store"
            class="bo-input"
            disabled
          >
            <option v-if="myStore" :value="myStore.id">{{ myStore.name }}</option>
            <option v-else value="">소속 매장 없음</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="proxy-manager">매니저(대행자)</label>
          <select
            id="proxy-manager"
            v-model="managerId"
            data-testid="proxy-manager"
            class="bo-input"
          >
            <option value="" disabled>선택</option>
            <option v-for="m in managerOptions" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="proxy-cartype">차종</label>
          <select
            id="proxy-cartype"
            v-model="carType"
            data-testid="proxy-cartype"
            class="bo-input"
          >
            <option value="" disabled>선택</option>
            <option v-for="c in carTypeOptions" :key="c.code" :value="c.code">{{ c.name }}</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="proxy-service">서비스</label>
          <select
            id="proxy-service"
            v-model="serviceType"
            data-testid="proxy-service"
            class="bo-input"
          >
            <option value="" disabled>선택</option>
            <option v-for="s in serviceTypeOptions" :key="s.code" :value="s.code">
              {{ s.name }}
            </option>
          </select>
        </div>
        <div>
          <label class="field-label" for="proxy-bay">베이</label>
          <select id="proxy-bay" v-model="bayId" data-testid="proxy-bay" class="bo-input">
            <option value="" disabled>선택</option>
            <option v-for="b in bayOptions" :key="b.id" :value="b.id">{{ b.code }}</option>
          </select>
        </div>
        <div>
          <span class="field-label">금액</span>
          <p data-testid="proxy-amount" class="pt-2 font-semibold text-[--color-content-strong]">
            {{ amount.toLocaleString('ko-KR') }}원
          </p>
        </div>
      </div>

      <!-- 날짜·시간 선택 — 일반예약(reserve/slot)과 동일한 휠 UI -->
      <div>
        <span class="field-label">날짜 · 시간 선택</span>
        <p
          v-if="selectedManager?.dayoffs.length"
          class="mb-2 text-xs text-[--color-content-muted]"
        >
          취소선 표시된 휴무(전일·교대조) 시간대는 선택할 수 없어요.
        </p>
        <ClientOnly>
          <div class="grid grid-cols-2 gap-3">
            <WheelPicker v-model="date" testid="proxy-date-wheel" :items="dateItems" />
            <WheelPicker v-model="time" testid="proxy-time-wheel" :items="timeItems" />
          </div>
          <template #fallback>
            <div
              class="h-[200px] animate-pulse rounded-xl border border-[--color-line-soft] bg-[--color-surface-1]"
            />
          </template>
        </ClientOnly>
      </div>

      <div class="border-t border-[--color-line-soft] pt-5">
        <button
          data-testid="proxy-submit"
          type="button"
          class="btn btn-primary w-full"
          :disabled="!canSubmit || submitting"
          @click="onSubmit"
        >
          대행 예약하기
        </button>
        <p
          v-if="result"
          data-testid="proxy-result"
          class="mt-3 text-center text-sm font-medium"
          :class="result.ok ? 'text-[--color-brand-accent]' : 'text-[--color-brand-primary]'"
        >
          {{ result.message }}
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* BO 입력 — 기존 토큰 재사용(하드코딩 hex 금지) */
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
