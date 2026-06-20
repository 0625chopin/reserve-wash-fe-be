package com.carwash.controller;

import com.carwash.dto.StoreResponse;
import com.carwash.service.StoreService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 매장 조회 — 승인 매장만 (require 6.1). DTO만 노출
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreResponse> stores() {
        return storeService.findApprovedStores();
    }
}
