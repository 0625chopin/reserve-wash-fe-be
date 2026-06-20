// 위저드 1페이지(/reserve) 진입 시 진행상태 초기화 (require 6.5.3)
// in-memory draft 싱글톤이 이전 선택을 남기지 않도록, /reserve에 들어올 때마다 항상 초기화한다.
// '이전' 버튼(/reserve/slot → /reserve)으로 돌아온 경우도 동일하게 매장 선택부터 새로 시작한다.
export default defineNuxtRouteMiddleware(() => {
  useReservationDraftStore().reset()
})
