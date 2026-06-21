import type { Bay, Manager, Price, Store } from '~/types/domain'

// 서버 하이드레이트 카탈로그 캐시 (2단계 교체 핵심)
// 부팅 시 1회 비동기 로드(loadCatalog) → 이후 storeService/priceService가 동기 읽기.
// SSR payload 직렬화를 위해 Nuxt useState 사용(SSR에서 채우고 client는 hydration 복원, 재요청 없음).
// 단방향 의존 유지: 본 모듈은 services 내부에 두어 services→stores 역의존을 만들지 않는다(README 계약).
//   (useState/useRuntimeConfig/$fetch 는 Nuxt 자동 임포트 — import 구문 작성하지 않음)

function storesState() {
  return useState<Store[]>('catalog:stores', () => [])
}
function managersState() {
  return useState<Manager[]>('catalog:managers', () => [])
}
function baysState() {
  return useState<Bay[]>('catalog:bays', () => [])
}
function pricesState() {
  return useState<Price[]>('catalog:prices', () => [])
}
function loadedState() {
  return useState<boolean>('catalog:loaded', () => false)
}

// 동기 읽기 접근자 — 호출부(서비스)는 이 값을 그대로 동기 사용
export function catalogStores(): Store[] {
  return storesState().value
}
export function catalogManagers(): Manager[] {
  return managersState().value
}
export function catalogBays(): Bay[] {
  return baysState().value
}
export function catalogPrices(): Price[] {
  return pricesState().value
}

// 부팅 1회 로드 — 4 엔드포인트 병렬 $fetch. loaded 가드로 idempotent.
// 실패 시 빈 배열 유지·loaded=false(더미 silent fallback 금지 — 회귀 진실성).
export async function loadCatalog(): Promise<void> {
  const loaded = loadedState()
  if (loaded.value) return
  // Nuxt 컴포저블(useState/useRuntimeConfig)은 반드시 await 이전에 호출한다.
  //   await 경계를 넘으면 Nuxt 인스턴스 컨텍스트가 유실되어 "composable ... outside of a plugin"
  //   에러가 난다(특히 BE 다운으로 $fetch가 실패할 때). state 참조를 미리 확보하고, await 이후에는
  //   .value 할당만 수행한다.
  const stores = storesState()
  const managers = managersState()
  const bays = baysState()
  const prices = pricesState()
  const base = useRuntimeConfig().public.apiBase
  try {
    const [s, m, b, p] = await Promise.all([
      $fetch<Store[]>(`${base}/stores`),
      $fetch<Manager[]>(`${base}/managers`),
      $fetch<Bay[]>(`${base}/bays`),
      $fetch<Price[]>(`${base}/prices`),
    ])
    stores.value = s
    managers.value = m
    bays.value = b
    prices.value = p
    loaded.value = true
  } catch (e) {
    console.warn('[catalog] 카탈로그 로드 실패 — 백엔드(:8080) 기동 여부를 확인하세요', e)
  }
}
