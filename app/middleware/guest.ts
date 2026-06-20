// 게스트 전용 가드 — 이미 로그인했다면 회원가입 화면 진입을 막고 /reserve로 (require 4.1)
export default defineNuxtRouteMiddleware(() => {
  const auth = useAuthStore()
  if (auth.isLoggedIn) {
    return navigateTo('/reserve')
  }
})
