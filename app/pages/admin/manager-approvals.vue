<script setup lang="ts">
// BO 관리자 매니저 가입 2차 최종 승인(S3) — require v1.7 §4.4, 상세 모달 v1.13. ADMIN 전용.
//   PENDING_APPROVAL_L2 목록을 행 클릭 시 모달로 상세 표시하고, 모달에서 최종 확정(→ACTIVE)·반려한다.
import { onMounted, ref } from 'vue'
import { getApprovedStores } from '~/services/storeService'
import { reloadCatalog } from '~/services/catalogCache'
import type { UserRole } from '~/types/enums'

definePageMeta({ middleware: ['auth', 'role-guard'], roles: ['ADMIN'] })

// BE ManagerSignupResponse와 일치(storeId 포함, v1.13)
interface ManagerSignup {
  id: string
  email: string
  name: string
  role: UserRole
  approvalStatus: string
  storeId: string
}

const rows = ref<ManagerSignup[]>([])
const message = ref('')
const selected = ref<ManagerSignup | null>(null) // 상세 모달 대상(없으면 닫힘)

const stores = getApprovedStores()

const ROLE_LABEL: Record<string, string> = {
  MANAGER: '일반매니저',
  STORE_ADMIN: '매장관리매니저',
  USER: '일반사용자',
  ADMIN: '관리자',
}
const STATUS_LABEL: Record<string, string> = {
  PENDING_APPROVAL_L1: '1차 승인 대기',
  PENDING_APPROVAL_L2: '2차 최종 승인 대기',
  ACTIVE: '활성',
  REJECTED: '반려',
}
function roleLabel(role: string) {
  return ROLE_LABEL[role] ?? role
}
function storeName(id?: string) {
  if (!id) return '-'
  return stores.find((s) => s.id === id)?.name ?? id
}

function base() {
  return useRuntimeConfig().public.apiBase
}

// 2차 승인 대기 목록(PENDING_APPROVAL_L2)
async function load() {
  const { $apiFetch } = useNuxtApp()
  try {
    rows.value = await $apiFetch<ManagerSignup[]>(`${base()}/admin/manager-approvals`)
  } catch {
    rows.value = []
  }
}

function openDetail(row: ManagerSignup) {
  message.value = ''
  selected.value = row
}
function closeDetail() {
  selected.value = null
}

// 2차 최종 확정(L2→ACTIVE) / 반려 — 모달에서 호출, 완료 후 모달 닫고 목록 갱신
async function patch(url: string) {
  const nuxtApp = useNuxtApp()
  try {
    await nuxtApp.$apiFetch(url, { method: 'PATCH' })
    message.value = '처리되었습니다.'
    // 최종 승인 시 BE가 manager 엔티티를 생성하므로 카탈로그를 갱신해 예약 매니저 목록에 반영(v2.4)
    await nuxtApp.runWithContext(() => reloadCatalog())
  } catch {
    message.value = '처리에 실패했습니다.'
  }
  closeDetail()
  await load()
}

onMounted(load)
</script>

