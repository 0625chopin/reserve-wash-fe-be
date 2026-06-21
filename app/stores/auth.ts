import { computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '~/types/domain'

// 인증 응답 — BE LoginResponse{token, user}와 일치
interface LoginResponse {
  token: string
  user: User
}

// 이메일 인증 응답 — BE VerificationResponse{expiresInSec} / VerifyResponse{token,user,pendingApproval}
interface VerificationResponse {
  expiresInSec: number
}
interface VerifyResponse {
  token: string | null
  user: User | null
  pendingApproval: boolean
}
// 가입 플로우 공통 결과 — 페이지가 분기/메시지 처리에 사용
interface SignupStepResult {
  ok: boolean
  expiresInSec?: number
  pendingApproval?: boolean
  status?: number
  error?: string
}

// $fetch 에러에서 상태코드·메시지 추출(ofetch FetchError)
function pickError(e: unknown, fallback: string): { status?: number; error: string } {
  const fe = e as { statusCode?: number; status?: number; data?: { message?: string } }
  const status = fe?.statusCode ?? fe?.status
  return { status, error: fe?.data?.message || fallback }
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

  // ── 이메일 인증 가입 플로우 (6자리 코드, 유효 3분) ─────────────────────

  // 1단계 — 코드 요청. 가입 정보를 보내면 BE가 코드를 메일 발송. 성공 시 expiresInSec(=180) 반환.
  async function requestSignupCode(payload: {
    email: string
    password: string
    name: string
    role: 'USER' | 'MANAGER'
    storeId?: string
  }): Promise<SignupStepResult> {
    try {
      const res = await $fetch<VerificationResponse>(`${apiBase()}/auth/signup/request`, {
        method: 'POST',
        body: payload,
      })
      return { ok: true, expiresInSec: res.expiresInSec }
    } catch (e) {
      const { status, error } = pickError(e, '코드 요청에 실패했습니다')
      return { ok: false, status, error }
    }
  }

  // 2단계 — 코드 검증. USER는 토큰 발급(자동 로그인), 매니저는 pendingApproval=true(승인 대기).
  async function verifySignupCode(email: string, code: string): Promise<SignupStepResult> {
    try {
      const res = await $fetch<VerifyResponse>(`${apiBase()}/auth/signup/verify`, {
        method: 'POST',
        body: { email, code },
      })
      if (res.token && res.user) {
        token.value = res.token
        currentUser.value = res.user
      }
      return { ok: true, pendingApproval: res.pendingApproval }
    } catch (e) {
      const { status, error } = pickError(e, '인증에 실패했습니다')
      return { ok: false, status, error }
    }
  }

  // 코드 재전송 — 새 코드·만료(3분) 갱신
  async function resendSignupCode(email: string): Promise<SignupStepResult> {
    try {
      const res = await $fetch<VerificationResponse>(`${apiBase()}/auth/signup/resend`, {
        method: 'POST',
        body: { email },
      })
      return { ok: true, expiresInSec: res.expiresInSec }
    } catch (e) {
      const { status, error } = pickError(e, '재전송에 실패했습니다')
      return { ok: false, status, error }
    }
  }

  function logout() {
    token.value = null
    currentUser.value = null
  }

  return {
    currentUser,
    isLoggedIn,
    login,
    signup,
    signupManager,
    requestSignupCode,
    verifySignupCode,
    resendSignupCode,
    logout,
  }
})
