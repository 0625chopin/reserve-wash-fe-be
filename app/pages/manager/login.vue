<script setup lang="ts">
// 매니저 로그인 (require v1.9) — 일반매장매니저·매장매니저관리자 공용 진입.
//   ACTIVE 계정만 로그인 가능(승인 대기 매니저는 거부). 로그인 후 역할군별 기본 화면으로 이동.
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
    error.value = '로그인할 수 없습니다 — 계정/비밀번호 또는 승인 대기 상태를 확인하세요'
    return
  }
  const redirect = route.query.redirect
  navigateTo(typeof redirect === 'string' ? redirect : roleHome(auth.currentUser?.role))
}

// 개발용 빠른 로그인 — 매니저 계열 시드 계정(dev 전용)
const isDev = import.meta.dev
const quickAccounts = [
  { key: 'manager', label: '일반매장매니저', email: 'manager@test.com' },
  { key: 'storeadmin', label: '매장매니저관리자', email: 'storeadmin@test.com' },
] as const

async function quickLogin(loginEmail: string) {
  error.value = ''
  email.value = loginEmail
  password.value = 'password'
  if (!(await auth.login(loginEmail, 'password'))) {
    error.value = '빠른 로그인 실패 — 백엔드(:8080) 기동 여부를 확인하세요'
    return
  }
  navigateTo(roleHome(auth.currentUser?.role))
}
</script>

<template>
  <section data-testid="page-manager-login" class="mx-auto max-w-md">
    <div class="mb-8 text-center">
      <span class="badge-accent mb-4">매니저 · 백오피스</span>
      <h1 class="text-3xl font-bold">매니저 로그인</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        일반매장매니저·매장매니저관리자 전용 로그인입니다.
      </p>
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
            placeholder="manager@test.com"
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

      <!-- 매니저 회원가입 / 다른 로그인 이동 -->
      <p class="mt-5 text-center text-sm text-[--color-content-muted]">
        매니저 계정이 없으신가요?
        <NuxtLink
          data-testid="link-manager-signup"
          to="/manager/signup"
          class="font-medium text-[--color-brand-primary]"
        >
          매니저 회원가입
        </NuxtLink>
      </p>
      <p class="mt-2 text-center text-sm text-[--color-content-muted]">
        <NuxtLink data-testid="link-user-login" to="/login" class="font-medium text-[--color-brand-primary]">
          일반 사용자 로그인
        </NuxtLink>
        ·
        <NuxtLink data-testid="link-admin-login" to="/admin/login" class="font-medium text-[--color-brand-primary]">
          관리자 로그인
        </NuxtLink>
      </p>

      <!-- 개발용 빠른 로그인 (dev 전용) -->
      <div v-if="isDev" class="mt-6 border-t border-[--color-line-soft] pt-5">
        <p class="mb-3 text-center text-xs text-[--color-content-muted]">개발용 빠른 로그인</p>
        <div class="grid grid-cols-2 gap-2">
          <button
            v-for="acc in quickAccounts"
            :key="acc.key"
            :data-testid="`quick-login-${acc.key}`"
            type="button"
            class="btn btn-ghost text-sm"
            @click="quickLogin(acc.email)"
          >
            {{ acc.label }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
