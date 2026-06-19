# 프론트엔드(FE) 개발 환경 설정 가이드

> 자동차 세차 예약 서비스(MVP) — 프론트엔드 온보딩 문서
>
> 이 문서는 **주니어 개발자도 처음부터 끝까지 그대로 따라 하면** FE 개발 환경을
> 완성할 수 있도록 작성되었습니다. 명령어는 복사·붙여넣기로 바로 실행할 수 있습니다.

---

## 1. 문서 개요 / 이 문서를 읽는 법

### 1.1 이 문서의 목표

이 프로젝트(`start-kit2`)는 **Nuxt 4 기반**의 풀스택 프레임워크 스타터입니다.
Nuxt는 내부적으로 **Vue 3(`<script setup>` Composition API) + Vite(빌드 도구) +
Nitro(서버 엔진)** 를 묶어 제공하며, 파일 기반 라우팅·자동 임포트·SSR을 기본으로
지원합니다.
이 가이드를 끝까지 따라 하면 다음을 할 수 있게 됩니다.

- 로컬 PC에 Node.js / npm / Git을 올바른 버전으로 설치
- 프로젝트를 클론하고 의존성 설치(이때 `nuxt prepare`로 `.nuxt` 타입이 생성됨)
- 개발 서버(`npm run dev`)를 띄워 첫 화면 확인
- 린트 / 포맷 / 타입 체크 도구 체인 이해 및 실행
- VS Code를 프로젝트 코드 스타일에 맞게 설정

### 1.2 읽는 방법

- 위에서부터 **순서대로** 따라 하세요. 각 단계는 앞 단계가 끝났다는 것을 전제로 합니다.
- 각 단계마다 다음 3가지가 함께 나옵니다.
  - **왜 하는지** — 이 단계의 목적
  - **확인 방법** — 제대로 됐는지 검증하는 명령/출력
  - **안 될 때** — 자주 발생하는 문제와 해결책(트러블슈팅은 9장에 모아둠)
- 명령어 블록 상단에 셸 표기가 있습니다. 별도 표기가 없으면 모든 OS 공통입니다.
  - `PowerShell` = Windows 11 기본 셸
  - `bash` = macOS / Linux 터미널

### 1.3 용어 빠른 정리

| 용어 | 의미 |
|---|---|
| Nuxt | Vue 3 기반 풀스택 프레임워크. 파일 기반 라우팅·자동 임포트·SSR을 기본 제공 |
| Nitro | Nuxt의 **서버 엔진**. SSR/서버 라우트를 구동하고 `.output/`으로 빌드 |
| Vite | **Nuxt 내부 빌드 도구**. 매우 빠른 HMR(코드 저장 시 즉시 반영) 제공 |
| 파일 기반 라우팅 | `app/pages/`의 파일 구조가 그대로 URL이 됨(예: `about.vue` → `/about`) |
| 자동 임포트 | `app/components/`·`app/stores/`·`app/composables/` 등은 `import` 없이 사용 가능 |
| SSR | 서버 사이드 렌더링. Nuxt는 기본 SSR이라 일부 코드는 서버에서도 실행됨 |
| HMR | Hot Module Replacement. 새로고침 없이 변경분만 화면에 반영 |
| oxlint | Rust로 만든 **빠른 1차 린터** (`.oxlintrc.json`) |
| ESLint | Vue/TypeScript 규칙을 보는 **2차 린터** (`eslint.config.ts`) |
| oxfmt | 코드 **포매터** (세미콜론·따옴표 등 스타일 정리, `.oxfmtrc.json`) |
| vue-tsc | `.vue` 파일까지 검사하는 **타입 체커**(`nuxt typecheck`가 내부적으로 사용) |

---

## 2. 사전 요구사항

### 2.1 필요한 도구와 버전

이 표의 버전은 **현재 개발 PC에서 검증 완료**된 값과 `package.json`의 `engines`
설정을 기준으로 합니다.

| 도구 | 요구 버전 | 검증된 버전(현재 PC) | 근거 |
|---|---|---|---|
| Node.js | `^22.18.0 \|\| >=24.12.0` | **v24.16.0** | `package.json`의 `engines.node` |
| npm | Node에 동봉된 버전 | **11.13.0** | Node 24.x 동봉 |
| Git | 최신 권장 | **2.54.0** | 클론/버전 관리용 |

> `engines.node`의 `^22.18.0 || >=24.12.0`는 **"22.18.0 이상의 22.x 또는,
> 24.12.0 이상"** 을 의미합니다. 즉 Node 22 LTS(22.18+) 또는 Node 24(24.12+)를
> 사용하면 됩니다. **23.x나 22.17 이하는 권장되지 않습니다.**

### 2.2 현재 버전 확인 (가장 먼저 할 일)

**왜 하는지:** 잘못된 Node 버전은 설치 실패나 빌드 오류의 가장 흔한 원인입니다.
무엇이 깔려 있는지부터 확인합니다.

