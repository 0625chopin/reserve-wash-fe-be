import type { Manager, Store } from '~/types/domain'
import { stores } from '~/data/stores'
import { managers } from '~/data/managers'

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
