import type { Manager } from '~/types/domain'

// 매니저 더미 (require 3.1, 6.1). isStoreAdmin=매장 최고권한 매니저.
// dayoffDates는 Phase 5에서 해당 매니저 지정 시 휴무 슬롯 비활성에 사용된다(require 6.1).
export const managers: Manager[] = [
  {
    id: 'mgr1',
    storeId: 'store1',
    name: '김매니저',
    isStoreAdmin: true,
    dayoffDates: ['2026-06-22', '2026-06-29'], // 휴무일 보유
  },
  {
    id: 'mgr2',
    storeId: 'store1',
    name: '이매니저',
    isStoreAdmin: false,
    dayoffDates: [],
  },
  {
    id: 'mgr3',
    storeId: 'store2',
    name: '박매니저',
    isStoreAdmin: true,
    dayoffDates: ['2026-06-23'],
  },
  {
    id: 'mgr4',
    storeId: 'store3',
    name: '최매니저',
    isStoreAdmin: true,
    dayoffDates: [],
  },
]
