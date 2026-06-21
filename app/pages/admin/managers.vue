<script setup lang="ts">
// BO 관리자 — 매니저 직접 등록 (require v1.12 §4.1). ADMIN 전용.
//   role(일반매니저=MANAGER / 매장관리매니저=STORE_ADMIN) 선택 + 소속 매장 지정.
//   생성 시 PENDING_APPROVAL_L2 → '가입 최종 승인'에서 관리자 최종 승인해야 활성화(로그인 가능).
import { ref } from 'vue'
import { getApprovedStores } from '~/services/storeService'
import type { UserRole } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

const stores = getApprovedStores()

const name = ref('')
const email = ref('')
const password = ref('')
const storeId = ref('')
const role = ref<Extract<UserRole, 'MANAGER' | 'STORE_ADMIN'>>('MANAGER')
const error = ref('')
const message = ref('')

async function onSubmit() {
  error.value = ''
  message.value = ''
  if (!name.value || !email.value || !password.value || !storeId.value) {
    error.value = '모든 항목을 입력하세요(소속 매장 포함)'
    return
  }
  const emailOk = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value)
  if (!emailOk) {
    error.value = '이메일 형식이 올바르지 않습니다'
    return
  }
  const { $apiFetch } = useNuxtApp()
  const base = useRuntimeConfig().public.apiBase
  try {
    await $apiFetch(`${base}/admin/managers`, {
      method: 'POST',
      body: {
        name: name.value,
        email: email.value,
        password: password.value,
        storeId: storeId.value,
        role: role.value,
      },
    })
    message.value = '등록되었습니다. 2차 최종 승인(가입 최종 승인)을 거쳐야 활성화됩니다.'
    // 입력 초기화(역할/매장은 연속 등록 편의를 위해 유지)
    name.value = ''
    email.value = ''
    password.value = ''
  } catch {
    error.value = '등록에 실패했습니다(이미 가입된 이메일이거나 입력값을 확인하세요).'
  }
}
</script>

<template>
  <section data-testid="page-admin-managers" class="mx-auto max-w-md">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매니저 등록</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        매니저를 직접 등록합니다. 역할(일반매니저/매장관리매니저)을 선택하며, 등록 후
        <strong>2차 최종 승인</strong>을 거쳐야 로그인할 수 있습니다.
      </p>
    </header>

    <div class="card p-6 sm:p-8">
      <form class="space-y-5" @submit.prevent="onSubmit">
        <div>
          <label for="admin-mgr-role" class="field-label">역할</label>
          <select
            id="admin-mgr-role"
            v-model="role"
            data-testid="admin-mgr-role"
            class="admin-mgr-input"
          >
            <option value="MANAGER">일반매니저</option>
            <option value="STORE_ADMIN">매장관리매니저</option>
          </select>
        </div>

        <div>
          <label for="admin-mgr-store" class="field-label">소속 매장</label>
          <select
            id="admin-mgr-store"
            v-model="storeId"
            data-testid="admin-mgr-store"
            class="admin-mgr-input"
          >
            <option value="" disabled>선택</option>
            <option v-for="s in stores" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>

        <div>
          <label for="admin-mgr-name" class="field-label">이름</label>
          <input
            id="admin-mgr-name"
            v-model="name"
            data-testid="admin-mgr-name"
            type="text"
            placeholder="김매니저"
            class="admin-mgr-input"
          />
        </div>

        <div>
          <label for="admin-mgr-email" class="field-label">이메일</label>
          <input
            id="admin-mgr-email"
            v-model="email"
            data-testid="admin-mgr-email"
            type="email"
            placeholder="manager@example.com"
            class="admin-mgr-input"
          />
        </div>

        <div>
          <label for="admin-mgr-password" class="field-label">초기 비밀번호</label>
          <input
            id="admin-mgr-password"
            v-model="password"
            data-testid="admin-mgr-password"
            type="password"
            placeholder="••••••••"
            class="admin-mgr-input"
          />
        </div>

        <p
          v-if="error"
          data-testid="admin-mgr-error"
          class="flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300"
        >
          <span aria-hidden="true">!</span>
          <span>{{ error }}</span>
        </p>

        <button data-testid="admin-mgr-submit" type="submit" class="btn btn-primary w-full">
          매니저 등록
        </button>
      </form>

      <p
        v-if="message"
        data-testid="admin-mgr-message"
        class="mt-4 text-sm text-[--color-brand-accent]"
      >
        {{ message }}
      </p>

      <p class="mt-5 text-center text-sm text-[--color-content-muted]">
        등록 후
        <NuxtLink to="/admin/manager-approvals" class="font-medium text-[--color-brand-primary]">
          가입 최종 승인
        </NuxtLink>
        에서 활성화하세요.
      </p>
    </div>
  </section>
</template>

<style scoped>
/* BO 입력 — 기존 토큰 재사용, 다크 테마 네이티브 select 가독성 확보 */
.admin-mgr-input {
  margin-top: 0.375rem;
  width: 100%;
  border-radius: 0.625rem;
  border: 1px solid var(--color-line);
  background-color: var(--color-surface-1);
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-content-strong);
}
.admin-mgr-input:focus {
  outline: 2px solid color-mix(in oklab, var(--color-brand-primary) 45%, transparent);
  outline-offset: 1px;
}
.admin-mgr-input option {
  background-color: var(--color-surface-1);
  color: var(--color-content-strong);
}
</style>
