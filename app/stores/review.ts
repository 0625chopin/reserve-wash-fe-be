import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Review } from '~/types/domain'

// 후기/평점 스토어 (require 9장). 1단계 in-memory.
// TODO(2·3단계): reviews 영속화·집계 쿼리를 서버/DB로 이관
export const useReviewStore = defineStore('review', () => {
  const reviews = ref<Review[]>([])

  // 후기 추가 (세차완료 예약만 — 자격 검증은 호출부/가드가 담당)
  function addReview(review: Review) {
    reviews.value.push(review)
  }

  // 예약당 중복 작성 방지 — 이미 후기가 있으면 true
  function hasReview(reservationId: string): boolean {
    return reviews.value.some((r) => r.reservationId === reservationId)
  }

  // 키별 평균 평점 산출 헬퍼
  function averageBy(keyOf: (r: Review) => string | null): Record<string, number> {
    const acc: Record<string, { sum: number; count: number }> = {}
    for (const r of reviews.value) {
      const key = keyOf(r)
      if (key === null) continue
      const cur = acc[key] ?? { sum: 0, count: 0 }
      cur.sum += r.rating
      cur.count += 1
      acc[key] = cur
    }
    return Object.fromEntries(
      Object.entries(acc).map(([key, { sum, count }]) => [key, sum / count]),
    )
  }

  // 매장별 평균 평점 (require 9.1)
  const averageByStore = computed(() => averageBy((r) => r.storeId))
  // 매니저별 평균 평점 — managerId null(대행 아님)은 제외
  const averageByManager = computed(() => averageBy((r) => r.managerId))

  // 후기 식별자 — 세션 내 증가 시퀀스(현재시각/랜덤 회피)
  let seq = 0
  function nextReviewId(): string {
    seq += 1
    return `rv-${seq}`
  }

  return { reviews, addReview, hasReview, averageByStore, averageByManager, nextReviewId }
})
