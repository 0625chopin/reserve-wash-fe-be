<script setup lang="ts">
// 예약 위저드 3페이지 — 예약 완료 요약 (draft.lastReservation 표시)
definePageMeta({ middleware: ['auth', 'reservation-wizard-guard'] })

const draft = useReservationDraftStore()

// 새 예약 — 진행 상태 초기화 후 1페이지로
function onNewReservation() {
  draft.reset()
  navigateTo('/reserve')
}
</script>

<template>
  <section data-testid="page-reserve-done" class="mx-auto max-w-lg">
    <!-- 완료 요약 카드 (가드가 lastReservation 없으면 /reserve로 보냄) -->
    <div
      v-if="draft.lastReservation"
      data-testid="reserve-result"
      class="done-card overflow-hidden rounded-2xl"
    >
      <!-- 헤더: 라임 채움 배지로 성공 상태 강조 -->
      <div class="done-header flex items-center gap-3 px-6 py-5">
        <span class="done-check" aria-hidden="true">✓</span>
        <span class="text-lg font-semibold text-[--color-content-strong]">예약이 접수되었어요</span>
      </div>
      <!-- 요약: 라벨(muted) + 값(strong) 위계 -->
      <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-3 px-6 py-5 text-sm">
        <dt class="text-[--color-content-muted]">매장</dt>
        <dd class="font-medium text-[--color-content-strong]">{{ draft.lastReservation.store }}</dd>
        <dt class="text-[--color-content-muted]">매니저</dt>
        <dd class="font-medium text-[--color-content-strong]">
          {{ draft.lastReservation.manager }}
        </dd>
        <dt class="text-[--color-content-muted]">차종 · 베이</dt>
        <dd class="font-medium text-[--color-content-strong]">
          {{ draft.lastReservation.carType }} · {{ draft.lastReservation.bay }}
        </dd>
        <dt class="text-[--color-content-muted]">서비스</dt>
        <dd class="font-medium text-[--color-content-strong]">
          {{ draft.lastReservation.service }}
        </dd>
        <dt class="text-[--color-content-muted]">일시</dt>
        <dd class="font-medium text-[--color-content-strong]">
          {{ draft.lastReservation.date }} {{ draft.lastReservation.time }}
        </dd>
        <dt class="text-[--color-content-muted]">금액</dt>
        <dd class="font-medium text-[--color-content-strong]">
          {{ draft.lastReservation.amount }} · 현장결제
        </dd>
      </dl>
      <!-- 후속 액션 -->
      <div class="flex gap-3 px-6 pb-6">
        <NuxtLink to="/reservations" data-testid="go-reservations" class="btn btn-ghost flex-1">
          예약 목록 보기
        </NuxtLink>
        <button
          data-testid="new-reservation"
          type="button"
          class="btn btn-primary flex-1"
          @click="onNewReservation"
        >
          새 예약
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* 카드 — surface-1 본문 + 라임 보더로 성공 상태 테두리, 깊은 그림자 (하드코딩 hex 금지) */
.done-card {
  background-color: var(--color-surface-1);
  border: 1px solid color-mix(in oklab, var(--color-brand-accent) 45%, var(--color-line));
  box-shadow:
    0 1px 2px rgb(0 0 0 / 0.4),
    0 24px 48px -16px rgb(0 0 0 / 0.7);
}

/* 헤더 — 라임을 면(面)으로 채워 성공을 한눈에 */
.done-header {
  background-color: color-mix(in oklab, var(--color-brand-accent) 14%, transparent);
  border-bottom: 1px solid color-mix(in oklab, var(--color-brand-accent) 28%, transparent);
}

/* 체크 아이콘 — 라임 채움 배지(고대비 다크 글리프) */
.done-check {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 9999px;
  background-color: var(--color-brand-accent);
  color: var(--color-surface-base);
  font-size: 0.9375rem;
  font-weight: 800;
}
</style>
