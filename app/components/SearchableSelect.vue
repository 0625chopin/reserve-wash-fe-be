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
    <input
      v-model="keyword"
      :placeholder="placeholder"
      :data-testid="`${tid}-input`"
      @focus="open = true"
      @input="onInput"
      @keydown="onKeydown"
    />
    <ul v-if="open && filtered.length" :data-testid="`${tid}-options`">
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
  display: inline-block;
}

.searchable-select ul {
  position: absolute;
  z-index: 10;
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid var(--color-border, #ccc);
  background: var(--color-background, #fff);
  min-width: 12rem;
  max-height: 16rem;
  overflow-y: auto;
}

.searchable-select li {
  padding: 0.4rem 0.8rem;
  cursor: pointer;
}

.searchable-select li.active,
.searchable-select li:hover {
  background: var(--color-background-mute, #eee);
}
</style>