<template>
  <section data-testid="page-admin-manager-approvals" class="mx-auto max-w-3xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">백오피스 · 관리자</span>
      <h1 class="text-3xl font-bold">매니저 가입 2차 최종 승인</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        신청 항목을 클릭하면 상세 정보가 모달로 표시됩니다. 최종 확정하면 계정이 활성화되어 로그인할 수 있습니다.
      </p>
    </header>

    <ul v-if="rows.length" class="space-y-3">
      <li v-for="r in rows" :key="r.id">
        <!-- 행 전체를 클릭 가능한 버튼으로 — 클릭 시 상세 모달 -->
        <button
          :data-testid="`manager-approval-row-${r.id}`"
          type="button"
          class="approval-row card flex w-full items-center justify-between gap-3 p-4 text-left"
          @click="openDetail(r)"
        >
          <div>
            <p class="font-semibold text-[--color-content-strong]">{{ r.name }} · {{ r.email }}</p>
            <p class="text-sm text-[--color-content-muted]">
              {{ roleLabel(r.role) }} · {{ storeName(r.storeId) }}
            </p>
          </div>
          <span class="text-sm font-medium text-[--color-brand-primary]">상세 보기</span>
        </button>
      </li>
    </ul>
    <p v-else data-testid="manager-approval-empty" class="text-sm text-[--color-content-muted]">
      2차 승인 대기 중인 가입 신청이 없습니다.
    </p>

    <p
      v-if="message"
      data-testid="manager-approval-message"
      class="mt-4 text-sm text-[--color-brand-accent]"
    >
      {{ message }}
    </p>

    <!-- 상세 모달 -->
    <Teleport to="body">
      <div
        v-if="selected"
        data-testid="manager-approval-modal"
        class="modal-overlay"
        @click.self="closeDetail"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="approval-modal-title"
          class="modal-card card"
        >
          <div class="mb-5 flex items-start justify-between gap-4">
            <div>
              <h2 id="approval-modal-title" class="text-xl font-bold text-[--color-content-strong]">
                가입 신청 상세
              </h2>
              <p class="mt-1 text-sm text-[--color-content-muted]">
                2차 최종 승인 여부를 결정하세요.
              </p>
            </div>
            <button
              data-testid="manager-approval-modal-close"
              type="button"
              class="btn btn-ghost !px-2.5 !py-1"
              aria-label="닫기"
              @click="closeDetail"
            >
              ✕
            </button>
          </div>

          <dl class="grid grid-cols-[7rem_1fr] gap-x-4 gap-y-3 text-sm">
            <dt class="text-[--color-content-muted]">이름</dt>
            <dd class="font-medium text-[--color-content-strong]">{{ selected.name }}</dd>
            <dt class="text-[--color-content-muted]">이메일</dt>
            <dd class="font-medium text-[--color-content-strong]">{{ selected.email }}</dd>
            <dt class="text-[--color-content-muted]">역할</dt>
            <dd class="font-medium text-[--color-content-strong]">{{ roleLabel(selected.role) }}</dd>
            <dt class="text-[--color-content-muted]">소속 매장</dt>
            <dd class="font-medium text-[--color-content-strong]">{{ storeName(selected.storeId) }}</dd>
            <dt class="text-[--color-content-muted]">승인 상태</dt>
            <dd class="font-medium text-[--color-content-strong]">
              {{ STATUS_LABEL[selected.approvalStatus] ?? selected.approvalStatus }}
            </dd>
            <dt class="text-[--color-content-muted]">계정 ID</dt>
            <dd class="font-mono text-xs text-[--color-content-muted]">{{ selected.id }}</dd>
          </dl>

          <div class="mt-7 flex items-center justify-end gap-2 border-t border-[--color-line-soft] pt-5">
            <button
              :data-testid="`manager-reject-${selected.id}`"
              type="button"
              class="btn btn-ghost"
              @click="patch(`${base()}/admin/manager-approvals/${selected.id}/reject`)"
            >
              반려
            </button>
            <button
              :data-testid="`manager-confirm-${selected.id}`"
              type="button"
              class="btn btn-primary"
              @click="patch(`${base()}/admin/manager-approvals/${selected.id}/confirm`)"
            >
              최종 승인(활성화)
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
/* 클릭 가능한 행 — hover 시 살짝 강조 */
.approval-row {
  transition:
    border-color 0.15s,
    background-color 0.15s;
}
.approval-row:hover {
  border-color: color-mix(in oklab, var(--color-brand-primary) 40%, var(--color-line));
}

/* 모달 오버레이 — 화면 중앙 + 백드롭 블러 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background-color: rgb(0 0 0 / 0.55);
  backdrop-filter: blur(4px);
}
.modal-card {
  width: 100%;
  max-width: 28rem;
  padding: 1.5rem;
}
</style>
