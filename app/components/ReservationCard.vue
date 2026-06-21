<script setup lang="ts">
// 예약 카드 — 본인 예약(액션 가능)/담당·매장 예약(읽기 전용) 공용 (require 6.1, v1.10 §6.6)
import { computed } from 'vue'
import type { Reservation } from '~/types/domain'
import type { ReservationStatus } from '~/types/enums'
import {
  getApprovedStores,
  getBaysByStore,
  getCarTypes,
  getManager,
  getServiceTypes,
} from '~/services/storeService'

const props = withDefaults(
  defineProps<{
    reservation: Reservation
    // 읽기 전용 — 세차완료/취소/후기 액션을 숨긴다(담당·매장 전체 목록은 표시 목적)
    readonly?: boolean
    // 고객 표시 — 담당/매장 뷰에서 예약자(userId)를 함께 노출
    showCustomer?: boolean
  }>(),
  { readonly: false, showCustomer: false },
)

const reservation = useReservationStore()
const review = useReviewStore()

const r = computed(() => props.reservation)

// 이름 매핑용 카탈로그 (services 경유)
const stores = getApprovedStores()
const carTypeOptions = getCarTypes()
const serviceTypeOptions = getServiceTypes()

function storeName(id: string) {
  return stores.find((s) => s.id === id)?.name ?? id
}
function managerName(id: string | null) {
  return id ? (getManager(id)?.name ?? id) : '-'
}
function carTypeName(code: string) {
  return carTypeOptions.find((c) => c.code === code)?.name ?? code
}
function serviceName(code: string) {
  return serviceTypeOptions.find((s) => s.code === code)?.name ?? code
}
function bayCode(storeId: string, bayId: string) {
  return getBaysByStore(storeId).find((b) => b.id === bayId)?.code ?? bayId
}
function priceLabel(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}

// 상태 라벨 (require 11.3)
const STATUS_LABEL: Record<ReservationStatus, string> = {
  HOLDING: '점유 중',
  RESERVED: '예약 확정',
  COMPLETED: '세차 완료',
  CANCELED: '취소됨',
}
</script>

<template>
  <li :data-testid="`reservation-${r.id}`" class="card p-5 sm:p-6">
    <!-- 상단: 매장 + 상태 뱃지 -->
    <div class="flex items-start justify-between gap-3">
      <div>
        <p class="text-lg font-bold text-[--color-content-strong]">
          {{ storeName(r.storeId) }}
        </p>
        <p class="mt-0.5 text-sm text-[--color-content-muted]">{{ r.date }} {{ r.timeSlot }}</p>
      </div>
      <span :data-testid="`status-${r.id}`" class="status-badge" :data-status="r.status">
        {{ STATUS_LABEL[r.status] }}
      </span>
    </div>

    <!-- 본문: 요약 -->
    <dl class="mt-4 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
      <template v-if="showCustomer">
        <dt class="text-[--color-content-muted]">고객</dt>
        <dd :data-testid="`customer-${r.id}`" class="text-[--color-content-strong]">
          {{ r.userId }}
        </dd>
      </template>
      <dt class="text-[--color-content-muted]">매니저</dt>
      <dd class="text-[--color-content-strong]">{{ managerName(r.managerId) }}</dd>
      <dt class="text-[--color-content-muted]">차종 · 베이</dt>
      <dd class="text-[--color-content-strong]">
        {{ carTypeName(r.carType) }} · {{ bayCode(r.storeId, r.bayId) }}
      </dd>
      <dt class="text-[--color-content-muted]">서비스</dt>
      <dd class="text-[--color-content-strong]">{{ serviceName(r.serviceType) }}</dd>
      <dt class="text-[--color-content-muted]">금액</dt>
      <dd class="text-[--color-content-strong]">{{ priceLabel(r.amount) }} · 현장결제</dd>
    </dl>

    <!-- 하단: 상태별 액션 (읽기 전용이면 액션 숨김, 상태만 안내) -->
    <div class="mt-5 flex items-center gap-2 border-t border-[--color-line-soft] pt-4">
      <template v-if="!readonly && r.status === 'RESERVED'">
        <button
          :data-testid="`complete-${r.id}`"
          type="button"
          class="btn btn-ghost"
          @click="reservation.completeReservation(r.id)"
        >
          세차완료
        </button>
        <button
          :data-testid="`cancel-${r.id}`"
          type="button"
          class="btn btn-ghost"
          @click="reservation.cancelReservation(r.id)"
        >
          예약 취소
        </button>
      </template>
      <template v-else-if="!readonly && r.status === 'COMPLETED'">
        <span
          :data-testid="`completed-${r.id}`"
          class="text-sm font-medium text-[--color-brand-accent]"
        >
          세차가 완료되었습니다.
        </span>
        <!-- 미작성이면 후기 작성 링크, 작성했으면 완료 표시 (require 9.1) -->
        <NuxtLink
          v-if="!review.hasReview(r.id)"
          :data-testid="`review-${r.id}`"
          :to="`/review/${r.id}`"
          class="btn btn-ghost ml-auto"
        >
          후기 작성
        </NuxtLink>
        <NuxtLink
          v-else
          :data-testid="`reviewed-${r.id}`"
          :to="`/review/${r.id}`"
          class="btn btn-ghost ml-auto"
        >
          ★ {{ review.reviewOf(r.id)?.rating ?? '-' }} · 후기 확인
        </NuxtLink>
      </template>
      <!-- 읽기 전용(담당/매장 뷰) 또는 취소/점유 상태 안내 -->
      <span v-else class="text-sm text-[--color-content-muted]">
        {{ STATUS_LABEL[r.status] }}
      </span>
    </div>
  </li>
</template>

<style scoped>
/* 상태 뱃지 — 상태별 색은 @theme 토큰만 사용 (하드코딩 hex 금지) */
.status-badge {
  flex: none;
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}
/* 예약 확정 — 브랜드(스카이) */
.status-badge[data-status='RESERVED'] {
  background-color: color-mix(in oklab, var(--color-brand-primary) 16%, transparent);
  color: var(--color-brand-primary);
  border: 1px solid color-mix(in oklab, var(--color-brand-primary) 35%, transparent);
}
/* 세차 완료 — 액센트(라임) */
.status-badge[data-status='COMPLETED'] {
  background-color: color-mix(in oklab, var(--color-brand-accent) 16%, transparent);
  color: var(--color-brand-accent);
  border: 1px solid color-mix(in oklab, var(--color-brand-accent) 35%, transparent);
}
/* 취소/점유 — muted */
.status-badge[data-status='CANCELED'],
.status-badge[data-status='HOLDING'] {
  background-color: var(--color-surface-2);
  color: var(--color-content-muted);
  border: 1px solid var(--color-line);
}
</style>
