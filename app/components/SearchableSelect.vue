<script setup lang="ts" generic="T">
// 재사용 검색 select — select 박스 + 텍스트 입력 실시간 필터 (require 6.3)
import { computed, ref } from 'vue'

const props = defineProps<{
  options: T[]
  labelKey: keyof T
  valueKey: keyof T
  placeholder?: string
  testid?: string
}>()

// 선택값(valueKey 값) — v-model 양방향 바인딩
const model = defineModel<T[keyof T] | null>()

const keyword = ref('')
const open = ref(false)
const highlighted = ref(0)

// 두 인스턴스 구분용 testid 네임스페이스
const tid = computed(() => props.testid ?? 'searchable')

// 텍스트 입력 시 labelKey 기준 대소문자 무시 필터 (require 6.3)
const filtered = computed(() =>
  props.options.filter((o) =>
    String(o[props.labelKey]).toLowerCase().includes(keyword.value.toLowerCase()),
  ),
)

function select(option: T) {
  model.value = option[props.valueKey]
  keyword.value = String(option[props.labelKey])
  open.value = false
}

// 입력 시 목록 열고 하이라이트 초기화 (인라인 복수 문장은 oxfmt와 충돌하므로 메서드로 분리)
function onInput() {
  open.value = true
  highlighted.value = 0
}

// 키보드 접근성(가능 범위): 화살표 이동 / Enter 선택 / Esc 닫기
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    open.value = true
    highlighted.value = Math.min(filtered.value.length - 1, highlighted.value + 1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    highlighted.value = Math.max(0, highlighted.value - 1)
  } else if (event.key === 'Enter') {
    const option = filtered.value[highlighted.value]
    if (option) {
      event.preventDefault()
      select(option)
    }
  } else if (event.key === 'Escape') {
    open.value = false
  }
}
</script>

<template>
  <div class="searchable-select">
    <!-- 검색 입력 — 좌측 돋보기 아이콘 + 공통 .input-field 스타일 -->
    <div class="input-wrap">
      <svg class="search-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2" />
        <path d="m20 20-3-3" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      </svg>
      <input
        v-model="keyword"
        :placeholder="placeholder"
        :data-testid="`${tid}-input`"
        class="input-field has-icon"
        @focus="open = true"
        @input="onInput"
        @keydown="onKeydown"
      />
    </div>

    <!-- 드롭다운 — 다크 표면/보더/하이라이트 -->
    <ul v-if="open && filtered.length" :data-testid="`${tid}-options`" class="options">
      <li
        v-for="(option, index) in filtered"
        :key="String(option[valueKey])"
        :data-testid="`${tid}-option`"
        :class="{ active: index === highlighted }"
        @mousedown.prevent="select(option)"
      >
        {{ option[labelKey] }}
      </li>
    </ul>
  </div>
</template>

<style scoped>
.searchable-select {
  position: relative;
  width: 100%;
}

/* 입력 래퍼 — 아이콘 절대배치 */
.input-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  top: 50%;
  left: 0.875rem;
  width: 1.125rem;
  height: 1.125rem;
  transform: translateY(-50%);
  color: var(--color-content-muted);
  pointer-events: none;
}
/* 아이콘 자리만큼 좌측 패딩 확보 */
.input-field.has-icon {
  padding-left: 2.5rem;
}

/* 드롭다운 패널 — 표면 위계/그림자/라운드 */
.options {
  position: absolute;
  z-index: 20;
  top: calc(100% + 0.375rem);
  left: 0;
  right: 0;
  margin: 0;
  padding: 0.375rem;
  list-style: none;
  border-radius: 0.75rem;
  border: 1px solid var(--color-line);
  background: var(--color-surface-2);
  max-height: 16rem;
  overflow-y: auto;
  box-shadow: 0 16px 40px -16px rgb(0 0 0 / 0.7);
}

/* 옵션 항목 — 기본/hover/active(키보드 하이라이트) */
.options li {
  display: flex;
  align-items: center;
  border-radius: 0.5rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.9375rem;
  color: var(--color-content);
  cursor: pointer;
  transition:
    background-color 0.15s var(--ease-out-soft),
    color 0.15s var(--ease-out-soft);
}
.options li:hover {
  background: color-mix(in oklab, var(--color-brand-primary) 14%, transparent);
  color: var(--color-content-strong);
}
/* 키보드 하이라이트 — 좌측 라임 액센트 바 + 강조 표면 */
.options li.active {
  background: color-mix(in oklab, var(--color-brand-primary) 20%, transparent);
  color: var(--color-content-strong);
  box-shadow: inset 2px 0 0 var(--color-brand-accent);
}
</style>
