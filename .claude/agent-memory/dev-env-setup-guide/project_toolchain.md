---
name: project-toolchain
description: start-kit2 FE 프로젝트의 린트/포맷/빌드 도구 체인 구성과 핵심 명령어 동작 방식
metadata:
  type: project
---

start-kit2는 Nuxt 4(Vue 3 `<script setup>` + Vite 내장 + Nitro) + TS 스타터. 일반적인 ESLint+Prettier 조합이 아닌
oxc 도구 체인을 1차, ESLint를 2차로 쓰는 비표준 구성.

**Why:** 빠른 1차 린트(oxlint, Rust)로 큰 오류를 거르고, Vue/TS 심화 규칙은 ESLint가 보조.
포맷은 oxfmt가 전담하여 ESLint/Prettier와의 포맷 규칙 충돌을 제거.

**How to apply:** 환경 안내/디버깅 시 아래 사실을 근거로 삼을 것(추측 금지).
- `npm run lint` = run-s로 oxlint(--fix) → eslint(--fix --cache) **순차**
- `npm run dev` = `nuxt dev` (**포트 3000**), `npm run build` = `nuxt build`, `npm run generate` = `nuxt generate`, `npm run preview` = `nuxt preview`
- `npm run postinstall` = `nuxt prepare`, `npm run type-check` = `nuxt typecheck`(내부적으로 vue-tsc)
- `npm run format` = `oxfmt app/`
- 포맷 규칙: `.oxfmtrc.json`에서 semi:false, singleQuote:true → 신규 코드 세미콜론 금지
- 타입검사는 tsc가 아닌 `nuxt typecheck`(내부 vue-tsc)
- ESLint flat config(`eslint.config.ts`): eslint-plugin-oxlint로 중복규칙 끔, prettier로 포맷규칙 끔, `.nuxt`/`.output` ignore 추가, app/pages·layouts·app.vue·error.vue에 `vue/multi-word-component-names` off(파일기반 단어1개 파일명 허용)
- VS Code: `.vscode/settings.json`에서 formatOnSave + defaultFormatter oxc.oxc-vscode + source.fixAll explicit. (vite.config.* fileNesting 패턴 잔존하나 실파일 없음 — 무해)
- `~`/`@` 별칭 → `app/`(srcDir). `.nuxt/tsconfig.json`이 별칭 정의(.nuxt 없으면 타입 불완전). 설정 진입점은 루트 `nuxt.config.ts`(기존 vite.config.ts/src/main.ts/src/router는 제거됨)
- SSR 기본 활성(Nuxt) — 클라이언트 전용 코드는 `import.meta.client`/`useCookie` 고려
- engines.node: `^22.18.0 || >=24.12.0` (검증 PC: v24.16.0, npm 11.13.0)
- 패키지 매니저는 npm 전용(package-lock.json). 테스트 러너 미설치.
관련: [[fe-env-guide-doc]]
