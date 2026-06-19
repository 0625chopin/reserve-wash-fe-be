import type { Price } from '~/types/domain'

// 차종 5 × 서비스 4 = 20개 단가 매트릭스 (require 10.3 확정 단가, 단위: 원)
// 열 순서: EXT(외부) / INT(내부) / FULL(풀패키지) / PREMIUM(프리미엄)
export const prices: Price[] = [
  { carType: 'LIGHT', serviceType: 'EXT', amount: 10000 },
  { carType: 'LIGHT', serviceType: 'INT', amount: 10000 },
  { carType: 'LIGHT', serviceType: 'FULL', amount: 18000 },
  { carType: 'LIGHT', serviceType: 'PREMIUM', amount: 28000 },
  { carType: 'SMALL', serviceType: 'EXT', amount: 12000 },
  { carType: 'SMALL', serviceType: 'INT', amount: 12000 },
  { carType: 'SMALL', serviceType: 'FULL', amount: 22000 },
  { carType: 'SMALL', serviceType: 'PREMIUM', amount: 32000 },
  { carType: 'MID', serviceType: 'EXT', amount: 15000 },
  { carType: 'MID', serviceType: 'INT', amount: 15000 },
  { carType: 'MID', serviceType: 'FULL', amount: 27000 },
  { carType: 'MID', serviceType: 'PREMIUM', amount: 38000 },
  { carType: 'LARGE', serviceType: 'EXT', amount: 18000 },
  { carType: 'LARGE', serviceType: 'INT', amount: 18000 },
  { carType: 'LARGE', serviceType: 'FULL', amount: 33000 },
  { carType: 'LARGE', serviceType: 'PREMIUM', amount: 45000 },
  { carType: 'VAN_ETC', serviceType: 'EXT', amount: 22000 },
  { carType: 'VAN_ETC', serviceType: 'INT', amount: 22000 },
  { carType: 'VAN_ETC', serviceType: 'FULL', amount: 40000 },
  { carType: 'VAN_ETC', serviceType: 'PREMIUM', amount: 55000 },
]
