<script setup lang="ts">
// 예약 페이지 (FW3~FW5) — 보호 라우트. 매장/매니저 선택·슬롯 그리드는 Phase 4·5
definePageMeta({ middleware: 'auth' })
</script>

<template>
  <section data-testid="page-reserve">
    <h1>예약</h1>
  </section>
</template>
