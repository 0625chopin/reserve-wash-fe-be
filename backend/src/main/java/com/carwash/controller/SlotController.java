package com.carwash.controller;

import com.carwash.dto.SlotStatusResponse;
import com.carwash.service.SlotQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 슬롯 점유 현황 조회 — 점유 정보는 공개(SecurityConfig permitAll). 그리드 표시용
@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotQueryService slotQueryService;

    public SlotController(SlotQueryService slotQueryService) {
        this.slotQueryService = slotQueryService;
    }

    @GetMapping
    public List<SlotStatusResponse> slots(
            @RequestParam String storeId, @RequestParam String date) {
        return slotQueryService.findByStoreAndDate(storeId, date);
    }
}
