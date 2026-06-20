package com.carwash.mapper;

import com.carwash.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 사용자 매퍼 — 로그인 조회는 Phase 3에서 확장
@Mapper
public interface UserMapper {

    List<User> findAll();

    User findById(String id);

    User findByEmail(String email);
}
