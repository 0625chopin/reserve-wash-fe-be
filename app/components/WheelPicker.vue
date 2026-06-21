<script setup lang="ts">
// 휠 방식 선택기 — 중앙 밴드에 맞춘 항목이 선택됨(iOS 스타일). 스크롤 스냅 + 클릭 지원.
import { onMounted, ref } from 'vue'

interface WheelItem {
  label: string
  value: string
  disabled?: boolean
}

const props = defineProps<{
  items: WheelItem[]
  testid?: string
}>()

const model = defineModel<string | null>()

// CSS의 항목 높이(px)와 반드시 일치
const ITEM_HEIGHT = 40
const listRef = ref<HTMLElement | null>(null)
let scrollTimer: ReturnType<typeof setTimeout> | null = null

// 마우스 드래그 스크롤 상태 — 데스크톱에서 휠을 끌어서 이동(터치/펜은 네이티브 스크롤 사용)
// pointerActive: 버튼이 눌린 상태(클릭 후보) / dragging: 임계값 초과 이동으로 실제 드래그 engage됨
let pointerActive = false
let dragging = false
let dragStartY = 0
let dragStartScroll = 0
let dragMoved = false
const DRAG_THRESHOLD = 4 // px — 이 미만 이동은 클릭으로 간주(항목 선택 보존)

function clampIndex(index: number): number {
  return Math.max(0, Math.min(props.items.length - 1, index))
}

// disabled면 가장 가까운 활성 항목 인덱스 반환(휴무일 등 건너뛰기)
function nearestEnabled(index: number): number {
  const items = props.items
  if (items[index] && !items[index].disabled) return index
  for (let d = 1; d < items.length; d++) {
    const up = index - d
    const down = index + d
    if (up >= 0 && items[up] && !items[up].disabled) return up
    if (down < items.length && items[down] && !items[down].disabled) return down
  }
  return index
}

function scrollToIndex(index: number, smooth = true) {
  const el = listRef.value
  if (!el) return
  el.scrollTo({ top: index * ITEM_HEIGHT, behavior: smooth ? 'smooth' : 'auto' })
}

function commitIndex(index: number) {
  const item = props.items[index]
  if (item) model.value = item.value
}

// 현재 스크롤 위치에서 가장 가까운(활성) 항목으로 스냅·확정
function snapToNearest() {
  const el = listRef.value
  if (!el) return
  const raw = clampIndex(Math.round(el.scrollTop / ITEM_HEIGHT))
  const index = nearestEnabled(raw)
  if (index !== raw) scrollToIndex(index)
  commitIndex(index)
}

// 스크롤이 멈추면 중앙 항목을 선택값으로 확정(드래그 중에는 release 시점에 스냅)
function onScroll() {
  const el = listRef.value
  if (!el) return
  if (scrollTimer) clearTimeout(scrollTimer)
  scrollTimer = setTimeout(() => {
    if (dragging) return
    snapToNearest()
  }, 120)
}

// 버튼 누름 — 클릭/드래그 후보 시작. 캡처는 실제 드래그가 시작될 때까지 미루어 항목 click을 보존.
function onPointerDown(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  const el = listRef.value
  if (!el) return
  pointerActive = true
  dragging = false
  dragMoved = false
  dragStartY = e.clientY
  dragStartScroll = el.scrollTop
}

function onPointerMove(e: PointerEvent) {
  if (!pointerActive) return
  const el = listRef.value
  if (!el) return
  const dy = e.clientY - dragStartY
  // 임계값을 넘는 순간에만 드래그 engage(이때부터 캡처·스냅 비활성). 그 전엔 클릭 후보 유지.
  if (!dragging) {
    if (Math.abs(dy) <= DRAG_THRESHOLD) return
    dragging = true
    dragMoved = true
    el.style.scrollSnapType = 'none' // 드래그 중 스냅 비활성 → 끊김 없이 따라옴
    try {
      el.setPointerCapture(e.pointerId)
    } catch {
      // pointerId가 유효하지 않아도 드래그 자체는 동작
    }
  }
  el.scrollTop = dragStartScroll - dy
}

