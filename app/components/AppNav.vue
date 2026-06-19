<script setup lang="ts">
// 공통 네비게이션. TODO(Phase 3): useAuthStore()로 로그인/로그아웃 상태·버튼 연동
</script>

<template>
  <nav class="app-nav">
    <NuxtLink data-testid="nav-login" to="/login">로그인</NuxtLink>
    <NuxtLink data-testid="nav-reserve" to="/reserve">예약</NuxtLink>
    <NuxtLink data-testid="nav-reservations" to="/reservations">예약 목록</NuxtLink>
  </nav>
</template>

<style scoped>
.app-nav {
  display: flex;
  gap: 1rem;
  padding: 1rem;
}
</style>
