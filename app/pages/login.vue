<script setup lang="ts">
// 로그인 페이지 (FW2) — 더미 로그인 + 폼 검증 (require 4장)
import { ref } from 'vue'

const auth = useAuthStore()
const route = useRoute()

const email = ref('')
const password = ref('')
const error = ref('')

function onSubmit() {
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
  if (!auth.login(email.value, password.value)) {
    error.value = '이메일 또는 비밀번호가 올바르지 않습니다'
    return
  }
  // 성공 — redirect 쿼리가 있으면 그곳, 없으면 /reserve
  const redirect = route.query.redirect
  navigateTo(typeof redirect === 'string' ? redirect : '/reserve')
}
</script>

<template>
  <section data-testid="page-login">
    <h1>로그인</h1>
    <form @submit.prevent="onSubmit">
      <input
        v-model="email"
        data-testid="login-email"
        type="email"
        placeholder="이메일"
        autocomplete="username"
      />
      <input
        v-model="password"
        data-testid="login-password"
        type="password"
        placeholder="비밀번호"
        autocomplete="current-password"
      />
      <button data-testid="login-submit" type="submit">로그인</button>
    </form>
    <p v-if="error" data-testid="login-error">{{ error }}</p>
  </section>
</template>
