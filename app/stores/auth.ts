import { computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '~/types/domain'
import { credentials, users } from '~/data/users'

export const useAuthStore = defineStore('auth', () => {
  // useCookie로 영속화 — SSR 미들웨어 가드가 서버/클라이언트 모두에서 인증 상태를 읽도록 (require 4장)
  const currentUser = useCookie<User | null>('auth_user', { default: () => null })
  const isLoggedIn = computed(() => currentUser.value !== null)

  // 더미 로그인 — 이메일 조회 후 비밀번호 검증 (1단계, require 4장)
  // 시드 사용자는 통일 더미 비번 'password', 가입 사용자는 credentials 맵의 비번을 사용한다.
  // TODO(2단계): authService($fetch) 호출로 교체
  function login(email: string, password: string): boolean {
    const found = users.find((u) => u.email === email)
    if (!found) {
      return false
    }
    const expected = credentials[email] ?? 'password'
    if (password === expected) {
      currentUser.value = found
      return true
    }
    return false
  }

  // 일반 사용자(USER) 즉시 가입 — 이메일 중복 검사 → in-memory 등록 → 자동 로그인 (1단계, require 4.3)
  // TODO(2단계: authService/$fetch): SMTP 이메일 인증·승인 분기(require 4.4)로 교체
  function signup(payload: { email: string; password: string; name: string }): boolean {
    // 이메일 중복 검사 — 이미 가입된 이메일이면 실패
    const exists = users.some((u) => u.email === payload.email)
    if (exists) {
      return false
    }
    // in-memory users 등록 (role 'USER' 고정, 승인 불필요 → 즉시 활성)
    const created: User = {
      id: `user${users.length + 1}`,
      email: payload.email,
      name: payload.name,
      role: 'USER',
    }
    users.push(created)
    // 가입 비밀번호를 보관해 로그아웃 후 재로그인이 가능하도록 한다
    credentials[payload.email] = payload.password
    // 즉시 활성 — 자동 로그인
    currentUser.value = created
    return true
  }

  function logout() {
    currentUser.value = null
  }

  return { currentUser, isLoggedIn, login, signup, logout }
})
