package com.carwash.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carwash.domain.Bay;
import com.carwash.domain.enums.BaySize;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 베이 매퍼 통합 테스트 — 매장별 베이 수·등급(XLARGE 신설 포함) 검증
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BayMapperTest {

    @Autowired
    private BayMapper bayMapper;

    @Test
    void findByStore_강남점_베이_4개_XLARGE_포함() {
        List<Bay> bays = bayMapper.findByStore("store1");

        assertThat(bays).hasSize(4);   // A1~A4 (A4=XLARGE 신설)
        assertThat(bays).extracting(Bay::getSize)
                .containsExactlyInAnyOrder(BaySize.SMALL, BaySize.MID, BaySize.LARGE, BaySize.XLARGE);
    }

    @Test
    void findByStore_홍대점_베이_2개() {
        assertThat(bayMapper.findByStore("store2")).hasSize(2);
    }

    @Test
    void findAll_전체_베이_25개_XLARGE_포함() {
        List<Bay> all = bayMapper.findAll();

        assertThat(all).hasSize(25);   // 원본 9(store1~3) + 확장 볼륨 시드 16(store4~8)
        assertThat(all).extracting(Bay::getSize).contains(BaySize.XLARGE);
    }

    @Test
    void findById_특대형_베이_등급_매핑() {
        Bay xlarge = bayMapper.findById("store1-A4");

        assertThat(xlarge).isNotNull();
        assertThat(xlarge.getStoreId()).isEqualTo("store1");
        assertThat(xlarge.getCode()).isEqualTo("A4");
        assertThat(xlarge.getSize()).isEqualTo(BaySize.XLARGE);   // VAN_ETC 수용
    }
}
