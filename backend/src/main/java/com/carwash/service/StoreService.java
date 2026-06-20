package com.carwash.service;

import com.carwash.dto.StoreResponse;
import com.carwash.mapper.StoreMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 매장 조회 서비스 — 승인 매장만 (require 6.1)
@Service
public class StoreService {

    private final StoreMapper storeMapper;

    public StoreService(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> findApprovedStores() {
        return storeMapper.findApproved().stream().map(StoreResponse::from).toList();
    }
}
