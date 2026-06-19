<script setup lang="ts">
// 예약 목록/취소/완료 (FW6/FW7) — 보호 라우트. 목록·상태 전이는 Phase 6
definePageMeta({ middleware: 'auth' })
</script>

<template>
  <section data-testid="page-reservations">
    <h1>예약 목록</h1>
  </section>
</template>
