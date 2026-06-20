package com.carwash.service;

import com.carwash.dto.ManagerResponse;
import com.carwash.mapper.ManagerMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 매니저 조회 서비스 — 전체(dayoffs 포함, 카탈로그 캐시용)
@Service
public class ManagerService {

    private final ManagerMapper managerMapper;

    public ManagerService(ManagerMapper managerMapper) {
        this.managerMapper = managerMapper;
    }

    @Transactional(readOnly = true)
    public List<ManagerResponse> findAll() {
        return managerMapper.findAll().stream().map(ManagerResponse::from).toList();
    }
}
