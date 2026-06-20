package com.carwash.dto;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserRole;

// 사용자 응답 DTO — FE User와 무변환 일치. passwordHash는 절대 포함하지 않는다.
//   storeId: 매니저 계열 소속 매장(USER/ADMIN은 null) — FE BO 매장 고정용
public record UserResponse(String id, String email, String name, UserRole role, String storeId) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole(), u.getStoreId());
    }
}
