package com.carwash.mapper;

import com.carwash.domain.Store;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 매장 매퍼 — SQL은 resources/mapper/StoreMapper.xml (namespace = 본 인터페이스 FQN)
@Mapper
public interface StoreMapper {

    List<Store> findAll();

    // 승인된 매장만 (require 6.1)
    List<Store> findApproved();

    Store findById(String id);
}
