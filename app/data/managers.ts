import type { Manager } from '~/types/domain'

// 매니저 더미 (require 3.1, 6.1). isStoreAdmin=매장 최고권한 매니저.
// dayoffs는 Phase 5에서 해당 매니저 지정 시 휴무 슬롯 비활성에 사용된다(require 5.5, 6.1).
// 휴무 유형: FULL_DAY=전일 / SHIFT_1(06~14)·SHIFT_2(14~22)·SHIFT_3(22~06) 교대조 단위.
// E2E 결정성을 위해 고정 날짜·교대 케이스를 포함한다.
export const managers: Manager[] = [
  {
    id: 'mgr1',
    storeId: 'store1',
    name: '김매니저',
    isStoreAdmin: true,
    // 전일 휴무 + 오전조(SHIFT_1) 부분 휴무 혼합
    dayoffs: [
      { date: '2026-06-22', type: 'FULL_DAY' },
      { date: '2026-06-23', type: 'SHIFT_1' },
      { date: '2026-06-29', type: 'FULL_DAY' },
    ],
  },
  {
    id: 'mgr2',
    storeId: 'store1',
    name: '이매니저',
    isStoreAdmin: false,
    dayoffs: [],
  },
  {
    id: 'mgr3',
    storeId: 'store2',
    name: '박매니저',
    isStoreAdmin: true,
    // 오후조(SHIFT_2) 부분 휴무
    dayoffs: [{ date: '2026-06-23', type: 'SHIFT_2' }],
  },
  {
    id: 'mgr4',
    storeId: 'store3',
    name: '최매니저',
    isStoreAdmin: true,
    dayoffs: [],
  },
]
