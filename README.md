# start-kit2

This project is built with [Nuxt 4](https://nuxt.com/) (Vue 3 `<script setup>` + Vite + Nitro). See the [Nuxt documentation](https://nuxt.com/docs/getting-started/introduction) to learn more.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default. Type checking runs through `nuxt typecheck` (which uses `vue-tsc` internally). In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Nuxt Configuration Reference](https://nuxt.com/docs/api/configuration/nuxt-config).

## Project Setup

```sh
npm install
```

> `postinstall` runs `nuxt prepare` to generate the `.nuxt` types. If it does not run automatically, run `npm run postinstall` manually.

### Compile and Hot-Reload for Development

The dev server runs at http://localhost:3000 by default.

```sh
npm run dev
```

### Type-Check and Build for Production

```sh
npm run type-check
npm run build
```

### Preview the Production Build

```sh
npm run preview
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
