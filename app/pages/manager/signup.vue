<script setup lang="ts">
// 매니저 회원가입 (M1, require v1.9 §4.1) — 소속 매장 선택 + role=MANAGER + 이메일 인증(6자리 코드, 3분).
//   1단계 정보 입력 → 코드 요청, 2단계 코드 검증 → PENDING_APPROVAL_L1 등록(자동 로그인 없음) → 승인 후 ACTIVE.
import { computed, onUnmounted, ref } from 'vue'
import { getApprovedStores } from '~/services/storeService'

definePageMeta({ middleware: 'guest' })

const auth = useAuthStore()
const stores = getApprovedStores()

const step = ref<'form' | 'code'>('form')
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const storeId = ref('')
const code = ref('')
const error = ref('')
const done = ref(false)
const submitting = ref(false)

// 3:00 카운트다운
const remaining = ref(0)
let timer: ReturnType<typeof setInterval> | null = null
function stopCountdown() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}
function startCountdown(sec: number) {
  stopCountdown()
  remaining.value = sec
  timer = setInterval(() => {
    remaining.value = Math.max(0, remaining.value - 1)
    if (remaining.value === 0) stopCountdown()
  }, 1000)
}
onUnmounted(stopCountdown)

const countdown = computed(() => {
  const m = Math.floor(remaining.value / 60)
  const s = remaining.value % 60
  return `${m}:${String(s).padStart(2, '0')}`
})
const expired = computed(() => remaining.value === 0)

async function onRequestCode() {
  error.value = ''
  if (!email.value || !password.value || !passwordConfirm.value || !name.value || !storeId.value) {
    error.value = '모든 항목을 입력하세요(소속 매장 포함)'
    return
  }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value)) {
    error.value = '이메일 형식이 올바르지 않습니다'
    return
  }
  if (password.value !== passwordConfirm.value) {
    error.value = '비밀번호가 일치하지 않습니다'
    return
  }
  submitting.value = true
  const res = await auth.requestSignupCode({
    email: email.value,
    password: password.value,
    name: name.value,
    role: 'MANAGER',
    storeId: storeId.value,
  })
  submitting.value = false
  if (!res.ok) {
    error.value = res.status === 409 ? '이미 가입된 이메일입니다' : (res.error ?? '코드 요청에 실패했습니다')
    return
  }
  code.value = ''
  step.value = 'code'
  startCountdown(res.expiresInSec ?? 180)
}

async function onVerify() {
  error.value = ''
  if (!/^\d{6}$/.test(code.value)) {
    error.value = '6자리 숫자 코드를 입력하세요'
    return
  }
  submitting.value = true
  const res = await auth.verifySignupCode(email.value, code.value)
  submitting.value = false
  if (!res.ok) {
    if (res.status === 410) error.value = '인증 시간이 만료되었습니다. 재전송해 주세요'
    else if (res.status === 429) error.value = '시도 횟수를 초과했습니다. 재전송해 주세요'
    else if (res.status === 404) error.value = '인증 요청이 없습니다. 다시 시도해 주세요'
    else error.value = '인증 코드가 일치하지 않습니다'
    return
  }
  stopCountdown()
  done.value = true // 매니저 — 승인 대기 안내
}

async function onResend() {
  error.value = ''
  const res = await auth.resendSignupCode(email.value)
  if (!res.ok) {
    error.value = res.error ?? '재전송에 실패했습니다'
    return
  }
  code.value = ''
  startCountdown(res.expiresInSec ?? 180)
}

function backToForm() {
  stopCountdown()
  step.value = 'form'
  error.value = ''
}
</script>

<template>
  <section data-testid="page-manager-signup" class="mx-auto max-w-md">
    <div class="mb-8 text-center">
      <span class="badge-accent mb-4">매니저 · 가입</span>
      <h1 class="text-3xl font-bold">매니저 회원가입</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        이메일 인증 후 승인(매장매니저관리자 1차 → 관리자 2차)을 거쳐야 로그인할 수 있어요.
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

    <div v-else class="card p-6 sm:p-8">
      <!-- 1단계: 정보 입력 -->
      <form v-if="step === 'form'" class="space-y-5" @submit.prevent="onRequestCode">
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

        <button
          data-testid="signup-submit"
          type="submit"
          class="btn btn-primary w-full"
          :disabled="submitting"
        >
          {{ submitting ? '인증 코드 발송 중…' : '인증 코드 받기' }}
        </button>
      </form>

      <!-- 2단계: 코드 입력 -->
      <template v-else>
        <p class="mb-1 text-sm text-[--color-content-muted]">
          <span class="font-medium text-[--color-content-strong]">{{ email }}</span> 으로
          6자리 인증 코드를 보냈어요.
        </p>
        <p class="mb-5 text-xs text-[--color-content-muted]">
          남은 시간
          <span data-testid="signup-countdown" class="font-bold text-[--color-brand-primary]">
            {{ countdown }}
          </span>
        </p>

        <form class="space-y-5" @submit.prevent="onVerify">
          <div>
            <label for="signup-code" class="field-label">인증 코드</label>
            <input
              id="signup-code"
              v-model="code"
              data-testid="signup-code"
              inputmode="numeric"
              maxlength="6"
              placeholder="------"
              autocomplete="one-time-code"
              class="input-field text-center text-lg tracking-[0.5em]"
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

          <button
            data-testid="signup-verify"
            type="submit"
            class="btn btn-primary w-full"
            :disabled="submitting"
          >
            {{ submitting ? '확인 중…' : '인증하고 가입 신청' }}
          </button>
        </form>

        <div class="mt-4 flex items-center justify-between text-sm">
          <button
            data-testid="signup-back"
            type="button"
            class="text-[--color-content-muted] hover:underline"
            @click="backToForm"
          >
            ← 정보 수정
          </button>
          <button
            data-testid="signup-resend"
            type="button"
            class="font-medium text-[--color-brand-primary] disabled:opacity-40"
            :disabled="!expired"
            @click="onResend"
          >
            코드 재전송
          </button>
        </div>
      </template>

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