```sh
node -v
npm -v
git --version
```

**확인 방법(성공 시 예시 출력):**

```text
v24.16.0
11.13.0
git version 2.54.0
```

- `node -v` 결과가 `v22.18.0`~`v22.x` 또는 `v24.12.0` 이상이면 정상입니다.
- 위 범위를 벗어나면 2.3절을 따라 버전을 맞추세요.

**안 될 때:**

- `node : 명령을 찾을 수 없습니다` / `command not found: node` → Node.js가 설치되지
  않았습니다. 2.3절로 이동.

### 2.3 Node.js 설치 / 버전 관리

여러 프로젝트를 오가며 Node 버전을 바꿔야 할 수 있으므로 **버전 매니저 사용을
권장**합니다.

#### Windows 11 — nvm-windows (권장)

**왜 하는지:** 프로젝트마다 다른 Node 버전을 손쉽게 전환하기 위함입니다.

1. [nvm-windows Releases](https://github.com/coreybutler/nvm-windows/releases)에서
   `nvm-setup.exe`를 받아 설치합니다.
2. 설치 후 **새 PowerShell 창**을 엽니다(기존 창은 PATH가 갱신 안 됨).
3. 아래를 실행합니다.

```powershell
# PowerShell
nvm install 24.16.0
nvm use 24.16.0
node -v   # v24.16.0 이 나오면 성공
```

> 참고: nvm-windows는 관리자 권한 PowerShell이 필요할 수 있습니다.
> `nvm use` 실행 시 권한 오류가 나면 PowerShell을 "관리자 권한으로 실행"하세요.

#### Windows 11 — 버전 매니저 없이 설치

[Node.js 공식 사이트](https://nodejs.org/)에서 **24.x LTS** 설치 프로그램(.msi)을
받아 설치합니다. 설치 마법사에서 기본값 그대로 진행하면 됩니다.

#### macOS / Linux — nvm

```bash
# bash / zsh
# 1) nvm 설치 (공식 설치 스크립트)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
# 2) 터미널 재시작 후
nvm install 24.16.0
nvm use 24.16.0
node -v
```

> macOS는 Homebrew(`brew install node@24`)로도 설치할 수 있지만, 버전 전환
> 유연성 때문에 nvm을 권장합니다.

**확인 방법:** 설치 후 2.2절의 `node -v` / `npm -v`를 다시 실행해 버전이 요구
범위 안인지 확인합니다.

---

## 3. 프로젝트 클론 & 의존성 설치

### 3.1 저장소 클론

**왜 하는지:** 원격 저장소의 코드를 로컬로 내려받습니다.
(이미 로컬에 프로젝트가 있다면 이 단계는 건너뛰고 3.2로 가세요.)

```sh
git clone https://github.com/0625chopin/start-kit.git start-kit2
cd start-kit2
```

> 위 주소는 이 프로젝트의 실제 저장소(GitHub)입니다.
> SSH로 클론하려면 `git@github.com:0625chopin/start-kit.git` 를 사용하세요.

**확인 방법:**

```sh
git status
```

- `On branch main` 과 같은 출력이 나오면 정상입니다.

### 3.2 의존성 설치

**왜 하는지:** `package.json`에 정의된 라이브러리(Nuxt, Vue, Pinia, 린터 등)를
`node_modules/`로 내려받습니다. 설치 마지막에 **`postinstall` 스크립트
(`nuxt prepare`)** 가 자동 실행되어 `.nuxt/` 디렉터리와 타입 파일
(`.nuxt/tsconfig.json` 등)을 생성합니다.

```sh
npm install
```

> 이 프로젝트는 **npm**을 사용합니다(`package-lock.json` 기준). pnpm/yarn를
> 섞어 쓰면 lock 파일이 충돌하므로 **npm만** 사용하세요.

**확인 방법(성공 시):**

- 마지막 줄에 `added N packages ... in Xs` 형태의 메시지가 출력됩니다.
- 루트에 `node_modules/` 폴더가 생기고, `package-lock.json`이 존재합니다.
- `postinstall`이 정상 실행되면 루트에 **`.nuxt/` 폴더**가 생깁니다(자동 생성물,
  `.gitignore`에 포함되어 커밋되지 않음).

```sh
# node_modules 와 .nuxt 가 생겼는지 확인
# PowerShell: Test-Path node_modules  →  True
#             Test-Path .nuxt         →  True
# bash:       ls -d node_modules .nuxt
```

> **`.nuxt`가 안 생겼다면:** `postinstall`이 어떤 이유로 건너뛰어졌을 수 있습니다.
> 아래 명령으로 수동 생성하세요. 이 작업은 타입 추론·자동 임포트가 동작하기 위해
> **반드시 필요**합니다.
>
> ```sh
> npm run postinstall   # 내부적으로 nuxt prepare 실행
> ```

**안 될 때:** 9장 트러블슈팅의 "install 실패" 항목을 참고하세요.

---

## 4. dev 서버 실행 & 첫 화면 확인

### 4.1 개발 서버 실행

**왜 하는지:** Nuxt 개발 서버(`nuxt dev`)를 띄워 브라우저에서 앱을 보고, 코드를
저장하면 즉시 반영(HMR)되는 환경을 만듭니다. Nuxt는 내부적으로 Vite를 사용합니다.

```sh
npm run dev
```

**확인 방법(성공 시 예시 출력):**

```text
Nuxt 4.x.x with Nitro 2.x.x

  ➜ Local:    http://localhost:3000/
  ➜ Network:  use --host to expose

✔ Vite client built in xxx ms
✔ Vite server built in xxx ms
✔ Nuxt Nitro server built in xxx ms
```

- 브라우저에서 **http://localhost:3000/** 로 접속합니다. (★ Nuxt 기본 포트는
  Vite의 5173이 아니라 **3000**입니다.)
- Vue 로고와 "You did it!" 류의 기본 환영 화면이 보이면 성공입니다.
- 상단 메뉴의 **Home / About** 링크(`<NuxtLink>`)를 눌러 파일 기반 라우팅이
  동작하는지 확인하세요. (`/about`은 `app/pages/about.vue`가 매핑된 페이지입니다.)

### 4.2 HMR 동작 확인

**왜 하는지:** 저장 시 자동 반영이 되는지 확인하면 개발 환경이 정상임을 알 수
있습니다.

- `app/components/HelloWorld.vue` 등에서 텍스트를 한 글자 바꾸고 저장해 보세요.
- 브라우저 새로고침 없이 화면이 즉시 바뀌면 HMR 정상입니다.

### 4.3 서버 종료

- 터미널에서 `Ctrl + C` 를 누르면 개발 서버가 종료됩니다.

**안 될 때(포트 충돌 등):** 9장 트러블슈팅 참고. 3000 포트가 이미 사용 중이면
Nuxt가 자동으로 3001 등 다음 포트를 사용하니, 실제 출력된 URL을 보세요.

---

## 5. 핵심 명령어 레퍼런스

아래 표는 **`package.json`의 `scripts` 실제 내용**과 1:1로 일치합니다.

| 명령어 | 실제 실행 내용 | 설명 |
|---|---|---|
| `npm install` | — | 의존성 설치 (설치 후 `postinstall`로 `nuxt prepare` 자동 실행) |
| `npm run dev` | `nuxt dev` | 개발 서버 실행(HMR), 기본 http://localhost:3000 |
| `npm run build` | `nuxt build` | 서버(SSR) 포함 프로덕션 빌드 → `.output/` 생성 |
| `npm run generate` | `nuxt generate` | 정적 사이트(SSG)로 사전 렌더링하여 빌드 |
| `npm run preview` | `nuxt preview` | 빌드 결과물(`.output/`)을 로컬에서 미리보기 |
| `npm run postinstall` | `nuxt prepare` | `.nuxt` 타입/자동 임포트 메타 생성(설치 시 자동, 수동도 가능) |
| `npm run type-check` | `nuxt typecheck` | 타입 검사 단독 실행(내부적으로 `vue-tsc` 사용) |
| `npm run lint` | `run-s "lint:*"` | `lint:oxlint` → `lint:eslint` **순차** 실행 |
| `npm run lint:oxlint` | `oxlint . --fix` | 1차 린트(빠름) + 자동 수정 |
| `npm run lint:eslint` | `eslint . --fix --cache` | 2차 린트(Vue/TS) + 자동 수정 + 캐시 |
| `npm run format` | `oxfmt app/` | `app/` 코드 포맷팅 |

> 핵심 포인트
>
> - `npm run build`는 **`nuxt build`** 입니다. SSR 서버를 포함한 산출물을
>   `.output/`에 생성합니다(이전 `dist/`가 아님). 완전 정적 배포가 필요하면
>   `npm run generate`를 사용합니다.
> - `npm run type-check`는 **`nuxt typecheck`** 이며 내부적으로 `vue-tsc`를
>   사용해 `.vue`까지 타입 검사합니다. 일반 `tsc`로는 `.vue` 타입을 못 잡습니다.
> - `npm run lint`는 `run-s`(npm-run-all2)로 **oxlint 먼저, 그다음 eslint**를
>   순차 실행합니다. 하나라도 실패하면 전체가 실패로 끝납니다.

---

## 6. 린트 / 포맷 파이프라인 이해 (중요)

> 이 프로젝트는 **일반적인 "ESLint + Prettier" 조합이 아닙니다.**
> oxc 도구 체인(oxlint + oxfmt)을 1차로 쓰고, ESLint를 2차로 보조합니다.
> 이 구조를 이해해야 린트 에러를 올바르게 다룰 수 있습니다.

### 6.1 역할 분담 한눈에 보기

| 도구 | 설정 파일 | 역할 | 실행 시점 |
|---|---|---|---|
| **oxlint** | `.oxlintrc.json` | 빠른 1차 린트(버그성 오류 잡기) | `npm run lint` 1단계 |
| **eslint** | `eslint.config.ts` | Vue/TS 심화 규칙 린트 | `npm run lint` 2단계 |
| **oxfmt** | `.oxfmtrc.json` | 코드 스타일(포맷) 정리 | `npm run format`, 저장 시 |

### 6.2 oxlint — `.oxlintrc.json`

실제 설정 내용:

```json
{
  "$schema": "./node_modules/oxlint/configuration_schema.json",
  "plugins": ["eslint", "typescript", "unicorn", "oxc", "vue"],
  "env": {
    "browser": true
  },
  "categories": {
    "correctness": "error"
  }
}
```

- `correctness` 카테고리(명백한 버그·오류성 규칙)를 **error**로 설정합니다.
- 매우 빠르므로 가장 먼저 돌려 큰 문제를 걸러냅니다.

### 6.3 eslint — `eslint.config.ts` (flat config)

실제 설정 내용:

```ts
import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginOxlint from 'eslint-plugin-oxlint'
import skipFormatting from 'eslint-config-prettier/flat'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  globalIgnores([
    '**/dist/**',
    '**/dist-ssr/**',
    '**/coverage/**',
    '**/.nuxt/**',
    '**/.output/**',
  ]),

  ...pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,

  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),

  // Nuxt 파일 기반 라우팅: pages/layouts/루트 컴포넌트는 단어 1개 파일명이 정상이므로 규칙 해제
  {
    name: 'app/nuxt-single-word-components',
    files: ['app/pages/**/*.vue', 'app/layouts/**/*.vue', 'app/app.vue', 'app/error.vue'],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  skipFormatting,
)
```

핵심 포인트:

- **Vue + TypeScript** 권장 규칙을 적용합니다.
- `eslint-plugin-oxlint`의 `buildFromOxlintConfigFile('.oxlintrc.json')`로
  **oxlint와 중복되는 규칙을 끕니다**(같은 경고가 두 번 뜨지 않도록).
- `eslint-config-prettier/flat`(`skipFormatting`)로 **포맷 관련 규칙을 끕니다**
  — 포맷은 oxfmt가 전담하므로 ESLint는 포맷을 건드리지 않습니다.
- Nuxt 자동 생성물인 **`.nuxt`/`.output`을 ignore**에 추가해 린트 대상에서 제외합니다.
- 파일 기반 라우팅에서는 `index.vue`·`about.vue`처럼 **단어 한 개짜리 파일명**이
  정상이므로, `app/pages`·`app/layouts`·`app.vue`·`error.vue`에 한해
  **`vue/multi-word-component-names` 규칙을 off** 했습니다. (일반 컴포넌트는
  여전히 여러 단어 이름 규칙이 적용됩니다.)

### 6.4 oxfmt — `.oxfmtrc.json`

실제 설정 내용:

```json
{
  "$schema": "./node_modules/oxfmt/configuration_schema.json",
  "semi": false,
  "singleQuote": true
}
```

- `semi: false` → **세미콜론을 붙이지 않습니다.**
- `singleQuote: true` → **작은따옴표(`'`)를 사용합니다.**
- 코드 스타일은 항상 이 설정을 따릅니다. (8장 컨벤션 참고)

### 6.5 실제로 어떻게 돌리나

```sh
# 포맷 정리 (app/ 전체)
npm run format

# 린트 + 자동 수정 (oxlint → eslint 순차)
npm run lint
```

**확인 방법:** `npm run lint`가 에러 없이 끝나면(종료 코드 0) 통과입니다.
일부는 `--fix`로 자동 수정되며, 자동 수정이 안 되는 항목은 메시지로 안내됩니다.

> 권장 작업 순서: 코드 작성 → `npm run format` → `npm run lint` →
> `npm run type-check`. (VS Code 저장 시 자동 포맷을 켜두면 앞 단계는 대부분
> 자동으로 처리됩니다 — 7장 참고.)

---

## 7. VS Code 설정

> 에디터는 자유지만, 이 프로젝트는 **VS Code 기준으로 자동 포맷/수정이 미리
> 설정**되어 있습니다. VS Code 사용을 권장합니다.

### 7.1 권장 확장 설치

프로젝트에 `.vscode/extensions.json`이 있어, VS Code로 프로젝트를 열면 우측
하단에 "권장 확장 설치" 알림이 뜹니다. 실제 권장 목록:

```json
{
  "recommendations": [
    "Vue.volar",
    "dbaeumer.vscode-eslint",
    "EditorConfig.EditorConfig",
    "oxc.oxc-vscode"
  ]
}
```

| 확장 ID | 용도 |
|---|---|
| `Vue.volar` | Vue 3 공식 언어 지원(문법/타입/자동완성). Nuxt의 `.vue` 파일도 동일하게 지원 |
| `dbaeumer.vscode-eslint` | ESLint 통합(에디터에서 린트 표시) |
| `EditorConfig.EditorConfig` | EditorConfig 지원 |
| `oxc.oxc-vscode` | **oxc 포매터/린터**(저장 시 포맷의 핵심) |

> 이 프로젝트의 `.vscode/extensions.json`에는 **별도의 Nuxt 전용 확장이
> 포함되어 있지 않습니다.** Nuxt 개발은 위 4종으로 충분합니다(`Vue.volar`가
> Nuxt의 Vue 파일 타입/자동완성을 담당). Nuxt 관련 추가 확장은 필요 시 개인
> 선택으로 설치하면 됩니다.

**설치 방법:**

1. VS Code에서 `Ctrl + Shift + X`(확장 탭)를 엽니다.
2. 검색창에 `@recommended` 를 입력하면 위 목록이 보입니다. 각각 Install.
3. 또는 알림의 "Install All"을 클릭합니다.

> 주의: 과거에 쓰던 Vue 확장 `Vetur`가 설치돼 있으면 **Volar와 충돌**합니다.
> Vetur는 비활성화하거나 제거하세요.

### 7.2 저장 시 자동 포맷/수정 (이미 설정됨)

프로젝트에 `.vscode/settings.json`이 포함되어 있어 별도 설정 없이 동작합니다.
실제 내용:

```json
{
  "explorer.fileNesting.enabled": true,
  "explorer.fileNesting.patterns": {
    "tsconfig.json": "tsconfig.*.json, env.d.ts, typed-router.d.ts",
    "vite.config.*": "jsconfig*, vitest.config.*, cypress.config.*, playwright.config.*",
    "package.json": "package-lock.json, pnpm*, .yarnrc*, yarn*, .eslint*, eslint*, .oxlint*, oxlint*, .oxfmt*, .prettier*, prettier*, .editorconfig"
  },
  "editor.codeActionsOnSave": {
    "source.fixAll": "explicit"
  },
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "oxc.oxc-vscode"
}
```

의미:

- `editor.formatOnSave: true` + `defaultFormatter: oxc.oxc-vscode`
  → **저장하면 oxc 포매터가 자동으로 코드를 포맷**합니다(세미콜론 제거,
  작은따옴표로 변환 등).
- `editor.codeActionsOnSave.source.fixAll: "explicit"`
  → 저장 시 **자동 수정 가능한 린트 문제를 함께 고칩니다.**
- `explorer.fileNesting.*` → 탐색기에서 설정 파일들을 보기 좋게 묶어줍니다(기능엔
  영향 없음).

**확인 방법:** `.ts`/`.vue` 파일에서 일부러 큰따옴표나 세미콜론을 넣고 저장해
보세요. 저장 즉시 작은따옴표로 바뀌고 세미콜론이 사라지면 정상입니다.

**안 될 때:**

- 저장해도 포맷이 안 됨 → `oxc.oxc-vscode` 확장이 설치/활성화됐는지 확인.
- 다른 포매터(Prettier 등)가 끼어듦 → 파일 우클릭 → "Format Document With..." →
  "Configure Default Formatter" → **oxc 선택**.

---

## 8. 코드 스타일 컨벤션 요약

> 이 프로젝트의 스타일은 oxfmt 설정과 팀 규칙(CLAUDE.md)에서 나옵니다. 아래를
> 지키면 린트/포맷에서 거의 막히지 않습니다.

### 8.1 포맷 규칙 (oxfmt)

- **세미콜론을 붙이지 않습니다** (`semi: false`).
- **문자열은 작은따옴표**를 씁니다 (`singleQuote: true`).

```ts
// 좋음 (이 프로젝트 스타일)
const name = 'tesla'
const count = ref(0)

// 나쁨 (세미콜론 + 큰따옴표 — 저장 시 자동으로 위 형태로 바뀜)
const name = "tesla";
const count = ref(0);
```

> 신규 코드 작성 시 **세미콜론을 직접 붙이지 마세요.** 저장 시 자동 제거되지만,
> 처음부터 붙이지 않는 습관을 들이는 것이 좋습니다.

### 8.2 경로 별칭 `~` / `@` → `app/`

Nuxt에서는 **`~`와 `@` 모두 `app/`(srcDir)를 가리킵니다.** (이전 Vue+Vite
구성에서 `@`가 `src/`였던 것과 달라졌습니다.) 별칭은 Nuxt가 자동 생성하는
`.nuxt/tsconfig.json`에 정의되므로 별도 설정이 필요 없습니다. 상대 경로 대신
별칭을 쓰면 import가 깔끔해집니다.

```ts
// 좋음 (CSS·에셋 등 자동 임포트가 안 되는 자원에 사용)
import logo from '~/assets/logo.svg'

// 피하기 (깊은 상대 경로)
import logo from '../../assets/logo.svg'
```

> 참고: `app/components/`, `app/stores/`, `app/composables/` 등은 **Nuxt 자동
> 임포트** 대상이라 `import` 문 없이 바로 사용할 수 있습니다. 별칭이 필요한 경우는
> 주로 에셋/CSS처럼 자동 임포트되지 않는 자원입니다.

### 8.3 언어 규칙 (팀 규칙)

- 주석/문서: **한국어**로 작성
- 변수명/함수명: **영어**(코드 표준 준수)

```ts
// 카운터를 1 증가시킨다
function increment() {
  count.value++
}
```

### 8.4 프레임워크 컨벤션 (현재 코드 기준)

- Vue 컴포넌트는 **`<script setup>` Composition API**를 사용합니다.
- Pinia 스토어는 **setup 스토어 문법**을 사용합니다.

```ts
// app/stores/counter.ts (현재 코드 스타일 예시)
// app/stores/ 안에 두면 @pinia/nuxt 모듈이 자동 임포트하므로 import 없이 사용 가능
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useCounterStore = defineStore('counter', () => {
  const count = ref(0)
  const doubleCount = computed(() => count.value * 2)
  function increment() {
    count.value++
  }

  return { count, doubleCount, increment }
})
```

- 라우팅은 **Nuxt 파일 기반 라우팅**을 사용합니다. `app/pages/`의 파일 구조가
  그대로 URL이 되며, Vue Router를 직접 설치·설정하지 않습니다(Nuxt 내장).
  - `app/pages/index.vue` → `/`
  - `app/pages/about.vue` → `/about`
  - 동적 라우트는 대괄호 파일명을 씁니다: `app/pages/review/[reservationId].vue`
    → `/review/:reservationId`
  - 라우트 가드/미들웨어는 `app/middleware/`에 둡니다.
- 화면 전환은 `<a>` 대신 **`<NuxtLink to="...">`** 를, 루트 컴포넌트
  `app/app.vue`에서는 현재 라우트를 렌더링하는 **`<NuxtPage />`** 를 사용합니다.

```vue
<!-- app/app.vue (현재 코드 스타일 예시) -->
<template>
  <nav>
    <NuxtLink to="/">Home</NuxtLink>
    <NuxtLink to="/about">About</NuxtLink>
  </nav>

  <NuxtPage />
</template>
```

### 8.5 SSR 주의 (Nuxt는 기본 서버 사이드 렌더링)

Nuxt는 기본적으로 **SSR**이라 컴포넌트 코드가 **서버에서도 한 번 실행**됩니다.
따라서 `window`·`document`·`localStorage` 같은 **브라우저 전용 API**를 그대로
쓰면 서버 렌더 단계에서 오류가 납니다. 클라이언트에서만 동작해야 하는 코드는
`import.meta.client` 가드로 감싸거나, 상태 저장에는 `useCookie` 같은 Nuxt
컴포저블을 사용하세요.

```ts
// localStorage 는 클라이언트에서만 접근
if (import.meta.client) {
  localStorage.setItem('key', 'value')
}
```

### 8.6 디렉터리 구조 빠른 참고 (Nuxt 4)

> Nuxt에는 별도의 진입점 파일(`main.ts`)이 없습니다. Nuxt가 앱을 부트스트랩하며,
> Pinia는 `nuxt.config.ts`의 `modules: ['@pinia/nuxt']`로 등록됩니다. 초기화
> 로직이 필요하면 `app/plugins/`에 플러그인을 둡니다.

| 경로 | 역할 |
|---|---|
| `nuxt.config.ts` | Nuxt 설정(루트). 모듈·CSS·런타임 옵션 등 정의 |
| `tsconfig.json` | `./.nuxt/tsconfig.json`을 확장(별칭·타입은 Nuxt가 생성) |
| `app/app.vue` | 루트 컴포넌트. `<NuxtPage />`로 현재 페이지를 렌더링 |
| `app/pages/` | **파일 기반 라우팅** 페이지(`index.vue`=`/`, `about.vue`=`/about`) |
| `app/components/` | 재사용 컴포넌트(`icons/` 포함). **자동 임포트** |
| `app/stores/` | Pinia 스토어. **자동 임포트**(`@pinia/nuxt`) |
| `app/composables/` | 컴포저블(`useXxx`). **자동 임포트** |
| `app/assets/` | CSS 등 정적 자산(`main.css`는 `nuxt.config.ts`에서 로드) |
| `app/layouts/` | (필요 시) 공통 레이아웃 |
| `app/middleware/` | (필요 시) 라우트 미들웨어/가드 |
| `app/plugins/` | (필요 시) 앱 초기화 플러그인 |
| `.nuxt/` | Nuxt 자동 생성물(타입·라우트 메타 등). gitignore, 커밋 안 함 |
| `.output/` | `nuxt build` 산출물(SSR 서버 포함). gitignore, 커밋 안 함 |

---

## 9. 트러블슈팅

### 9.1 Node 버전 불일치

**증상:** `npm install` 또는 실행 시 `Unsupported engine`, `EBADENGINE` 경고/오류,
또는 빌드가 알 수 없는 이유로 실패.

**원인:** 설치된 Node가 `engines.node`(`^22.18.0 || >=24.12.0`) 범위를 벗어남.

**해결:**

```sh
node -v   # 현재 버전 확인
```

- 범위를 벗어났다면 2.3절을 따라 24.16.0(또는 22.18+)으로 맞춥니다.
- nvm 사용 중이라면:

```sh
nvm install 24.16.0
nvm use 24.16.0
```

### 9.2 `npm install` 실패

**증상:** 설치 중 네트워크/권한/캐시 관련 오류로 중단.

**점검 순서:**

1. 네트워크/프록시(사내망)인지 확인.
2. Node/npm 버전이 정상인지(9.1).
3. lock 파일과 캐시 정리 후 재설치(아래는 **node_modules와 lock을 지우는
   파괴적 작업**이므로, 먼저 git 상태가 깨끗한지 확인하세요):

```sh
# 주의: 아래 두 줄은 되돌리기 어렵습니다. 실행 전 변경사항을 커밋/백업하세요.
# PowerShell
Remove-Item -Recurse -Force node_modules; Remove-Item -Force package-lock.json
npm cache verify
npm install
```

```bash
# bash (macOS/Linux)
rm -rf node_modules package-lock.json
npm cache verify
npm install
```

> 더 안전한 대안: 먼저 `npm install` 만 다시 시도하거나, `node_modules`만 지우고
> `package-lock.json`은 보존한 채 `npm ci`로 재현 설치를 시도해 보세요.

### 9.3 포트 충돌 (3000 사용 중)

**증상:** `npm run dev` 시 3000 대신 3001 등 다른 포트로 뜨거나, 접속이 안 됨.

**원인:** 다른 프로세스(다른 Nuxt 앱 등)가 3000을 점유 중.

**해결:**

- 출력된 **실제 URL**(예: `http://localhost:3001/`)로 접속하면 됩니다.
- 특정 포트로 고정하려면 환경 변수 `PORT`를 지정해 실행할 수 있습니다.

```powershell
# PowerShell — 포트를 3001 로 지정해 실행
$env:PORT=3001; npm run dev
```

```bash
# bash (macOS/Linux) — 포트를 3001 로 지정해 실행
PORT=3001 npm run dev
```

- 3000을 꼭 비우고 싶다면 점유 프로세스를 종료:

```powershell
# PowerShell — 3000 포트를 쓰는 프로세스 PID 확인 후 종료
netstat -ano | Select-String ":3000"
# 위에서 확인한 <PID> 를 넣어 종료
Stop-Process -Id <PID> -Force
```

```bash
# bash (macOS/Linux)
lsof -i :3000        # PID 확인
kill -9 <PID>
```

### 9.4 저장 시 세미콜론이 자동으로 붙는다 / 큰따옴표로 바뀐다

**증상:** 저장하면 oxfmt 규칙과 반대로 스타일이 바뀜.

**원인:** 다른 포매터(Prettier 등)가 기본 포매터로 잡혀 있음.

**해결:**

- `.vscode/settings.json`의 `editor.defaultFormatter`가 `oxc.oxc-vscode`인지 확인.
- 파일 우클릭 → "Format Document With..." → "Configure Default Formatter" →
  **oxc** 선택.
- 사용자(User) 설정에 전역 Prettier 강제가 걸려 있으면, 워크스페이스 설정이
  우선하도록 두거나 전역 강제를 해제하세요.

### 9.5 `.vue` 파일에서 타입 에러가 잡히지 않는다 / 잘못 잡힌다

**증상:** 일반 `tsc`로는 `.vue` 타입을 제대로 못 잡음. 또는 자동 임포트한
컴포넌트/스토어를 "찾을 수 없다"는 타입 오류가 남.

**원인:** `.vue` 타입 검사는 `tsc`가 아니라 **`nuxt typecheck`(내부적으로
vue-tsc)** 가 담당합니다. 또한 자동 임포트 타입은 **`.nuxt/`** 에 생성되므로
`.nuxt`가 없으면 타입을 못 찾습니다.

**해결:**

```sh
# .nuxt 타입이 없거나 꼬였으면 먼저 재생성
npm run postinstall   # nuxt prepare

# 타입 검사
npm run type-check
```

- `npm run type-check`(`nuxt typecheck`)가 `.vue`까지 포함한 타입 검사를 수행합니다.
- 자동 임포트가 타입에서 안 잡히면 대개 `.nuxt`가 비어 있거나 오래된 경우이니
  `npm run postinstall`로 재생성하세요.

### 9.6 ESLint가 에디터에서 빨간 줄을 안 보여준다

**증상:** 에디터에 린트 경고가 표시되지 않음.

**해결:**

- `dbaeumer.vscode-eslint` 확장 설치/활성화 확인.
- 이 프로젝트의 ESLint 설정은 **flat config(`eslint.config.ts`)** 입니다. 구버전
  ESLint 확장은 flat config를 못 읽을 수 있으니 확장을 최신으로 업데이트하세요.
- 그래도 안 되면 명령으로 확인: `npm run lint`.

### 9.7 oxc 포매터/린터가 동작하지 않는다

**증상:** 저장 시 포맷이 안 되거나 oxlint 결과가 에디터에 없음.

**해결:**

- `oxc.oxc-vscode` 확장 설치/활성화 확인.
- 명령줄로 직접 확인: `npm run format`, `npm run lint:oxlint`.

### 9.8 npm 스크립트가 `run-s`에서 멈춘다 / 빌드가 실패한다

**증상:** `npm run lint` 또는 `npm run build` 가 중간에 실패.

**원인:** `npm run lint`의 `run-s`(순차, `npm-run-all2`)는 하위 작업 중 하나가
실패하면 전체가 실패로 끝납니다. `npm run build`(`nuxt build`)는 타입 오류와는
별개로 빌드 자체 오류(서버 코드/SSR 등)로 실패할 수 있습니다.

**해결:** 어떤 단계가 실패했는지 메시지를 보고, 해당 단독 명령으로 재현하세요.

```sh
# lint 가 실패하면 단계별로
npm run lint:oxlint
npm run lint:eslint

# build 가 실패하면 타입과 빌드를 분리해 원인 파악
npm run type-check    # 타입 문제인지 (nuxt typecheck)
npm run build         # 빌드 자체 문제인지 (nuxt build)
```

> SSR 관련 오류(`window is not defined`, `localStorage is not defined` 등)가
> 빌드/실행 중 보이면 8.5 SSR 주의를 참고해 `import.meta.client` 가드를 적용하세요.

---

## 10. 환경 설정 완료 체크리스트 (DoD)

아래가 **모두 통과하면** FE 개발 환경 설정이 완료된 것입니다.

- [ ] `node -v` 결과가 `^22.18.0 || >=24.12.0` 범위 안 (예: `v24.16.0`)
- [ ] `npm -v`, `git --version` 정상 출력
- [ ] `npm install` 성공 (`node_modules/`, `package-lock.json` 존재)
- [ ] `postinstall`(nuxt prepare)로 **`.nuxt/` 폴더 생성** 확인 (없으면 `npm run postinstall`)
- [ ] `npm run dev` 실행 후 http://localhost:3000 에서 첫 화면 확인
- [ ] Home / About 파일 기반 라우팅 동작 확인 (`<NuxtLink>` 클릭)
- [ ] 코드 저장 시 oxc 포매터가 작동(세미콜론 제거·작은따옴표 변환) — VS Code 사용 시
- [ ] `npm run lint` 가 에러 없이 종료(종료 코드 0)
- [ ] `npm run type-check`(nuxt typecheck) 가 에러 없이 종료
- [ ] `npm run build` 성공(`.output/` 생성) 및 `npm run preview` 로 미리보기 확인
- [ ] 권장 VS Code 확장 4종 설치(`Vue.volar`, `dbaeumer.vscode-eslint`,
      `EditorConfig.EditorConfig`, `oxc.oxc-vscode`)

### 10.1 한 번에 검증하기 (선택)

아래를 순서대로 돌려 전부 통과하면 안심해도 됩니다.

```sh
npm run format
npm run lint
npm run type-check
npm run build
npm run preview   # 확인 후 Ctrl + C 로 종료
```

---

## 부록 A. 확인이 필요한 항목

이 문서는 실제 프로젝트 설정 파일을 근거로 작성했습니다. 다만 아래는 저장소에서
직접 정의되지 않아 **별도 확인이 필요**합니다.

- 테스트 러너(vitest/cypress/playwright)는 **현재 미설치**입니다. 테스트 환경이
  필요해지면 별도 가이드가 필요합니다.
- `tsconfig.json`은 `./.nuxt/tsconfig.json`을 확장만 하므로, 경로 별칭(`~`/`@`)
  등 실제 타입 설정은 **`.nuxt`가 생성된 뒤에야** 완전해집니다(설치 시 자동 생성).
- 이 문서의 `npm run dev` 출력 예시는 Nuxt/Nitro 버전 표기 형식을 단순화한
  것으로, 실제 출력 문구·버전은 설치된 버전에 따라 다를 수 있습니다.
