package com.carwash.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carwash.domain.Slot;
import com.carwash.domain.enums.SlotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 슬롯 매퍼 통합 테스트 — 시드 RESERVED 슬롯 조회 + 낙관적 락 영향 행 수(Phase 4 선검증)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SlotMapperTest {

    @Autowired
    private SlotMapper slotMapper;

    @Test
    void findByKey_와_낙관락_갱신_시퀀스() {
        // 1) 시드 슬롯 조회 — 강남점 A1 2026-06-25 10:00 = RESERVED, version 0
        Slot slot = slotMapper.findByKey("store1", "store1-A1", "2026-06-25", "10:00");

        assertThat(slot).isNotNull();
        assertThat(slot.getId()).isNotNull();          // 내부 surrogate PK
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.RESERVED);
        assertThat(slot.getVersion()).isEqualTo(0L);

        // 2) 낙관적 락: 현재 version으로 갱신 → 1행 영향(성공)
        int updated = slotMapper.updateStatusWithVersion(slot.getId(), "COMPLETED", 0L);
        assertThat(updated).isEqualTo(1);

        Slot after = slotMapper.findByKey("store1", "store1-A1", "2026-06-25", "10:00");
        assertThat(after.getStatus()).isEqualTo(SlotStatus.COMPLETED);
        assertThat(after.getVersion()).isEqualTo(1L);   // version + 1

        // 3) 과거 version(0)으로 재시도 → 0행 영향(충돌 판정, Phase 4에서 409 매핑)
        int conflict = slotMapper.updateStatusWithVersion(slot.getId(), "RESERVED", 0L);
        assertThat(conflict).isEqualTo(0);
    }
}
