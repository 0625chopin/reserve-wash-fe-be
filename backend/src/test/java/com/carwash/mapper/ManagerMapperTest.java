package com.carwash.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carwash.domain.Manager;
import com.carwash.domain.ManagerDayoff;
import com.carwash.domain.enums.DayoffType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 매니저 매퍼 통합 테스트 — dayoffs <collection> 조립 검증
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ManagerMapperTest {

    @Autowired
    private ManagerMapper managerMapper;

    @Test
    void findByStore_강남점_매니저_2명_휴무_매핑() {
        List<Manager> managers = managerMapper.findByStore("store1");

        assertThat(managers).hasSize(2);   // mgr1, mgr2
        Manager mgr1 = managers.stream().filter(m -> m.getId().equals("mgr1")).findFirst().orElseThrow();
        Manager mgr2 = managers.stream().filter(m -> m.getId().equals("mgr2")).findFirst().orElseThrow();

        assertThat(mgr1.isStoreAdmin()).isTrue();
        assertThat(mgr1.getDayoffs()).hasSize(3);   // FULL_DAY/SHIFT_1/FULL_DAY
        // 휴무 없는 매니저는 phantom 원소 없이 빈 리스트 (notNullColumn)
        assertThat(mgr2.getDayoffs()).isEmpty();
    }

    @Test
    void findById_mgr1_휴무_상세() {
        Manager mgr1 = managerMapper.findById("mgr1");

        assertThat(mgr1.getDayoffs()).extracting(ManagerDayoff::getDate)
                .containsExactlyInAnyOrder("2026-06-22", "2026-06-23", "2026-06-29");
        assertThat(mgr1.getDayoffs()).extracting(ManagerDayoff::getType)
                .contains(DayoffType.FULL_DAY, DayoffType.SHIFT_1);
    }

    @Test
    void findById_mgr3_오후조_휴무() {
        Manager mgr3 = managerMapper.findById("mgr3");

        assertThat(mgr3.getDayoffs()).hasSize(1);
        assertThat(mgr3.getDayoffs().get(0).getType()).isEqualTo(DayoffType.SHIFT_2);
    }
}
