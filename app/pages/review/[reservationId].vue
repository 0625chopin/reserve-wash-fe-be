<script setup lang="ts">
// 후기 작성 (동적 라우트) — 세차완료·본인 예약만 (require 9장). 자격은 review-guard 미들웨어가 보장
import { computed, ref } from 'vue'
import { getApprovedStores, getManager } from '~/services/storeService'

definePageMeta({ middleware: ['auth', 'review-guard'] })

const route = useRoute()
const reservation = useReservationStore()
const review = useReviewStore()

const reservationId = String(route.params.reservationId)
const target = computed(() => reservation.reservations.find((r) => r.id === reservationId))

const stores = getApprovedStores()
function storeName(id: string) {
  return stores.find((s) => s.id === id)?.name ?? id
}
function managerName(id: string | null) {
  return id ? (getManager(id)?.name ?? id) : '-'
}

// 이미 작성한 예약이면 작성완료 상태로 전환 (중복 방지)
const alreadyReviewed = computed(() => review.hasReview(reservationId))

// 평점(1~5)·텍스트 입력
const rating = ref(0)
const text = ref('')
const canSubmit = computed(() => rating.value >= 1 && rating.value <= 5)

function setRating(n: number) {
  rating.value = n
}

function onSubmit() {
  const t = target.value
  if (!t || !canSubmit.value) return
  review.addReview({
    id: review.nextReviewId(),
    reservationId,
    userId: t.userId,
    storeId: t.storeId,
    managerId: t.managerId,
    rating: rating.value,
    text: text.value,
    createdAt: new Date().toISOString(),
  })
}

// 통합(전체) 평균 평점 (require 9.1) — 매장/매니저 구분 없는 서비스 전체 평균
function fmtAvg(v: number | undefined) {
  return v === undefined ? '-' : v.toFixed(1)
}
const overallAvg = computed(() => review.averageOverall)
// 내가 작성한 후기(평점·문구) — 작성 완료 화면에 표시
const myReview = computed(() => review.reviewOf(reservationId))
</script>

<template>
  <section data-testid="page-review" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">후기</span>
      <h1 class="text-3xl font-bold">후기 작성</h1>
      <p v-if="target" class="mt-2 text-sm text-[--color-content-muted]">
        {{ storeName(target.storeId) }} · {{ managerName(target.managerId) }} · {{ target.date }}
        {{ target.timeSlot }}
      </p>
    </header>

    <!-- 작성 완료 상태 — 평균 평점 표시 -->
    <div v-if="alreadyReviewed" data-testid="review-done" class="card p-6 sm:p-8">
      <div class="mb-5 flex items-center gap-3">
        <span class="review-check" aria-hidden="true">✓</span>
        <span class="text-lg font-semibold text-[--color-content-strong]">후기가 등록되었어요</span>
      </div>
      <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
        <dt class="text-[--color-content-muted]">내 평점</dt>
        <dd data-testid="my-rating" class="font-medium text-[--color-content-strong]">
          ★ {{ myReview?.rating ?? '-' }} / 5
        </dd>
        <dt class="text-[--color-content-muted]">통합 평균 평점</dt>
        <dd data-testid="avg-overall" class="font-medium text-[--color-content-strong]">
          ★ {{ fmtAvg(overallAvg) }}
        </dd>
      </dl>

      <!-- 내가 작성한 후기 문구 -->
      <div v-if="myReview?.text" class="mt-4">
        <span class="field-label">내 후기</span>
        <p
          data-testid="my-review-text"
          class="rounded-xl border border-[--color-line-soft] bg-[--color-surface-1] px-4 py-3 text-sm text-[--color-content]"
        >
          {{ myReview.text }}
        </p>
      </div>

      <div class="mt-6 border-t border-[--color-line-soft] pt-5">
        <NuxtLink to="/reservations" class="btn btn-primary w-full">예약 목록으로</NuxtLink>
      </div>
    </div>

    <!-- 작성 폼 -->
    <div v-else class="card space-y-6 p-6 sm:p-8">
      <!-- 별점 -->
      <div>
        <span class="field-label">평점</span>
        <div class="flex gap-1" role="radiogroup" aria-label="평점 선택">
          <button
            v-for="n in 5"
            :key="n"
            :data-testid="`star-${n}`"
            type="button"
            class="star"
            :class="{ 'star-on': n <= rating }"
            :aria-label="`${n}점`"
            :aria-pressed="n <= rating"
            @click="setRating(n)"
          >
            ★
          </button>
        </div>
      </div>

      <!-- 후기 텍스트 -->
      <div>
        <span class="field-label">후기 (선택)</span>
        <textarea
          v-model="text"
          data-testid="review-text"
          rows="4"
          class="input-field resize-none"
          placeholder="세차 경험은 어떠셨나요?"
        />
      </div>

      <!-- 제출 -->
      <div class="border-t border-[--color-line-soft] pt-5">
        <button
          data-testid="review-submit"
          type="button"
          class="btn btn-primary w-full"
          :disabled="!canSubmit"
          @click="onSubmit"
        >
          후기 등록
        </button>
        <p v-if="!canSubmit" class="mt-2 text-center text-xs text-[--color-content-muted]">
          평점(1~5)을 선택해 주세요.
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* 별점 버튼 — 기본 muted, 선택 시 라임 accent (하드코딩 hex 금지) */
.star {
  font-size: 2rem;
  line-height: 1;
  color: var(--color-line);
  cursor: pointer;
  transition: color 0.15s var(--ease-out-soft);
}
.star:hover {
  color: color-mix(in oklab, var(--color-brand-accent) 50%, var(--color-line));
}
.star-on {
  color: var(--color-brand-accent);
}

/* 완료 체크 배지 */
.review-check {
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
