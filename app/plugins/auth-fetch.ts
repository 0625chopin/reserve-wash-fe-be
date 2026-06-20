// 인증 헤더 주입 fetch 기반 (Phase 3) — 보호 API 호출 시 Authorization: Bearer 주입.
// Phase 3에선 login/signup이 무인증 호출이라 실사용은 없고, Phase 4+ 보호 API가 useNuxtApp().$apiFetch로 소비한다.
export default defineNuxtPlugin(() => {
  const apiFetch = $fetch.create({
    onRequest({ options }) {
      const token = useCookie<string | null>('access_token').value
      if (token) {
        const headers = new Headers(options.headers as HeadersInit | undefined)
        headers.set('Authorization', `Bearer ${token}`)
        options.headers = headers
      }
    },
  })

  return {
    provide: { apiFetch },
  }
})
