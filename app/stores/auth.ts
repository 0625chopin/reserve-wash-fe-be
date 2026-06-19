import { computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '~/types/domain'
import { users } from '~/data/users'

export const useAuthStore = defineStore('auth', () => {
  // useCookie로 영속화 — SSR 미들웨어 가드가 서버/클라이언트 모두에서 인증 상태를 읽도록 (require 4장)
  const currentUser = useCookie<User | null>('auth_user', { default: () => null })
  const isLoggedIn = computed(() => currentUser.value !== null)

  // 더미 로그인 — 이메일 조회 + 비밀번호 'password' 통일 가정 (1단계, require 4장)
  // TODO(2단계): authService($fetch) 호출로 교체
  function login(email: string, password: string): boolean {
    const found = users.find((u) => u.email === email)
    if (found && password === 'password') {
      currentUser.value = found
      return true
    }
    return false
  }

  function logout() {
    currentUser.value = null
  }

  return { currentUser, isLoggedIn, login, logout }
})
