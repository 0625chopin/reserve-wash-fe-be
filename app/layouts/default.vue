<!-- 기본 레이아웃: 공통 네비(AppNav) + 페이지 슬롯. AppNav는 Nuxt 자동 임포트 -->
<template>
  <div class="app-shell">
    <AppNav />
    <main>
      <slot />
    </main>
  </div>
</template>
