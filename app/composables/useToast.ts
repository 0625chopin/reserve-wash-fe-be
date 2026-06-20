import { readonly } from 'vue'

// 토스트 알림 상태 — 충돌/안내 등 단문 메시지 표시용 (require 7.1 충돌 재선택 UX)
interface ToastState {
  visible: boolean
  message: string
}

// 자동 닫힘 타이머 — 클라이언트에서만 동작(모듈 스코프)
let timer: ReturnType<typeof setTimeout> | null = null

// 재사용 토스트 컴포저블 — SSR 직렬화 안전을 위해 useState로 전역 단일 인스턴스(요청 간 상태 누수 방지)
export function useToast() {
  const toast = useState<ToastState>('toast', () => ({ visible: false, message: '' }))

  // 메시지 표시 후 durationMs 뒤 자동 닫힘 — 타이머는 브라우저 전용(import.meta.client 가드)
  function show(message: string, durationMs = 4000) {
    toast.value = { visible: true, message }
    if (import.meta.client) {
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => {
        toast.value.visible = false
      }, durationMs)
    }
  }

  function hide() {
    toast.value.visible = false
    if (import.meta.client && timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  // 외부에서는 show/hide로만 제어하도록 상태는 읽기 전용으로 노출
  return { toast: readonly(toast), show, hide }
}
