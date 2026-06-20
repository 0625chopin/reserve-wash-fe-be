// 예약 위저드 진입 가드 — 미완료 단계로 직접 진입 시 1페이지로 되돌림 (require 6.5.3)
// 진행 상태는 in-memory(reservationDraft)라 새로고침 시 초기화되므로, 직접 URL 진입을 차단한다.
// Nuxt 미들웨어 키는 파일명을 kebab-case로 변환한 'reservation-wizard-guard'.
export default defineNuxtRouteMiddleware((to) => {
  const draft = useReservationDraftStore()
  // 2페이지: 1페이지(매장·매니저·차종·서비스) 미완료면 차단
  if (to.path === '/reserve/slot' && !draft.canProceedToSlot) {
    return navigateTo('/reserve')
  }
  // 3페이지: 확정 결과가 없으면 차단
  if (to.path === '/reserve/done' && !draft.lastReservation) {
    return navigateTo('/reserve')
  }
})
