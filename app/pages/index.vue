<script setup lang="ts">
// 홈 진입 시 예약 페이지로 리다이렉트 (미인증이면 auth 미들웨어가 /login으로 연쇄)
await navigateTo('/reserve')
</script>

<style scoped>
/* 로딩 점멸 — 절제된 펄스(라임 액센트) */
.splash-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 9999px;
  background-color: var(--color-brand-accent);
  box-shadow: 0 0 14px var(--color-brand-accent);
  animation: splash-pulse 1s var(--ease-out-soft) infinite;
}
@keyframes splash-pulse {
  0%,
  100% {
    opacity: 0.3;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}
</style>

<template>
  <!-- 리다이렉트 중 깜빡임 방지용 미니멀 스플래시 -->
  <section
    data-testid="page-index"
    class="flex min-h-[40vh] flex-col items-center justify-center gap-3"
  >
    <span class="text-xl font-extrabold tracking-tight text-[--color-content-strong]">
      WASH<span class="text-[--color-brand-primary]">.</span>
    </span>
    <span class="splash-dot" aria-hidden="true" />
    <span class="sr-only">예약 페이지로 이동 중…</span>
  </section>
</template>
