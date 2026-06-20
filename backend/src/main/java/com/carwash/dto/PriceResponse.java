package com.carwash.dto;

import com.carwash.domain.Price;
import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ServiceType;

// 가격 응답 DTO — FE Price와 무변환 일치. carType/serviceType enum은 name 직렬화
public record PriceResponse(CarType carType, ServiceType serviceType, int amount) {

    public static PriceResponse from(Price p) {
        return new PriceResponse(p.getCarType(), p.getServiceType(), p.getAmount());
    }
}
