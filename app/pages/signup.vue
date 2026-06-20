<script setup lang="ts">
// 회원가입 페이지 (FW1, 일반 사용자) — 즉시 가입(더미) + 폼 검증 (require 4.1·4.3)
import { ref } from 'vue'

definePageMeta({ middleware: 'guest' })

const auth = useAuthStore()

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const error = ref('')

async function onSubmit() {
  error.value = ''
  // 필수값 검증
  if (!email.value || !password.value || !passwordConfirm.value || !name.value) {
    error.value = '모든 항목을 입력하세요'
    return
  }
  // 이메일 형식 검증(login.vue와 동일 정규식)
  const emailOk = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value)
  if (!emailOk) {
    error.value = '이메일 형식이 올바르지 않습니다'
    return
  }
  // 비밀번호 일치 검증
  if (password.value !== passwordConfirm.value) {
    error.value = '비밀번호가 일치하지 않습니다'
    return
  }
  // 이메일 중복 → signup이 false 반환
  if (!(await auth.signup({ email: email.value, password: password.value, name: name.value }))) {
    error.value = '이미 가입된 이메일입니다'
    return
  }
  // 즉시 활성(자동 로그인) → 예약 화면으로
  navigateTo('/reserve')
}
</script>

<template>
  <section data-testid="page-signup" class="mx-auto max-w-md">
    <!-- 헤더: 가입 안내 -->
    <div class="mb-8 text-center">
      <span class="badge-accent mb-4">WASH. 가입</span>
      <h1 class="text-3xl font-bold">세차 예약을 시작하세요</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        간단한 정보로 회원가입하고 바로 예약할 수 있어요.
      </p>
    </div>

    <!-- 회원가입 카드 -->
    <div class="card p-6 sm:p-8">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <div>
          <label for="signup-name" class="field-label">이름</label>
          <input
            id="signup-name"
            v-model="name"
            data-testid="signup-name"
            type="text"
            placeholder="홍길동"
            autocomplete="name"
            class="input-field"
          />
        </div>

        <div>
          <label for="signup-email" class="field-label">이메일</label>
          <input
            id="signup-email"
            v-model="email"
            data-testid="signup-email"
            type="email"
            placeholder="you@example.com"
            autocomplete="username"
            class="input-field"
          />
        </div>

        <div>
          <label for="signup-password" class="field-label">비밀번호</label>
          <input
            id="signup-password"
            v-model="password"
            data-testid="signup-password"
            type="password"
            placeholder="••••••••"
            autocomplete="new-password"
            class="input-field"
          />
        </div>

        <div>
          <label for="signup-password-confirm" class="field-label">비밀번호 확인</label>
          <input
            id="signup-password-confirm"
            v-model="passwordConfirm"
            data-testid="signup-password-confirm"
            type="password"
            placeholder="••••••••"
            autocomplete="new-password"
            class="input-field"
          />
        </div>

        <!-- 에러 메시지 — login.vue와 동일 톤 -->
        <p
          v-if="error"
          data-testid="signup-error"
          class="flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300"
        >
          <span aria-hidden="true">!</span>
          <span>{{ error }}</span>
        </p>

        <button data-testid="signup-submit" type="submit" class="btn btn-primary w-full">
          회원가입
        </button>
      </form>

      <!-- 로그인 화면으로 상호 이동 -->
      <p class="mt-5 text-center text-sm text-[--color-content-muted]">
        이미 계정이 있으신가요?
        <NuxtLink
          data-testid="link-login"
          to="/login"
          class="font-medium text-[--color-brand-primary]"
        >
          로그인
        </NuxtLink>
      </p>
    </div>
  </section>
</template>
