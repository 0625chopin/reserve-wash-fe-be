<script setup lang="ts">
// 예약 목록/취소/완료 (FW6/FW7) — 보호 라우트. 목록·상태 전이는 Phase 6
definePageMeta({ middleware: 'auth' })
</script>

<template>
  <section data-testid="page-reservations" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <h1 class="text-3xl font-bold">예약 목록</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        진행 중이거나 완료된 예약을 확인하세요.
      </p>
    </header>

    <!-- 의도된 빈 상태 — 콘텐츠는 Phase 6에서 추가 예정 -->
    <div class="card flex flex-col items-center gap-4 px-6 py-14 text-center">
      <div
        class="flex h-14 w-14 items-center justify-center rounded-2xl border border-[--color-line] bg-[--color-surface-2]"
      >
        <svg
          class="h-7 w-7 text-[--color-content-muted]"
          viewBox="0 0 24 24"
          fill="none"
          aria-hidden="true"
        >
          <rect
            x="4"
            y="5"
            width="16"
            height="15"
            rx="2"
            stroke="currentColor"
            stroke-width="1.6"
          />
          <path
            d="M8 3v4M16 3v4M4 10h16"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
          />
        </svg>
      </div>
      <div>
        <p class="font-semibold text-[--color-content-strong]">아직 예약이 없어요</p>
        <p class="mt-1 text-sm text-[--color-content-muted]">
          첫 세차를 예약하고 차를 빛나게 만들어 보세요.
        </p>
      </div>
      <NuxtLink to="/reserve" class="btn btn-primary">예약하러 가기</NuxtLink>
    </div>
  </section>
</template>
