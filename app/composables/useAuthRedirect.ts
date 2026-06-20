import type { UserRole } from '~/types/enums'

// 로그인 후 역할군별 기본 랜딩 경로 (require v1.8·v1.9 §12.4)
export function roleHome(role: UserRole | null | undefined): string {
  switch (role) {
    case 'MANAGER':
      return '/manager/reserve'
    case 'STORE_ADMIN':
      return '/store-admin/dayoff-approvals'
    case 'ADMIN':
      return '/admin/manager-approvals'
    default:
      return '/reserve'
  }
}
