// 매출 원차트 가공(v2.4) — 전 매장 매출 비중을 상위 5개 + 나머지 합산 'ETC'로 변환.
//   서버(GET /api/admin/sales/by-store)는 매장별 합계만 반환하고, Top5+ETC·비중(%) 가공은 FE에서 수행
//   하여 차트 컴포넌트를 1차(ROADMAP_1 Phase 9)와 무변경으로 둔다(README 단방향 의존 유지).

export interface SalesByStoreRow {
  storeId: string
  storeName: string
  amount: number
}

export interface SalesSlice {
  label: string
  amount: number
  percent: number // 비중 % (소수 1자리)
  color: string
}

// 6색 팔레트 — 상위 5개 + ETC
const PALETTE = ['#38bdf8', '#34d399', '#f59e0b', '#f472b6', '#a78bfa', '#94a3b8']

// 매출 합계 목록 → 파이 슬라이스(상위 5 + ETC). 금액 0 매장은 차트에서 제외.
export function buildSalesSlices(rows: SalesByStoreRow[]): SalesSlice[] {
  const sorted = [...rows].filter((r) => r.amount > 0).sort((a, b) => b.amount - a.amount)
  if (sorted.length === 0) return []

  const top = sorted.slice(0, 5)
  const restSum = sorted.slice(5).reduce((sum, r) => sum + r.amount, 0)

  const base = top.map((r, i) => ({ label: r.storeName, amount: r.amount, color: PALETTE[i]! }))
  if (restSum > 0) base.push({ label: 'ETC', amount: restSum, color: PALETTE[5]! })

  const total = base.reduce((sum, s) => sum + s.amount, 0)
  return base.map((s) => ({
    ...s,
    percent: total ? Math.round((s.amount / total) * 1000) / 10 : 0,
  }))
}
