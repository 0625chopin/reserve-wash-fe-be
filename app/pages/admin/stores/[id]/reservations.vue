<script setup lang="ts">
// BO 관리자 — 매장별 예약자 관리 (S4, require 11.1). ADMIN 전용.
import { onMounted, ref } from 'vue'
import type { CarType, ReservationStatus, ServiceType } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

// BE AdminReservationResponse와 일치(무변환)
interface AdminReservation {
  id: string
  userId: string
  userName: string
  userEmail: string
  storeId: string
  bayId: string
  managerId: string | null
  date: string
  timeSlot: string
  carType: CarType
  serviceType: ServiceType
  amount: number
  status: ReservationStatus
}

const route = useRoute()
const storeId = route.params.id as string
const rows = ref<AdminReservation[]>([])
const loaded = ref(false)

onMounted(async () => {
  const { $apiFetch } = useNuxtApp()
  const base = useRuntimeConfig().public.apiBase
  try {
    rows.value = await $apiFetch<AdminReservation[]>(`${base}/admin/stores/${storeId}/reservations`)
  } catch {
    rows.value = []
  } finally {
    loaded.value = true
  }
})

const STATUS_LABEL: Record<ReservationStatus, string> = {
  HOLDING: '점유 중',
  RESERVED: '예약 확정',
  COMPLETED: '세차 완료',
  CANCELED: '취소됨',
}
</script>

<template>
  <section data-testid="page-admin-reservations" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매장 예약자 관리</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">매장 {{ storeId }}의 예약 현황입니다.</p>
    </header>

    <ul v-if="rows.length" class="space-y-3">
      <li
        v-for="r in rows"
        :key="r.id"
        :data-testid="`admin-reservation-row-${r.id}`"
        class="admin-reservation-row card flex items-center justify-between gap-4 p-4 sm:p-5"
      >
        <div>
          <p class="font-semibold text-[--color-content-strong]">
            {{ r.userName }} · {{ r.userEmail }}
          </p>
          <p class="mt-0.5 text-sm text-[--color-content-muted]">
            {{ r.date }} {{ r.timeSlot }} · {{ r.bayId }}
          </p>
        </div>
        <span class="status-badge" :data-status="r.status">{{ STATUS_LABEL[r.status] }}</span>
      </li>
    </ul>

    <div
      v-else-if="loaded"
      data-testid="admin-reservations-empty"
      class="card px-6 py-12 text-center text-sm text-[--color-content-muted]"
    >
      이 매장에는 아직 예약이 없습니다.
    </div>
  </section>
</template>

<style scoped>
.status-badge {
  flex: none;
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
  background-color: var(--color-surface-2);
  color: var(--color-content-muted);
  border: 1px solid var(--color-line);
}
.status-badge[data-status='RESERVED'] {
  background-color: color-mix(in oklab, var(--color-brand-primary) 16%, transparent);
  color: var(--color-brand-primary);
  border-color: color-mix(in oklab, var(--color-brand-primary) 35%, transparent);
}
.status-badge[data-status='COMPLETED'] {
  background-color: color-mix(in oklab, var(--color-brand-accent) 16%, transparent);
  color: var(--color-brand-accent);
  border-color: color-mix(in oklab, var(--color-brand-accent) 35%, transparent);
}
</style>
