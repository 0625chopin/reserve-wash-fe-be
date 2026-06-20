<script setup lang="ts">
// 예약 위저드 2페이지 — 날짜·시간 선택 후 그 시간대 베이 버튼 그리드에서 베이 선택 → 예약하기
import { computed, watch } from 'vue'
import { isManagerFullDayOff, isManagerOffAt } from '~/services/storeService'
import { loadSlots } from '~/services/reservationService'

definePageMeta({ middleware: ['auth', 'reservation-wizard-guard'] })

const draft = useReservationDraftStore()
const { toast, show: showToast } = useToast()

// 매장·날짜가 정해지면 서버에서 해당 날짜 점유 슬롯을 로드해 그리드에 반영(2단계 하이드레이트)
watch(
  () => [draft.storeId, draft.date] as const,
  async ([sid, d]) => {
    if (sid && d) await loadSlots(sid, d)
  },
  { immediate: true },
)

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

// 오늘부터 21일치 날짜 항목 생성 (require 5.1 전일자 운영)
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

// 전일(FULL_DAY) 휴무 날짜만 날짜 단계에서 disabled (교대조 휴무는 시간 단계에서 처리, require 5.5/6.1)
const dateItems = computed(() => {
  const mgr = draft.selectedManager
  return baseDates.map((d) => ({
    ...d,
    disabled: mgr ? isManagerFullDayOff(mgr, d.value) : false,
  }))
})

// 30분 단위 시간 슬롯 48개 — 매니저 교대조 휴무 시간대는 disabled (require 5.5, 6.1)
const timeItems = computed(() => {
  const mgr = draft.selectedManager
  const date = draft.date
  return generateTimeSlots().map((t) => ({
    label: t,
    value: t,
    disabled: mgr && date ? isManagerOffAt(mgr, date, t) : false,
  }))
})

async function onReserve() {
  if (!draft.canConfirm) return
  // 확정은 서버(UNIQUE+낙관락)가 최종 판정 — 충돌 시 false → 재선택 토스트 (require 7장)
  if (await draft.confirm()) {
    navigateTo('/reserve/done')
  } else {
    showToast('선택하신 슬롯이 방금 예약되었습니다. 다른 슬롯을 선택해 주세요.')
  }
}
</script>

<template>
  <section data-testid="page-reserve-slot" class="mx-auto max-w-2xl">
    <!-- 페이지 헤더 -->
    <header class="mb-8">
      <span class="badge-accent mb-3">세차 예약 · 2/3</span>
      <h1 class="text-3xl font-bold">언제, 어느 베이에 맡길까요?</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        날짜·시간을 고르면 해당 시간대의 베이 현황이 표시돼요. 예약된 베이는 선택할 수 없어요.
      </p>
    </header>

    <div class="card space-y-8 p-6 sm:p-8 sm:py-10">
      <!-- 날짜·시간 휠 -->
      <div>
        <span class="field-label">날짜 · 시간 선택</span>
        <p
          v-if="draft.selectedManager?.dayoffs.length"
          class="mb-2 text-xs text-[--color-content-muted]"
        >
          취소선 표시된 휴무(전일·교대조) 시간대는 선택할 수 없어요.
        </p>
        <!-- 휠 선택기는 클라이언트 전용(스크롤·날짜 계산) -->
        <ClientOnly>
          <div class="grid grid-cols-2 gap-3">
            <WheelPicker v-model="draft.date" testid="date-wheel" :items="dateItems" />
            <WheelPicker v-model="draft.time" testid="time-wheel" :items="timeItems" />
          </div>
          <template #fallback>
            <div
              class="h-[200px] animate-pulse rounded-xl border border-[--color-line-soft] bg-[--color-surface-1]"
            />
          </template>
        </ClientOnly>
      </div>

      <!-- 선택 시간대 베이 점유 그리드 (날짜·시간 선택 후) — 베이 선택 유일 경로 -->
      <ClientOnly>
        <div v-if="draft.date && draft.time">
          <span class="field-label">{{ draft.date }} {{ draft.time }} 베이 선택</span>
          <p class="mb-2 text-xs text-[--color-content-muted]">
            예약된 베이는 비활성이에요. 원하는 베이를 눌러 선택하세요.
          </p>
          <SlotGrid
            :store-id="draft.storeId as string"
            :date="draft.date"
            :time-slot="draft.time"
            :bays="draft.storeBays"
            @hold="(bayId) => draft.holdBay(bayId)"
          />
          <!-- 선택한 베이가 이 시간대에 점유됐을 때 사전 재선택 유도 (require 6.1) -->
          <p
            v-if="draft.selectedBayOccupied"
            data-testid="bay-occupied-notice"
            class="mt-2 text-xs font-medium text-[--color-brand-accent]"
          >
            선택하신 베이는 이 시간대에 이미 예약되어 있어요. 위 그리드에서 다른 베이를 선택해
            주세요.
          </p>
        </div>
      </ClientOnly>

      <!-- 하단: 이전 / 예약하기 -->
      <div class="flex items-center gap-3 border-t border-[--color-line-soft] pt-6">
        <NuxtLink to="/reserve" data-testid="reserve-prev" class="btn btn-ghost"> 이전 </NuxtLink>
        <button
          data-testid="reserve-submit"
          type="button"
          class="btn btn-primary flex-1"
          :disabled="!draft.canConfirm"
          @click="onReserve"
        >
          예약하기
        </button>
      </div>
      <p v-if="!draft.canConfirm" class="text-center text-xs text-[--color-content-muted]">
        날짜 · 시간 · 베이를 모두 선택해 주세요.
      </p>
    </div>

    <!-- 충돌/안내 토스트 — 하단 중앙, useToast(클라이언트 타이머)로 자동 닫힘 -->
    <Teleport to="body">
      <Transition name="conflict">
        <div
          v-if="toast.visible"
          data-testid="toast"
          role="alert"
          aria-live="assertive"
          class="conflict-toast fixed inset-x-0 bottom-6 z-50 mx-auto w-fit max-w-[90%] rounded-xl px-4 py-3 text-sm"
        >
          {{ toast.message }}
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
/* 충돌/안내 토스트 — surface-2 + 라인 보더, 고대비 텍스트 (하드코딩 hex 금지) */
.conflict-toast {
  background-color: var(--color-surface-2);
  border: 1px solid var(--color-line);
  color: var(--color-content-strong);
  box-shadow: 0 12px 32px -12px rgb(0 0 0 / 0.6);
}

/* 등장/퇴장 — 아래에서 살짝 떠오르며 페이드 */
.conflict-enter-active,
.conflict-leave-active {
  transition:
    opacity 0.2s var(--ease-out-soft),
    transform 0.2s var(--ease-out-soft);
}
.conflict-enter-from,
.conflict-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
