import type { Bay, Manager, Store } from '~/types/domain'
import type { BaySize, CarType } from '~/types/enums'
import { bays, stores } from '~/data/stores'
import { managers } from '~/data/managers'
import { carTypes, type CarTypeOption } from '~/data/carTypes'

// 크기 등급 순서 — 큰 베이가 작은 차도 수용
const SIZE_RANK: Record<BaySize, number> = { SMALL: 1, MID: 2, LARGE: 3 }

// 매장/매니저 데이터 접근 추상화 (단방향 의존: services → data·types, README 계약)
// TODO(2단계): stores/managers import를 $fetch/useFetch 서버 호출로 교체(시그니처 유지)

// 승인된 매장만 반환 — 미승인 매장은 예약 목록에서 제외 (require 6.1)
export function getApprovedStores(): Store[] {
  return stores.filter((s) => s.approved)
}

// 특정 매장의 매니저만 반환 (require 6.3 매니저 선택)
export function getManagersByStore(storeId: string): Manager[] {
  return managers.filter((m) => m.storeId === storeId)
}

// 매니저 id로 조회 — 선택 매니저 휴무(dayoffDates) 컨텍스트용 (require 6.1)
export function getManager(id: string): Manager | undefined {
  return managers.find((m) => m.id === id)
}

// 특정 매장의 베이만 반환 (require 5.1, 5.2 — 슬롯 = 매장×베이×날짜×시간)
export function getBaysByStore(storeId: string): Bay[] {
  return bays.filter((b) => b.storeId === storeId)
}

// 차종 카탈로그 (require 10.1)
export function getCarTypes(): CarTypeOption[] {
  return carTypes
}

// 차 크기에 맞는 베이만 반환 — 차가 요구하는 크기 이상을 수용하는 베이
export function getBaysForCar(storeId: string, carType: CarType): Bay[] {
  const car = carTypes.find((c) => c.code === carType)
  if (!car) return []
  const min = SIZE_RANK[car.size]
  return bays.filter((b) => b.storeId === storeId && SIZE_RANK[b.size] >= min)
}
