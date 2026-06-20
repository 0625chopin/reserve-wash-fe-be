import type { ServiceType } from '~/types/enums'

// 서비스 카탈로그 (require 10.2 확정 분류)
export interface ServiceTypeOption {
  code: ServiceType
  name: string
}

export const serviceTypes: ServiceTypeOption[] = [
  { code: 'EXT', name: '외부세차' },
  { code: 'INT', name: '내부세차' },
  { code: 'FULL', name: '풀패키지(외부+내부)' },
  { code: 'PREMIUM', name: '프리미엄(왁스·광택)' },
]
