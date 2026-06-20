import type { SlotStatus } from '~/types/enums'

// 예약/슬롯 데이터 접근 추상화 (단방향 의존: services → types, README 계약)
// 2단계 교체: 1차의 더미 시드 → 서버(GET /api/slots) 하이드레이트 캐시.
//   슬롯 점유의 진실은 reservation 스토어(런타임 맵) + 본 캐시 합성으로 getStatus가 판정한다.
//   (useState/$fetch/useRuntimeConfig 는 Nuxt 자동 임포트 — import 구문 작성하지 않음)

// 슬롯 식별 키 = (매장, 베이, 날짜, 30분 시간단위) — UNIQUE (require 5.2)
function slotKey(storeId: string, bayId: string, date: string, timeSlot: string): string {
  return `${storeId}|${bayId}|${date}|${timeSlot}`
}

// 서버 점유 슬롯 캐시(useState — SSR/CSR 공유). 동기 읽기로 그리드가 소비.
function slotCache() {
  return useState<Record<string, SlotStatus>>('slots:status', () => ({}))
}

// 슬롯 점유 현황 응답(BE SlotStatusResponse)
interface SlotStatusResponse {
  storeId: string
  bayId: string
  date: string
  timeSlot: string
  status: SlotStatus
}

// 매장·날짜 단위 점유 슬롯을 서버에서 로드해 캐시에 반영(날짜 선택 시 호출).
// 점유 현황은 공개 API(permitAll)라 일반 $fetch 사용.
export async function loadSlots(storeId: string, date: string): Promise<void> {
  const base = useRuntimeConfig().public.apiBase
  try {
    const rows = await $fetch<SlotStatusResponse[]>(`${base}/slots`, {
      query: { storeId, date },
    })
    const cache = slotCache()
    for (const r of rows) {
      cache.value[slotKey(r.storeId, r.bayId, r.date, r.timeSlot)] = r.status
    }
  } catch (e) {
    console.warn('[slots] 슬롯 상태 로드 실패 — 백엔드(:8080) 기동 여부 확인', e)
  }
}

// 슬롯의 서버 점유 상태 동기 조회 — 캐시에 없으면 AVAILABLE (시그니처 유지)
// 호출부(reservation 스토어 getStatus)는 이 시그니처에만 의존한다.
export function getSeededSlotStatus(
  storeId: string,
  bayId: string,
  date: string,
  timeSlot: string,
): SlotStatus {
  return slotCache().value[slotKey(storeId, bayId, date, timeSlot)] ?? 'AVAILABLE'
}
