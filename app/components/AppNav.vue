<script setup lang="ts">
// 공통 네비게이션 — 로그인 상태에 따라 로그인 링크/로그아웃 버튼 분기 (require 4장)
const auth = useAuthStore()

function onLogout() {
  auth.logout()
  navigateTo('/login')
}
</script>

<template>
  <nav class="app-nav">
    <NuxtLink data-testid="nav-reserve" to="/reserve">예약</NuxtLink>
    <NuxtLink data-testid="nav-reservations" to="/reservations">예약 목록</NuxtLink>
    <NuxtLink v-if="!auth.isLoggedIn" data-testid="nav-login" to="/login">로그인</NuxtLink>
    <template v-else>
      <span data-testid="nav-username">{{ auth.currentUser?.name }}</span>
      <button data-testid="nav-logout" type="button" @click="onLogout">로그아웃</button>
    </template>
  </nav>
</template>

<style scoped>
.app-nav {
  display: flex;
  gap: 1rem;
  align-items: center;
  padding: 1rem;
}
</style>
