import { computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '~/types/domain'

// 인증 응답 — BE LoginResponse{token, user}와 일치
interface LoginResponse {
  token: string
  user: User
}

export const useAuthStore = defineStore('auth', () => {
  // JWT는 useCookie('access_token')로 보관 — SSR 미들웨어 가드가 서버/클라이언트 모두에서 읽음 (require 4장)
  const token = useCookie<string | null>('access_token', { default: () => null })
  // 사용자 표시(AppNav 이름 등)·SSR 미들웨어용으로 user도 쿠키 보관
  const currentUser = useCookie<User | null>('auth_user', { default: () => null })
  const isLoggedIn = computed(() => token.value !== null)

  function apiBase(): string {
    return useRuntimeConfig().public.apiBase
  }

  // 실제 로그인 — BE JWT 발급 (2단계 교체, require 4장). 실패 시 false
  async function login(email: string, password: string): Promise<boolean> {
    try {
      const res = await $fetch<LoginResponse>(`${apiBase()}/auth/login`, {
        method: 'POST',
        body: { email, password },
      })
      token.value = res.token
      currentUser.value = res.user
      return true
    } catch {
      return false
    }
  }

  // 일반 사용자(USER) 즉시 가입 — BE 영속 + 자동 로그인(토큰 발급). 중복 이메일 등 실패 시 false
  async function signup(payload: {
    email: string
    password: string
    name: string
  }): Promise<boolean> {
    try {
      const res = await $fetch<LoginResponse>(`${apiBase()}/auth/signup`, {
        method: 'POST',
        body: payload,
      })
      token.value = res.token
      currentUser.value = res.user
      return true
    } catch {
      return false
    }
  }

  // 매니저 회원가입 — 소속 매장 지정, PENDING_APPROVAL_L1로 신청(자동 로그인 없음, require v1.9).
  //   성공해도 승인 전이라 로그인하지 않는다(토큰 미발급). 중복 이메일 등 실패 시 false
  async function signupManager(payload: {
    email: string
    password: string
    name: string
    storeId: string
  }): Promise<boolean> {
    try {
      await $fetch(`${apiBase()}/auth/signup-manager`, {
        method: 'POST',
        body: payload,
      })
      return true
    } catch {
      return false
    }
  }

  function logout() {
    token.value = null
    currentUser.value = null
  }

  return { currentUser, isLoggedIn, login, signup, signupManager, logout }
})
