package com.carwash.dto;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;

// 관리자 매장별 사용자 관리 응답 (S5, require 11.1) — passwordHash 미포함
public record AdminUserResponse(
        String id,
        String email,
        String name,
        UserRole role) {

    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole());
    }
}
