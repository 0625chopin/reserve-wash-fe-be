package com.carwash.dto;

import com.carwash.domain.Reservation;
import com.carwash.domain.User;
import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ReservationStatus;
import com.carwash.domain.enums.ServiceType;

// 관리자 매장별 예약자 관리 응답 (S4, require 11.1) — 예약 + 고객 정보 평면화
public record AdminReservationResponse(
        String id,
        String userId,
        String userName,
        String userEmail,
        String storeId,
        String bayId,
        String managerId,
        String date,
        String timeSlot,
        CarType carType,
        ServiceType serviceType,
        int amount,
        ReservationStatus status) {

    // 고객(user)이 조회되지 않으면 이름/이메일은 빈 값으로 둔다(방어).
    public static AdminReservationResponse from(Reservation r, User u) {
        return new AdminReservationResponse(
                r.getId(), r.getUserId(),
                u != null ? u.getName() : "",
                u != null ? u.getEmail() : "",
                r.getStoreId(), r.getBayId(), r.getManagerId(),
                r.getDate(), r.getTimeSlot(), r.getCarType(), r.getServiceType(),
                r.getAmount(), r.getStatus());
    }
}
