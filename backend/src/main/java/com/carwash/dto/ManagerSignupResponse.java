package com.carwash.dto;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.domain.enums.UserRole;

// 매니저 가입 승인 응답 (M7 1차·S3 2차) — passwordHash는 절대 포함하지 않는다.
//   storeId: 소속 매장(상세 보기용, require v1.13). 매니저 계열은 항상 보유.
public record ManagerSignupResponse(
        String id,
        String email,
        String name,
        UserRole role,
        UserApprovalStatus approvalStatus,
        String storeId) {

    public static ManagerSignupResponse from(User u) {
        return new ManagerSignupResponse(
                u.getId(), u.getEmail(), u.getName(), u.getRole(), u.getApprovalStatus(),
                u.getStoreId());
    }
}
