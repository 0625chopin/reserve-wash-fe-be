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
    // ⚠ 재할당(stores.value = s)이 아니라 제자리(splice) 교체.
    //   getApprovedStores() 등은 이 배열 '참조'를 컴포넌트 setup에서 캡처하므로, 재할당하면 캐시 갱신이
    //   이미 마운트된 화면에 반영되지 않는다. 같은 반응형 배열을 in-place로 갈아끼우면 reloadCatalog 시
    //   전체 새로고침 없이도 열려 있는 화면이 즉시 갱신된다.
    replaceInPlace(stores.value, s)
    replaceInPlace(managers.value, m)
    replaceInPlace(bays.value, b)
    replaceInPlace(prices.value, p)
    loaded.value = true
  } catch (e) {
    console.warn('[catalog] 카탈로그 로드 실패 — 백엔드(:8080) 기동 여부를 확인하세요', e)
  }
}

// 반응형 배열을 참조 유지한 채 내용만 교체(splice) — 캡처된 참조의 반응성 보존
function replaceInPlace<T>(target: T[], next: T[]): void {
  target.splice(0, target.length, ...next)
}

// 카탈로그 강제 재하이드레이트 — 관리자 매장 CRUD(adminStoreService) 등 카탈로그를 바꾸는 작업 직후 호출.
//   loaded 가드를 해제하고 4 엔드포인트를 다시 fetch(제자리 교체)하여, 전체 새로고침 없이도 신규/수정/삭제
//   매장이 이미 열린 화면(예약 위저드·BO 매장 select 등)에까지 반영되게 한다.
export async function reloadCatalog(): Promise<void> {
  loadedState().value = false
  await loadCatalog()
}
