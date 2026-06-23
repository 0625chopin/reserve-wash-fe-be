import type { BaySize } from '~/types/enums'
import { reloadCatalog } from '~/services/catalogCache'

// 관리자 매장 CRUD API 래퍼 (v2.4) — 보호 API라 useNuxtApp().$apiFetch(Authorization 자동 주입) 사용.
//   1차(ROADMAP_1 Phase 9) in-memory adminStore를 본 서버 호출로 교체(시그니처 동기→Promise, additive).
//   매장 변경(생성/수정/삭제) 직후 reloadCatalog()로 카탈로그 캐시를 갱신하여, 전체 새로고침 없이도
//   예약 화면·BO 매장 select에 반영되게 한다(부팅 1회 로드 가드를 mutation 시점에만 해제).

// BE AdminStoreResponse / AdminStoreRequest와 무변환 일치
export interface AdminBay {
  id: string
  storeId: string
  code: string
  size: BaySize
}
export interface AdminStore {
  id: string
  name: string
  bayCount: number
  approved: boolean
  bays: AdminBay[]
}
export interface AdminStorePayload {
  name: string
  bayCount: number
  approved: boolean
  bays: { code: string; size: BaySize }[]
}

function base() {
  return useRuntimeConfig().public.apiBase
}

// 관리자용 전체 목록(승인/미승인 포함)
export function listAdminStores(): Promise<AdminStore[]> {
  const { $apiFetch } = useNuxtApp()
  return $apiFetch<AdminStore[]>(`${base()}/admin/stores`)
}

export function getAdminStore(id: string): Promise<AdminStore> {
  const { $apiFetch } = useNuxtApp()
  return $apiFetch<AdminStore>(`${base()}/admin/stores/${id}`)
}

export async function createAdminStore(payload: AdminStorePayload): Promise<AdminStore> {
  const nuxtApp = useNuxtApp()
  const created = await nuxtApp.$apiFetch<AdminStore>(`${base()}/admin/stores`, {
    method: 'POST',
    body: payload,
  })
  // await 이후라 Nuxt 컨텍스트가 유실될 수 있어 runWithContext로 복원해 카탈로그 재로드
  await nuxtApp.runWithContext(() => reloadCatalog()) // 신규 매장(+베이) 즉시 반영
  return created
}

export async function updateAdminStore(
  id: string,
  payload: AdminStorePayload,
): Promise<AdminStore> {
  const nuxtApp = useNuxtApp()
  const updated = await nuxtApp.$apiFetch<AdminStore>(`${base()}/admin/stores/${id}`, {
    method: 'PUT',
    body: payload,
  })
  await nuxtApp.runWithContext(() => reloadCatalog()) // 변경(이름·승인·베이) 반영
  return updated
}

export async function deleteAdminStore(id: string): Promise<void> {
  const nuxtApp = useNuxtApp()
  await nuxtApp.$apiFetch<void>(`${base()}/admin/stores/${id}`, { method: 'DELETE' })
  await nuxtApp.runWithContext(() => reloadCatalog()) // 삭제 매장을 카탈로그에서 제거
}
