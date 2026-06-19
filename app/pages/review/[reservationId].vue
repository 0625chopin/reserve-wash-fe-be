<script setup lang="ts">
// 후기 작성 (동적 라우트) — 보호 라우트. 평점/작성 자격 검증은 Phase 7
definePageMeta({ middleware: 'auth' })

const route = useRoute()
</script>

<template>
  <section data-testid="page-review">
    <h1>후기 작성</h1>
    <p>예약 ID: {{ route.params.reservationId }}</p>
  </section>
</template>
