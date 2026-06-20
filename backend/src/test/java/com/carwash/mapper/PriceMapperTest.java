package com.carwash.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carwash.domain.Price;
import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ServiceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 가격 매퍼 통합 테스트 — 매트릭스 20행 + 확정 단가 단정 (require 10.3)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PriceMapperTest {

    @Autowired
    private PriceMapper priceMapper;

    @Test
    void findAll_가격_20행() {
        assertThat(priceMapper.findAll()).hasSize(20);   // 차종 5 × 서비스 4
    }

    @Test
    void findOne_VAN_ETC_PREMIUM_55000() {
        Price price = priceMapper.findOne(CarType.VAN_ETC, ServiceType.PREMIUM);

        assertThat(price).isNotNull();
        assertThat(price.getAmount()).isEqualTo(55000);
    }

    @Test
    void findOne_LIGHT_EXT_10000() {
        assertThat(priceMapper.findOne(CarType.LIGHT, ServiceType.EXT).getAmount()).isEqualTo(10000);
    }
}
