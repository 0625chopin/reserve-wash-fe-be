# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어 및 커뮤니케이션 규칙

- 기본 응답 언어: 한국어
- 코드 주석: 한국어로 작성
- 커밋 메시지: 한국어로 작성
- 문서화: 한국어로 작성
- 변수명/함수명: 영어 (코드 표준 준수)
- 문자열 개행
- 수집, 계획, 작업 생성, 구현 등 단계를 최대한 상세히 작성 (CoT - Chain of Thought 방식)

## 프로젝트 개요

`create-vue` 기반 Vue 3 + Vite 스타터 템플릿. 런타임 스택은 Vue 3 (`<script setup>` Composition API), Vue Router, Pinia이며, 타입스크립트로 작성됨.

## 주요 명령어

```sh
npm install          # 의존성 설치
npm run dev          # Vite 개발 서버 (HMR)
npm run build        # type-check + 프로덕션 빌드를 병렬 실행 (run-p)
npm run build-only   # 타입 체크 없이 vite build만
npm run preview      # 빌드 결과물 로컬 미리보기
npm run type-check   # vue-tsc --build (타입 검사 단독 실행)
npm run lint         # lint:oxlint → lint:eslint 순차 실행 (run-s), 둘 다 --fix
npm run format       # oxfmt로 src/ 포맷팅
```

테스트 러너는 아직 설정되어 있지 않음 (vitest/cypress/playwright 미설치).

## 린트/포맷 파이프라인 (중요)

이 프로젝트는 일반적인 ESLint + Prettier 조합이 아니라 **oxc 도구 체인(oxlint + oxfmt)을 1차로, ESLint를 2차로** 사용함:

- **oxlint** (`.oxlintrc.json`): `correctness` 카테고리를 error로. 빠른 1차 린트.
- **eslint** (`eslint.config.ts`, flat config): Vue + TypeScript 규칙. `eslint-plugin-oxlint`로 oxlint와 중복되는 규칙을 끄고, `eslint-config-prettier`로 포맷 관련 규칙을 끔.
- **oxfmt** (`.oxfmtrc.json`): 세미콜론 없음(`semi: false`), 작은따옴표(`singleQuote: true`). 코드 스타일은 이 설정을 따를 것 — 새 코드 작성 시 세미콜론을 붙이지 말 것.
- VS Code는 저장 시 oxc 포매터(`oxc.oxc-vscode`)와 `source.fixAll`을 자동 적용 (`.vscode/settings.json`).

## 구조

- `src/main.ts` — 앱 진입점. Pinia와 Router를 `createApp`에 등록 후 `#app`에 마운트.
- `src/router/index.ts` — 라우트 정의. `/about`은 라우트 레벨 코드 스플리팅(동적 import)을 사용하는 패턴 참고.
- `src/stores/` — Pinia 스토어. setup 스토어 문법(`defineStore('id', () => {...})`) 사용.
- `src/views/` — 라우트에 매핑되는 페이지 컴포넌트.
- `src/components/` — 재사용 컴포넌트 (`icons/` 하위 포함).
- `@` 별칭은 `src/`를 가리킴 (`vite.config.ts`, `tsconfig.app.json`).

## TypeScript 설정

- `tsconfig.json`은 프로젝트 레퍼런스 루트로, `tsconfig.app.json`(앱 코드)과 `tsconfig.node.json`(빌드 설정 파일)을 참조.
- `.vue` 파일 타입 검사는 `tsc`가 아닌 `vue-tsc`로 수행됨 — 타입 검사는 반드시 `npm run type-check`를 사용.
