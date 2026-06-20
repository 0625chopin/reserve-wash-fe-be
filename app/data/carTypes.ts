import type { BaySize, CarType } from '~/types/enums'

// 차종 카탈로그 (require 10.1 확정 분류) + 베이 필터용 크기 등급
export interface CarTypeOption {
  code: CarType
  name: string
  size: BaySize // 이 차종이 요구하는 베이 최소 크기
}

export const carTypes: CarTypeOption[] = [
  { code: 'LIGHT', name: '경형', size: 'SMALL' },
  { code: 'SMALL', name: '소형', size: 'SMALL' },
  { code: 'MID', name: '준중형·중형', size: 'MID' },
  { code: 'LARGE', name: '대형·SUV', size: 'LARGE' },
  { code: 'VAN_ETC', name: '승합·기타', size: 'XLARGE' }, // 2차 Q5: 특대형 베이 수용
]
