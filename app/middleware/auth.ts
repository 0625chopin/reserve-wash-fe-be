// 미인증 시 /login 으로 보내는 라우트 미들웨어 (require 4장)
export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn) {
    return navigateTo({ path: '/login', query: { redirect: to.fullPath } })
  }
})
