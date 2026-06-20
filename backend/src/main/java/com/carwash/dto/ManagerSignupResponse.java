package com.carwash.dto;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.domain.enums.UserRole;

// 매니저 가입 승인 응답 (M7 1차·S3 2차) — passwordHash는 절대 포함하지 않는다.
public record ManagerSignupResponse(
        String id,
        String email,
        String name,
        UserRole role,
        UserApprovalStatus approvalStatus) {

    public static ManagerSignupResponse from(User u) {
        return new ManagerSignupResponse(
                u.getId(), u.getEmail(), u.getName(), u.getRole(), u.getApprovalStatus());
    }
}
