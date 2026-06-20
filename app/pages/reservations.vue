<script setup lang="ts">
// 예약 목록/취소/완료 (FW6/FW7) — 보호 라우트 (Phase 6)
import { computed } from 'vue'
import type { ReservationStatus } from '~/types/enums'
import {
  getApprovedStores,
  getBaysByStore,
  getCarTypes,
  getManager,
  getServiceTypes,
} from '~/services/storeService'

definePageMeta({ middleware: 'auth' })

const auth = useAuthStore()
const reservation = useReservationStore()

// 이름 매핑용 카탈로그 (services 경유)
const stores = getApprovedStores()
const carTypeOptions = getCarTypes()
const serviceTypeOptions = getServiceTypes()

// 내 예약 — 최신이 위로 (require 6.1)
const myReservations = computed(() =>
  reservation.reservations.filter((r) => r.userId === auth.currentUser?.id).slice().reverse(),
)

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
  <section data-testid="page-reservations" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <h1 class="text-3xl font-bold">예약 목록</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        진행 중이거나 완료된 예약을 확인하세요.
      </p>
    </header>

    <!-- 예약 목록 -->
    <ul v-if="myReservations.length" class="space-y-4">
      <li
        v-for="r in myReservations"
        :key="r.id"
        :data-testid="`reservation-${r.id}`"
        class="card p-5 sm:p-6"
      >
        <!-- 상단: 매장 + 상태 뱃지 -->
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-lg font-bold text-[--color-content-strong]">{{ storeName(r.storeId) }}</p>
            <p class="mt-0.5 text-sm text-[--color-content-muted]">{{ r.date }} {{ r.timeSlot }}</p>
          </div>
          <span :data-testid="`status-${r.id}`" class="status-badge" :data-status="r.status">
            {{ STATUS_LABEL[r.status] }}
          </span>
        </div>

        <!-- 본문: 요약 -->
        <dl class="mt-4 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
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

        <!-- 하단: 상태별 액션 -->
        <div class="mt-5 flex items-center gap-2 border-t border-[--color-line-soft] pt-4">
          <template v-if="r.status === 'RESERVED'">
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
          <NuxtLink
            v-else-if="r.status === 'COMPLETED'"
            :data-testid="`review-${r.id}`"
            :to="`/review/${r.id}`"
            class="btn btn-primary"
          >
            후기 작성
          </NuxtLink>
          <span v-else class="text-sm text-[--color-content-muted]">취소된 예약입니다.</span>
        </div>
      </li>
    </ul>

    <!-- 의도된 빈 상태 -->
    <div v-else class="card flex flex-col items-center gap-4 px-6 py-14 text-center">
      <div
        class="flex h-14 w-14 items-center justify-center rounded-2xl border border-[--color-line] bg-[--color-surface-2]"
      >
        <svg
          class="h-7 w-7 text-[--color-content-muted]"
          viewBox="0 0 24 24"
          fill="none"
          aria-hidden="true"
        >
          <rect x="4" y="5" width="16" height="15" rx="2" stroke="currentColor" stroke-width="1.6" />
          <path
            d="M8 3v4M16 3v4M4 10h16"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
          />
        </svg>
      </div>
      <div>
        <p class="font-semibold text-[--color-content-strong]">아직 예약이 없어요</p>
        <p class="mt-1 text-sm text-[--color-content-muted]">
          첫 세차를 예약하고 차를 빛나게 만들어 보세요.
        </p>
      </div>
      <NuxtLink to="/reserve" class="btn btn-primary">예약하러 가기</NuxtLink>
    </div>
  </section>
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
