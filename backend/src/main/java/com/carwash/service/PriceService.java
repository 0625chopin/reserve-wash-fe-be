package com.carwash.service;

import com.carwash.dto.PriceResponse;
import com.carwash.mapper.PriceMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 가격 조회 서비스 — 전체 매트릭스(카탈로그 캐시용, require 10.3)
@Service
public class PriceService {

    private final PriceMapper priceMapper;

    public PriceService(PriceMapper priceMapper) {
        this.priceMapper = priceMapper;
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> findAll() {
        return priceMapper.findAll().stream().map(PriceResponse::from).toList();
    }
}
