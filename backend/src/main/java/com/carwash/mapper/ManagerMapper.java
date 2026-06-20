package com.carwash.mapper;

import com.carwash.domain.Manager;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 매니저 매퍼 — dayoffs는 <collection> 단일 조인으로 조립(N+1 회피)
@Mapper
public interface ManagerMapper {

    List<Manager> findByStore(String storeId);

    Manager findById(String id);
}
