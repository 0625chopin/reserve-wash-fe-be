<script setup lang="ts">
// 선택 시간대(날짜·시간) 기준 베이 점유 미니그리드 — 베이 N칸을 status별로 시각화 (require 5.2, 6.1, 7장)
// 휠이 시간 선택을 담당하므로 본 그리드는 '해당 시간대 베이 점유 현황 + 베이 선택' 역할(하이브리드)
import { computed } from 'vue'
import type { Bay } from '~/types/domain'
import type { SlotStatus } from '~/types/enums'

const props = defineProps<{
  storeId: string
  date: string
  timeSlot: string
  bays: Bay[]
  disabled?: boolean // 매니저 휴무 등으로 시간대 전체 비활성
}>()

const emit = defineEmits<{ (e: 'hold', bayId: string): void }>()

// 자동 임포트되는 Pinia 스토어 — 슬롯 status는 런타임 맵 + 더미 시드 합성
const reservation = useReservationStore()

// 선택 가능 여부 — 휴무일 전체 비활성 + RESERVED/COMPLETED 점유 베이 비활성 (require 6.1)
// HOLDING(본인 선택)은 다시 클릭 가능
function isSelectable(status: SlotStatus): boolean {
  return !props.disabled && status !== 'RESERVED' && status !== 'COMPLETED'
}

// 베이별 (현재 시간대) 상태·선택가능 여부 — slotStatus 변화에 반응
const cells = computed(() =>
  props.bays.map((bay) => {
    const status = reservation.getStatus({
      storeId: props.storeId,
      bayId: bay.id,
      date: props.date,
      timeSlot: props.timeSlot,
      status: 'AVAILABLE',
    })
    return { bay, status, selectable: isSelectable(status) }
  }),
)

const STATUS_LABEL: Record<SlotStatus, string> = {
  AVAILABLE: '예약 가능',
  HOLDING: '선택됨',
  RESERVED: '예약됨',
  COMPLETED: '완료',
}

// status → 시각 클래스 (휴무일이면 점유와 무관하게 off 표시)
function cellClass(status: SlotStatus): string {
  if (props.disabled) return 'slot-off'
  if (status === 'HOLDING') return 'slot-holding'
  if (status === 'RESERVED' || status === 'COMPLETED') return 'slot-reserved'
  return 'slot-available'
}

function onClick(bayId: string, selectable: boolean) {
  if (selectable) emit('hold', bayId)
}
</script>

<template>
  <div data-testid="slot-grid" class="grid grid-cols-2 gap-2 sm:grid-cols-3">
    <button
      v-for="cell in cells"
      :key="cell.bay.id"
      type="button"
      :data-testid="`slot-${storeId}-${cell.bay.id}-${timeSlot}`"
      :data-status="cell.status"
      :disabled="!cell.selectable"
      class="slot-cell"
      :class="cellClass(cell.status)"
      @click="onClick(cell.bay.id, cell.selectable)"
    >
      <span class="slot-code">{{ cell.bay.code }}</span>
      <span class="slot-status">{{ STATUS_LABEL[cell.status] }}</span>
    </button>
  </div>
</template>

<style scoped>
/* 베이 칸 — surface/보더 기본, 색은 @theme 토큰만 사용(하드코딩 hex 금지) */
.slot-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  border-radius: 0.625rem;
  border: 1px solid var(--color-line);
  background-color: var(--color-surface-1);
  padding: 0.625rem 0.5rem;
  color: var(--color-content);
  cursor: pointer;
  transition:
    border-color 0.2s var(--ease-out-soft),
    background-color 0.2s var(--ease-out-soft),
    transform 0.12s var(--ease-out-soft);
}
.slot-cell:disabled {
  cursor: not-allowed;
}
.slot-cell:not(:disabled):active {
  transform: translateY(1px);
}
.slot-code {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-content-strong);
}
.slot-status {
  font-size: 0.6875rem;
  color: var(--color-content-muted);
}

/* 예약 가능 — hover 시 브랜드 보더 강조 */
.slot-available:hover {
  border-color: var(--color-brand-primary);
  background-color: var(--color-surface-2);
}

/* 선택됨(HOLDING) — 브랜드 채움 */
.slot-holding {
  border-color: var(--color-brand-primary);
  background-color: color-mix(in oklab, var(--color-brand-primary) 22%, transparent);
}
.slot-holding .slot-status {
  color: var(--color-brand-primary);
}

/* 예약됨/완료(점유) — muted + 점선 보더, 선택 불가 */
.slot-reserved {
  opacity: 0.5;
  background-color: var(--color-surface-base);
  border-style: dashed;
}

/* 휴무 — 취소선 + 흐림, 선택 불가 */
.slot-off {
  opacity: 0.4;
  text-decoration: line-through;
}
</style>
