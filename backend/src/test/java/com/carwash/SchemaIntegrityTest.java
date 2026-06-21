package com.carwash;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// Phase 1 DoD 게이트 — H2(schema.sql/data.sql 로드)에서 스키마 무결성 단정
//   ROADMAP_2 466~469: 도메인 10테이블 + slot UNIQUE 제약 + price 20행.
//   Phase 9에서 알림 발송 이력 테이블(notification_log)이 가산되어 총 11테이블.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SchemaIntegrityTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 전체_테이블_11종이_생성된다() {
        // 도메인 10테이블(Phase 1) + notification_log(Phase 9 알림 이력) = 11
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                Integer.class);
        assertThat(count).isEqualTo(11);
    }

    @Test
    void slot_UNIQUE_제약이_최종방어선으로_존재한다() {
        // require 5.2·7.3 — uk_slot_store_bay_date_time UNIQUE(store_id,bay_id,date,time_slot)
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                        + "WHERE CONSTRAINT_NAME = 'UK_SLOT_STORE_BAY_DATE_TIME' "
                        + "AND CONSTRAINT_TYPE = 'UNIQUE'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void price_매트릭스가_20행_시드된다() {
        // require 10.3 — 차종 5 × 서비스 4
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM price", Integer.class)).isEqualTo(20);
    }
}
