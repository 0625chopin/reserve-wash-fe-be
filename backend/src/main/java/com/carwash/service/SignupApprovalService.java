package com.carwash.service;

import com.carwash.domain.Manager;
import com.carwash.domain.User;
import com.carwash.domain.enums.UserApprovalStatus;
import com.carwash.domain.enums.UserRole;
import com.carwash.dto.ManagerSignupResponse;
import com.carwash.mapper.ManagerMapper;
import com.carwash.mapper.UserMapper;
import java.util.List;
import java.util.UUID;
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
    private final ManagerMapper managerMapper;
    private final NotificationService notificationService;

    public SignupApprovalService(
            UserMapper userMapper,
            ManagerMapper managerMapper,
            NotificationService notificationService) {
        this.userMapper = userMapper;
        this.managerMapper = managerMapper;
        this.notificationService = notificationService;
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
        notificationService.notifySignupApprovalResult(user, "1차 승인");   // 결재 결과 통지(Phase 9)
    }

    // S3 2차 최종 승인 — PENDING_APPROVAL_L2 → ACTIVE (관리자)
    @Transactional
    public void confirmL2(String userId) {
        User user = loadUser(userId);
        user.confirmSignupL2();
        userMapper.updateApprovalStatus(user.getId(), user.getApprovalStatus().name());
        activateManagerEntity(user);   // 예약 배정 대상 manager 엔티티 생성·연결(v2.4)
        notificationService.notifySignupApprovalResult(user, "최종 승인");   // 결재 결과 통지(Phase 9)
    }

    // 매니저 최종 승인 시 manager 엔티티 생성 + users.manager_id 연결 (v2.4)
    //   일반매니저(MANAGER)만 예약 배정 대상 엔티티를 만든다. 매장관리매니저(STORE_ADMIN)는
    //   매장 단위로 관리하므로(시드 storeadmin↔manager_id NULL 관례) 엔티티를 만들지 않는다.
    //   이미 연결돼 있으면(중복 승인 등) 재생성하지 않는다.
    private void activateManagerEntity(User user) {
        if (user.getRole() != UserRole.MANAGER) return;
        if (user.getManagerId() != null) return;
        if (user.getStoreId() == null) return;   // 소속 매장 없으면 배정 대상 엔티티 생성 불가
        String managerId = "mgr-" + UUID.randomUUID().toString().substring(0, 8);
        managerMapper.insert(Manager.builder()
                .id(managerId)
                .storeId(user.getStoreId())
                .name(user.getName())
                .isStoreAdmin(false)
                .build());
        userMapper.updateManagerId(user.getId(), managerId);
    }

    // 어느 단계든 반려 — → REJECTED
    @Transactional
    public void reject(String userId) {
        User user = loadUser(userId);
        user.rejectSignup();
        userMapper.updateApprovalStatus(user.getId(), user.getApprovalStatus().name());
        notificationService.notifySignupApprovalResult(user, "반려");   // 결재 결과 통지(Phase 9)
    }

    private User loadUser(String userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "가입 신청 사용자를 찾을 수 없습니다.");
        }
        return user;
    }
}
