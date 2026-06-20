import type { CarType, ServiceType } from '~/types/enums'
import { catalogPrices } from '~/services/catalogCache'

// 차종 × 서비스 단가 조회 (require 10.3)
// 단방향 의존: services → catalogCache·types (README 계약). 호출부는 이 시그니처만 의존.
// 2단계 교체: 1차의 ~/data/prices import → 서버 하이드레이트 캐시 동기 읽기(시그니처 유지).
export function getPrice(carType: CarType, serviceType: ServiceType): number {
  const found = catalogPrices().find((p) => p.carType === carType && p.serviceType === serviceType)
  if (!found) {
    throw new Error(`단가를 찾을 수 없습니다: ${carType} / ${serviceType}`)
  }
  return found.amount
}
