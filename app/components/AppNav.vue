<script setup lang="ts">
// 공통 네비게이션 — 로그인 상태에 따라 로그인 링크/로그아웃 버튼 분기 (require 4장)
const auth = useAuthStore()

function onLogout() {
  auth.logout()
  navigateTo('/login')
}
</script>

<template>
  <!-- 스티키 헤더: 반투명 네이비 + 블러로 깊이감 -->
  <header
    class="sticky top-0 z-30 border-b border-[--color-line-soft] bg-[#0b1120]/80 backdrop-blur-md"
  >
    <div class="container-app flex h-16 items-center justify-between gap-4">
      <!-- 워드마크 -->
      <NuxtLink to="/reserve" class="group flex items-center gap-2">
        <span
          class="inline-block h-2.5 w-2.5 rounded-full bg-[--color-brand-accent] shadow-[0_0_12px_var(--color-brand-accent)]"
        />
        <span class="text-lg font-extrabold tracking-tight text-[--color-content-strong]">
          WASH<span class="text-[--color-brand-primary]">.</span>
        </span>
      </NuxtLink>

      <!-- 네비 + 인증 상태 -->
      <nav class="flex items-center gap-1 sm:gap-2">
        <NuxtLink data-testid="nav-reserve" to="/reserve" class="nav-link">예약</NuxtLink>
        <NuxtLink data-testid="nav-reservations" to="/reservations" class="nav-link">
          예약 목록
        </NuxtLink>

        <span class="mx-1 hidden h-5 w-px bg-[--color-line] sm:block" />

        <template v-if="!auth.isLoggedIn">
          <NuxtLink
            data-testid="nav-signup"
            to="/signup"
            class="btn btn-ghost !px-4 !py-1.5 !text-sm"
          >
            회원가입
          </NuxtLink>
          <NuxtLink
            data-testid="nav-login"
            to="/login"
            class="btn btn-primary !px-4 !py-1.5 !text-sm"
          >
            로그인
          </NuxtLink>
        </template>
        <template v-else>
          <span
            data-testid="nav-username"
            class="hidden items-center gap-1.5 px-2 text-sm font-medium text-[--color-content] sm:inline-flex"
          >
            <span class="h-1.5 w-1.5 rounded-full bg-[--color-brand-accent]" />
            {{ auth.currentUser?.name }}
          </span>
          <button
            data-testid="nav-logout"
            type="button"
            class="btn btn-ghost !px-3 !py-1.5 !text-sm"
            @click="onLogout"
          >
            로그아웃
          </button>
        </template>
      </nav>
    </div>
  </header>
</template>

<style scoped>
/* 네비 링크 — 기본/hover/활성(라우트 매칭) 상태 */
.nav-link {
  border-radius: 0.5rem;
  padding: 0.375rem 0.75rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-content-muted);
  transition:
    color 0.2s var(--ease-out-soft),
    background-color 0.2s var(--ease-out-soft);
}
.nav-link:hover {
  color: var(--color-content-strong);
  background-color: var(--color-surface-2);
}
/* Nuxt가 부여하는 활성 라우트 클래스 — 일렉트릭 스카이로 강조 */
.nav-link.router-link-active {
  color: var(--color-brand-primary);
}
</style>
