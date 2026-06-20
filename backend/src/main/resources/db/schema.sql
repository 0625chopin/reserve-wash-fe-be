-- 2차 Phase 1 — 도메인 스키마 DDL (H2 MODE=MySQL / Phase 10에서 MySQL 이행)
-- MyBatis는 ORM 자동 DDL이 없으므로 스키마를 SQL로 직접 관리한다(require_v1.md v1.5).
-- id 정책(Phase 0 확정): 마스터(users/store/bay/manager)·reservation·review = VARCHAR 문자열 무변환,
--   slot/manager_dayoff/store_holiday = 내부 BIGINT AUTO_INCREMENT surrogate(FE 미노출), price = 복합 PK.
-- 예약어 회피: `date`·`text` 컬럼은 백틱 인용(H2 MySQL 모드·MySQL 공통).
-- 멱등 재생성: H2 mem DB(jdbc:h2:mem:carwash;DB_CLOSE_DELAY=-1)는 JVM 전역 공유라
--   테스트에서 여러 Spring 컨텍스트가 spring.sql.init을 재실행하면 data.sql INSERT가 중복된다.
--   매 초기화 시 DROP 후 CREATE로 깨끗이 재생성(AUTO_INCREMENT도 리셋)하여 시드 중복을 방지한다.

DROP TABLE IF EXISTS store_holiday;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS price;
DROP TABLE IF EXISTS slot;
DROP TABLE IF EXISTS manager_dayoff;
DROP TABLE IF EXISTS manager;
DROP TABLE IF EXISTS bay;
DROP TABLE IF EXISTS store;
DROP TABLE IF EXISTS users;

-- 사용자 (require 3.1)
CREATE TABLE IF NOT EXISTS users (
    id             VARCHAR(64)  PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    name           VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,   -- UserRole enum 문자열
    password_hash  VARCHAR(100) NOT NULL    -- BCrypt 해시 (Phase 3 인증)
);

-- 매장 (require 5.1)
CREATE TABLE IF NOT EXISTS store (
    id         VARCHAR(64)  PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    bay_count  INT          NOT NULL,   -- 동일 시간대 최대 수용 = 베이 수 N (require 5.2)
    approved   BOOLEAN      NOT NULL    -- 승인된 매장만 노출 (require 6.1)
);

-- 베이 — 매장 내 개별 세차 라인 (require 5.1)
CREATE TABLE IF NOT EXISTS bay (
    id        VARCHAR(64) PRIMARY KEY,
    store_id  VARCHAR(64) NOT NULL,
    code      VARCHAR(10) NOT NULL,    -- 'A1' ~ 'AN' (매장 내 식별자)
    size      VARCHAR(10) NOT NULL     -- BaySize enum: SMALL/MID/LARGE/XLARGE (Phase 0 Q1)
);

-- 매니저 (require 3.1·6.1)
CREATE TABLE IF NOT EXISTS manager (
    id              VARCHAR(64)  PRIMARY KEY,
    store_id        VARCHAR(64)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    is_store_admin  BOOLEAN      NOT NULL   -- 매장 최고권한 매니저 여부
);

-- 매니저 휴무 = (매니저, 날짜, 휴무 유형) (require 5.4·5.5)
CREATE TABLE IF NOT EXISTS manager_dayoff (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,   -- 내부 surrogate
    manager_id   VARCHAR(64) NOT NULL,
    `date`       VARCHAR(10) NOT NULL,                -- 'YYYY-MM-DD'
    dayoff_type  VARCHAR(20) NOT NULL                 -- DayoffType enum: FULL_DAY/SHIFT_1~3
);

-- 슬롯 = (매장, 베이, 날짜, 30분 시간단위), 시스템 전체 UNIQUE (require 5.2·7.3 최종 방어선)
CREATE TABLE IF NOT EXISTS slot (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,     -- 내부 surrogate(FE 미노출)
    store_id   VARCHAR(64) NOT NULL,
    bay_id     VARCHAR(64) NOT NULL,
    `date`     VARCHAR(10) NOT NULL,                  -- 'YYYY-MM-DD'
    time_slot  VARCHAR(5)  NOT NULL,                  -- 'HH:mm' (30분 단위 시작 시각)
    status     VARCHAR(20) NOT NULL,                  -- SlotStatus enum
    version    BIGINT      NOT NULL DEFAULT 0,        -- 낙관적 락용 버전 (Phase 4)
    CONSTRAINT uk_slot_store_bay_date_time UNIQUE (store_id, bay_id, `date`, time_slot)
);

-- 가격 — 차종 × 서비스 단가 (require 10.3), (car_type, service_type) 복합 PK
CREATE TABLE IF NOT EXISTS price (
    car_type      VARCHAR(20) NOT NULL,   -- CarType enum
    service_type  VARCHAR(20) NOT NULL,   -- ServiceType enum
    amount        INT         NOT NULL,   -- 원 단위
    PRIMARY KEY (car_type, service_type)
);

-- 예약 (require 6장·11.3)
CREATE TABLE IF NOT EXISTS reservation (
    id            VARCHAR(64) PRIMARY KEY,
    user_id       VARCHAR(64) NOT NULL,
    store_id      VARCHAR(64) NOT NULL,
    bay_id        VARCHAR(64) NOT NULL,
    manager_id    VARCHAR(64),               -- nullable: 대행 예약이 아니면 NULL
    `date`        VARCHAR(10) NOT NULL,
    time_slot     VARCHAR(5)  NOT NULL,
    car_type      VARCHAR(20) NOT NULL,
    service_type  VARCHAR(20) NOT NULL,
    amount        INT         NOT NULL,
    status        VARCHAR(20) NOT NULL       -- ReservationStatus enum
);

-- 후기/평점 — 예약(세차) 완료 사용자만 작성 (require 9.1)
CREATE TABLE IF NOT EXISTS review (
    id              VARCHAR(64)  PRIMARY KEY,
    reservation_id  VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    store_id        VARCHAR(64)  NOT NULL,
    manager_id      VARCHAR(64),               -- nullable
    rating          INT          NOT NULL,     -- 1 ~ 5 정수
    `text`          VARCHAR(1000),
    created_at      VARCHAR(40)  NOT NULL       -- ISO 문자열
);

-- 매장 휴일 — 매니저 신청 → 관리자 승인 (require 5.4, Phase 7 결재 연계)
CREATE TABLE IF NOT EXISTS store_holiday (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,   -- 내부 surrogate
    store_id  VARCHAR(64) NOT NULL,
    `date`    VARCHAR(10) NOT NULL,
    approved  BOOLEAN     NOT NULL
);
