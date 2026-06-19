<script setup lang="ts" generic="T">
// 재사용 검색 select — select 박스 + 텍스트 입력 실시간 필터 (require 6.3)
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

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
// 사용자가 직접 타이핑 중인지 여부 — 선택 후 재오픈 시 전체 목록을 보여주기 위함
const isSearching = ref(false)

// 두 인스턴스 구분용 testid 네임스페이스
const tid = computed(() => props.testid ?? 'searchable')

// 타이핑 중이면 keyword로 필터, 아니면(선택 표시 상태) 전체 목록 노출 (require 6.3)
const filtered = computed(() => {
  if (!isSearching.value) return props.options
  const q = keyword.value.toLowerCase()
  return props.options.filter((o) => String(o[props.labelKey]).toLowerCase().includes(q))
})

function select(option: T) {
  model.value = option[props.valueKey]
  keyword.value = String(option[props.labelKey])
  isSearching.value = false
  open.value = false
}

// 입력 시 목록 열고 검색 모드 진입 (인라인 복수 문장은 oxfmt와 충돌하므로 메서드로 분리)
function onInput() {
  open.value = true
  isSearching.value = true
  highlighted.value = 0
}

// 포커스 시 목록 열고, 기존 텍스트를 전체 선택해 바로 재검색 가능하게 함
function onFocus(event: FocusEvent) {
  open.value = true
  isSearching.value = false
  const input = event.target as HTMLInputElement
  input.select()
}

// 클릭 시(선택 후 포커스가 유지돼 focus 이벤트가 없을 때 포함) 목록을 다시 연다
function onClick() {
  open.value = true
  isSearching.value = false
}

// 외부에서 model이 바뀌면(예: 매장 변경으로 매니저 초기화) 입력 표시를 동기화
watch(model, (value) => {
  if (value == null) {
    keyword.value = ''
    isSearching.value = false
    return
  }
  const selected = props.options.find((o) => o[props.valueKey] === value)
  if (selected) {
    keyword.value = String(selected[props.labelKey])
    isSearching.value = false
  }
})

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

// 컴포넌트 루트 — 바깥 클릭 판별용
const rootRef = ref<HTMLElement | null>(null)

// 바깥(다른 select 포함)을 클릭하면 이 목록을 닫는다.
// 다른 SearchableSelect를 클릭해도 이 핸들러가 발동해 자동으로 한 번에 하나만 열림.
function onDocumentPointerDown(event: MouseEvent) {
  if (!open.value) return
  const root = rootRef.value
  if (root && !root.contains(event.target as Node)) {
    open.value = false
  }
}

onMounted(() => {
  document.addEventListener('mousedown', onDocumentPointerDown)
})
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocumentPointerDown)
})
</script>

<template>
  <div ref="rootRef" class="searchable-select">
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
        @focus="onFocus"
        @click="onClick"
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
        :class="{ active: index === highlighted, selected: option[valueKey] === model }"
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
/* 키보드 하이라이트 — 강조 표면만(좌측 바 없음. 앞테두리는 선택값 전용) */
.options li.active {
  background: color-mix(in oklab, var(--color-brand-primary) 20%, transparent);
  color: var(--color-content-strong);
}

/* 현재 선택된 값 — 좌측 초록색 앞테두리로 표시 */
.options li.selected {
  color: var(--color-content-strong);
  box-shadow: inset 2px 0 0 var(--color-brand-accent);
}
</style>
