import type { Bay, Slot } from '~/types/domain'

// 하루(00:00 ~ 23:30)를 30분 단위 'HH:mm' 배열로 생성 (require 5.2)
// 시 0~23 × 분 {0, 30} = 48개. 순수 함수(브라우저 API 미사용, SSR 안전).
export function generateTimeSlots(): string[] {
  const result: string[] = []
  for (let hour = 0; hour < 24; hour++) {
    for (const minute of [0, 30]) {
      const hh = String(hour).padStart(2, '0')
      const mm = String(minute).padStart(2, '0')
      result.push(`${hh}:${mm}`)
    }
  }
  return result
}

// 특정 매장의 특정 날짜 슬롯 전체 생성 (베이 × 30분 시간단위, require 5.2)
// 반환 개수 = 베이 수 × 48. 초기 status는 모두 'AVAILABLE'.
export function generateSlotsForDate(storeId: string, bays: Bay[], date: string): Slot[] {
  const times = generateTimeSlots()
  const slots: Slot[] = []
  for (const bay of bays) {
    for (const timeSlot of times) {
      slots.push({ storeId, bayId: bay.id, date, timeSlot, status: 'AVAILABLE' })
    }
  }
  return slots
}
