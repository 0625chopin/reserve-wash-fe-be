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
    void findApproved_미승인_매장은_제외한다() {
        // store3(판교점)은 approved=false → 노출 제외 (require 6.1)
        List<Store> approved = storeMapper.findApproved();

        assertThat(approved).hasSize(2);
        assertThat(approved).allMatch(Store::isApproved);
        assertThat(approved).extracting(Store::getId).containsExactlyInAnyOrder("store1", "store2");
    }

    @Test
    void findAll_전체_3매장() {
        assertThat(storeMapper.findAll()).hasSize(3);
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
