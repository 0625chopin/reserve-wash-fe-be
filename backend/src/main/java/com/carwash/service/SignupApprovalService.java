package com.carwash.service;

import com.carwash.domain.User;
import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.dto.ManagerSignupResponse;
import com.carwash.mapper.UserMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 매니저 가입 2단계 승인 (require v1.7 §4.4) — 상태값 변경 방식.
//   M7 매장매니저관리자 1차: PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2.
//   S3 관리자 2차 최종:      PENDING_APPROVAL_L2 → ACTIVE(로그인 가능).
//   불가능한 단계 전이는 도메인 메서드가 IllegalStateException(→ GlobalExceptionHandler 409).
//   ⚠️ 가입=2단계, 휴가/반차=1단계(ApprovalService)로 단계 수가 다름.
@Service
public class SignupApprovalService {

    private final UserMapper userMapper;

    public SignupApprovalService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // 1차 승인 대기 목록(M7 검토용)
    @Transactional(readOnly = true)
    public List<ManagerSignupResponse> listPendingL1() {
        return userMapper.findByApprovalStatus(UserApprovalStatus.PENDING_APPROVAL_L1.name())
                .stream().map(ManagerSignupResponse::from).toList();
    }

    // 2차 승인 대기 목록(S3 검토용)
    @Transactional(readOnly = true)
    public List<ManagerSignupResponse> listPendingL2() {
        return userMapper.findByApprovalStatus(UserApprovalStatus.PENDING_APPROVAL_L2.name())
                .stream().map(ManagerSignupResponse::from).toList();
    }

    // M7 1차 승인 — PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2 (매장매니저관리자)
    @Transactional
    public void approveL1(String userId) {
        User user = loadUser(userId);
        user.approveSignupL1();
        userMapper.updateApprovalStatus(user.getId(), user.getApprovalStatus().name());
    }

    // S3 2차 최종 승인 — PENDING_APPROVAL_L2 → ACTIVE (관리자)
    @Transactional
    public void confirmL2(String userId) {
        User user = loadUser(userId);
        user.confirmSignupL2();
        userMapper.updateApprovalStatus(user.getId(), user.getApprovalStatus().name());
    }

    // 어느 단계든 반려 — → REJECTED
    @Transactional
    public void reject(String userId) {
        User user = loadUser(userId);
        user.rejectSignup();
        userMapper.updateApprovalStatus(user.getId(), user.getApprovalStatus().name());
    }

    private User loadUser(String userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "가입 신청 사용자를 찾을 수 없습니다.");
        }
        return user;
    }
}
