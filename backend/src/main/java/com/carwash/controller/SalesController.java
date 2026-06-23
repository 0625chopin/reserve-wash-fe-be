package com.carwash.controller;

import com.carwash.dto.SalesByStoreResponse;
import com.carwash.dto.SalesResponse;
import com.carwash.service.SalesService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 매장별 매출 집계 (S8, require 11.1) — /api/admin/** 는 ADMIN 한정.
@RestController
@RequestMapping("/api/admin")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/stores/{id}/sales")
    public SalesResponse sales(@PathVariable String id) {
        return salesService.storeSales(id);
    }

    // 전 매장 매출 비중 집계 (v2.4) — 원차트(Top5+ETC)용. 매장별 COMPLETED 금액 내림차순.
    @GetMapping("/sales/by-store")
    public List<SalesByStoreResponse> salesByStore() {
        return salesService.salesByStore();
    }
}
