<script setup lang="ts">
// 매니저 회원가입 (M1, require v1.9 §4.1) — 소속 매장 선택 + role=MANAGER 신청.
//   가입 즉시 PENDING_APPROVAL_L1로 등록(자동 로그인 없음) → 2단계 승인(M7→S3) 후 ACTIVE.
import { ref } from 'vue'
import { getApprovedStores } from '~/services/storeService'

definePageMeta({ middleware: 'guest' })

const auth = useAuthStore()
const stores = getApprovedStores()

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const storeId = ref('')
const error = ref('')
const done = ref(false)

async function onSubmit() {
  error.value = ''
  // 필수값 검증(소속 매장 포함)
  if (!email.value || !password.value || !passwordConfirm.value || !name.value || !storeId.value) {
    error.value = '모든 항목을 입력하세요(소속 매장 포함)'
    return
  }
  const emailOk = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value)
  if (!emailOk) {
    error.value = '이메일 형식이 올바르지 않습니다'
    return
  }
  if (password.value !== passwordConfirm.value) {
    error.value = '비밀번호가 일치하지 않습니다'
    return
  }
  // 매니저 가입 — 성공해도 승인 대기라 자동 로그인하지 않는다
  const ok = await auth.signupManager({
    email: email.value,
    password: password.value,
    name: name.value,
    storeId: storeId.value,
  })
  if (!ok) {
    error.value = '이미 가입된 이메일이거나 가입에 실패했습니다'
    return
  }
  done.value = true
}
</script>

<template>
  <section data-testid="page-manager-signup" class="mx-auto max-w-md">
    <div class="mb-8 text-center">
      <span class="badge-accent mb-4">매니저 · 가입</span>
      <h1 class="text-3xl font-bold">매니저 회원가입</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        가입 신청 후 승인(매장매니저관리자 1차 → 관리자 2차)을 거쳐야 로그인할 수 있어요.
      </p>
    </div>

    <!-- 가입 완료(승인 대기) 안내 -->
    <div v-if="done" data-testid="manager-signup-done" class="card p-6 text-center sm:p-8">
      <p class="text-lg font-bold text-[--color-content-strong]">가입 신청이 접수되었습니다</p>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        승인(매장매니저관리자 1차 → 관리자 2차)이 완료되면 로그인할 수 있습니다.
      </p>
      <NuxtLink data-testid="link-manager-login" to="/manager/login" class="btn btn-primary mt-5 inline-block">
        매니저 로그인으로 이동
      </NuxtLink>
    </div>

    <!-- 가입 폼 -->
    <div v-else class="card p-6 sm:p-8">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <div>
          <label for="signup-name" class="field-label">이름</label>
          <input
            id="signup-name"
            v-model="name"
            data-testid="signup-name"
            type="text"
            placeholder="김매니저"
            autocomplete="name"
            class="input-field"
          />
        </div>

        <div>
          <label for="signup-store" class="field-label">소속 매장</label>
          <select id="signup-store" v-model="storeId" data-testid="signup-store" class="input-field">
            <option value="" disabled>선택</option>
            <option v-for="s in stores" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
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

        <p
          v-if="error"
          data-testid="signup-error"
          class="flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300"
        >
          <span aria-hidden="true">!</span>
          <span>{{ error }}</span>
        </p>

        <button data-testid="signup-submit" type="submit" class="btn btn-primary w-full">
          매니저 가입 신청
        </button>
      </form>

      <p class="mt-5 text-center text-sm text-[--color-content-muted]">
        이미 매니저 계정이 있으신가요?
        <NuxtLink
          data-testid="link-manager-login"
          to="/manager/login"
          class="font-medium text-[--color-brand-primary]"
        >
          매니저 로그인
        </NuxtLink>
      </p>
    </div>
  </section>
</template>
