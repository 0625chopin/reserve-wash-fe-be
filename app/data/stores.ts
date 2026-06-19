import type { Bay, Store } from '~/types/domain'

// 매장 더미 (require 5.1). approved=false 매장은 예약 목록에서 제외(require 6.1, Phase 4 필터)
export const stores: Store[] = [
  { id: 'store1', name: '강남점', bayCount: 3, approved: true },
  { id: 'store2', name: '홍대점', bayCount: 2, approved: true },
  { id: 'store3', name: '판교점', bayCount: 3, approved: false }, // 미승인 — 노출 제외 대상
]

// 베이 더미 — 매장별 'A1' ~ 'A{bayCount}' (require 5.1, 5.2). bayCount와 개수 일치
export const bays: Bay[] = [
  { id: 'store1-A1', storeId: 'store1', code: 'A1' },
  { id: 'store1-A2', storeId: 'store1', code: 'A2' },
  { id: 'store1-A3', storeId: 'store1', code: 'A3' },
  { id: 'store2-A1', storeId: 'store2', code: 'A1' },
  { id: 'store2-A2', storeId: 'store2', code: 'A2' },
  { id: 'store3-A1', storeId: 'store3', code: 'A1' },
  { id: 'store3-A2', storeId: 'store3', code: 'A2' },
  { id: 'store3-A3', storeId: 'store3', code: 'A3' },
]
