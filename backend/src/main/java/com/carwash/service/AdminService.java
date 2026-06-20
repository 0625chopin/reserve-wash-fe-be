package com.carwash.service;

import com.carwash.domain.Reservation;
import com.carwash.domain.User;
import com.carwash.dto.AdminReservationResponse;
import com.carwash.dto.AdminUserResponse;
import com.carwash.mapper.ReservationMapper;
import com.carwash.mapper.UserMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 매장 관리 (S4·S5, require 11.1·3.2) — 인가는 경로 기반(ADMIN).
//   매장별 예약자(S4)·사용자(S5)를 조회한다. 사용자 정보는 findAll 1회 맵으로 조인(N+1 회피).
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;

    public AdminService(ReservationMapper reservationMapper, UserMapper userMapper) {
        this.reservationMapper = reservationMapper;
        this.userMapper = userMapper;
    }

    // S4 — 매장별 예약자 관리: 예약 목록 + 고객 이름/이메일 평면화
    public List<AdminReservationResponse> storeReservations(String storeId) {
        Map<String, User> users = userMap();
        return reservationMapper.findByStore(storeId).stream()
                .map(r -> AdminReservationResponse.from(r, users.get(r.getUserId())))
                .toList();
    }

    // S5 — 매장별 사용자 관리: 해당 매장에 예약 이력이 있는 고객을 중복 없이(distinct, 최초 등장 순)
    public List<AdminUserResponse> storeUsers(String storeId) {
        Map<String, User> users = userMap();
        Map<String, AdminUserResponse> distinct = new LinkedHashMap<>();
        for (Reservation r : reservationMapper.findByStore(storeId)) {
            User u = users.get(r.getUserId());
            if (u != null) {
                distinct.putIfAbsent(u.getId(), AdminUserResponse.from(u));
            }
        }
        return List.copyOf(distinct.values());
    }

    private Map<String, User> userMap() {
        return userMapper.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
