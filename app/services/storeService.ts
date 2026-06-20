import type { Bay, Manager, Store } from '~/types/domain'
import type { BaySize, CarType, DayoffType } from '~/types/enums'
import { bays, stores } from '~/data/stores'
import { managers } from '~/data/managers'
import { carTypes, type CarTypeOption } from '~/data/carTypes'
import { serviceTypes, type ServiceTypeOption } from '~/data/serviceTypes'

// 크기 등급 순서 — 큰 베이가 작은 차도 수용 (2차 Q1: 특대형 XLARGE 신설)
const SIZE_RANK: Record<BaySize, number> = { SMALL: 1, MID: 2, LARGE: 3, XLARGE: 4 }

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

// 매니저 id로 조회 — 선택 매니저 휴무(dayoffs) 컨텍스트용 (require 6.1)
export function getManager(id: string): Manager | undefined {
  return managers.find((m) => m.id === id)
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
  return bays.filter((b) => b.storeId === storeId)
}

// 차종 카탈로그 (require 10.1)
export function getCarTypes(): CarTypeOption[] {
  return carTypes
}

// 서비스 카탈로그 (require 10.2)
export function getServiceTypes(): ServiceTypeOption[] {
  return serviceTypes
}

// 차 크기에 맞는 베이만 반환 — 차가 요구하는 크기 이상을 수용하는 베이
export function getBaysForCar(storeId: string, carType: CarType): Bay[] {
  const car = carTypes.find((c) => c.code === carType)
  if (!car) return []
  const min = SIZE_RANK[car.size]
  return bays.filter((b) => b.storeId === storeId && SIZE_RANK[b.size] >= min)
}
