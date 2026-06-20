package com.carwash.mapper;

import com.carwash.domain.Price;
import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ServiceType;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 가격 매퍼 — 차종 × 서비스 단가 (require 10.3)
@Mapper
public interface PriceMapper {

    List<Price> findAll();

    Price findOne(@Param("carType") CarType carType, @Param("serviceType") ServiceType serviceType);
}
