package com.carwash.mapper;

import com.carwash.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 사용자 매퍼 — 로그인 조회는 Phase 3에서 확장
@Mapper
public interface UserMapper {

    List<User> findAll();

    User findById(String id);

    // 로그인 인증용 — password_hash 포함 조회 (Phase 3)
    User findByEmail(String email);

    // 회원가입 영속 (Phase 3)
    int insert(User user);
}
