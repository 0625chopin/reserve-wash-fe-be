// 후기 작성 자격 가드 — 세차완료(COMPLETED)·본인 예약만 진입 허용 (require 9.1)
// 미존재/미완료/타인 예약이면 예약 목록으로 되돌린다. (in-memory라 새로고침 시도 차단)
export default defineNuxtRouteMiddleware((to) => {
  const id = String(to.params.reservationId)
  const auth = useAuthStore()
  const reservation = useReservationStore()
  const target = reservation.reservations.find((r) => r.id === id)
  if (!target || target.status !== 'COMPLETED' || target.userId !== auth.currentUser?.id) {
    return navigateTo('/reservations')
  }
})
