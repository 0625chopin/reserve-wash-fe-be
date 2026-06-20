import type { User } from '~/types/domain'

// 로그인용 더미 사용자 (require 3.1).
// Phase 3 더미 로그인은 email로 조회하며, 시드 사용자 비밀번호는 'password'로 통일 가정(ROADMAP Phase 3).
export const users: User[] = [
  { id: 'user1', email: 'user@test.com', name: '홍길동', role: 'USER' },
  { id: 'user2', email: 'user2@test.com', name: '김고객', role: 'USER' },
  { id: 'manager1', email: 'manager@test.com', name: '김매니저', role: 'MANAGER' },
  { id: 'admin1', email: 'admin@test.com', name: '관리자', role: 'ADMIN' },
]

// Phase 3.1 회원가입(FW1) — 가입 사용자의 비밀번호를 in-memory로 보관(email → password).
// 시드 사용자는 이 맵에 없으므로 login()이 통일 더미 비번 'password'로 검증하고,
// 가입 사용자는 입력한 비번으로 재로그인할 수 있다. (1단계 한계 — 새로고침 시 초기화)
// TODO(2단계): 서버/DB 인증으로 교체되면 이 맵은 제거(require 12.3).
export const credentials: Record<string, string> = {}
