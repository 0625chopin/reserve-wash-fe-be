import { loadCatalog } from '~/services/catalogCache'

// 부팅 시 카탈로그(매장/매니저/베이/가격)를 1회 await 로드.
// Nuxt는 async 플러그인을 렌더 이전에 await하므로, 어떤 페이지/스토어 setup(예: reservationDraft가
// setup 최상위에서 getApprovedStores를 동기 호출)보다 먼저 캐시가 채워짐이 보장된다.
export default defineNuxtPlugin(async () => {
  await loadCatalog()
})
