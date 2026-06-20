package com.carwash.dto;

import com.carwash.domain.Reservation;
import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ReservationStatus;
import com.carwash.domain.enums.ServiceType;

// 예약 응답 DTO — FE Reservation과 무변환 일치
public record ReservationResponse(
        String id,
        String userId,
        String storeId,
        String bayId,
        String managerId,
        String date,
        String timeSlot,
        CarType carType,
        ServiceType serviceType,
        int amount,
        ReservationStatus status) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getUserId(), r.getStoreId(), r.getBayId(), r.getManagerId(),
                r.getDate(), r.getTimeSlot(), r.getCarType(), r.getServiceType(),
                r.getAmount(), r.getStatus());
    }
}
