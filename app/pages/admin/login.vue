<script setup lang="ts">
// 관리자 로그인 (require v1.9) — ADMIN 전용 진입. 관리자 회원가입은 두지 않는다(시드/내부 생성).
import { ref } from 'vue'

definePageMeta({ middleware: ['guest'] })

const auth = useAuthStore()
const route = useRoute()

const email = ref('')
const password = ref('')
const error = ref('')

async function onSubmit() {
  error.value = ''
  if (!email.value || !password.value) {
    error.value = '이메일과 비밀번호를 입력하세요'
    return
  }
  const emailOk = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value)
  if (!emailOk) {
    error.value = '이메일 형식이 올바르지 않습니다'
    return
  }
  if (!(await auth.login(email.value, password.value))) {
    error.value = '로그인할 수 없습니다 — 계정/비밀번호를 확인하세요'
    return
  }
  const redirect = route.query.redirect
  navigateTo(typeof redirect === 'string' ? redirect : roleHome(auth.currentUser?.role))
}

// 개발용 빠른 로그인 — 관리자 시드 계정(dev 전용)
const isDev = import.meta.dev

async function quickLogin() {
  error.value = ''
  email.value = 'admin@test.com'
  password.value = 'password'
  if (!(await auth.login('admin@test.com', 'password'))) {
    error.value = '빠른 로그인 실패 — 백엔드(:8080) 기동 여부를 확인하세요'
    return
  }
  navigateTo(roleHome(auth.currentUser?.role))
}
</script>

<template>
  <section data-testid="page-admin-login" class="mx-auto max-w-md">
    <div class="mb-8 text-center">
      <span class="badge-accent mb-4">관리자 · 백오피스</span>
      <h1 class="text-3xl font-bold">관리자 로그인</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">관리자 전용 로그인입니다.</p>
    </div>

    <div class="card p-6 sm:p-8">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <div>
          <label for="login-email" class="field-label">이메일</label>
          <input
            id="login-email"
            v-model="email"
            data-testid="login-email"
            type="email"
            placeholder="admin@test.com"
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

        <p
          v-if="error"
          data-testid="login-error"
          class="flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300"
        >
          <span aria-hidden="true">!</span>
          <span>{{ error }}</span>
        </p>

        <button data-testid="login-submit" type="submit" class="btn btn-primary w-full">로그인</button>
      </form>

      <p class="mt-5 text-center text-sm text-[--color-content-muted]">
        <NuxtLink data-testid="link-user-login" to="/login" class="font-medium text-[--color-brand-primary]">
          일반 사용자 로그인
        </NuxtLink>
        ·
        <NuxtLink data-testid="link-manager-login" to="/manager/login" class="font-medium text-[--color-brand-primary]">
          매니저 로그인
        </NuxtLink>
      </p>

      <!-- 개발용 빠른 로그인 (dev 전용) -->
      <div v-if="isDev" class="mt-6 border-t border-[--color-line-soft] pt-5">
        <p class="mb-3 text-center text-xs text-[--color-content-muted]">개발용 빠른 로그인</p>
        <button
          data-testid="quick-login-admin"
          type="button"
          class="btn btn-ghost w-full text-sm"
          @click="quickLogin"
        >
          관리자
        </button>
      </div>
    </div>
  </section>
</template>
