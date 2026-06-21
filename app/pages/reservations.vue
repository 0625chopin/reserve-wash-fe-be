<script setup lang="ts">
// 예약 목록 — 역할별 분기 (require 6.1, v1.10/v1.11 §6.6)
//   · USER       : 본인 예약(userId 기준)
//   · MANAGER    : 내 예약 + 담당 예약(managerId 기준 — 고객이 지정·대행 등록) 2탭
//   · STORE_ADMIN: 매장 전체 예약(storeId 기준 — 본인 소속 매장)
//   · ADMIN      : 담당 예약(매장 선택 후 선택 매장의 예약만 — 소속 매장이 없어 매장을 직접 선택)
import { computed, onMounted, ref, watch } from 'vue'
import type { Reservation } from '~/types/domain'
import { getApprovedStores } from '~/services/storeService'

definePageMeta({ middleware: 'auth' })

const auth = useAuthStore()
const reservation = useReservationStore()

const role = computed(() => auth.currentUser?.role)
const isManager = computed(() => role.value === 'MANAGER')
const isStoreAdmin = computed(() => role.value === 'STORE_ADMIN')
const isAdmin = computed(() => role.value === 'ADMIN')

// MANAGER 전용 탭 — 기본은 '담당 예약'(매니저 핵심 업무: 고객/대행 예약 확인)
const activeTab = ref<'mine' | 'assigned'>('assigned')

// ADMIN 매장 선택 — 관리자는 소속 매장이 없어, 매장을 선택해야 그 매장의 담당 예약을 본다
const adminStores = getApprovedStores()
const selectedStoreId = ref('')
watch(selectedStoreId, (id) => {
  if (id) reservation.fetchAdminStoreReservations(id)
  else reservation.storeReservations = []
})

// 진입 시 역할에 맞는 목록을 서버에서 하이드레이션 (새로고침/직접 진입 정합)
//   ADMIN은 매장 선택 시점에 로드하므로 진입 시 별도 조회 없음
onMounted(() => {
  if (isAdmin.value) return
  reservation.fetchMine()
  if (isManager.value) reservation.fetchManagerAssigned()
  if (isStoreAdmin.value) reservation.fetchStoreReservations()
})

// 내 예약(본인 userId) — 최신이 위로 (require 6.1)
const myReservations = computed(() =>
  reservation.reservations
    .filter((r) => r.userId === auth.currentUser?.id)
    .slice()
    .reverse(),
)
// 담당 예약(서버에서 managerId로 필터링됨) — 최신이 위로
const assignedReservations = computed(() => reservation.managerAssigned.slice().reverse())
// 매장 전체 예약 — 최신이 위로
const storeAllReservations = computed(() => reservation.storeReservations.slice().reverse())

// 현재 표시 뷰 — 역할/탭에 따라 목록·읽기전용·고객표시·빈상태 문구를 결정
interface ListView {
  list: Reservation[]
  readonly: boolean
  showCustomer: boolean
  emptyTitle: string
  emptyHint: string
}
const view = computed<ListView>(() => {
  if (isAdmin.value) {
    return {
      list: selectedStoreId.value ? storeAllReservations.value : [],
      readonly: true,
      showCustomer: true,
      emptyTitle: selectedStoreId.value ? '매장 예약이 없어요' : '매장을 선택하세요',
      emptyHint: selectedStoreId.value
        ? '선택한 매장에 등록된 예약이 없습니다.'
        : '위에서 매장을 선택하면 해당 매장의 담당 예약을 보여줍니다.',
    }
  }
  if (isStoreAdmin.value) {
    return {
      list: storeAllReservations.value,
      readonly: true,
      showCustomer: true,
      emptyTitle: '매장 예약이 없어요',
      emptyHint: '아직 이 매장에 등록된 예약이 없습니다.',
    }
  }
  if (isManager.value && activeTab.value === 'assigned') {
    return {
      list: assignedReservations.value,
      readonly: true,
      showCustomer: true,
      emptyTitle: '담당 예약이 없어요',
      emptyHint: '고객이 회원님을 지정했거나 회원님이 대행 등록한 예약이 여기에 표시됩니다.',
    }
  }
  // USER/ADMIN, 또는 MANAGER의 '내 예약' 탭
  return {
    list: myReservations.value,
    readonly: false,
    showCustomer: false,
    emptyTitle: '아직 예약이 없어요',
    emptyHint: '첫 세차를 예약하고 차를 빛나게 만들어 보세요.',
  }
})

