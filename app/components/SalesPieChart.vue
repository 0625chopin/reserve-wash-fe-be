<script setup lang="ts">
// 매출 원형(파이) 차트(v2.4) — 외부 차트 라이브러리 없이 CSS conic-gradient로 렌더.
//   슬라이스(상위 5개 + ETC)와 범례를 표시한다. 가공은 useSalesChart.buildSalesSlices가 담당.
import { computed } from 'vue'
import type { SalesSlice } from '~/composables/useSalesChart'

const props = defineProps<{ slices: SalesSlice[] }>()

// 누적 비중으로 conic-gradient 색 구간 생성: "color1 0% 30%, color2 30% 55%, ..."
const gradient = computed(() => {
  if (!props.slices.length) return 'var(--color-surface-2)'
  let acc = 0
  const stops = props.slices.map((s) => {
    const start = acc
    acc += s.percent
    return `${s.color} ${start}% ${acc}%`
  })
  return `conic-gradient(${stops.join(', ')})`
})

function won(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}
</script>

<template>
  <div data-testid="admin-sales-pie" class="flex flex-col items-center gap-6 sm:flex-row sm:items-start">
    <!-- 원형 차트 -->
    <div
      v-if="slices.length"
      class="size-44 shrink-0 rounded-full"
      :style="{ background: gradient }"
      role="img"
      aria-label="매장별 매출 비중 원형 차트"
    />
    <p v-else data-testid="admin-sales-pie-empty" class="text-sm text-[--color-content-muted]">
      매출 데이터가 없습니다.
    </p>

    <!-- 범례 -->
    <ul v-if="slices.length" class="flex-1 space-y-2">
      <li
        v-for="s in slices"
        :key="s.label"
        :data-testid="`admin-sales-slice-${s.label}`"
        class="flex items-center justify-between gap-3 text-sm"
      >
        <span class="flex items-center gap-2">
          <span class="inline-block size-3 rounded-sm" :style="{ backgroundColor: s.color }" />
          <span class="font-medium text-[--color-content-strong]">{{ s.label }}</span>
        </span>
        <span class="text-[--color-content-muted]">
          {{ s.percent }}% · {{ won(s.amount) }}
        </span>
      </li>
    </ul>
  </div>
</template>
