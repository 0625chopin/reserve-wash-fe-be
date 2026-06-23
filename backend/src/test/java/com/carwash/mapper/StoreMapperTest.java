package com.carwash.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carwash.domain.Store;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 매장 매퍼 통합 테스트 — 시드(data.sql)가 로드된 H2에서 조회 검증
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StoreMapperTest {

    @Autowired
    private StoreMapper storeMapper;

    @Test
    void findApproved_전_매장_승인_완료() {
        // 전 매장 approved=true → 8개 모두 노출 (require 6.1)
        List<Store> approved = storeMapper.findApproved();

        assertThat(approved).hasSize(8);
        assertThat(approved).allMatch(Store::isApproved);
        assertThat(approved).extracting(Store::getId)
                .containsExactlyInAnyOrder(
                        "store1", "store2", "store3", "store4", "store5", "store6", "store7", "store8");
    }

    @Test
    void findAll_전체_8매장() {
        // 원본 3 + 확장 볼륨 시드 5(store4~store8)
        assertThat(storeMapper.findAll()).hasSize(8);
    }

    @Test
    void findById_강남점_정보_매핑() {
        Store store = storeMapper.findById("store1");

        assertThat(store).isNotNull();
        assertThat(store.getName()).isEqualTo("강남점");
        assertThat(store.getBayCount()).isEqualTo(4);   // XLARGE 베이 추가로 3→4
        assertThat(store.isApproved()).isTrue();
    }
}
