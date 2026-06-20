import tailwindcss from '@tailwindcss/vite'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  modules: ['@pinia/nuxt'],
  css: ['~/assets/main.css'],
  // $fetch baseURL — 2차 BE(:8080) 호출 기준. 프록시 사용 시 '/api', 직접 호출 시 'http://localhost:8080/api'
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080/api',
    },
  },
  vite: {
    plugins: [tailwindcss()],
  },
})
