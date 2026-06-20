package com.carwash.dto;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;

// 사용자 응답 DTO — FE User와 무변환 일치. passwordHash는 절대 포함하지 않는다.
public record UserResponse(String id, String email, String name, UserRole role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole());
    }
}
