package com.carwash.service;

import com.carwash.dto.SlotStatusResponse;
import com.carwash.mapper.SlotMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 슬롯 점유 현황 조회 — 매장·날짜 단위 배치(희소). FE 그리드 하이드레이트용
@Service
public class SlotQueryService {

    private final SlotMapper slotMapper;

    public SlotQueryService(SlotMapper slotMapper) {
        this.slotMapper = slotMapper;
    }

    @Transactional(readOnly = true)
    public List<SlotStatusResponse> findByStoreAndDate(String storeId, String date) {
        return slotMapper.findByStoreAndDate(storeId, date).stream()
                .map(SlotStatusResponse::from)
                .toList();
    }
}
