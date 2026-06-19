<script setup lang="ts">
// 홈 진입 시 예약 페이지로 리다이렉트 (미인증이면 auth 미들웨어가 /login으로 연쇄)
await navigateTo('/reserve')
</script>

<template>
  <section data-testid="page-index" />
</template>
