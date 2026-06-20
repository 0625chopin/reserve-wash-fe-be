package com.carwash.service;

import com.carwash.domain.Manager;
import com.carwash.domain.ManagerDayoff;
import com.carwash.domain.User;
import com.carwash.domain.enums.DayoffType;
import com.carwash.dto.ConfirmRequest;
import com.carwash.dto.ProxyReservationRequest;
import com.carwash.dto.ReservationResponse;
import com.carwash.mapper.ManagerMapper;
import com.carwash.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// 매니저 대행 예약 (M3, require 6.2·3.2) — 인가는 경로 기반(MANAGER/STORE_ADMIN).
//   고객 이메일 해석·매니저 소속 매장·본인 휴무 검증 후 Phase 4 confirm 경로로 위임(동시성 동일).
@Service
public class ManagerReservationService {

    private final UserMapper userMapper;
    private final ManagerMapper managerMapper;
    private final ReservationService reservationService;

    public ManagerReservationService(
            UserMapper userMapper, ManagerMapper managerMapper, ReservationService reservationService) {
        this.userMapper = userMapper;
        this.managerMapper = managerMapper;
        this.reservationService = reservationService;
    }

    // 대행 예약 — 검증 통과 시 confirm 위임(슬롯 INSERT·UNIQUE·낙관락 동일). 충돌 시 409는 confirm이 던짐.
    public ReservationResponse proxyReserve(ProxyReservationRequest req) {
        User customer = userMapper.findByEmail(req.customerEmail());
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "고객을 찾을 수 없습니다.");
        }
        Manager manager = managerMapper.findById(req.managerId());
        if (manager == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매니저를 찾을 수 없습니다.");
        }
        if (!manager.getStoreId().equals(req.storeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매니저 소속 매장이 아닙니다.");
        }
        if (isManagerOffAt(manager, req.date(), req.timeSlot())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매니저 휴무 시간대입니다.");
        }
        // 대행: 예약 userId=고객, managerId=대행 매니저 — confirm 경로 재사용
        ConfirmRequest cr = new ConfirmRequest(
                req.storeId(), req.bayId(), req.date(), req.timeSlot(),
                req.managerId(), req.carType(), req.serviceType(), req.amount());
        return reservationService.confirm(customer.getId(), cr);
    }

    // 매니저가 (날짜, 시간)에 휴무인지 — 전일이면 항상, 교대조면 해당 시간대만 (require 5.5)
    //   FE storeService.isManagerOffAt와 동일 경계(SHIFT_1 06:00~14:00 / SHIFT_2 14:00~22:00 / SHIFT_3 22:00~06:00).
    private boolean isManagerOffAt(Manager manager, String date, String timeSlot) {
        if (manager.getDayoffs() == null) {
            return false;
        }
        for (ManagerDayoff d : manager.getDayoffs()) {
            if (!date.equals(d.getDate())) {
                continue;
            }
            if (d.getType() == DayoffType.FULL_DAY || isInShift(d.getType(), timeSlot)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInShift(DayoffType type, String timeSlot) {
        int t = toMinutes(timeSlot);
        return switch (type) {
            case SHIFT_1 -> t >= 360 && t < 840;     // 06:00~14:00
            case SHIFT_2 -> t >= 840 && t < 1320;    // 14:00~22:00
            case SHIFT_3 -> t >= 1320 || t < 360;    // 22:00~06:00(익일)
            default -> false;
        };
    }

    private int toMinutes(String timeSlot) {
        String[] parts = timeSlot.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
