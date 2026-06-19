import type { User } from '~/types/domain'

// 로그인용 더미 사용자 (require 3.1).
// Phase 3 더미 로그인은 email로 조회하며 비밀번호는 'password'로 통일 가정(ROADMAP Phase 3).
export const users: User[] = [
  { id: 'user1', email: 'user@test.com', name: '홍길동', role: 'USER' },
  { id: 'user2', email: 'user2@test.com', name: '김고객', role: 'USER' },
  { id: 'manager1', email: 'manager@test.com', name: '김매니저', role: 'MANAGER' },
  { id: 'admin1', email: 'admin@test.com', name: '관리자', role: 'ADMIN' },
]
