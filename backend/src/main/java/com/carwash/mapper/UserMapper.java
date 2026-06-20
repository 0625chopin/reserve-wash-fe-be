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

    // 가입 승인 대기 목록 조회(1차 PENDING_APPROVAL_L1 / 2차 PENDING_APPROVAL_L2)
    List<User> findByApprovalStatus(@Param("approvalStatus") String approvalStatus);
}
