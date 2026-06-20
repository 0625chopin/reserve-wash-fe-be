// 위저드 신규 진입 시 진행상태 초기화 (require 6.5.3)
// in-memory draft가 싱글톤이라 재진입 시 이전 선택이 남는 문제를 방지한다.
// 단, '/reserve/slot'에서 '이전'으로 돌아온 경우(from=/reserve/slot)는 선택을 유지한다.
export default defineNuxtRouteMiddleware((to, from) => {
  if (from.path !== '/reserve/slot') {
    useReservationDraftStore().reset()
  }
})
