<script setup lang="ts">
// 로그인 페이지 (FW2) — 더미 로그인 + 폼 검증 (require 4장)
import { ref } from 'vue'

const auth = useAuthStore()
const route = useRoute()

const email = ref('')
const password = ref('')
const error = ref('')

async function onSubmit() {
  error.value = ''
  // 필수값 검증
  if (!email.value || !password.value) {
    error.value = '이메일과 비밀번호를 입력하세요'
    return
  }
  // 이메일 형식 검증(단순형)
  const emailOk = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value)
  if (!emailOk) {
    error.value = '이메일 형식이 올바르지 않습니다'
    return
  }
  // 로그인 시도 — 실패 시 계정/비번 구분 없는 통합 문구
  if (!(await auth.login(email.value, password.value))) {
    error.value = '이메일 또는 비밀번호가 올바르지 않습니다'
    return
  }
  // 성공 — redirect 쿼리가 있으면 그곳, 없으면 /reserve
  const redirect = route.query.redirect
  navigateTo(typeof redirect === 'string' ? redirect : '/reserve')
}
</script>

<template>
  <section data-testid="page-login" class="mx-auto max-w-md">
    <!-- 헤더: 환영 문구 + 보조 설명 -->
    <div class="mb-8 text-center">
      <span class="badge-accent mb-4">WASH. 멤버</span>
      <h1 class="text-3xl font-bold">다시 오신 걸 환영합니다</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        계정에 로그인하고 세차 예약을 이어가세요.
      </p>
    </div>

    <!-- 로그인 카드 -->
    <div class="card p-6 sm:p-8">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <div>
          <label for="login-email" class="field-label">이메일</label>
          <input
            id="login-email"
            v-model="email"
            data-testid="login-email"
            type="email"
            placeholder="you@example.com"
            autocomplete="username"
            class="input-field"
          />
        </div>

        <div>
          <label for="login-password" class="field-label">비밀번호</label>
          <input
            id="login-password"
            v-model="password"
            data-testid="login-password"
            type="password"
            placeholder="••••••••"
            autocomplete="current-password"
            class="input-field"
          />
        </div>

        <!-- 에러 메시지 — 시맨틱 레드 톤 -->
        <p
          v-if="error"
          data-testid="login-error"
          class="flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300"
        >
          <span aria-hidden="true">!</span>
          <span>{{ error }}</span>
        </p>

        <button data-testid="login-submit" type="submit" class="btn btn-primary w-full">
          로그인
        </button>
      </form>

      <!-- 회원가입 화면으로 상호 이동 -->
      <p class="mt-5 text-center text-sm text-[--color-content-muted]">
        아직 계정이 없으신가요?
        <NuxtLink
          data-testid="link-signup"
          to="/signup"
          class="font-medium text-[--color-brand-primary]"
        >
          회원가입
        </NuxtLink>
      </p>
    </div>
  </section>
</template>
