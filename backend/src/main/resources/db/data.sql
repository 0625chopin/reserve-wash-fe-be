-- 2차 Phase 1 — 시드 데이터 (H2 in-memory, 기동 시 schema.sql 이후 주입)
-- 1차 FE 더미(app/data/*)와 동일 값으로 시드하여 FE↔BE 무변환 정합을 보장한다.
-- enum 값은 FE 리터럴 문자열 그대로 INSERT(MyBatis 기본 EnumTypeHandler 매핑).
-- Phase 0 Q1/Q5 확정: 베이 4등급(XLARGE 신설). VAN_ETC 예약 보존 위해 강남점에 XLARGE 베이(store1-A4) 추가
--   → 강남점 bay_count 3→4로 정합(T7에서 FE app/data/stores.ts도 동일 반영).

-- 매장 (app/data/stores.ts) — store3은 미승인(approved=false)
INSERT INTO store (id, name, bay_count, approved) VALUES
 ('store1', '강남점', 4, TRUE),
 ('store2', '홍대점', 2, TRUE),
 ('store3', '판교점', 3, FALSE);

-- 베이 (app/data/stores.ts + XLARGE 신설 store1-A4) — size = BaySize enum
INSERT INTO bay (id, store_id, code, size) VALUES
 ('store1-A1', 'store1', 'A1', 'SMALL'),
 ('store1-A2', 'store1', 'A2', 'MID'),
 ('store1-A3', 'store1', 'A3', 'LARGE'),
 ('store1-A4', 'store1', 'A4', 'XLARGE'),   -- 특대형 신설(VAN_ETC 수용)
 ('store2-A1', 'store2', 'A1', 'SMALL'),
 ('store2-A2', 'store2', 'A2', 'LARGE'),
 ('store3-A1', 'store3', 'A1', 'MID'),
 ('store3-A2', 'store3', 'A2', 'LARGE'),
 ('store3-A3', 'store3', 'A3', 'LARGE');

-- 매니저 (app/data/managers.ts) — is_store_admin = 매장 최고권한
INSERT INTO manager (id, store_id, name, is_store_admin) VALUES
 ('mgr1', 'store1', '김매니저', TRUE),
 ('mgr2', 'store1', '이매니저', FALSE),
 ('mgr3', 'store2', '박매니저', TRUE),
 ('mgr4', 'store3', '최매니저', TRUE);

-- 매니저 휴무 (app/data/managers.ts) — dayoff_type = DayoffType enum
INSERT INTO manager_dayoff (manager_id, `date`, dayoff_type) VALUES
 ('mgr1', '2026-06-22', 'FULL_DAY'),
 ('mgr1', '2026-06-23', 'SHIFT_1'),
 ('mgr1', '2026-06-29', 'FULL_DAY'),
 ('mgr3', '2026-06-23', 'SHIFT_2');

-- 사용자 (app/data/users.ts) — password_hash = BCrypt('password') 통일(1차 더미 비번과 동일)
--   approval_status: 기존 계정은 ACTIVE(로그인 가능). pending* 2명은 매니저 가입 2단계 승인(M7→S3) 검증용 더미.
INSERT INTO users (id, email, name, role, password_hash, approval_status) VALUES
 ('user1', 'user@test.com', '홍길동', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE'),
 ('user2', 'user2@test.com', '김고객', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE'),
 ('manager1', 'manager@test.com', '김매니저', 'MANAGER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE'),
 ('storeadmin1', 'storeadmin@test.com', '매장관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE'),
 ('admin1', 'admin@test.com', '관리자', 'ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE'),
 -- 매니저 계열 가입 2단계 승인 대기 더미(store1 소속, 1차 승인 대기 PENDING_APPROVAL_L1)
 ('pending1', 'pending1@test.com', '신입매니저', 'MANAGER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'PENDING_APPROVAL_L1'),
 ('pending2', 'pending2@test.com', '대기매니저', 'MANAGER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'PENDING_APPROVAL_L1');

-- 가격 매트릭스 20행 (app/data/prices.ts = require 10.3 확정 단가)
INSERT INTO price (car_type, service_type, amount) VALUES
 ('LIGHT', 'EXT', 10000), ('LIGHT', 'INT', 10000), ('LIGHT', 'FULL', 18000), ('LIGHT', 'PREMIUM', 28000),
 ('SMALL', 'EXT', 12000), ('SMALL', 'INT', 12000), ('SMALL', 'FULL', 22000), ('SMALL', 'PREMIUM', 32000),
 ('MID', 'EXT', 15000), ('MID', 'INT', 15000), ('MID', 'FULL', 27000), ('MID', 'PREMIUM', 38000),
 ('LARGE', 'EXT', 18000), ('LARGE', 'INT', 18000), ('LARGE', 'FULL', 33000), ('LARGE', 'PREMIUM', 45000),
 ('VAN_ETC', 'EXT', 22000), ('VAN_ETC', 'INT', 22000), ('VAN_ETC', 'FULL', 40000), ('VAN_ETC', 'PREMIUM', 55000);

-- 슬롯 선점유 시드 (app/services/reservationService.ts) — 강남점 A1 2026-06-25 10:00 = RESERVED
-- 1차 E2E의 베이 점유 비활성·동시 예약 충돌 시나리오를 결정적으로 재현(version 0 = 낙관락 초기값)
INSERT INTO slot (store_id, bay_id, `date`, time_slot, status, version) VALUES
 ('store1', 'store1-A1', '2026-06-25', '10:00', 'RESERVED', 0);
