<!-- 기본 레이아웃: 스티키 헤더(AppNav) + 본문 컨테이너. AppNav는 Nuxt 자동 임포트 -->
<template>
  <div class="min-h-screen flex flex-col">
    <AppNav />
    <main class="flex-1 py-10 sm:py-14">
      <div class="container-app">
        <slot />
      </div>
    </main>
  </div>
</template>
