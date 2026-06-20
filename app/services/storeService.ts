import type { Bay, Manager, Store } from '~/types/domain'
import type { BaySize, CarType, DayoffType } from '~/types/enums'
import { carTypes, type CarTypeOption } from '~/data/carTypes'
import { serviceTypes, type ServiceTypeOption } from '~/data/serviceTypes'
import { catalogBays, catalogManagers, catalogStores } from '~/services/catalogCache'

// 크기 등급 순서 — 큰 베이가 작은 차도 수용 (2차 Q1: 특대형 XLARGE 신설)
const SIZE_RANK: Record<BaySize, number> = { SMALL: 1, MID: 2, LARGE: 3, XLARGE: 4 }

// 매장/매니저/베이 데이터 접근 추상화 (단방향 의존: services → catalogCache·data·types, README 계약)
// 2단계 교체: 1차의 ~/data 직접 import → 서버 하이드레이트 캐시(catalogCache) 동기 읽기.
//   시그니처·동기 반환 타입을 유지하여 컴포넌트/스토어/computed 무변경(additive).
//   carTypes/serviceTypes는 순수 프레젠테이션 카탈로그라 FE 정적 유지.

// 승인된 매장만 반환 — 서버가 이미 승인 매장만 응답(require 6.1)
export function getApprovedStores(): Store[] {
  return catalogStores()
}

// 특정 매장의 매니저만 반환 (require 6.3 매니저 선택)
export function getManagersByStore(storeId: string): Manager[] {
  return catalogManagers().filter((m) => m.storeId === storeId)
}

// 매니저 id로 조회 — 선택 매니저 휴무(dayoffs) 컨텍스트용 (require 6.1)
export function getManager(id: string): Manager | undefined {
  return catalogManagers().find((m) => m.id === id)
}

// 3교대 근무 시간 경계 (require 5.5) — 'HH:mm' → 분 단위로 변환
function toMinutes(timeSlot: string): number {
  const [hh, mm] = timeSlot.split(':')
  return Number(hh) * 60 + Number(mm)
}

// 해당 timeSlot이 교대조 근무 시간대에 속하는지 (require 5.5)
// SHIFT_1 06:00~14:00 / SHIFT_2 14:00~22:00 / SHIFT_3 22:00~06:00(익일, 자정 넘김)
function isInShift(type: DayoffType, timeSlot: string): boolean {
  const t = toMinutes(timeSlot)
  if (type === 'SHIFT_1') return t >= 360 && t < 840
  if (type === 'SHIFT_2') return t >= 840 && t < 1320
  if (type === 'SHIFT_3') return t >= 1320 || t < 360
  return false
}

// 매니저가 특정 (날짜, 시간)에 휴무인지 — 전일이면 항상, 교대조면 해당 시간대만 (require 5.5, 6.1)
export function isManagerOffAt(manager: Manager, date: string, timeSlot: string): boolean {
  return manager.dayoffs.some((d) => {
    if (d.date !== date) return false
    if (d.type === 'FULL_DAY') return true
    return isInShift(d.type, timeSlot)
  })
}

// 매니저가 특정 날짜에 전일 휴무인지 — 날짜 단위 비활성용 (require 6.1)
export function isManagerFullDayOff(manager: Manager, date: string): boolean {
  return manager.dayoffs.some((d) => d.date === date && d.type === 'FULL_DAY')
}

// 특정 매장의 베이만 반환 (require 5.1, 5.2 — 슬롯 = 매장×베이×날짜×시간)
export function getBaysByStore(storeId: string): Bay[] {
  return catalogBays().filter((b) => b.storeId === storeId)
}

// 차종 카탈로그 (require 10.1) — FE 정적 유지
export function getCarTypes(): CarTypeOption[] {
  return carTypes
}

// 서비스 카탈로그 (require 10.2) — FE 정적 유지
export function getServiceTypes(): ServiceTypeOption[] {
  return serviceTypes
}

// 차 크기에 맞는 베이만 반환 — 차가 요구하는 크기 이상을 수용하는 베이 (누적 로직, 캐시된 bays 기준)
export function getBaysForCar(storeId: string, carType: CarType): Bay[] {
  const car = carTypes.find((c) => c.code === carType)
  if (!car) return []
  const min = SIZE_RANK[car.size]
  return catalogBays().filter((b) => b.storeId === storeId && SIZE_RANK[b.size] >= min)
}
