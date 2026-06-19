<script setup lang="ts">
// 후기 작성 (동적 라우트) — 보호 라우트. 평점/작성 자격 검증은 Phase 7
definePageMeta({ middleware: 'auth' })

const route = useRoute()
</script>

<template>
  <section data-testid="page-review" class="mx-auto max-w-2xl">
    <header class="mb-8">
      <span class="badge-accent mb-3">후기</span>
      <h1 class="text-3xl font-bold">후기 작성</h1>
      <p class="mt-2 text-sm text-[--color-content-muted]">
        예약 ID
        <code
          class="ml-1 rounded-md border border-[--color-line-soft] bg-[--color-surface-2] px-1.5 py-0.5 font-mono text-[--color-brand-primary]"
          >{{ route.params.reservationId }}</code
        >
      </p>
    </header>

    <!-- 의도된 빈 상태 — 평점/작성 폼은 Phase 7에서 추가 예정 -->
    <div class="card flex flex-col items-center gap-4 px-6 py-14 text-center">
      <div
        class="flex h-14 w-14 items-center justify-center rounded-2xl border border-[--color-line] bg-[--color-surface-2]"
      >
        <svg
          class="h-7 w-7 text-[--color-brand-accent]"
          viewBox="0 0 24 24"
          fill="none"
          aria-hidden="true"
        >
          <path
            d="m12 4 2.35 4.76 5.25.76-3.8 3.7.9 5.23L12 16.9l-4.7 2.47.9-5.23-3.8-3.7 5.25-.76L12 4Z"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linejoin="round"
          />
        </svg>
      </div>
      <div>
        <p class="font-semibold text-[--color-content-strong]">곧 후기를 남길 수 있어요</p>
        <p class="mt-1 text-sm text-[--color-content-muted]">
          평점과 후기 작성 기능을 준비 중입니다.
        </p>
      </div>
    </div>
  </section>
</template>