// 버튼 뗌 — 드래그였으면 스냅 복원·확정, 순수 클릭이면 selectItem이 처리하도록 둠
function endDrag() {
  if (!pointerActive) return
  pointerActive = false
  const wasDragging = dragging
  dragging = false
  const el = listRef.value
  if (el) el.style.scrollSnapType = ''
  if (wasDragging) snapToNearest()
}

function selectItem(index: number) {
  // 드래그 끝에 발생하는 click은 무시(드래그로 이동한 위치 유지)
  if (dragMoved) {
    dragMoved = false
    return
  }
  const item = props.items[index]
  if (!item || item.disabled) return
  scrollToIndex(index)
  commitIndex(index)
}

onMounted(() => {
  const current = props.items.findIndex((i) => i.value === model.value)
  const index = current >= 0 ? current : nearestEnabled(0)
  scrollToIndex(index, false)
  commitIndex(index)
})
</script>

<template>
  <div class="wheel" :data-testid="testid">
    <!-- 중앙 선택 밴드 -->
    <div class="wheel-band" aria-hidden="true" />
    <ul
      ref="listRef"
      class="wheel-list"
      @scroll="onScroll"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="endDrag"
      @pointercancel="endDrag"
    >
      <li
        v-for="(item, index) in items"
        :key="item.value"
        :data-testid="testid ? `${testid}-item` : undefined"
        :data-value="item.value"
        :class="['wheel-item', { disabled: item.disabled }]"
        @click="selectItem(index)"
      >
        {{ item.label }}
      </li>
    </ul>
  </div>
</template>

<style scoped>
.wheel {
  position: relative;
  height: 200px; /* 5행 × 40px */
  overflow: hidden;
  border-radius: 0.75rem;
  border: 1px solid var(--color-line-soft);
  background: var(--color-surface-1);
}

/* 중앙 밴드 — 선택 영역 강조(스카이) */
.wheel-band {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 40px;
  transform: translateY(-50%);
  border-top: 1px solid color-mix(in oklab, var(--color-brand-primary) 45%, transparent);
  border-bottom: 1px solid color-mix(in oklab, var(--color-brand-primary) 45%, transparent);
  background: color-mix(in oklab, var(--color-brand-primary) 8%, transparent);
  pointer-events: none;
}

.wheel-list {
  position: relative;
  z-index: 1; /* 밴드 위에 텍스트가 보이도록 */
  height: 100%;
  margin: 0;
  padding: 80px 0; /* 첫/마지막 항목이 중앙에 올 수 있도록(2행분) */
  list-style: none;
  overflow-y: auto;
  overscroll-behavior: contain; /* 휠 스크롤이 페이지로 전파돼 흔들리는 것 방지 */
  scroll-snap-type: y mandatory;
  scrollbar-width: none;
  cursor: grab; /* 데스크톱: 끌어서 스크롤 가능함을 시사 */
  user-select: none; /* 드래그 중 텍스트 선택 방지 */
  touch-action: pan-y; /* 터치: 세로 네이티브 스크롤 유지 */
}
.wheel-list:active {
  cursor: grabbing;
}
.wheel-list::-webkit-scrollbar {
  display: none;
}

.wheel-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  scroll-snap-align: center;
  font-size: 0.9375rem;
  font-variant-numeric: tabular-nums;
  color: var(--color-content-muted);
  cursor: pointer;
  transition: color 0.15s var(--ease-out-soft);
}
.wheel-item:hover:not(.disabled) {
  color: var(--color-content-strong);
}
.wheel-item.disabled {
  color: color-mix(in oklab, var(--color-content-muted) 40%, transparent);
  text-decoration: line-through;
  cursor: not-allowed;
}
</style>
