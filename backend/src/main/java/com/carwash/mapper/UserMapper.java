package com.carwash.mapper;

import com.carwash.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 사용자 매퍼 — 로그인 조회는 Phase 3에서 확장, 가입 승인 상태 조회/갱신은 require v1.7 §4.4
@Mapper
public interface UserMapper {

    List<User> findAll();

    User findById(String id);

    // 로그인 인증용 — password_hash 포함 조회 (Phase 3)
    User findByEmail(String email);

    // 회원가입 영속 (Phase 3)
    int insert(User user);

    // 가입 승인 상태 갱신 (M7/S3, require v1.7 §4.4)
    int updateApprovalStatus(@Param("id") String id, @Param("approvalStatus") String approvalStatus);

    // 매니저 가입 최종 승인 시 manager 엔티티 연결(v2.4) — users.manager_id 세팅
    int updateManagerId(@Param("id") String id, @Param("managerId") String managerId);

    // 가입 승인 대기 목록 조회(1차 PENDING_APPROVAL_L1 / 2차 PENDING_APPROVAL_L2)
    List<User> findByApprovalStatus(@Param("approvalStatus") String approvalStatus);

    // 매니저 엔티티 id로 로그인 계정 조회 (Phase 9) — 휴가/반차 결재 수신자 해석.
    //   users.manager_id FK(require v1.10 §6.6) 재사용. 미연결(NULL)이면 결과 없음 → 발송 skip.
    User findByManagerId(@Param("managerId") String managerId);
}
