<script setup lang="ts">
// 회원가입 페이지 (FW1) — 일반/매니저 두 케이스 분기 (require 4.1·4.3, v1.12)
//   · 일반(USER)    : 즉시 가입 + 자동 로그인(현 상태 유지)
//   · 매니저(MANAGER): 소속 매장 추가 입력 → PENDING_APPROVAL_L1 신청(자동 로그인 없음, 2단계 승인 후 ACTIVE)
import { ref } from 'vue'
import { getApprovedStores } from '~/services/storeService'

definePageMeta({ middleware: 'guest' })

const auth = useAuthStore()
const stores = getApprovedStores()

// 가입 유형 — 기본 '일반'(기존 동작 유지)
const type = ref<'user' | 'manager'>('user')

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const storeId = ref('')
const error = ref('')
const done = ref(false) // 매니저 가입 완료(승인 대기) 안내

async function onSubmit() {
  error.value = ''
  const isManager = type.value === 'manager'
  // 필수값 검증(매니저는 소속 매장 포함)
  if (!email.value || !password.value || !passwordConfirm.value || !name.value) {
    error.value = '모든 항목을 입력하세요'
    return
  }
  if (isManager && !storeId.value) {
    error.value = '소속 매장을 선택하세요'
    return
  }
  // 이메일 형식 검증
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

  if (isManager) {
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
    return
  }

  // 일반 가입 — 이메일 중복 시 false
  if (!(await auth.signup({ email: email.value, password: password.value, name: name.value }))) {
    error.value = '이미 가입된 이메일입니다'
    return
  }
  // 즉시 활성(자동 로그인) → 예약 화면으로
  navigateTo('/reserve')
}

// 유형 전환 시 에러 초기화(입력값은 유지)
function setType(t: 'user' | 'manager') {
  type.value = t
  error.value = ''
}
</script>

<template>
  <section data-testid="page-signup" class="mx-auto max-w-md">
    <!-- 헤더: 가입 안내 -->
    <div class="mb-8 text-center">
      <span class="badge-accent mb-4">WASH. 가입</span>
      <h1 class="text-3xl font-bold">세차 예약을 시작하세요</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        {{
          type === 'manager'
            ? '매니저 가입은 승인(매장매니저관리자 1차 → 관리자 2차) 후 로그인할 수 있어요.'
            : '간단한 정보로 회원가입하고 바로 예약할 수 있어요.'
        }}
      </p>
    </div>

    <!-- 매니저 가입 완료(승인 대기) 안내 -->
    <div v-if="done" data-testid="manager-signup-done" class="card p-6 text-center sm:p-8">
      <p class="text-lg font-bold text-[--color-content-strong]">가입 신청이 접수되었습니다</p>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        승인(매장매니저관리자 1차 → 관리자 2차)이 완료되면 로그인할 수 있습니다.
      </p>
      <NuxtLink
        data-testid="link-manager-login"
        to="/manager/login"
        class="btn btn-primary mt-5 inline-block"
      >
        매니저 로그인으로 이동
      </NuxtLink>
    </div>

    <!-- 회원가입 카드 -->
    <div v-else class="card p-6 sm:p-8">
      <!-- 가입 유형 토글 -->
      <div
        role="tablist"
        class="mb-6 flex rounded-xl border border-[--color-line] bg-[--color-surface-2] p-1"
      >
        <button
          data-testid="signup-type-user"
          type="button"
          role="tab"
          class="signup-type-tab"
          :data-active="type === 'user'"
          @click="setType('user')"
        >
          일반 회원가입
        </button>
        <button
          data-testid="signup-type-manager"
          type="button"
          role="tab"
          class="signup-type-tab"
          :data-active="type === 'manager'"
          @click="setType('manager')"
        >
          매니저 회원가입
        </button>
      </div>

      <form class="space-y-5" @submit.prevent="onSubmit">
        <div>
          <label for="signup-name" class="field-label">이름</label>
          <input
            id="signup-name"
            v-model="name"
            data-testid="signup-name"
            type="text"
            :placeholder="type === 'manager' ? '김매니저' : '홍길동'"
            autocomplete="name"
            class="input-field"
          />
        </div>

        <!-- 매니저: 소속 매장 추가 입력 -->
        <div v-if="type === 'manager'">
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

        <!-- 에러 메시지 -->
        <p
          v-if="error"
          data-testid="signup-error"
          class="flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300"
        >
          <span aria-hidden="true">!</span>
          <span>{{ error }}</span>
        </p>

        <button data-testid="signup-submit" type="submit" class="btn btn-primary w-full">
          {{ type === 'manager' ? '매니저 가입 신청' : '회원가입' }}
        </button>
      </form>

      <!-- 로그인 화면으로 상호 이동 -->
      <p class="mt-5 text-center text-sm text-[--color-content-muted]">
        이미 계정이 있으신가요?
        <NuxtLink
          data-testid="link-login"
          :to="type === 'manager' ? '/manager/login' : '/login'"
          class="font-medium text-[--color-brand-primary]"
        >
          로그인
        </NuxtLink>
      </p>
    </div>
  </section>
</template>

<style scoped>
/* 가입 유형 토글 — 활성 탭만 surface로 강조 */
.signup-type-tab {
  flex: 1;
  border-radius: 0.625rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-content-muted);
  transition:
    background-color 0.15s,
    color 0.15s;
}
.signup-type-tab[data-active='true'] {
  background-color: var(--color-surface);
  color: var(--color-content-strong);
  box-shadow: 0 1px 2px rgb(0 0 0 / 0.06);
}
</style>
