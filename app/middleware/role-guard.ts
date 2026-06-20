import type { UserRole } from '~/types/enums'

// 역할 기반 라우트 가드 (require 3.2) — 페이지 meta.roles에 명시된 역할만 허용.
// 권한 외 역할은 기본 보호 라우트(/reserve)로 리다이렉트한다. 'auth' 미들웨어 뒤에 함께 사용.
export default defineNuxtRouteMiddleware((to) => {
  const roles = to.meta.roles as UserRole[] | undefined
  if (!roles || roles.length === 0) return
  const auth = useAuthStore()
  const role = auth.currentUser?.role
  if (!role || !roles.includes(role)) {
    return navigateTo('/reserve')
  }
})