// 역할별 헤더 부제
const subtitle = computed(() => {
  if (isAdmin.value) return '담당 예약 — 매장을 선택해 해당 매장의 예약을 확인하세요.'
  if (isStoreAdmin.value) return '매장 전체의 모든 매니저 예약 건을 확인하세요.'
  if (isManager.value) return '내 예약과 담당 예약(고객이 지정·대행 등록한 건)을 확인하세요.'
  return '진행 중이거나 완료된 예약을 확인하세요.'
})
</script>

<template>
  <section data-testid="page-reservations" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <h1 class="text-3xl font-bold">예약 목록</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">{{ subtitle }}</p>
    </header>

    <!-- ADMIN: 매장 선택 → 선택 매장의 담당 예약만 표시 (require v1.11 §6.6) -->
    <div v-if="isAdmin" class="mb-6">
      <label
        for="admin-store-select"
        class="mb-1.5 block text-sm font-medium text-[--color-content-muted]"
      >
        매장 선택
      </label>
      <select
        id="admin-store-select"
        v-model="selectedStoreId"
        data-testid="admin-store-select"
        class="admin-store-select"
      >
        <option value="">매장을 선택하세요</option>
        <option v-for="s in adminStores" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>
    </div>

    <!-- MANAGER: 내 예약 / 담당 예약 탭 분리 (require v1.10 §6.6) -->
    <div
      v-if="isManager"
      role="tablist"
      class="mb-6 inline-flex rounded-xl border border-[--color-line] bg-[--color-surface-2] p-1"
    >
      <button
        data-testid="tab-mine"
        type="button"
        role="tab"
        class="reservations-tab"
        :data-active="activeTab === 'mine'"
        @click="activeTab = 'mine'"
      >
        내 예약
      </button>
      <button
        data-testid="tab-assigned"
        type="button"
        role="tab"
        class="reservations-tab"
        :data-active="activeTab === 'assigned'"
        @click="activeTab = 'assigned'"
      >
        담당 예약
      </button>
    </div>

    <!-- 예약 목록 -->
    <ul v-if="view.list.length" class="space-y-4">
      <ReservationCard
        v-for="r in view.list"
        :key="r.id"
        :reservation="r"
        :readonly="view.readonly"
        :show-customer="view.showCustomer"
      />
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
          <rect
            x="4"
            y="5"
            width="16"
            height="15"
            rx="2"
            stroke="currentColor"
            stroke-width="1.6"
          />
          <path
            d="M8 3v4M16 3v4M4 10h16"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
          />
        </svg>
      </div>
      <div>
        <p class="font-semibold text-[--color-content-strong]">{{ view.emptyTitle }}</p>
        <p class="mt-1 text-sm text-[--color-content-muted]">{{ view.emptyHint }}</p>
      </div>
      <NuxtLink
        v-if="!isManager && !isStoreAdmin && !isAdmin"
        to="/reserve"
        class="btn btn-primary"
      >
        예약하러 가기
      </NuxtLink>
    </div>
  </section>
</template>

<style scoped>
/* 역할 탭 — 활성 탭만 surface로 강조 */
.reservations-tab {
  border-radius: 0.625rem;
  padding: 0.4rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-content-muted);
  transition:
    background-color 0.15s,
    color 0.15s;
}
.reservations-tab[data-active='true'] {
  background-color: var(--color-surface);
  color: var(--color-content-strong);
  box-shadow: 0 1px 2px rgb(0 0 0 / 0.06);
}

/* 관리자 매장 선택 — BO 입력(.bo-input) 토큰 재사용. 다크 테마 네이티브 select 가독성 확보 */
.admin-store-select {
  width: 100%;
  max-width: 20rem;
  border-radius: 0.625rem;
  border: 1px solid var(--color-line);
  background-color: var(--color-surface-1);
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-content-strong);
}
.admin-store-select:focus {
  outline: 2px solid color-mix(in oklab, var(--color-brand-primary) 45%, transparent);
  outline-offset: 1px;
}
/* 드롭다운 옵션 — 기본(흰 배경/밝은 글자)로 인한 문구 안 보임 방지: 배경/글자색 명시 */
.admin-store-select option {
  background-color: var(--color-surface-1);
  color: var(--color-content-strong);
}
</style>
