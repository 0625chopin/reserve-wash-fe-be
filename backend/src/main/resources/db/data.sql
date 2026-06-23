-- 2차 Phase 1 — 시드 데이터 (H2 in-memory, 기동 시 schema.sql 이후 주입)
-- 1차 FE 더미(app/data/*)와 동일 값으로 시드하여 FE↔BE 무변환 정합을 보장한다.
-- enum 값은 FE 리터럴 문자열 그대로 INSERT(MyBatis 기본 EnumTypeHandler 매핑).
-- Phase 0 Q1/Q5 확정: 베이 4등급(XLARGE 신설). VAN_ETC 예약 보존 위해 강남점에 XLARGE 베이(store1-A4) 추가
--   → 강남점 bay_count 3→4로 정합(T7에서 FE app/data/stores.ts도 동일 반영).

-- 매장 (app/data/stores.ts) — 전 매장 승인 완료(approved=true)
INSERT INTO store (id, name, bay_count, approved) VALUES
 ('store1', '강남점', 4, TRUE),
 ('store2', '홍대점', 2, TRUE),
 ('store3', '판교점', 3, TRUE);

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
--   manager_id: 로그인 매니저 계정 ↔ manager 엔티티 연결(require v1.10 §6.6). manager1→mgr1(담당 예약 조회 키). USER/STORE_ADMIN/ADMIN은 NULL.
--     (STORE_ADMIN은 매장 전체 예약을 store_id로 조회하므로 manager_id 불필요.)
INSERT INTO users (id, email, name, role, password_hash, approval_status, store_id, manager_id) VALUES
 ('user1', 'user@test.com', '홍길동', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user2', 'user2@test.com', '김고객', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('manager1', 'manager@test.com', '김매니저', 'MANAGER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store1', 'mgr1'),
 ('storeadmin1', 'storeadmin@test.com', '매장관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store1', NULL),
 ('admin1', 'admin@test.com', '관리자', 'ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 -- 매니저 계열 가입 2단계 승인 대기 더미(store1 소속, 1차 승인 대기 PENDING_APPROVAL_L1) — 승인 전이라 manager 엔티티 미연결(NULL)
 ('pending1', 'pending1@test.com', '신입매니저', 'MANAGER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'PENDING_APPROVAL_L1', 'store1', NULL),
 ('pending2', 'pending2@test.com', '대기매니저', 'MANAGER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'PENDING_APPROVAL_L1', 'store1', NULL);

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

-- ===========================================================================
-- 풍부한 테스트 데이터(볼륨 시드) — 기준일 2026-06-23
--   목적: BO/FO 화면(예약 목록·매출·후기·승인·휴무 달력)을 실제 데이터로 검증.
--   원칙: 가격은 price 매트릭스와 정확히 일치, 차종↔베이 크기 정합(VAN_ETC→XLARGE),
--         예약↔슬롯 점유 상태 정합(COMPLETED/RESERVED/HOLDING), CANCELED는 슬롯 해제(슬롯 미생성).
-- ===========================================================================

-- 추가 고객(USER) 8명 + 매장2 운영 계정(매니저/매장관리자). 비밀번호 모두 'password'.
INSERT INTO users (id, email, name, role, password_hash, approval_status, store_id, manager_id) VALUES
 ('user3',  'user3@test.com',  '이영희', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user4',  'user4@test.com',  '박철수', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user5',  'user5@test.com',  '정미경', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user6',  'user6@test.com',  '최동현', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user7',  'user7@test.com',  '강수진', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user8',  'user8@test.com',  '윤재호', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user9',  'user9@test.com',  '한지민', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user10', 'user10@test.com', '서준영', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 -- 매장2(홍대점) 운영 계정 — manager2는 mgr3(박매니저) 엔티티 연결, storeadmin2는 매장 단위 조회(manager_id 불필요)
 ('manager2',    'manager2@test.com',    '박매니저',   'MANAGER',     '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store2', 'mgr3'),
 ('storeadmin2', 'storeadmin2@test.com', '홍대관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store2', NULL);

-- 예약 26건 — 과거(COMPLETED)·미래(RESERVED)·진행중(HOLDING)·취소(CANCELED). amount는 price 매트릭스와 일치.
INSERT INTO reservation (id, user_id, store_id, bay_id, manager_id, `date`, time_slot, car_type, service_type, amount, status) VALUES
 -- 완료(강남점)
 ('res001', 'user1',  'store1', 'store1-A2', 'mgr1', '2026-06-10', '09:00', 'MID',     'FULL',    27000, 'COMPLETED'),
 ('res002', 'user2',  'store1', 'store1-A1', 'mgr2', '2026-06-11', '10:00', 'SMALL',   'EXT',     12000, 'COMPLETED'),
 ('res003', 'user3',  'store1', 'store1-A3', 'mgr1', '2026-06-12', '11:00', 'LARGE',   'PREMIUM', 45000, 'COMPLETED'),
 ('res004', 'user4',  'store1', 'store1-A4', 'mgr1', '2026-06-13', '13:00', 'VAN_ETC', 'FULL',    40000, 'COMPLETED'),
 ('res005', 'user5',  'store1', 'store1-A2', 'mgr2', '2026-06-15', '14:00', 'MID',     'INT',     15000, 'COMPLETED'),
 ('res006', 'user1',  'store1', 'store1-A1', 'mgr1', '2026-06-16', '15:00', 'SMALL',   'FULL',    22000, 'COMPLETED'),
 ('res007', 'user6',  'store1', 'store1-A3', 'mgr2', '2026-06-17', '09:30', 'LARGE',   'EXT',     18000, 'COMPLETED'),
 ('res008', 'user7',  'store1', 'store1-A2', 'mgr1', '2026-06-18', '10:30', 'MID',     'PREMIUM', 38000, 'COMPLETED'),
 -- 완료(홍대점)
 ('res009', 'user2',  'store2', 'store2-A1', 'mgr3', '2026-06-12', '10:00', 'SMALL',   'PREMIUM', 32000, 'COMPLETED'),
 ('res010', 'user8',  'store2', 'store2-A2', 'mgr3', '2026-06-14', '11:00', 'LARGE',   'FULL',    33000, 'COMPLETED'),
 ('res011', 'user3',  'store2', 'store2-A1', 'mgr3', '2026-06-19', '13:00', 'LIGHT',   'FULL',    18000, 'COMPLETED'),
 ('res012', 'user9',  'store2', 'store2-A2', 'mgr3', '2026-06-20', '14:00', 'LARGE',   'INT',     18000, 'COMPLETED'),
 -- 예약 확정(강남점, 미래)
 ('res013', 'user1',  'store1', 'store1-A2', 'mgr1', '2026-06-24', '09:00', 'MID',     'FULL',    27000, 'RESERVED'),
 ('res014', 'user2',  'store1', 'store1-A3', 'mgr2', '2026-06-24', '10:00', 'LARGE',   'EXT',     18000, 'RESERVED'),
 ('res015', 'user4',  'store1', 'store1-A4', 'mgr1', '2026-06-27', '11:00', 'VAN_ETC', 'PREMIUM', 55000, 'RESERVED'),
 ('res016', 'user5',  'store1', 'store1-A1', 'mgr2', '2026-06-26', '13:00', 'SMALL',   'INT',     12000, 'RESERVED'),
 ('res017', 'user10', 'store1', 'store1-A2', 'mgr1', '2026-06-27', '14:00', 'MID',     'EXT',     15000, 'RESERVED'),
 ('res018', 'user6',  'store1', 'store1-A3', 'mgr1', '2026-06-30', '15:00', 'LARGE',   'FULL',    33000, 'RESERVED'),
 -- 예약 확정(홍대점, 미래)
 ('res019', 'user2',  'store2', 'store2-A1', 'mgr3', '2026-06-24', '10:00', 'SMALL',   'FULL',    22000, 'RESERVED'),
 ('res020', 'user8',  'store2', 'store2-A2', 'mgr3', '2026-06-25', '11:00', 'LARGE',   'PREMIUM', 45000, 'RESERVED'),
 ('res021', 'user7',  'store2', 'store2-A1', 'mgr3', '2026-06-28', '13:00', 'LIGHT',   'EXT',     10000, 'RESERVED'),
 -- 진행중(점유, 확정 전) — manager_id NULL(대행 아님)
 ('res022', 'user9',  'store1', 'store1-A1', NULL,   '2026-06-23', '16:00', 'SMALL',   'EXT',     12000, 'HOLDING'),
 ('res023', 'user3',  'store1', 'store1-A3', NULL,   '2026-06-23', '16:30', 'LARGE',   'INT',     18000, 'HOLDING'),
 -- 취소
 ('res024', 'user1',  'store1', 'store1-A2', 'mgr1', '2026-06-21', '09:00', 'MID',     'EXT',     15000, 'CANCELED'),
 ('res025', 'user8',  'store2', 'store2-A2', 'mgr3', '2026-06-22', '10:00', 'LARGE',   'EXT',     18000, 'CANCELED'),
 ('res026', 'user5',  'store1', 'store1-A4', 'mgr1', '2026-06-26', '09:00', 'VAN_ETC', 'INT',     22000, 'CANCELED');

-- 후기 — 완료 예약 일부에 작성(res005·res012는 미작성으로 남겨 '완료·후기없음' 상태 검증). rating 1~5 다양.
INSERT INTO review (id, reservation_id, user_id, store_id, manager_id, rating, `text`, created_at) VALUES
 ('rev001', 'res001', 'user1', 'store1', 'mgr1', 5, '꼼꼼하게 세차해 주셔서 만족합니다. 다음에 또 올게요!',       '2026-06-10T11:30:00'),
 ('rev002', 'res002', 'user2', 'store1', 'mgr2', 4, '깔끔하고 빠릅니다. 대기 시간도 짧았어요.',                  '2026-06-11T11:15:00'),
 ('rev003', 'res003', 'user3', 'store1', 'mgr1', 5, '프리미엄 코스 정말 좋네요. 광택이 다릅니다.',               '2026-06-12T12:40:00'),
 ('rev004', 'res004', 'user4', 'store1', 'mgr1', 3, '큰 차도 받아줘서 좋은데 시간이 조금 오래 걸렸어요.',         '2026-06-13T15:10:00'),
 ('rev005', 'res006', 'user1', 'store1', 'mgr1', 4, '실내까지 깨끗해졌습니다. 가성비 좋아요.',                   '2026-06-16T16:20:00'),
 ('rev006', 'res007', 'user6', 'store1', 'mgr2', 2, '외부만 빠르게 했는데 일부 얼룩이 남아 있었어요.',           '2026-06-17T10:45:00'),
 ('rev007', 'res008', 'user7', 'store1', 'mgr1', 5, '프리미엄 추천합니다. 매니저분 친절하세요.',                 '2026-06-18T11:50:00'),
 ('rev008', 'res009', 'user2', 'store2', 'mgr3', 5, '홍대점도 만족스럽네요. 재방문 의사 있습니다.',              '2026-06-12T11:20:00'),
 ('rev009', 'res010', 'user8', 'store2', 'mgr3', 4, '대형차 풀세차 깔끔했습니다.',                              '2026-06-14T12:30:00'),
 ('rev010', 'res011', 'user3', 'store2', 'mgr3', 3, '무난했어요. 평범한 수준입니다.',                           '2026-06-19T14:15:00');

-- 슬롯 점유 — 위 예약과 정합. COMPLETED→COMPLETED / RESERVED→RESERVED / HOLDING→HOLDING. CANCELED는 슬롯 미생성(해제).
INSERT INTO slot (store_id, bay_id, `date`, time_slot, status, version) VALUES
 ('store1', 'store1-A2', '2026-06-10', '09:00', 'COMPLETED', 0),
 ('store1', 'store1-A1', '2026-06-11', '10:00', 'COMPLETED', 0),
 ('store1', 'store1-A3', '2026-06-12', '11:00', 'COMPLETED', 0),
 ('store1', 'store1-A4', '2026-06-13', '13:00', 'COMPLETED', 0),
 ('store1', 'store1-A2', '2026-06-15', '14:00', 'COMPLETED', 0),
 ('store1', 'store1-A1', '2026-06-16', '15:00', 'COMPLETED', 0),
 ('store1', 'store1-A3', '2026-06-17', '09:30', 'COMPLETED', 0),
 ('store1', 'store1-A2', '2026-06-18', '10:30', 'COMPLETED', 0),
 ('store2', 'store2-A1', '2026-06-12', '10:00', 'COMPLETED', 0),
 ('store2', 'store2-A2', '2026-06-14', '11:00', 'COMPLETED', 0),
 ('store2', 'store2-A1', '2026-06-19', '13:00', 'COMPLETED', 0),
 ('store2', 'store2-A2', '2026-06-20', '14:00', 'COMPLETED', 0),
 ('store1', 'store1-A2', '2026-06-24', '09:00', 'RESERVED', 0),
 ('store1', 'store1-A3', '2026-06-24', '10:00', 'RESERVED', 0),
 ('store1', 'store1-A4', '2026-06-27', '11:00', 'RESERVED', 0),
 ('store1', 'store1-A1', '2026-06-26', '13:00', 'RESERVED', 0),
 ('store1', 'store1-A2', '2026-06-27', '14:00', 'RESERVED', 0),
 ('store1', 'store1-A3', '2026-06-30', '15:00', 'RESERVED', 0),
 ('store2', 'store2-A1', '2026-06-24', '10:00', 'RESERVED', 0),
 ('store2', 'store2-A2', '2026-06-25', '11:00', 'RESERVED', 0),
 ('store2', 'store2-A1', '2026-06-28', '13:00', 'RESERVED', 0),
 ('store1', 'store1-A1', '2026-06-23', '16:00', 'HOLDING', 0),
 ('store1', 'store1-A3', '2026-06-23', '16:30', 'HOLDING', 0);

-- 매니저 휴무 추가 — APPROVED(확정·카탈로그 반영)와 SUBMITTED(결재 대기) 혼합.
INSERT INTO manager_dayoff (manager_id, `date`, dayoff_type, status) VALUES
 -- mgr2(강남점)는 매퍼 테스트가 '휴무 0건'을 단정하므로 카탈로그에 반영되는 APPROVED는 금지 → 결재 대기(SUBMITTED)로만 추가
 ('mgr2', '2026-06-25', 'SHIFT_2',  'SUBMITTED'),
 ('mgr2', '2026-06-28', 'FULL_DAY', 'SUBMITTED'),
 ('mgr4', '2026-06-26', 'FULL_DAY', 'APPROVED'),
 ('mgr3', '2026-06-30', 'SHIFT_1',  'SUBMITTED');
 -- (mgr1은 매퍼/카탈로그 테스트가 휴무 3건을 고정 단정하므로 추가하지 않음)

-- 매장 휴일 — CONFIRMED(확정)·SUBMITTED(대기)·REJECTED(반려) 혼합으로 결재 화면 검증.
INSERT INTO store_holiday (store_id, `date`, status) VALUES
 ('store1', '2026-06-29', 'CONFIRMED'),
 ('store1', '2026-07-15', 'CONFIRMED'),
 ('store2', '2026-07-04', 'SUBMITTED'),
 ('store2', '2026-06-30', 'REJECTED');

-- ===========================================================================
-- 확장 볼륨 시드 — 신규 매장 5개(store4~store8) + 매니저 8명 + 고객/예약/후기 대량 (기준일 2026-06-23)
--   목적: 다매장 환경에서 BO/FO 화면(매장 선택·예약 목록·매출·후기·승인·휴무 달력)을 풍부하게 검증.
--   ⚠ 기존 store1~store3·mgr1~mgr4 범위는 매퍼 테스트가 개수를 고정 단정하므로 절대 변경하지 않고,
--      신규 매장에만 추가한다. (전역 카운트 단정은 테스트 코드에서 신규 총계로 갱신)
--   원칙(상단 볼륨 시드와 동일): 가격=price 매트릭스 일치, 차종↔베이 크기 정합(VAN_ETC→XLARGE),
--      예약↔슬롯 상태 정합(COMPLETED/RESERVED/HOLDING), CANCELED는 슬롯 미생성(해제).
-- ===========================================================================

-- 신규 매장 5개 — 전부 승인 완료(FO 노출)
INSERT INTO store (id, name, bay_count, approved) VALUES
 ('store4', '잠실점', 4, TRUE),
 ('store5', '신촌점', 3, TRUE),
 ('store6', '수원점', 3, TRUE),
 ('store7', '분당점', 4, TRUE),
 ('store8', '일산점', 2, TRUE);

-- 신규 베이 16개 — 매장별 A1~A{bay_count}, 차종 수용 위해 크기 분산(XLARGE는 VAN_ETC 수용)
INSERT INTO bay (id, store_id, code, size) VALUES
 ('store4-A1', 'store4', 'A1', 'SMALL'),
 ('store4-A2', 'store4', 'A2', 'MID'),
 ('store4-A3', 'store4', 'A3', 'LARGE'),
 ('store4-A4', 'store4', 'A4', 'XLARGE'),
 ('store5-A1', 'store5', 'A1', 'SMALL'),
 ('store5-A2', 'store5', 'A2', 'MID'),
 ('store5-A3', 'store5', 'A3', 'LARGE'),
 ('store6-A1', 'store6', 'A1', 'SMALL'),
 ('store6-A2', 'store6', 'A2', 'MID'),
 ('store6-A3', 'store6', 'A3', 'LARGE'),
 ('store7-A1', 'store7', 'A1', 'SMALL'),
 ('store7-A2', 'store7', 'A2', 'MID'),
 ('store7-A3', 'store7', 'A3', 'LARGE'),
 ('store7-A4', 'store7', 'A4', 'XLARGE'),
 ('store8-A1', 'store8', 'A1', 'SMALL'),
 ('store8-A2', 'store8', 'A2', 'LARGE');

-- 신규 매니저 8명 — 매장별 최고권한(is_store_admin=TRUE) 1명 + 일반 매니저
INSERT INTO manager (id, store_id, name, is_store_admin) VALUES
 ('mgr5',  'store4', '이지은', TRUE),
 ('mgr6',  'store4', '정태양', FALSE),
 ('mgr7',  'store5', '김하늘', TRUE),
 ('mgr8',  'store6', '박서준', TRUE),
 ('mgr9',  'store6', '최유나', FALSE),
 ('mgr10', 'store7', '강민호', TRUE),
 ('mgr11', 'store7', '윤소라', FALSE),
 ('mgr12', 'store8', '임재현', TRUE);

-- 신규 운영 계정(MANAGER↔manager 엔티티 연결, STORE_ADMIN은 매장 단위 조회라 manager_id NULL) + 추가 고객 10명. 비밀번호 모두 'password'.
INSERT INTO users (id, email, name, role, password_hash, approval_status, store_id, manager_id) VALUES
 -- 신규 매장 운영 계정
 ('manager3',    'manager3@test.com',    '이지은',     'MANAGER',     '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store4', 'mgr5'),
 ('manager4',    'manager4@test.com',    '김하늘',     'MANAGER',     '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store5', 'mgr7'),
 ('manager5',    'manager5@test.com',    '박서준',     'MANAGER',     '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store6', 'mgr8'),
 ('manager6',    'manager6@test.com',    '강민호',     'MANAGER',     '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store7', 'mgr10'),
 ('manager7',    'manager7@test.com',    '임재현',     'MANAGER',     '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store8', 'mgr12'),
 ('storeadmin3', 'storeadmin3@test.com', '잠실관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store4', NULL),
 ('storeadmin4', 'storeadmin4@test.com', '신촌관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store5', NULL),
 ('storeadmin5', 'storeadmin5@test.com', '수원관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store6', NULL),
 ('storeadmin6', 'storeadmin6@test.com', '분당관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store7', NULL),
 ('storeadmin7', 'storeadmin7@test.com', '일산관리자', 'STORE_ADMIN', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', 'store8', NULL),
 -- 추가 고객 10명
 ('user11', 'user11@test.com', '오세훈', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user12', 'user12@test.com', '신예린', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user13', 'user13@test.com', '권도윤', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user14', 'user14@test.com', '배수지', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user15', 'user15@test.com', '문지후', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user16', 'user16@test.com', '조하늘', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user17', 'user17@test.com', '남궁민', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user18', 'user18@test.com', '하예나', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user19', 'user19@test.com', '구준회', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL),
 ('user20', 'user20@test.com', '진서연', 'USER', '$2a$10$k5ibZ1m9eEpsN1ZnJsGNhedyyMqtrWWPhz2UUFcFmeBdg5EnivgSq', 'ACTIVE', NULL, NULL);

-- 신규 매장 예약 32건(res027~res058) — 과거(COMPLETED)·미래(RESERVED)·진행중(HOLDING)·취소(CANCELED) 혼합.
--   manager_id는 해당 매장 소속 매니저, HOLDING은 NULL(대행 아님). store8(일산점)은 신규 오픈으로 예약 없음.
INSERT INTO reservation (id, user_id, store_id, bay_id, manager_id, `date`, time_slot, car_type, service_type, amount, status) VALUES
 -- 잠실점(store4) 완료
 ('res027', 'user11', 'store4', 'store4-A2', 'mgr5', '2026-06-05', '09:00', 'MID',     'FULL',    27000, 'COMPLETED'),
 ('res028', 'user12', 'store4', 'store4-A1', 'mgr6', '2026-06-07', '10:00', 'SMALL',   'EXT',     12000, 'COMPLETED'),
 ('res029', 'user13', 'store4', 'store4-A3', 'mgr5', '2026-06-09', '11:00', 'LARGE',   'PREMIUM', 45000, 'COMPLETED'),
 ('res030', 'user14', 'store4', 'store4-A4', 'mgr5', '2026-06-11', '13:00', 'VAN_ETC', 'FULL',    40000, 'COMPLETED'),
 ('res031', 'user11', 'store4', 'store4-A2', 'mgr6', '2026-06-14', '14:00', 'MID',     'INT',     15000, 'COMPLETED'),
 -- 잠실점 미래 확정
 ('res032', 'user12', 'store4', 'store4-A1', 'mgr5', '2026-06-25', '09:00', 'SMALL',   'FULL',    22000, 'RESERVED'),
 ('res033', 'user15', 'store4', 'store4-A3', 'mgr6', '2026-06-26', '10:00', 'LARGE',   'EXT',     18000, 'RESERVED'),
 ('res034', 'user16', 'store4', 'store4-A4', 'mgr5', '2026-06-28', '11:00', 'VAN_ETC', 'PREMIUM', 55000, 'RESERVED'),
 -- 잠실점 진행중 / 취소
 ('res035', 'user13', 'store4', 'store4-A1', NULL,   '2026-06-23', '16:00', 'SMALL',   'EXT',     12000, 'HOLDING'),
 ('res036', 'user14', 'store4', 'store4-A2', 'mgr5', '2026-06-27', '09:00', 'MID',     'EXT',     15000, 'CANCELED'),
 -- 신촌점(store5) 완료
 ('res037', 'user15', 'store5', 'store5-A2', 'mgr7', '2026-06-06', '09:00', 'MID',     'FULL',    27000, 'COMPLETED'),
 ('res038', 'user16', 'store5', 'store5-A1', 'mgr7', '2026-06-08', '10:00', 'SMALL',   'PREMIUM', 32000, 'COMPLETED'),
 ('res039', 'user17', 'store5', 'store5-A3', 'mgr7', '2026-06-12', '11:00', 'LARGE',   'FULL',    33000, 'COMPLETED'),
 -- 신촌점 미래 확정 / 진행중 / 취소
 ('res040', 'user18', 'store5', 'store5-A2', 'mgr7', '2026-06-24', '13:00', 'MID',     'PREMIUM', 38000, 'RESERVED'),
 ('res041', 'user19', 'store5', 'store5-A3', 'mgr7', '2026-06-29', '14:00', 'LARGE',   'INT',     18000, 'RESERVED'),
 ('res042', 'user17', 'store5', 'store5-A1', NULL,   '2026-06-23', '16:30', 'SMALL',   'INT',     12000, 'HOLDING'),
 ('res043', 'user18', 'store5', 'store5-A2', 'mgr7', '2026-06-30', '09:00', 'MID',     'EXT',     15000, 'CANCELED'),
 -- 수원점(store6) 완료
 ('res044', 'user19', 'store6', 'store6-A1', 'mgr8', '2026-06-04', '09:00', 'SMALL',   'FULL',    22000, 'COMPLETED'),
 ('res045', 'user20', 'store6', 'store6-A2', 'mgr9', '2026-06-10', '10:00', 'MID',     'EXT',     15000, 'COMPLETED'),
 ('res046', 'user11', 'store6', 'store6-A3', 'mgr8', '2026-06-15', '11:00', 'LARGE',   'PREMIUM', 45000, 'COMPLETED'),
 -- 수원점 미래 확정 / 진행중 / 취소
 ('res047', 'user12', 'store6', 'store6-A1', 'mgr9', '2026-06-25', '13:00', 'LIGHT',   'FULL',    18000, 'RESERVED'),
 ('res048', 'user20', 'store6', 'store6-A3', 'mgr8', '2026-07-01', '14:00', 'LARGE',   'FULL',    33000, 'RESERVED'),
 ('res049', 'user19', 'store6', 'store6-A2', NULL,   '2026-06-23', '17:00', 'MID',     'INT',     15000, 'HOLDING'),
 ('res050', 'user20', 'store6', 'store6-A1', 'mgr8', '2026-06-28', '09:00', 'SMALL',   'EXT',     12000, 'CANCELED'),
 -- 분당점(store7) 완료
 ('res051', 'user13', 'store7', 'store7-A2', 'mgr10', '2026-06-03', '09:00', 'MID',     'PREMIUM', 38000, 'COMPLETED'),
 ('res052', 'user14', 'store7', 'store7-A4', 'mgr10', '2026-06-13', '10:00', 'VAN_ETC', 'INT',     22000, 'COMPLETED'),
 ('res053', 'user15', 'store7', 'store7-A3', 'mgr11', '2026-06-18', '11:00', 'LARGE',   'EXT',     18000, 'COMPLETED'),
 -- 분당점 미래 확정 / 진행중 / 취소
 ('res054', 'user16', 'store7', 'store7-A1', 'mgr10', '2026-06-26', '13:00', 'SMALL',   'PREMIUM', 32000, 'RESERVED'),
 ('res055', 'user17', 'store7', 'store7-A4', 'mgr11', '2026-07-02', '14:00', 'VAN_ETC', 'FULL',    40000, 'RESERVED'),
 ('res056', 'user18', 'store7', 'store7-A2', 'mgr10', '2026-07-05', '15:00', 'MID',     'FULL',    27000, 'RESERVED'),
 ('res057', 'user14', 'store7', 'store7-A3', NULL,    '2026-06-23', '17:30', 'LARGE',   'INT',     18000, 'HOLDING'),
 ('res058', 'user15', 'store7', 'store7-A2', 'mgr11', '2026-06-29', '09:00', 'MID',     'EXT',     15000, 'CANCELED');

-- 신규 매장 후기 9건 — 완료 예약 일부에 작성(일부 완료 예약은 미작성으로 '완료·후기없음' 상태 유지). rating 2~5 분산.
INSERT INTO review (id, reservation_id, user_id, store_id, manager_id, rating, `text`, created_at) VALUES
 ('rev011', 'res027', 'user11', 'store4', 'mgr5',  5, '잠실점 풀세차 만족합니다. 매니저분이 꼼꼼하세요.',          '2026-06-05T11:00:00'),
 ('rev012', 'res029', 'user13', 'store4', 'mgr5',  4, '대형차 프리미엄 광택 좋네요. 주차도 편했어요.',            '2026-06-09T12:30:00'),
 ('rev013', 'res030', 'user14', 'store4', 'mgr5',  3, '특대형도 받아줘서 좋은데 살짝 대기했습니다.',              '2026-06-11T15:00:00'),
 ('rev014', 'res037', 'user15', 'store5', 'mgr7',  5, '신촌점 접근성 좋고 마감 깔끔합니다.',                      '2026-06-06T11:10:00'),
 ('rev015', 'res039', 'user17', 'store5', 'mgr7',  4, '풀세차 가성비 괜찮아요. 재방문 의사 있습니다.',            '2026-06-12T12:40:00'),
 ('rev016', 'res044', 'user19', 'store6', 'mgr8',  5, '수원점 친절하고 빠릅니다. 추천해요.',                      '2026-06-04T10:30:00'),
 ('rev017', 'res046', 'user11', 'store6', 'mgr8',  2, '프리미엄인데 일부 얼룩이 남아 아쉬웠습니다.',              '2026-06-15T12:20:00'),
 ('rev018', 'res051', 'user13', 'store7', 'mgr10', 5, '분당점 프리미엄 최고예요. 광택 오래갑니다.',               '2026-06-03T10:40:00'),
 ('rev019', 'res053', 'user15', 'store7', 'mgr11', 4, '외부세차 빠르고 깔끔했어요.',                              '2026-06-18T12:00:00');

-- 신규 매장 슬롯 점유 — 위 예약과 정합. COMPLETED→COMPLETED / RESERVED→RESERVED / HOLDING→HOLDING. CANCELED는 슬롯 미생성.
INSERT INTO slot (store_id, bay_id, `date`, time_slot, status, version) VALUES
 -- COMPLETED
 ('store4', 'store4-A2', '2026-06-05', '09:00', 'COMPLETED', 0),
 ('store4', 'store4-A1', '2026-06-07', '10:00', 'COMPLETED', 0),
 ('store4', 'store4-A3', '2026-06-09', '11:00', 'COMPLETED', 0),
 ('store4', 'store4-A4', '2026-06-11', '13:00', 'COMPLETED', 0),
 ('store4', 'store4-A2', '2026-06-14', '14:00', 'COMPLETED', 0),
 ('store5', 'store5-A2', '2026-06-06', '09:00', 'COMPLETED', 0),
 ('store5', 'store5-A1', '2026-06-08', '10:00', 'COMPLETED', 0),
 ('store5', 'store5-A3', '2026-06-12', '11:00', 'COMPLETED', 0),
 ('store6', 'store6-A1', '2026-06-04', '09:00', 'COMPLETED', 0),
 ('store6', 'store6-A2', '2026-06-10', '10:00', 'COMPLETED', 0),
 ('store6', 'store6-A3', '2026-06-15', '11:00', 'COMPLETED', 0),
 ('store7', 'store7-A2', '2026-06-03', '09:00', 'COMPLETED', 0),
 ('store7', 'store7-A4', '2026-06-13', '10:00', 'COMPLETED', 0),
 ('store7', 'store7-A3', '2026-06-18', '11:00', 'COMPLETED', 0),
 -- RESERVED
 ('store4', 'store4-A1', '2026-06-25', '09:00', 'RESERVED', 0),
 ('store4', 'store4-A3', '2026-06-26', '10:00', 'RESERVED', 0),
 ('store4', 'store4-A4', '2026-06-28', '11:00', 'RESERVED', 0),
 ('store5', 'store5-A2', '2026-06-24', '13:00', 'RESERVED', 0),
 ('store5', 'store5-A3', '2026-06-29', '14:00', 'RESERVED', 0),
 ('store6', 'store6-A1', '2026-06-25', '13:00', 'RESERVED', 0),
 ('store6', 'store6-A3', '2026-07-01', '14:00', 'RESERVED', 0),
 ('store7', 'store7-A1', '2026-06-26', '13:00', 'RESERVED', 0),
 ('store7', 'store7-A4', '2026-07-02', '14:00', 'RESERVED', 0),
 ('store7', 'store7-A2', '2026-07-05', '15:00', 'RESERVED', 0),
 -- HOLDING
 ('store4', 'store4-A1', '2026-06-23', '16:00', 'HOLDING', 0),
 ('store5', 'store5-A1', '2026-06-23', '16:30', 'HOLDING', 0),
 ('store6', 'store6-A2', '2026-06-23', '17:00', 'HOLDING', 0),
 ('store7', 'store7-A3', '2026-06-23', '17:30', 'HOLDING', 0);

-- 신규 매니저 휴무 — APPROVED(확정·카탈로그 반영)·SUBMITTED(결재 대기) 혼합. 모두 7월(신규 매장 예약과 날짜 미충돌).
INSERT INTO manager_dayoff (manager_id, `date`, dayoff_type, status) VALUES
 ('mgr5',  '2026-07-08', 'FULL_DAY', 'APPROVED'),
 ('mgr5',  '2026-07-10', 'SHIFT_1',  'SUBMITTED'),
 ('mgr6',  '2026-07-03', 'SHIFT_2',  'APPROVED'),
 ('mgr7',  '2026-07-06', 'FULL_DAY', 'APPROVED'),
 ('mgr8',  '2026-07-07', 'SHIFT_1',  'SUBMITTED'),
 ('mgr9',  '2026-07-09', 'FULL_DAY', 'APPROVED'),
 ('mgr10', '2026-07-11', 'SHIFT_2',  'SUBMITTED'),
 ('mgr11', '2026-07-12', 'FULL_DAY', 'APPROVED'),
 ('mgr12', '2026-07-04', 'SHIFT_1',  'SUBMITTED');

-- 신규 매장 휴일 — CONFIRMED·SUBMITTED 혼합으로 결재 화면 검증.
INSERT INTO store_holiday (store_id, `date`, status) VALUES
 ('store4', '2026-07-20', 'CONFIRMED'),
 ('store5', '2026-07-22', 'SUBMITTED'),
 ('store6', '2026-07-25', 'CONFIRMED'),
 ('store7', '2026-08-01', 'SUBMITTED');
