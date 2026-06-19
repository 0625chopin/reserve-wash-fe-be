import type { CarType, ServiceType } from '~/types/enums'
import { prices } from '~/data/prices'

// 차종 × 서비스 단가 조회 (require 10.3)
// 단방향 의존: services는 data·types만 의존한다(README 계약). 호출부는 이 시그니처만 의존.
// TODO(2단계): prices import + find를 $fetch/useFetch 서버 호출로 교체(시그니처 유지)
export function getPrice(carType: CarType, serviceType: ServiceType): number {
  const found = prices.find((p) => p.carType === carType && p.serviceType === serviceType)
  if (!found) {
    throw new Error(`단가를 찾을 수 없습니다: ${carType} / ${serviceType}`)
  }
  return found.amount
}
