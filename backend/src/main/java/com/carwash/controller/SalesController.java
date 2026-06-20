package com.carwash.controller;

import com.carwash.dto.SalesResponse;
import com.carwash.service.SalesService;
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
}
